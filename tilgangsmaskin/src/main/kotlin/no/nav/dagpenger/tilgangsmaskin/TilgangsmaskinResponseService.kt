package no.nav.dagpenger.tilgangsmaskin

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.ktor.http.HttpStatusCode
import no.nav.dagpenger.oidc.OidcToken

class TilgangsmaskinResponseService(
    val tilgangsmaskinClient: TilgangsmaskinClientInterface,
) {
    companion object {
        private val sikkerlogg = KotlinLogging.logger("tjenestekall")
        private val logger = KotlinLogging.logger {}
    }

    fun evaluerTilgangTilPersonKomplett(
        ident: Ident,
        token: OidcToken,
    ) {
        logger.info { "Evalurer tilgang til person med regelsett 'komplett'" }
        val response =
            tilgangsmaskinClient.harTilgangTilPersonKomplett(
                ident = ident,
                token = token,
            )
        logger.info { "Mottatt svar fra tilgangsmaskinen" }

        withLoggingContext(
            "komponent" to "StPeter",
            "navIdent" to token.navIdent(),
        ) {
            when (response) {
                is TilgangsmaskinResponse.TilgangAvvist -> {
                    withLoggingContext(
                        "ident" to "$ident",
                        "traceId" to response.traceId,
                        "begrunnelse" to response.title,
                    ) {
                        sikkerlogg.info { "Tilgang avvist" }
                    }
                    throw TilgangAvvistException(
                        type = response.type,
                        status = HttpStatusCode.fromValue(response.status),
                        title = response.title,
                        navIdent = response.navIdent,
                        begrunnelse = response.begrunnelse,
                        traceId = response.traceId,
                        kanOverstyres = response.kanOverstyres,
                    )
                }

                is TilgangsmaskinResponse.NavIdentIkkeFunnet -> {
                    withLoggingContext(
                        "ident" to "$ident",
                        "begrunnelse" to response.title,
                    ) {
                        sikkerlogg.info { "Tilgang avvist" }
                    }
                    throw NavIdentIkkeFunnetException(
                        detail = response.detail,
                        instance = response.instance,
                        status = HttpStatusCode.fromValue(response.status),
                        title = response.title,
                        navident = response.navident,
                    )
                }

                is TilgangsmaskinResponse.TilgangGodkjent -> {
                    withLoggingContext(
                        "ident" to "$ident",
                    ) {
                        sikkerlogg.info { "Tilgang godkjent" }
                    }
                }
            }
        }
    }
}
