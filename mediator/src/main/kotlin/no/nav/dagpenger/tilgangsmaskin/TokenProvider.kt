package no.nav.dagpenger.tilgangsmaskin

import com.natpryce.konfig.Configuration
import com.natpryce.konfig.ConfigurationMap
import com.natpryce.konfig.Key
import com.natpryce.konfig.stringType
import no.nav.dagpenger.oauth2.CachedOauth2Client
import no.nav.dagpenger.oauth2.OAuth2Config

class TokenProvider {
    constructor(config: Configuration) {
        this.configuration = config
    }

    constructor(config: Map<String, String>) {
        this.configuration = ConfigurationMap(config)
    }

    private val configuration: Configuration

    private val azureAdClient: CachedOauth2Client by lazy {
        val azureAdConfig = OAuth2Config.AzureAd(configuration)
        CachedOauth2Client(
            tokenEndpointUrl = azureAdConfig.tokenEndpointUrl,
            authType = azureAdConfig.clientSecret(),
        )
    }

    val oboExchanger: (String) -> String by lazy {
        val scope = configuration[Key("TILGANGSMASKIN_API_SCOPE", stringType)]
        { token: String ->
            val accessToken =
                azureAdClient
                    .onBehalfOf(token, scope)
                    .access_token
            requireNotNull(accessToken) { "Failed to get access token" }
            accessToken
        }
    }
}
