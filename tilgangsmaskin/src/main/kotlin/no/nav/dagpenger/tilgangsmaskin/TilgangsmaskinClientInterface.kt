package no.nav.dagpenger.tilgangsmaskin

import no.nav.dagpenger.oidc.OidcToken

interface TilgangsmaskinClientInterface {
    fun harTilgangTilPersonKomplett(
        ident: String,
        token: OidcToken,
    ): TilgangsmaskinResponse

    fun harTilgangTilPersonKjerne(
        ident: String,
        token: OidcToken,
    ): TilgangsmaskinResponse
}
