package no.nav.dagpenger.tilgangsmaskin

import io.ktor.http.HttpStatusCode
import java.net.URI

open class TilgangAvvistException(
    val type: URI,
    val title: String,
    val status: HttpStatusCode,
    val navIdent: String,
    val begrunnelse: String,
    val traceId: String,
    val kanOverstyres: Boolean,
) : RuntimeException(begrunnelse)
