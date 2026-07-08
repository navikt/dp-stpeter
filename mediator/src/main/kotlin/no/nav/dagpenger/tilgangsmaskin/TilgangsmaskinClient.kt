package no.nav.dagpenger.tilgangsmaskin

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.BadRequestException
import kotlinx.coroutines.runBlocking

class TilgangsmaskinClient(
    val tilgangsMaskinApiUrl: String,
    val tokenProvider: suspend (String) -> String,
    val httpClient: HttpClient,
) : TilgangsmaskinClientInterface {
    override fun harTilgangTilPersonKomplett(
        ident: String,
        token: String,
    ): TilgangsmaskinResponse = request(ident, token, "komplett")

    override fun harTilgangTilPersonKjerne(
        ident: String,
        token: String,
    ): TilgangsmaskinResponse = request(ident, token, "kjerne")

    private fun request(
        ident: String,
        token: String,
        endpoint: String,
    ): TilgangsmaskinResponse =
        runBlocking {
            runCatching {
                val post =
                    httpClient
                        .post("$tilgangsMaskinApiUrl/api/v1/$endpoint") {
                            val oboToken = tokenProvider.invoke(token)
                            header(HttpHeaders.Authorization, "Bearer $oboToken")
                            header(HttpHeaders.ContentType, ContentType.Application.Json)
                            accept(ContentType.Application.Json)
                            setBody(ident)
                        }
                when (post.status) {
                    HttpStatusCode.Unauthorized -> {
                        post.body<TilgangsmaskinResponse.TilgangAvvist>()
                    }

                    HttpStatusCode.NoContent -> {
                        TilgangsmaskinResponse.TilgangGodkjent(
                            harTilgang = true,
                        )
                    }

                    HttpStatusCode.NotFound -> {
                        post.body<TilgangsmaskinResponse.NavIdentIkkeFunnet>()
                    }

                    else -> {
                        throw RuntimeException("Feil ved kall til tilgangsmaskin for ident $ident, status: ${post.status}")
                    }
                }
            }.onFailure {
                throw BadRequestException("Feil ved kall til tilgangsmaskin for ident $ident", it)
            }.getOrThrow()
        }
}
