package no.dagpenger.stpeter.plugin.test

import no.nav.dagpenger.oauth2.CachedOauth2Client
import no.nav.dagpenger.oauth2.OAuth2Config
import no.nav.security.mock.oauth2.MockOAuth2Server
import kotlin.getValue

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

//        System.setProperty("STPETER_URL", stPeterMockServer.url())
//        System.setProperty("STPETER_SCOPE", stPeterMockServer.scope)
//        System.setProperty("AZURE_APP_WELL_KNOWN_URL", mockOAuth2Server.wellKnownUrl("azureAd").toString())
//        System.setProperty("AZURE_APP_CLIENT_ID", "test-client-id")
//        System.setProperty("AZURE_APP_CLIENT_SECRET", "test-secret")

        config.clear()
        config.putAll(
            mapOf(
                "STPETER_URL" to stPeterMockServer.url(),
                "STPETER_SCOPE" to stPeterMockServer.scope,
                "AZURE_APP_WELL_KNOWN_URL" to mockOAuth2Server.wellKnownUrl("azureAd").toString(),
                "AZURE_APP_CLIENT_ID" to "test-client-id",
                "AZURE_APP_CLIENT_SECRET" to "test-secret",
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
        claims: Map<String, String> = emptyMap(),
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

    val azureAdClient: CachedOauth2Client by lazy {
        val azureAdConfig =
            OAuth2Config.AzureAd(
                mapOf(
                    OAuth2Config.AzureAd.CLIENT_ID_KEY to "test-client-id",
                    OAuth2Config.AzureAd.CLIENT_SECRET_KEY to "test-secret",
                    OAuth2Config.AzureAd.WELLKNOWN_URL_KEY to
                        mockOAuth2Server
                            .wellKnownUrl("default")
                            .toString(),
                ),
            )
        CachedOauth2Client(
            tokenEndpointUrl = azureAdConfig.tokenEndpointUrl,
            authType = azureAdConfig.clientSecret(),
        )
    }

    val oboExchanger: (String) -> String by lazy {
        { token: String ->
            val accessToken =
                azureAdClient
                    .onBehalfOf(token, stPeterMockServer.scope)
                    .access_token
            requireNotNull(accessToken) { "Failed to get access token" }
            accessToken
        }
    }
}
