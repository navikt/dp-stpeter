package no.dagpenger.stpeter.plugin

class StPeterConfig(
    val url: String = getEnvOrSystem("STPETER_URL"),
    val scope: String = getEnvOrSystem("STPETER_SCOPE"),
    val clientId: String = getEnvOrSystem("AZURE_APP_CLIENT_ID"),
    val clientSecret: String = getEnvOrSystem("AZURE_APP_CLIENT_SECRET"),
    val jwk: String = getEnvOrSystem("AZURE_APP_JWK"),
    val wellKnownUrl: String = getEnvOrSystem("AZURE_APP_WELL_KNOWN_URL"),
    val tokenEndpoint: String = getEnvOrSystem("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT"),
) {
    constructor(config: Map<String, String>) : this(
        url = config["STPETER_URL"] ?: error("Missing STPETER_URL in config"),
        scope = config["STPETER_SCOPE"] ?: error("Missing STPETER_SCOPE in config"),
        clientId = config["AZURE_APP_CLIENT_ID"] ?: error("Missing AZURE_APP_CLIENT_ID in config"),
        clientSecret = config["AZURE_APP_CLIENT_SECRET"] ?: error("Missing AZURE_APP_CLIENT_SECRET in config"),
        jwk = config["AZURE_APP_JWK"] ?: error("Missing AZURE_APP_JWK in config"),
        wellKnownUrl = config["AZURE_APP_WELL_KNOWN_URL"] ?: error("Missing AZURE_APP_WELL_KNOWN_URL in config"),
        tokenEndpoint =
            config["AZURE_OPENID_CONFIG_TOKEN_ENDPOINT"]
                ?: error("Missing AZURE_OPENID_CONFIG_TOKEN_ENDPOINT in config"),
    )

    fun toMap(): Map<String, String> =
        mapOf(
            "STPETER_URL" to url,
            "STPETER_SCOPE" to scope,
            "AZURE_APP_CLIENT_ID" to clientId,
            "AZURE_APP_CLIENT_SECRET" to clientSecret,
            "AZURE_APP_JWK" to jwk,
            "AZURE_APP_WELL_KNOWN_URL" to wellKnownUrl,
            "AZURE_OPENID_CONFIG_TOKEN_ENDPOINT" to tokenEndpoint,
        )

    // Unngår at hemmeligheter havner i logger via default toString()/logging av config-objektet.
    override fun toString(): String =
        "StPeterConfig(url='$url', scope='$scope', clientId='$clientId', clientSecret='******', " +
            "jwk='******', wellKnownUrl='$wellKnownUrl', tokenEndpoint='$tokenEndpoint')"
}

private fun getEnvOrSystem(name: String) =
    System.getenv(name)
        ?: System.getProperty(name)
        ?: error("Missing environment variable or system property: $name")
