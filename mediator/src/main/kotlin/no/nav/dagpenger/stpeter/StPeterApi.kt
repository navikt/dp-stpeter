package no.nav.dagpenger.stpeter

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.request.receive
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import no.nav.dagpenger.Ident.Companion.tilPersonIdentfikator
import no.nav.dagpenger.api.auth.AuthFactory
import no.nav.dagpenger.api.authenticationConfig
import no.nav.dagpenger.api.models.IdentForesporsel
import no.nav.dagpenger.api.token
import no.nav.dagpenger.tilgangsmaskin.TilgangsmaskinClient
import no.nav.dagpenger.tilgangsmaskin.TilgangsmaskinResponseService

internal fun Application.stpeterApi(
    authFactory: AuthFactory,
    tilgangsmaskinClient: TilgangsmaskinClient,
) {
    val tilgangsmaskinResponseService =
        TilgangsmaskinResponseService(
            tilgangsmaskinClient = tilgangsmaskinClient,
        )
    authenticationConfig(authFactory)

    routing {
        route("/api/v1") {
            swaggerUI(path = "openapi", swaggerFile = "st-peter-api.yaml", {
            })

            get { call.respond(HttpStatusCode.OK) }

            authenticate("azureAd") {
                post("/person") {
                    val identForespørsel = call.receive<IdentForesporsel>()
                    val ident = identForespørsel.ident.tilPersonIdentfikator()

                    val tilgangTilPersonKomplett =
                        tilgangsmaskinResponseService.evaluerTilgangTilPersonKomplett(
                            ident = ident.identifikator(),
                            token = call.token(),
                        )

                    val status = HttpStatusCode.fromValue(tilgangTilPersonKomplett.status)
                    val problem = tilgangTilPersonKomplett.reason
                    if (problem == null) {
                        call.respond(status)
                    } else {
                        call.response.header("Content-Type", "application/problem+json")
                        call.respond(status = status, message = problem)
                    }
                }
            }
        }
    }
}
