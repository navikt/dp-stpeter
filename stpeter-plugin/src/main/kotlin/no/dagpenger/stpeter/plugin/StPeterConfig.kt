package no.dagpenger.stpeter.plugin

data class StPeterConfig(
    val url: String = getEnvOrSystem("STPETER_URL"),
    val scope: String = getEnvOrSystem("STPETER_SCOPE"),
) {
    constructor(config: Map<String, String>) : this(
        url = config["STPETER_URL"] ?: error("Missing STPETER_URL in config"),
        scope = config["STPETER_SCOPE"] ?: error("Missing STPETER_SCOPE in config"),
    )
}

private fun getEnvOrSystem(name: String) =
    System.getenv(name)
        ?: System.getProperty(name)
        ?: error("Missing environment variable or system property: $name")
