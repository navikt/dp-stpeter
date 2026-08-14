package no.nav.dagpenger.konfigurasjon

import com.natpryce.konfig.ConfigurationMap
import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.PropertyGroup
import com.natpryce.konfig.getValue
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType

object Configuration {
    const val APP_NAME = "dp-stpeter"

    private val defaultProperties =
        ConfigurationMap(
            mapOf(),
        )

    object Grupper : PropertyGroup() {
        val saksbehandler by stringType
    }

    val tilgangsMaskinApiUrl by lazy { properties[Key("TILGANGSMASKIN_API_URL", stringType)] }

    val properties =
        ConfigurationProperties.systemProperties() overriding EnvironmentVariables() overriding defaultProperties

    val config: Map<String, String> =
        properties.list().reversed().fold(emptyMap()) { map, pair ->
            map + pair.second
        }
}
