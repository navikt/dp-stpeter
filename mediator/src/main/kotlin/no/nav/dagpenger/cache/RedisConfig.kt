package no.nav.dagpenger.cache

import java.net.URI

private const val REDIS_URI_ENV = "REDIS_URI_STPETER"
private const val REDIS_USERNAME_ENV = "REDIS_USERNAME_STPETER"
private const val REDIS_PASSWORD_ENV = "REDIS_PASSWORD_STPETER"

data class RedisConfig(
    val uri: URI = URI(requiredValue(REDIS_URI_ENV)),
    val username: String = requiredValue(REDIS_USERNAME_ENV),
    val password: String = requiredValue(REDIS_PASSWORD_ENV),
)

private fun requiredValue(envar: String) = System.getenv(envar) ?: error("missing envvar $envar")
