package no.nav.dagpenger.tilgangsmaskin

import no.nav.dagpenger.api.models.IdentForesporsel
import no.nav.dagpenger.oidc.OidcToken

interface TilgangsmaskinClientInterface {
    fun harTilgangTilPersonKomplett(
        ident: IdentForesporsel,
        token: OidcToken,
        callId: String?,
    ): TilgangsmaskinResponse

    fun harTilgangTilPersonKjerne(
        ident: IdentForesporsel,
        token: OidcToken,
        callId: String?,
    ): TilgangsmaskinResponse
}
