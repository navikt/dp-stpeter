package no.nav.dagpenger.tilgangsmaskin

import io.ktor.http.HttpStatusCode

class TilgangsmaskinResponseService(
    val tilgangsmaskinClient: TilgangsmaskinClientInterface,
) {
    fun evaluerTilgangTilPersonKomplett(
        ident: String,
        token: String,
    ): Boolean {
        val response =
            tilgangsmaskinClient.harTilgangTilPersonKomplett(
                ident = ident,
                token = token,
            )

        return when (response) {
            is TilgangsmaskinResponse.TilgangAvvist -> {
                // Logg eller håndter avvist tilgang
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
                throw NavIdentIkkeFunnetException(
                    detail = response.detail,
                    instance = response.instance,
                    status = HttpStatusCode.fromValue(response.status),
                    title = response.title,
                    navident = response.navident,
                )
            }

            is TilgangsmaskinResponse.TilgangGodkjent -> {
                true
            }
        }
    }
}
