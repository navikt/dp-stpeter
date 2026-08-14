package no.nav.dagpenger.tilgangsmaskin

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.BadRequestException
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.Ident
import no.nav.dagpenger.Ident.Companion.tilPersonIdentfikator
import no.nav.dagpenger.oidc.OidcToken

class TilgangsmaskinClient(
    val tilgangsMaskinApiUrl: String,
    val tokenProvider: suspend (String) -> String,
    val httpClient: HttpClient,
    val cache: TilgangsmaskinCache,
) : TilgangsmaskinClientInterface {
    companion object {
        private val sikkerlogg = KotlinLogging.logger("tjenestekall")
    }

    override fun harTilgangTilPersonKomplett(
        ident: String,
        token: OidcToken,
    ): TilgangsmaskinResponse = request(ident.tilPersonIdentfikator(), token, "komplett")

    override fun harTilgangTilPersonKjerne(
        ident: String,
        token: OidcToken,
    ): TilgangsmaskinResponse = request(ident.tilPersonIdentfikator(), token, "kjerne")

    private fun request(
        ident: Ident,
        token: OidcToken,
        endpoint: String,
    ): TilgangsmaskinResponse =
        runBlocking {
            runCatching {
                cache
                    .get(
                        token = token,
                        ident = ident.identifikator(),
                    )?.let {
                        return@runBlocking it
                    }

                val response = sjekkTilgang(endpoint, token, ident.identifikator()).toTilgangsmaskinResponse(ident)

                cache.set(
                    token = token,
                    ident = ident.identifikator(),
                    value = response,
                )

                return@runBlocking response
            }.onFailure {
                throw BadRequestException("Feil ved kall til tilgangsmaskin for ident $ident", it)
            }.getOrThrow()
        }

    private suspend fun sjekkTilgang(
        endpoint: String,
        token: OidcToken,
        ident: String,
    ): HttpResponse =
        httpClient
            .post("$tilgangsMaskinApiUrl/api/v1/$endpoint") {
                val oboToken = tokenProvider.invoke(token.token())
                header(HttpHeaders.Authorization, "Bearer $oboToken")
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                accept(ContentType.Application.ProblemJson)
                accept(ContentType.Application.Json)
                accept(ContentType.Text.Plain)
                setBody(ident)
            }

    private suspend fun HttpResponse.toTilgangsmaskinResponse(ident: Ident): TilgangsmaskinResponse =
        when (status) {
            HttpStatusCode.Forbidden -> {
                val body = body<TilgangsmaskinResponse.TilgangAvvist>()
                sikkerlogg.info { "Status:$status Begrunnelse ${body.title}" }
                body
            }

            HttpStatusCode.NoContent -> {
                sikkerlogg.info {
                    "Status:$status Tilgang godkjent for ident $ident"
                }
                TilgangsmaskinResponse.TilgangGodkjent()
            }

            HttpStatusCode.NotFound -> {
                val body = body<TilgangsmaskinResponse.NavIdentIkkeFunnet>()
                sikkerlogg.info { "Status:$status Begrunnelse ${body.detail}" }
                body
            }

            else -> {
                throw RuntimeException("Feil ved kall til tilgangsmaskin for ident $ident, status: $status")
            }
        }
}
