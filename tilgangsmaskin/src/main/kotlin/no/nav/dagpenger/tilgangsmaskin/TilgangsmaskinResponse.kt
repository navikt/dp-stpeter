package no.nav.dagpenger.tilgangsmaskin

import java.net.URI

sealed interface TilgangsmaskinResponse {
    data class TilgangGodkjent(
        val harTilgang: Boolean,
    ) : TilgangsmaskinResponse

    data class TilgangAvvist(
        val type: URI,
        val title: String,
        val status: Int,
        val navIdent: String,
        val begrunnelse: String,
        val traceId: String,
        val kanOverstyres: Boolean,
    ) : TilgangsmaskinResponse

    data class NavIdentIkkeFunnet(
        val detail: String,
        val instance: String,
        val status: Int,
        val title: String,
        val navident: String,
    ) : TilgangsmaskinResponse
}
