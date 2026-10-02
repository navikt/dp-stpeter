package no.nav.dagpenger.tilgangsmaskin

import io.ktor.http.HttpStatusCode
import java.net.URI

open class TilgangAvvistException(
    val type: URI,
    val title: String,
    val status: HttpStatusCode,
    val navIdent: String? = null,
    val begrunnelse: String? = null,
    val traceId: String? = null,
    val kanOverstyres: Boolean? = null,
) : RuntimeException(begrunnelse)
