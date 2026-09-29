package no.nav.dagpenger.tilgangsmaskin

import no.nav.dagpenger.oidc.OidcToken

interface TilgangsmaskinClientInterface {
    fun harTilgangTilPersonKomplett(
        ident: Ident,
        token: OidcToken,
    ): TilgangsmaskinResponse

    fun harTilgangTilPersonKjerne(
        ident: Ident,
        token: OidcToken,
    ): TilgangsmaskinResponse
}
