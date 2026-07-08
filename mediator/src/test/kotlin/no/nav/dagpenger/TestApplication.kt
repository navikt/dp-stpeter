package no.nav.dagpenger

import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.serialization.jackson3.JacksonConverter
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.testing.testApplication
import no.nav.dagpenger.api.statusPagesConfig
import no.nav.dagpenger.oauth2.OAuth2Config
import no.nav.dagpenger.tilgangsmaskin.TokenProvider
import no.nav.security.mock.oauth2.MockOAuth2Server

object TestApplication {
    val mockOAuth2Server: MockOAuth2Server by lazy {
        MockOAuth2Server().also { server ->
            server.start()
        }
    }

    const val AZURE_AD_ISSUER_ID = "azureAd"
    const val CLIENT_ID = "clientId"
    const val SAKSBEHANDLER_GRUPPE = "saksbehandler-gruppe"

    val tokenProvider =
        TokenProvider(
            mapOf(
                OAuth2Config.AzureAd.CLIENT_ID_KEY to CLIENT_ID,
                OAuth2Config.AzureAd.CLIENT_SECRET_KEY to "clientSecret",
                OAuth2Config.AzureAd.TOKEN_ENDPOINT_KEY to mockOAuth2Server.tokenEndpointUrl("default").toString(),
            ),
        )

    fun testAzureAdToken(
        adGrupper: List<String> = listOf(SAKSBEHANDLER_GRUPPE),
        navIdent: String,
    ): String =
        mockOAuth2Server
            .issueToken(
                issuerId = AZURE_AD_ISSUER_ID,
                audience = CLIENT_ID,
                claims =
                    mapOf(
                        "NAVident" to navIdent,
                        "groups" to adGrupper,
                    ),
            ).serialize()

    internal fun withMockAuthServerAndTestApplication(
        moduleFunction: Application.() -> Unit,
        test: suspend TestContext.() -> Unit,
    ) {
        testApplication {
            application {
                install(io.ktor.server.plugins.contentnegotiation.ContentNegotiation) {
                    register(ContentType.Application.Json, JacksonConverter(objectMapper))
                }
                install(StatusPages) {
                    statusPagesConfig()
                }
                moduleFunction()
            }

            val testClient =
                createClient {
                    install(ContentNegotiation) {
                        register(ContentType.Application.Json, JacksonConverter(objectMapper))
                    }
                }

            test(TestContext(testClient))
        }
    }
}
