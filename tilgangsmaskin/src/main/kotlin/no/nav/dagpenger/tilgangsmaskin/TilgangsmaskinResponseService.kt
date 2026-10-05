package no.nav.dagpenger.tilgangsmaskin

import io.ktor.http.HttpStatusCode
import no.nav.dagpenger.logging.TeamLogg
import no.nav.dagpenger.oidc.OidcToken

class TilgangsmaskinResponseService(
    val tilgangsmaskinClient: TilgangsmaskinClientInterface,
) {
    companion object {
        private val teamLogg = TeamLogg(TilgangsmaskinResponseService::class)
    }

    fun evaluerTilgangTilPersonKomplett(
        ident: Ident,
        token: OidcToken,
    ) {
        teamLogg.withContext(
            "ident" to ident.toString(),
            "navIdent" to token.navIdent(),
        ) {
            info { "Evalurer tilgang til person med regelsett 'komplett'" }
            val response =
                tilgangsmaskinClient.harTilgangTilPersonKomplett(
                    ident = ident,
                    token = token,
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
                }
            }
        }
    }
}
