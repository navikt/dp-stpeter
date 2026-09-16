package no.dagpenger.stpeter.plugin

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
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
                    oboExchanger = stPeterMock.oboExchanger,
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
                    ) {
                    }
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
    })
