package no.nav.dagpenger.tilgangsmaskin

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.BadRequestException
import no.nav.dagpenger.TestApplication.testAzureAdToken
import no.nav.dagpenger.TestApplication.tokenProvider

class TilgangsmaskinClientSpec :
    StringSpec({

        "skal gi tilgang" {

            TilgangsmaskinSystem
                .nyttScenario {
                    content = ""
                    status = HttpStatusCode.NoContent
                }.test {
                    val tilgangsmaskinClient =
                        TilgangsmaskinClient(
                            tilgangsMaskinApiUrl = "http://localhost",
                            tokenProvider = tokenProvider.oboExchanger,
                            httpClient = httpClient,
                        )

                    tilgangsmaskinClient
                        .harTilgangTilPersonKomplett(
                            ident = "12345678901",
                            token = testAzureAdToken(listOf("dagpenger-saksbehandler"), "Z123456"),
                        ).shouldBeInstanceOf<TilgangsmaskinResponse.TilgangGodkjent>()
                }
        }

        "skal avvise tilgang" {
            TilgangsmaskinSystem
                .nyttScenario {
                    // language=json
                    content =
                        """
                        {
                          "type": "https://confluence.adeo.no/display/TM/Tilgangsmaskin+API+og+regelsett",
                          "title": "AVVIST_STRENGT_FORTROLIG_ADRESSE",
                          "status": 403,
                          "instance": "Z990883/03508331575",
                          "brukerIdent": "03508331575",
                          "navIdent": "Z990883",
                          "traceId": "444290be30ed4fdd9a849654bad9dc1b",
                          "begrunnelse": "Du har ikke tilgang til brukere med strengt fortrolig adresse",
                          "kanOverstyres": false
                        }
                        """.trimIndent()
                    status = HttpStatusCode.Unauthorized
                }.test {
                    val tilgangsmaskinClient =
                        TilgangsmaskinClient(
                            tilgangsMaskinApiUrl = "http://localhost",
                            tokenProvider = tokenProvider.oboExchanger,
                            httpClient = httpClient,
                        )

                    tilgangsmaskinClient
                        .harTilgangTilPersonKomplett(
                            ident = "12345678901",
                            token = testAzureAdToken(listOf("dagpenger-saksbehandler"), "Z123456"),
                        ).shouldBeInstanceOf<TilgangsmaskinResponse.TilgangAvvist>()
                }
        }

        "NavIdent ikke funnet" {
            TilgangsmaskinSystem
                .nyttScenario {
                    // language=json
                    content =
                        """
                        {
                          "detail": "Fant ingen oid for navident A222222, er den fremdeles gyldig?",
                          "instance": "/api/v1/ccf/komplett/A222222",
                          "status": 404,
                          "title": "Uventet respons fra Entra",
                          "navident": "A222222"
                        }
                        """.trimIndent()
                    status = HttpStatusCode.NotFound
                }.test {
                    val tilgangsmaskinClient =
                        TilgangsmaskinClient(
                            tilgangsMaskinApiUrl = "http://localhost",
                            tokenProvider = tokenProvider.oboExchanger,
                            httpClient = httpClient,
                        )

                    tilgangsmaskinClient
                        .harTilgangTilPersonKomplett(
                            ident = "12345678901",
                            token = testAzureAdToken(listOf("dagpenger-saksbehandler"), "Z123456"),
                        ).shouldBeInstanceOf<TilgangsmaskinResponse.NavIdentIkkeFunnet>()
                }
        }

        "ukjent respons" {
            TilgangsmaskinSystem
                .nyttScenario {
                    // language=json
                    content = ""
                    status = HttpStatusCode.BadRequest
                }.test {
                    val tilgangsmaskinClient =
                        TilgangsmaskinClient(
                            tilgangsMaskinApiUrl = "http://localhost",
                            tokenProvider = tokenProvider.oboExchanger,
                            httpClient = httpClient,
                        )

                    shouldThrow<BadRequestException> {
                        tilgangsmaskinClient
                            .harTilgangTilPersonKomplett(
                                ident = "12345678901",
                                token = testAzureAdToken(listOf("dagpenger-saksbehandler"), "Z123456"),
                            )
                    }
                }
        }
    })
