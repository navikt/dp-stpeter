package no.nav.dagpenger.tilgangsmaskin

import io.ktor.http.HttpStatusCode
import no.nav.dagpenger.api.models.IdentForesporsel
import no.nav.dagpenger.logging.Oppslagslogg
import no.nav.dagpenger.logging.TeamLogg
import no.nav.dagpenger.oidc.OidcToken
import no.nav.dagpenger.toContextMap
import kotlin.to

class TilgangsmaskinResponseService(
    val tilgangsmaskinClient: TilgangsmaskinClientInterface,
) {
    companion object {
        private val teamLogg = TeamLogg(TilgangsmaskinResponseService::class)
        private val oppslagslogg = Oppslagslogg()
    }

    fun evaluerTilgangTilPersonKomplett(
        identForesporsel: IdentForesporsel,
        token: OidcToken,
        callId: String?,
    ) {
        val ident = identForesporsel.tilIdent()

        teamLogg.withContext(
            "navIdent" to token.navIdent(),
            "callId" to callId,
            *identForesporsel.toContextMap(),
        ) {
            info { "Evaluerer tilgang til person med regelsett 'komplett'" }
            val response =
                tilgangsmaskinClient.harTilgangTilPersonKomplett(
                    ident = identForesporsel,
                    token = token,
                    callId = callId,
                )
            info { "Mottatt svar fra tilgangsmaskinen" }

            when (response) {
                is TilgangsmaskinResponse.TilgangAvvist -> {
                    info { "Svarer med TilgangAvvistException" }
                    throw TilgangAvvistException(
                        type = response.type,
                        status = HttpStatusCode.fromValue(response.status),
                        title = response.title,
                        navIdent = response.navIdent,
                        begrunnelse = response.begrunnelse,
                        traceId = response.traceId,
                        kanOverstyres = response.kanOverstyres,
                    )
                }

                is TilgangsmaskinResponse.NavIdentIkkeFunnet -> {
                    info { "Svarer med NavIdentIkkeFunnetException" }
                    throw NavIdentIkkeFunnetException(
                        detail = response.detail,
                        instance = response.instance,
                        status = HttpStatusCode.fromValue(response.status),
                        title = response.title,
                        navident = response.navident,
                    )
                }

                is TilgangsmaskinResponse.TilgangGodkjent -> {
                    info { "Svarer med TilgangGodkjent" }
                    if (identForesporsel.oppslagslogg) {
                        oppslagslogg.les(
                            appName = identForesporsel.application,
                            navIdent = token.navIdent(),
                            borgerIdent = ident.identifikator(),
                            callId = callId,
                        )
                    }
                }
            }
        }
    }
}
