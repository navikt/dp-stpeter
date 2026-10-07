package no.dagpenger.stpeter.plugin

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.http.HttpStatusCode
import no.dagpenger.stpeter.plugin.test.StPeterWithOAuthMock

class StPeterPluginSpec :
    StringSpec({

        val stPeterMock = StPeterWithOAuthMock()
        lateinit var stPeter: StPeterPlugin

        beforeSpec {
            stPeterMock.start()
            stPeter =
                StPeterPlugin(
                    config = StPeterConfig(config = stPeterMock.config()),
                )
        }
        afterSpec {
            stPeterMock.shutdown()
        }

        "skal ikke kaste exception når tilgang til person er tillatt" {
            shouldNotThrow<TilgangAvvistException> {

                stPeterMock.withStPeterAllowAccessToPerson {
                    stPeter.vedTilgangTilPerson(
                        ident = "12345678901",
                        token =
                            stPeterMock
                                .issueToken(
                                    issuerId = "azureAd",
                                    audience = "dp-arena-innsyn",
                                    claims =
                                        mapOf(
                                            "idtyp" to "app",
                                            "azp_name" to "dp-arena-innsyn",
                                        ),
                                ),
                        originRoute = "/test/{ident}",
                    ) {
                    }

                    stPeter.vedTilgangTilPersonUtenOppslagslogg(
                        ident = "12345678901",
                        token =
                            stPeterMock
                                .issueToken(
                                    issuerId = "azureAd",
                                    audience = "dp-arena-innsyn",
                                    claims =
                                        mapOf(
                                            "idtyp" to "app",
                                            "azp_name" to "dp-arena-innsyn",
                                        ),
                                ),
                    ) {
                    }
                }
            }
        }

        "skal avvise originRoute som ikke er en trygg rutemal" {
            listOf(
                "/person/12345678901",
                "/person/123456",
                "/person/id-123456",
                "/person/550e8400-e29b-41d4-a716-446655440000",
                "/person/id-550e8400-e29b-41d4-a716-446655440000",
                "/person/{ident}?mode=full",
                "https://client.nav.no/person",
                "/${"a".repeat(200)}",
            ).forEach { route ->
                shouldThrow<IllegalArgumentException> {
                    stPeter.vedTilgangTilPerson(
                        ident = "12345678901",
                        token = "not-used",
                        originRoute = route,
                    ) {}
                }
            }
        }

        "skal kaste exception når tilgang til person blir avvist" {
            val exception =
                shouldThrow<TilgangAvvistException> {
                    stPeterMock.withStPeterDenyAccessToPerson {
                        stPeter.vedTilgangTilPerson(
                            ident = "12345678901",
                            token =
                                stPeterMock
                                    .issueToken(
                                        issuerId = "azureAd",
                                        audience = "dp-arena-innsyn",
                                        claims =
                                            mapOf(
                                                "idtyp" to "app",
                                                "azp_name" to "dp-arena-innsyn",
                                            ),
                                    ),
                            originRoute = "/test/{ident}",
                        ) {
                        }
                    }
                }
            exception.status shouldBe HttpStatusCode.Forbidden
        }

        "skal kaste exception når saksbehandler ikke finnes" {
            val exception =
                shouldThrow<TilgangAvvistException> {
                    stPeterMock.withStPeterSaksbehandlerNotFound {
                        stPeter.vedTilgangTilPerson(
                            ident = "12345678901",
                            token =
                                stPeterMock
                                    .issueToken(
                                        issuerId = "azureAd",
                                        audience = "dp-arena-innsyn",
                                        claims =
                                            mapOf(
                                                "idtyp" to "app",
                                                "azp_name" to "dp-arena-innsyn",
                                            ),
                                    ),
                        ) {
                        }
                    }
                }
            exception.status shouldBe HttpStatusCode.NotFound
        }

        "skal kaste exception når stpeter returnerer uventet statuskode" {
            val exception =
                shouldThrow<TilgangAvvistException> {
                    stPeterMock.withStPeterResponse(HttpStatusCode.InternalServerError) {
                        stPeter.vedTilgangTilPerson(
                            ident = "12345678901",
                            token =
                                stPeterMock
                                    .issueToken(
                                        issuerId = "azureAd",
                                        audience = "dp-arena-innsyn",
                                        claims =
                                            mapOf(
                                                "idtyp" to "app",
                                                "azp_name" to "dp-arena-innsyn",
                                            ),
                                    ),
                        ) {
                        }
                    }
                }
            exception.message shouldContain "status=500"
        }
    })
