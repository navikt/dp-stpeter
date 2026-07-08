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
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import no.nav.dagpenger.TestApplication.testAzureAdToken
import no.nav.dagpenger.TestApplication.withMockAuthServerAndTestApplication
import no.nav.dagpenger.api.models.HttpProblem
import no.nav.dagpenger.objectMapper

class StPeterApiSpec :
    StringSpec({

        "skal ha tilgang" {

            StPeterSystem
                .godkjentScenario()
                .test {
                    withMockAuthServerAndTestApplication(this.api) {
                        val token =
                            testAzureAdToken(
                                navIdent = "Z123456",
                            )
                        client
                            .post {
                                url("/api/v1/person")
                                setBody("""{"ident":"12345678901"}""")
                                this.header(HttpHeaders.Authorization, "Bearer $token")
                                this.header(HttpHeaders.Accept, "application/problem+json")
                                this.header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                            }.apply {
                                status.value shouldBe 204
                                val bodyAsText = bodyAsText()
                                bodyAsText.isEmpty() shouldBeEqual true
                            }
                    }
                }
        }

        "skal gi 404 hvis NavIdent ikke finnes" {

            StPeterSystem
                .navIdentIkkeFunnetScenario()
                .test {
                    withMockAuthServerAndTestApplication(this.api) {
                        val token =
                            testAzureAdToken(
                                navIdent = "Z123456",
                            )
                        client
                            .post {
                                url("/api/v1/person")
                                setBody("""{"ident":"12345678901"}""")
                                this.header(HttpHeaders.Authorization, "Bearer $token")
                                this.header(HttpHeaders.Accept, "application/problem+json")
                                this.header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                            }.apply {
                                status.value shouldBe 404
                                val body = bodyAsText()
                                body.shouldBeValidJson()
                                val httpProblem = objectMapper.readValue(body, HttpProblem::class.java)
                                httpProblem.shouldBeInstanceOf<HttpProblem>()
                            }
                    }
                }
        }

        "skal ikke ha tilgang" {
            StPeterSystem.avvisScenario().test {
                withMockAuthServerAndTestApplication(this.api) {
                    val token =
                        testAzureAdToken(
                            navIdent = "Z123456",
                        )
                    client
                        .post {
                            url("/api/v1/person")
                            setBody("""{"ident":"12345678901"}""")
                            this.header(HttpHeaders.Authorization, "Bearer $token")
                            this.header(HttpHeaders.Accept, "application/problem+json")
                            this.header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        }.apply {
                            status.value shouldBe 403
                            val body = bodyAsText()
                            body.shouldBeValidJson()
                            val httpProblem = objectMapper.readValue(body, HttpProblem::class.java)
                            httpProblem.shouldBeInstanceOf<HttpProblem>()
                        }
                }
            }
        }

        "skal gi 401 ved manglende saksbehandler gruppe" {
            StPeterSystem.avvisScenario().test {
                withMockAuthServerAndTestApplication(this.api) {
                    val token =
                        testAzureAdToken(
                            adGrupper = listOf("feil-gruppe"),
                            navIdent = "Z123456",
                        )
                    client
                        .post {
                            url("/api/v1/person")
                            setBody("""{"ident":"12345678901"}""")
                            this.header(HttpHeaders.Authorization, "Bearer $token")
                            this.header(HttpHeaders.Accept, "application/problem+json")
                            this.header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        }.apply {
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
            StPeterSystem.avvisScenario().test {
                withMockAuthServerAndTestApplication(this.api) {
                    client
                        .post {
                            url("/api/v1/person")
                            setBody("""{"ident":"12345678901"}""")
                            this.header(HttpHeaders.Accept, "application/problem+json")
                            this.header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        }.apply {
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
            StPeterSystem.avvisScenario().test {
                withMockAuthServerAndTestApplication(this.api) {
                    val token =
                        testAzureAdToken(
                            navIdent = "Z123456",
                        )
                    client
                        .post {
                            url("/api/v1/person")
                            setBody("""{"ident":"123456789"}""")
                            this.header(HttpHeaders.Authorization, "Bearer $token")
                            this.header(HttpHeaders.Accept, "application/problem+json")
                            this.header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        }.apply {
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
            StPeterSystem.avvisScenario().test {
                withMockAuthServerAndTestApplication(this.api) {
                    val token =
                        testAzureAdToken(
                            navIdent = "Z123456",
                        )
                    client
                        .post {
                            url("/api/v1/person")
                            this.header(HttpHeaders.Authorization, "Bearer $token")
                            this.header(HttpHeaders.Accept, "application/problem+json")
                            this.header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                        }.apply {
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
