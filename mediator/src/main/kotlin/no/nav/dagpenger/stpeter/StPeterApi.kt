package no.nav.dagpenger.stpeter

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.request.receive
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
import no.nav.dagpenger.tilgangsmaskin.TilgangAvvistException
import no.nav.dagpenger.tilgangsmaskin.TilgangsmaskinClient
import no.nav.dagpenger.tilgangsmaskin.TilgangsmaskinResponseService
import java.net.URI

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

                    val harTilgang =
                        tilgangsmaskinResponseService.evaluerTilgangTilPersonKomplett(
                            ident = ident.identifikator(),
                            token = call.token(),
                        )

                    if (harTilgang) {
                        call.respond(HttpStatusCode.NoContent)
                    } else {
                        throw TilgangAvvistException(
                            type = URI.create("urn:error:forbidden"),
                            status = HttpStatusCode.Forbidden,
                            title = "En ukjent feil oppstod ved evaluering av tilgang",
                        )
                    }
                }
            }
        }
    }
}
