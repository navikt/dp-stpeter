package no.nav.dagpenger.api

import io.ktor.http.HttpHeaders
import io.ktor.server.application.ApplicationCall
import no.nav.dagpenger.oidc.OidcToken

fun ApplicationCall.token(): OidcToken {
    val token: String = requireNotNull(this.request.headers[HttpHeaders.Authorization]).split(" ")[1]

    return OidcToken(token)
}
