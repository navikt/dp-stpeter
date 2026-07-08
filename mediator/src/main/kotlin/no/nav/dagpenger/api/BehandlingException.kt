package no.nav.dagpenger.api

import io.ktor.http.HttpStatusCode
import java.net.URI

sealed class BehandlingException(
    val httpStatus: HttpStatusCode,
    val type: URI,
    val title: String,
    val extensions: Map<String, Any?> = emptyMap(),
    message: String = title,
) : RuntimeException(message)
