package no.dagpenger.stpeter.plugin

import io.ktor.http.HttpStatusCode
import java.net.URI

open class TilgangAvvistException(
    val title: String,
    val status: HttpStatusCode,
    val type: URI,
    val detail: String,
    val instance: URI,
    val navIdent: String? = null,
    val traceId: String? = null,
    val kanOverstyres: Boolean? = null,
) : RuntimeException(detail)
