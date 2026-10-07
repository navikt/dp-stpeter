package no.dagpenger.stpeter.plugin

import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.dagpenger.oauth2.CachedOauth2Client
import no.nav.dagpenger.oauth2.OAuth2Config
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

private const val MAX_ORIGIN_ROUTE_LENGTH = 200

private val ROUTE_SEGMENT_PATTERN = Regex("[A-Za-z][A-Za-z._~-]*|v[0-9]{1,2}|\\{[A-Za-z][A-Za-z0-9_]*}")

class StPeterPlugin(
    private val config: StPeterConfig = StPeterConfig(),
) {
    private val client: HttpClient =
        HttpClient
            .newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build()

    private val azureAdClient: CachedOauth2Client by lazy {
        val azureAdConfig = OAuth2Config.AzureAd(config.toMap())
        CachedOauth2Client(
            tokenEndpointUrl = azureAdConfig.tokenEndpointUrl,
            authType = azureAdConfig.clientSecret(),
        )
    }

    val oboExchanger: (String) -> String by lazy {
        { token: String ->
            val accessToken =
                azureAdClient
                    .onBehalfOf(token, config.scope)
                    .access_token
            requireNotNull(accessToken) { "Failed to get access token" }
            accessToken
        }
    }

    private val url by lazy { config.url }

    suspend fun vedTilgangTilPerson(
        ident: String,
        token: String,
        /**
         *  Rutemal hos klienten som startet oppslaget. Bruk {parameter} for dynamiske segmenter.
         *  Ikke ta med query-parametere eller konkrete identifikatorer.
         */
        originRoute: String? = null,
        vedTilgangBlock: suspend () -> Unit,
    ) {
        vedTilgangTilPerson(ident, token, oppslagslogg = true, originRoute = originRoute, vedTilgangBlock)
    }

    suspend fun vedTilgangTilPersonUtenOppslagslogg(
        ident: String,
        token: String,
        /**
         *  Rutemal hos klienten som startet oppslaget. Bruk {parameter} for dynamiske segmenter.
         *  Ikke ta med query-parametere eller konkrete identifikatorer.
         */
        originRoute: String? = null,
        vedTilgangBlock: suspend () -> Unit,
    ) {
        vedTilgangTilPerson(ident, token, oppslagslogg = false, originRoute = originRoute, vedTilgangBlock)
    }

    private suspend fun vedTilgangTilPerson(
        ident: String,
        token: String,
        oppslagslogg: Boolean,
        originRoute: String? = null,
        vedTilgangBlock: suspend () -> Unit,
    ) {
        requireValidOriginRoute(originRoute)

        try {
            val oboToken = oboExchanger(token)
            val identForesporsel =
                mapOf(
                    "ident" to ident,
                    "oppslagslogg" to oppslagslogg,
                    "application" to config.appName,
                    "originRoute" to originRoute,
                )
            val request =
                HttpRequest
                    .newBuilder()
                    .uri(URI.create("$url/api/v1/person"))
                    .header("Authorization", "Bearer $oboToken")
                    .header("Accept", "application/problem+json")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(identForesporsel)))
                    .build()

            val response =
                withContext(Dispatchers.IO) {
                    client.send(request, HttpResponse.BodyHandlers.ofString())
                }

            when (response.statusCode()) {
                HttpStatusCode.Forbidden.value -> {
                    throw response.body().tilgangAvvist()
                }

                HttpStatusCode.NoContent.value -> {
                    vedTilgangBlock()
                }

                HttpStatusCode.NotFound.value -> {
                    throw response.body().tilgangAvvist()
                }

                else -> {
                    throw TilgangAvvistException(
                        status = response.statusCode().let { HttpStatusCode.fromValue(it) },
                        type = URI("urn:error:unknown"),
                        detail = "Uventet svar fra stpeter: status=${response.statusCode()}, body=${response.body()}",
                        instance = URI("$url/api/v1/person"),
                        title = "En ukjent feil oppstod ved evaluering av tilgang",
                    )
                }
            }
        } catch (e: TilgangAvvistException) {
            throw e
        } catch (e: RuntimeException) {
            throw TilgangAvvistException(
                status = HttpStatusCode.InternalServerError,
                type = URI("urn:error:unknown"),
                detail = "Ukjent feil ved kontakt med StPeter: ${e.message}",
                instance = URI("$url/api/v1/person"),
                title = "En ukjent feil oppstod ved evaluering av tilgang",
            )
        }
    }

    private fun requireValidOriginRoute(originRoute: String?) {
        if (originRoute == null) return

        require(
            originRoute.length in 2..MAX_ORIGIN_ROUTE_LENGTH &&
                originRoute.startsWith("/") &&
                originRoute
                    .removePrefix("/")
                    .split("/")
                    .all { segment ->
                        ROUTE_SEGMENT_PATTERN.matches(segment) &&
                            segment != "." &&
                            segment != ".."
                    },
        ) {
            "originRoute må være en rutemal uten query-parametere eller konkrete identifikatorer"
        }
    }

    private fun String.tilgangAvvist(): TilgangAvvistException {
        val problem = objectMapper.readValue(this, StPeterProblem::class.java)
        return TilgangAvvistException(
            title = problem.title,
            status = HttpStatusCode.fromValue(problem.status),
            type = URI(problem.type),
            detail = problem.detail,
            instance = URI(problem.instance),
            navIdent = problem.navIdent,
            traceId = problem.traceId,
            kanOverstyres = problem.kanOverstyres,
        )
    }
}
