package no.nav.dagpenger.tilgangsmaskin

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
import no.nav.dagpenger.api.models.IdentForesporsel
import no.nav.dagpenger.logging.TeamLogg
import no.nav.dagpenger.oidc.OidcToken
import no.nav.dagpenger.toContextMap

private const val METRIC_NAME = "dp_stpeter_tilgangsmaskin_klient"

class TilgangsmaskinClient(
    val tilgangsMaskinApiUrl: String,
    val tokenProvider: suspend (String) -> String,
    val httpClient: HttpClient,
    val cache: TilgangsmaskinCache,
) : TilgangsmaskinClientInterface {
    companion object {
        private val teamLogg = TeamLogg(TilgangsmaskinClient::class)
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
        ident: IdentForesporsel,
        token: OidcToken,
        callId: String?,
    ): TilgangsmaskinResponse = sjekkTilgang(ident, token, "komplett", callId = callId)

    override fun harTilgangTilPersonKjerne(
        ident: IdentForesporsel,
        token: OidcToken,
        callId: String?,
    ): TilgangsmaskinResponse = sjekkTilgang(ident, token, "kjerne", callId)

    private fun sjekkTilgang(
        identForesporsel: IdentForesporsel,
        token: OidcToken,
        endpoint: String,
        callId: String?,
    ): TilgangsmaskinResponse =
        runBlocking {
            cache
                .get(
                    token = token,
                    ident = identForesporsel.ident,
                    endpoint = endpoint,
                )?.let {
                    teamLogg.info(
                        "navIdent" to token.navIdent(),
                        "endpoint" to endpoint,
                        "callId" to callId,
                        *identForesporsel.toContextMap(),
                    ) { "Fant svar i cache" }
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
            val response =
                requestTilgangsmaskin(
                    endpoint = endpoint,
                    token = token,
                    identForesporsel = identForesporsel,
                    callId = callId,
                )

            cache.set(
                token = token,
                ident = identForesporsel.ident,
                endpoint = endpoint,
                value = response,
            )

            response
        }

    private suspend fun requestTilgangsmaskin(
        endpoint: String,
        token: OidcToken,
        identForesporsel: IdentForesporsel,
        callId: String?,
    ): TilgangsmaskinResponse {
        return teamLogg.withContextAsync(
            "navIdent" to token.navIdent(),
            "endpoint" to endpoint,
            "callId" to callId,
            *identForesporsel.toContextMap(),
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
                            setBody(identForesporsel.ident)
                        }.toTilgangsmaskinResponse(token = token, identForesporsel = identForesporsel, callId = callId)

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
        identForesporsel: IdentForesporsel,
        callId: String?,
    ): TilgangsmaskinResponse {
        val requestUrl = call.request.url.toString()
        val statusValue = status.value.toString()

        return teamLogg.withContextAsync(
            "requestUrl" to requestUrl,
            "status" to statusValue,
            "navIdent" to token.navIdent(),
            "callId" to callId,
            *identForesporsel.toContextMap(),
        ) {
            when (status) {
                HttpStatusCode.Forbidden -> {
                    val body = body<TilgangsmaskinResponse.TilgangAvvist>()
                    info(
                        "traceId" to body.traceId,
                        "begrunnelse" to body.title,
                    ) { "Tilgang avvist ($status)" }
                    body
                }

                HttpStatusCode.NoContent -> {
                    info { "Tilgang godkjent ($status)" }
                    TilgangsmaskinResponse.TilgangGodkjent()
                }

                HttpStatusCode.NotFound -> {
                    val body = body<TilgangsmaskinResponse.NavIdentIkkeFunnet>()
                    info { "NavIdent ikke funnet ($status)" }
                    body
                }

                else -> {
                    teamLogg.warn { "Feil ved kall tilgangsmaskinen ($status)." }
                    if (status.value in 400 until 500) {
                        throw BadRequestException("Feil ved kall til tilgangsmaskinen. Status: $status")
                    }
                    if (status.value in 500 until 600) {
                        throw ServerResponseException(
                            this@toTilgangsmaskinResponse,
                            "Feil ved kall til tilgangsmaskinen. Status: $status",
                        )
                    }
                    throw RuntimeException("Ukjent feil ved kall til tilgangsmaskin status: $status")
                }
            }
        }
    }
}
