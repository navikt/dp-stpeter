package no.nav.dagpenger.api

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.calllogging.processingTimeMillis
import io.ktor.server.request.document
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import no.nav.dagpenger.UUIDv7
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

    install(CallId) {
        verify { it.isNotEmpty() }
        generate { UUIDv7.ny().toString() }
    }

    install(CallLogging) {
        disableDefaultColors()
        filter { call ->
            !setOf(
                "isalive",
                "isready",
                "metrics",
            ).contains(call.request.document())
        }
        format { call ->
            val status = call.response.status()?.value ?: "Unhandled"
            val method = call.request.httpMethod.value
            val path = call.request.path()
            val duration = call.processingTimeMillis()
            val queryParams = call.request.queryParameters.entries()
            "$status $method $path $queryParams $duration ms"
        }
    }
}

internal fun Application.apiConfig(
    authFactory: AuthFactory,
    tilgangsmaskinClient: TilgangsmaskinClient,
) {
    stpeterApi(authFactory, tilgangsmaskinClient)
}
