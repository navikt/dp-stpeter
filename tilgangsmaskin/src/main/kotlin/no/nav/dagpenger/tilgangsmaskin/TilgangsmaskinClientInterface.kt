package no.nav.dagpenger.tilgangsmaskin

interface TilgangsmaskinClientInterface {
    fun harTilgangTilPersonKomplett(
        ident: String,
        token: String,
    ): TilgangsmaskinResponse

    fun harTilgangTilPersonKjerne(
        ident: String,
        token: String,
    ): TilgangsmaskinResponse
}
