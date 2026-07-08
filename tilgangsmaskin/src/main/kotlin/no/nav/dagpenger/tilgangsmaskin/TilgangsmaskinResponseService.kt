package no.nav.dagpenger.tilgangsmaskin

import no.nav.dagpenger.api.models.HttpProblem
import java.net.URI

class TilgangsmaskinResponseService(
    val tilgangsmaskinClient: TilgangsmaskinClientInterface,
) {
    data class TilgangResultat(
        val status: Int,
        val reason: HttpProblem? = null,
    )

    fun evaluerTilgangTilPersonKomplett(
        ident: String,
        token: String,
    ): TilgangResultat {
        val response =
            tilgangsmaskinClient.harTilgangTilPersonKomplett(
                ident = ident,
                token = token,
            )

        return when (response) {
            is TilgangsmaskinResponse.TilgangAvvist -> {
                // Logg eller håndter avvist tilgang
                TilgangResultat(
                    status = response.status,
                    reason =
                        HttpProblem(
                            type = response.type,
                            title = response.title,
                            status = response.status,
                            detail = response.begrunnelse,
                            properties =
                                mutableMapOf(
                                    "tilgangsmaskinTraceId" to response.traceId,
                                    "kanOverstyres" to response.kanOverstyres,
                                ),
                        ),
                )
            }

            is TilgangsmaskinResponse.NavIdentIkkeFunnet -> {
                TilgangResultat(
                    status = response.status,
                    reason =
                        HttpProblem(
                            type = URI("urn:error:not_found"),
                            title = response.title,
                            status = response.status,
                            detail = response.detail,
                            properties =
                                mutableMapOf(
                                    "navIdent" to response.navident,
                                ),
                        ),
                )
            }

            is TilgangsmaskinResponse.TilgangGodkjent -> {
                TilgangResultat(
                    status = 204,
                    reason = null,
                )
            }
        }
    }
}
