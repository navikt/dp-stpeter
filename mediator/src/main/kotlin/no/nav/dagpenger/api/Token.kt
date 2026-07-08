package no.nav.dagpenger.api

import io.ktor.http.HttpHeaders
import io.ktor.server.application.ApplicationCall

fun ApplicationCall.token(): String {
    val token: String = requireNotNull(this.request.headers[HttpHeaders.Authorization]).split(" ")[1]

    return token
}
