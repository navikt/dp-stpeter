package no.nav.dagpenger.tilgangsmaskin

import io.ktor.http.HttpStatusCode

open class NavIdentIkkeFunnetException(
    val detail: String,
    val instance: String,
    val status: HttpStatusCode,
    val title: String,
    val navident: String,
) : RuntimeException(detail)
