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
        vedTilgangBlock: suspend () -> Unit,
    ) {
        val oboToken = oboExchanger(token)
        val request =
            HttpRequest
                .newBuilder()
                .uri(URI.create("$url/api/v1/person"))
                .header("Authorization", "Bearer $oboToken")
                .header("Accept", "application/problem+json")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(mapOf("ident" to ident))))
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
                    status = HttpStatusCode.Forbidden,
                    type = URI("urn:error:forbidden"),
                    detail = "Uventet svar fra stpeter: status=${response.statusCode()}, body=${response.body()}",
                    instance = URI("$url/api/v1/person"),
                    title = "En ukjent feil oppstod ved evaluering av tilgang",
                )
            }
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
