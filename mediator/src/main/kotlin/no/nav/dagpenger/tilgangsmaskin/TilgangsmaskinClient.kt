package no.nav.dagpenger.tilgangsmaskin

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.coroutines.withLoggingContextAsync
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.BadRequestException
import io.micrometer.core.instrument.Clock
import io.micrometer.core.instrument.Tag
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.prometheus.metrics.model.registry.PrometheusRegistry
import kotlinx.coroutines.runBlocking
import kotlinx.io.IOException
import no.nav.dagpenger.oidc.OidcToken

private const val METRIC_NAME = "dp_stpeter_tilgangsmaskin_klient"

class TilgangsmaskinClient(
    val tilgangsMaskinApiUrl: String,
    val tokenProvider: suspend (String) -> String,
    val httpClient: HttpClient,
    val cache: TilgangsmaskinCache,
) : TilgangsmaskinClientInterface {
    companion object {
        private val sikkerlogg = KotlinLogging.logger("tjenestekall")
        private val logger = KotlinLogging.logger {}
    }

    private val prometheus =
        PrometheusMeterRegistry(
            PrometheusConfig.DEFAULT,
            PrometheusRegistry.defaultRegistry,
            Clock.SYSTEM,
        )

    private fun utfall(name: String) =
        prometheus
            .counter(METRIC_NAME, listOf(Tag.of("utfall", name)))
            .increment()

    fun utfallCount(name: String): Double = prometheus.counter(METRIC_NAME, listOf(Tag.of("utfall", name))).count()

    override fun harTilgangTilPersonKomplett(
        ident: Ident,
        token: OidcToken,
    ): TilgangsmaskinResponse = sjekkTilgang(ident, token, "komplett")

    override fun harTilgangTilPersonKjerne(
        ident: Ident,
        token: OidcToken,
    ): TilgangsmaskinResponse = sjekkTilgang(ident, token, "kjerne")

    private fun sjekkTilgang(
        ident: Ident,
        token: OidcToken,
        endpoint: String,
    ): TilgangsmaskinResponse =
        runBlocking {
            cache
                .get(
                    token = token,
                    ident = ident.identifikator(),
                    endpoint = endpoint,
                )?.let {
                    return@runBlocking it
                }

            // Kaster BadRequestException (400) for reelle 4xx-svar og ServerResponseException
            // (havner på 500 via StatusPages) for 5xx. IOException (f.eks. timeout) og andre
            // uventede feil skal IKKE fanges opp og tvinges til 400 her — de skal falle gjennom
            // til StatusPages' generiske Throwable-handler og svare 500.
            //
            // 🔴 Rød sone: feilklassifiseringen under avgjør hva som blir alarmert på i Grafana.
            // Vurder om "io_error" (tapt kontakt/timeout) bør skilles fra "server_error" i egen
            // alert med strengere terskel, siden det kan indikere nettverks-/DNS-problemer og
            // ikke bare at tilgangsmaskin selv svarer med feil.
            val response = requestTilgangsmaskin(endpoint, token, ident)

            cache.set(
                token = token,
                ident = ident.identifikator(),
                endpoint = endpoint,
                value = response,
            )

            response
        }

    private suspend fun requestTilgangsmaskin(
        endpoint: String,
        token: OidcToken,
        ident: Ident,
    ): TilgangsmaskinResponse {
        try {
            val response =
                httpClient
                    .post("$tilgangsMaskinApiUrl/api/v1/$endpoint") {
                        val oboToken = tokenProvider.invoke(token.token())
                        header(HttpHeaders.Authorization, "Bearer $oboToken")
                        header(HttpHeaders.ContentType, ContentType.Application.Json)
                        accept(ContentType.Application.ProblemJson)
                        accept(ContentType.Application.Json)
                        accept(ContentType.Text.Plain)
                        setBody(ident.identifikator())
                    }.toTilgangsmaskinResponse(token, ident)

            utfall("success")

            return response
        } catch (e: IOException) {
            utfall("io_error")
            sikkerlogg.warn(e) {
                "Fikk ikke kontakt med tilgangsmaskin (endpoint=$endpoint): ${e.message}"
            }
            throw e
        } catch (e: BadRequestException) {
            utfall("client_error")
            throw e
        } catch (e: ServerResponseException) {
            utfall("server_error")
            throw e
        } catch (e: RuntimeException) {
            utfall("unknown_error")
            throw e
        }
    }

    private suspend fun HttpResponse.toTilgangsmaskinResponse(
        token: OidcToken,
        ident: Ident,
    ): TilgangsmaskinResponse =
        withLoggingContextAsync(
            "requestUrl" to call.request.url.toString(),
            "status" to status.toString(),
            "komponent" to this.javaClass.simpleName,
        ) {
            when (status) {
                HttpStatusCode.Forbidden -> {
                    val body = body<TilgangsmaskinResponse.TilgangAvvist>()
                    logger.info { "Tilgang avvist" }
                    withLoggingContextAsync(
                        "traceId" to body.traceId,
                        "navIdent" to body.navIdent,
                        "begrunnelse" to body.title,
                    ) {
                        sikkerlogg.info { "Tilgang avvist" }
                    }
                    body
                }

                HttpStatusCode.NoContent -> {
                    logger.info { "Tilgang godkjent" }
                    withLoggingContextAsync(
                        "navIdent" to token.navIdent(),
                    ) {
                        sikkerlogg.info { "Tilgang godkjent" }
                    }
                    TilgangsmaskinResponse.TilgangGodkjent()
                }

                HttpStatusCode.NotFound -> {
                    val body = body<TilgangsmaskinResponse.NavIdentIkkeFunnet>()
                    sikkerlogg.info { "NavIdent ikke funnet" }
                    withLoggingContextAsync(
                        "navIdent" to body.navident,
                    ) {
                        sikkerlogg.info { "Tilgang avvist. ${body.title}. ${body.detail}." }
                    }
                    body
                }

                else -> {
                    if (status.value in 400 until 500) {
                        logger.warn { "Feil ved kall til tilgangsmaskinen." }
                        throw BadRequestException("Feil ved kall til tilgangsmaskinen.")
                    }
                    if (status.value in 500 until 600) {
                        logger.warn { "Feil ved kall tilgangsmaskinen." }
                        throw ServerResponseException(this, "Feil ved kall til tilgangsmaskinen.")
                    }
                    logger.warn { "Feil ved kall tilgangsmaskinen." }
                    throw RuntimeException("Ukjent feil ved kall til tilgangsmaskin status: $status")
                }
            }
        }
}
