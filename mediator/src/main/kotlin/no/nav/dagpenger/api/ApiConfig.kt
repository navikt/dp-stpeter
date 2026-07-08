package no.nav.dagpenger.api

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.jwt
import no.nav.dagpenger.api.auth.AuthFactory
import no.nav.dagpenger.stpeter.stpeterApi
import no.nav.dagpenger.tilgangsmaskin.TilgangsmaskinClient

internal fun Application.authenticationConfig(authFactory: AuthFactory) {
    install(Authentication) {
        jwt("azureAd") {
            with(authFactory) {
                azureAd()
            }
        }
    }
}

internal fun Application.apiConfig(
    authFactory: AuthFactory,
    tilgangsmaskinClient: TilgangsmaskinClient,
) {
    stpeterApi(authFactory, tilgangsmaskinClient)
}
