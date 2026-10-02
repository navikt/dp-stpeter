package no.dagpenger.stpeter.plugin.test

import io.ktor.http.HttpStatusCode
import no.nav.security.mock.oauth2.MockOAuth2Server

class StPeterWithOAuthMock {
    internal val stPeterMockServer: StPeterMockServer by lazy {
        StPeterMockServer().also { server ->
            server.start()
        }
    }

    private val config: MutableMap<String, String> = mutableMapOf()

    fun config(): Map<String, String> = config.toMap()

    internal val mockOAuth2Server: MockOAuth2Server by lazy {
        MockOAuth2Server().also { server ->
            server.start()
        }
    }

    fun start() {
        stPeterMockServer.start()
        mockOAuth2Server.start()

        config.clear()
        config.putAll(
            mapOf(
                "STPETER_URL" to stPeterMockServer.url(),
                "STPETER_SCOPE" to stPeterMockServer.scope,
                "AZURE_APP_WELL_KNOWN_URL" to mockOAuth2Server.wellKnownUrl("azureAd").toString(),
                "AZURE_APP_CLIENT_ID" to "test-client-id",
                "AZURE_APP_CLIENT_SECRET" to "test-secret",
                "AZURE_APP_JWK" to mockOAuth2Server.jwksUrl("azureAd").toString(),
                "AZURE_OPENID_CONFIG_TOKEN_ENDPOINT" to mockOAuth2Server.tokenEndpointUrl("azureAd").toString(),
            ),
        )
    }

    fun shutdown() {
        stPeterMockServer.shutdown()
        mockOAuth2Server.shutdown()
    }

    fun issueToken(
        issuerId: String = "azureAd",
        audience: String = "dp-arena-innsyn",
        claims: Map<String, Any> = emptyMap(),
    ): String =
        mockOAuth2Server
            .issueToken(
                issuerId = issuerId,
                audience = audience,
                claims = claims,
            ).serialize()

    suspend fun withStPeterAllowAccessToPerson(block: suspend () -> Unit) {
        stPeterMockServer.withStPeterAllowAccessToPerson {
            block()
        }
    }

    suspend fun withStPeterDenyAccessToPerson(block: suspend () -> Unit) {
        stPeterMockServer.withStPeterDenyAccessToPerson {
            block()
        }
    }

    suspend fun withStPeterSaksbehandlerNotFound(block: suspend () -> Unit) {
        stPeterMockServer.withStPeterSaksbehandlerNotFound {
            block()
        }
    }

    suspend fun withStPeterResponse(
        httpStatusCode: HttpStatusCode,
        block: suspend () -> Unit,
    ) {
        stPeterMockServer.withStPeterResponse(httpStatusCode) {
            block()
        }
    }
}
