package no.nav.dagpenger.tilgangsmaskin

import io.github.oshai.kotlinlogging.KotlinLogging
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
import no.nav.dagpenger.logging.TeamLogg
import no.nav.dagpenger.oidc.OidcToken

private const val METRIC_NAME = "dp_stpeter_tilgangsmaskin_klient"

class TilgangsmaskinClient(
    val tilgangsMaskinApiUrl: String,
    val tokenProvider: suspend (String) -> String,
    val httpClient: HttpClient,
    val cache: TilgangsmaskinCache,
) : TilgangsmaskinClientInterface {
    companion object {
        private val teamLogg = TeamLogg(TilgangsmaskinClient::class)
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
        return teamLogg.withContextAsync(
            "navIdent" to token.navIdent(),
            "ident" to ident.toString(),
            "endpoint" to endpoint,
        ) {
            try {
                teamLogg.info { "Sender forespørsel til tilgangsmaskin" }
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
                teamLogg.info { "Mottatt svar fra tilgangsmaskin" }

                return@withContextAsync response
            } catch (e: IOException) {
                utfall("io_error")
                teamLogg.warn(cause = e) {
                    "Fikk ikke kontakt med tilgangsmaskin"
                }
                throw e
            } catch (e: BadRequestException) {
                utfall("client_error")
                teamLogg.warn(cause = e) {
                    "Feil ved kall til tilgangsmaskinen"
                }
                throw e
            } catch (e: ServerResponseException) {
                utfall("server_error")
                teamLogg.warn(cause = e) {
                    "Feil ved kall til tilgangsmaskinen"
                }
                throw e
            } catch (e: RuntimeException) {
                utfall("unknown_error")
                teamLogg.warn(cause = e) {
                    "Ukjent feil ved kall til tilgangsmaskinen"
                }
                throw e
            } finally {
                teamLogg.info { "Ferdig med forespørsel til tilgangsmaskin" }
            }
        }
    }

    private suspend fun HttpResponse.toTilgangsmaskinResponse(
        token: OidcToken,
        ident: Ident,
    ): TilgangsmaskinResponse {
        val requestUrl = call.request.url.toString()
        val statusValue = status.value.toString()

        return teamLogg.withContextAsync(
            "requestUrl" to requestUrl,
            "status" to statusValue,
            "navIdent" to token.navIdent(),
            "ident" to ident.toString(),
        ) {
            when (status) {
                HttpStatusCode.Forbidden -> {
                    val body = body<TilgangsmaskinResponse.TilgangAvvist>()
                    info(
                        "traceId" to body.traceId,
                        "begrunnelse" to body.title,
                    ) { "Tilgang avvist" }
                    body
                }

                HttpStatusCode.NoContent -> {
                    info { "Tilgang godkjent" }
                    TilgangsmaskinResponse.TilgangGodkjent()
                }

                HttpStatusCode.NotFound -> {
                    val body = body<TilgangsmaskinResponse.NavIdentIkkeFunnet>()
                    info { "NavIdent ikke funnet" }
                    body
                }

                else -> {
                    if (status.value in 400 until 500) {
                        throw BadRequestException("Feil ved kall til tilgangsmaskinen. Status: $status")
                    }
                    if (status.value in 500 until 600) {
                        teamLogg.warn { "Feil ved kall tilgangsmaskinen." }
                        throw ServerResponseException(
                            this@toTilgangsmaskinResponse,
                            "Feil ved kall til tilgangsmaskinen. Status: $status",
                        )
                    }
                    logger.warn { "Feil ved kall tilgangsmaskinen." }
                    throw RuntimeException("Ukjent feil ved kall til tilgangsmaskin status: $status")
                }
            }
        }
    }
}
