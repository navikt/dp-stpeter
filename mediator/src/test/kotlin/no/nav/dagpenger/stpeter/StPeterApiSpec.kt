package no.nav.dagpenger.stpeter

import io.kotest.assertions.json.shouldBeValidJson
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import no.nav.dagpenger.RedisTestServer
import no.nav.dagpenger.TestApplication.testAzureAdToken
import no.nav.dagpenger.TestApplication.withMockAuthServerAndTestApplication
import no.nav.dagpenger.TestContext
import no.nav.dagpenger.api.models.HttpProblem
import no.nav.dagpenger.objectMapper

private const val IDENT_MED_TILGANG = "12345678901"
private const val IDENT_UTEN_TILGANG = "12345678903"
private const val UGYLDIG_IDENT = "123456789"

class StPeterApiSpec :
    StringSpec({

        val redis = RedisTestServer()

        beforeSpec {
            redis.start()
        }

        afterSpec {
            redis.close()
        }

        "skal ha tilgang" {

            StPeterSystem
                .godkjentScenario()
                .test(redis) {
                    withMockAuthServerAndTestApplication(this.api) {
                        val token =
                            testAzureAdToken(
                                navIdent = "Z123456",
                            )
                        sjekkTilgang(token, IDENT_MED_TILGANG).apply {
                            status.value shouldBe 204
                            val bodyAsText = bodyAsText()
                            bodyAsText.isEmpty() shouldBeEqual true
                            tilgangsmaskinClient.cache.cacheHitCount() shouldBe 0
                            tilgangsmaskinClient.cache.cacheMissCount() shouldBe 1
                        }

                        sjekkTilgang(token, IDENT_MED_TILGANG).apply {
                            status.value shouldBe 204
                            val bodyAsText = bodyAsText()
                            bodyAsText.isEmpty() shouldBeEqual true
                            tilgangsmaskinClient.cache.cacheHitCount() shouldBe 1
                            tilgangsmaskinClient.cache.cacheMissCount() shouldBe 1
                        }
                    }
                }
        }

        "skal gi 404 hvis NavIdent ikke finnes" {

            StPeterSystem
                .navIdentIkkeFunnetScenario()
                .test(redis) {
                    withMockAuthServerAndTestApplication(this.api) {
                        val token =
                            testAzureAdToken(
                                navIdent = "Z404404",
                            )
                        sjekkTilgang(token, IDENT_UTEN_TILGANG).apply {
                            status.value shouldBe 404
                            val body = bodyAsText()
                            body.shouldBeValidJson()
                            val httpProblem = objectMapper.readValue(body, HttpProblem::class.java)
                            httpProblem.shouldBeInstanceOf<HttpProblem>()
                            tilgangsmaskinClient.cache.cacheHitCount() shouldBe 0
                            tilgangsmaskinClient.cache.cacheMissCount() shouldBe 1
                        }

                        sjekkTilgang(token, IDENT_UTEN_TILGANG).apply {
                            status.value shouldBe 404
                            val body = bodyAsText()
                            body.shouldBeValidJson()
                            val httpProblem = objectMapper.readValue(body, HttpProblem::class.java)
                            httpProblem.shouldBeInstanceOf<HttpProblem>()
                            tilgangsmaskinClient.cache.cacheHitCount() shouldBe 1
                            tilgangsmaskinClient.cache.cacheMissCount() shouldBe 1
                        }
                    }
                }
        }

        "skal ikke ha tilgang" {
            StPeterSystem.avvisScenario().test(redis) {

                withMockAuthServerAndTestApplication(this.api) {
                    val token =
                        testAzureAdToken(
                            navIdent = "Z403403",
                        )
                    sjekkTilgang(token, IDENT_UTEN_TILGANG).apply {
                        status.value shouldBe 403
                        val body = bodyAsText()
                        body.shouldBeValidJson()
                        val httpProblem = objectMapper.readValue(body, HttpProblem::class.java)
                        httpProblem.shouldBeInstanceOf<HttpProblem>()
                        tilgangsmaskinClient.cache.cacheHitCount() shouldBe 0
                        tilgangsmaskinClient.cache.cacheMissCount() shouldBe 1
                    }

                    sjekkTilgang(token, IDENT_UTEN_TILGANG).apply {
                        status.value shouldBe 403
                        val body = bodyAsText()
                        body.shouldBeValidJson()
                        val httpProblem = objectMapper.readValue(body, HttpProblem::class.java)
                        httpProblem.shouldBeInstanceOf<HttpProblem>()
                        tilgangsmaskinClient.cache.cacheHitCount() shouldBe 1
                        tilgangsmaskinClient.cache.cacheMissCount() shouldBe 1
                    }
                }
            }
        }

        "skal gi 401 ved manglende saksbehandler gruppe" {
            StPeterSystem.avvisScenario().test(redis) {
                withMockAuthServerAndTestApplication(this.api) {
                    val token =
                        testAzureAdToken(
                            adGrupper = listOf("feil-gruppe"),
                            navIdent = "Z123456",
                        )
                    sjekkTilgang(token, IDENT_UTEN_TILGANG).apply {
                        status.value shouldBe 401
                        val body = bodyAsText()
                        body.shouldBeValidJson()
                        val httpProblem = objectMapper.readValue(body, HttpProblem::class.java)
                        httpProblem.shouldBeInstanceOf<HttpProblem>()
                    }
                }
            }
        }

        "skal gi 401 ved manglende token" {
            StPeterSystem.avvisScenario().test(redis) {
                withMockAuthServerAndTestApplication(this.api) {
                    sjekkTilgang(
                        token = null,
                        ident = IDENT_UTEN_TILGANG,
                    ).apply {
                        status.value shouldBe 401
                        val body = bodyAsText()
                        body.shouldBeValidJson()
                        val httpProblem = objectMapper.readValue(body, HttpProblem::class.java)
                        httpProblem.shouldBeInstanceOf<HttpProblem>()
                    }
                }
            }
        }

        "skal gi 422 ved ugyldig fødselsnummer" {
            StPeterSystem.avvisScenario().test(redis) {
                withMockAuthServerAndTestApplication(this.api) {
                    val token =
                        testAzureAdToken(
                            navIdent = "Z123456",
                        )
                    sjekkTilgang(token, UGYLDIG_IDENT).apply {
                        status.value shouldBe 422
                        val body = bodyAsText()
                        body.shouldBeValidJson()
                        val httpProblem = objectMapper.readValue(body, HttpProblem::class.java)
                        httpProblem.shouldBeInstanceOf<HttpProblem>()
                    }
                }
            }
        }

        "skal gi 400 ved manglende body" {
            StPeterSystem.avvisScenario().test(redis) {
                withMockAuthServerAndTestApplication(this.api) {
                    val token =
                        testAzureAdToken(
                            navIdent = "Z400400",
                        )
                    sjekkTilgangUtenBody(token).apply {
                        status.value shouldBe 400
                        val body = bodyAsText()
                        body.shouldBeValidJson()
                        val httpProblem = objectMapper.readValue(body, HttpProblem::class.java)
                        httpProblem.shouldBeInstanceOf<HttpProblem>()
                    }
                }
            }
        }
    })

private suspend fun TestContext.sjekkTilgang(
    token: String?,
    ident: String,
): HttpResponse =
    client
        .post {
            url("/api/v1/person")
            setBody("""{"ident":"$ident"}""")
            this.header(HttpHeaders.Authorization, "Bearer $token")
            this.header(HttpHeaders.Accept, "application/problem+json")
            this.header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        }

private suspend fun TestContext.sjekkTilgangUtenBody(token: String?): HttpResponse =
    client
        .post {
            url("/api/v1/person")
            this.header(HttpHeaders.Authorization, "Bearer $token")
            this.header(HttpHeaders.Accept, "application/problem+json")
            this.header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        }
