package no.nav.dagpenger.cache

import no.nav.dagpenger.konfigurasjon.Configuration
import no.nav.dagpenger.konfigurasjon.Configuration.properties
import java.net.URI

data class RedisConfig(
    val uri: URI = URI(properties[Configuration.Redis.uriStpeter]),
    val username: String = properties[Configuration.Redis.usernameStpeter],
    val password: String = properties[Configuration.Redis.passwordStpeter],
)
