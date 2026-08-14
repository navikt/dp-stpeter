package no.nav.dagpenger.tilgangsmaskin

import io.micrometer.core.instrument.Clock
import io.micrometer.core.instrument.Tag
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.prometheus.metrics.model.registry.PrometheusRegistry
import no.nav.dagpenger.cache.Key
import no.nav.dagpenger.cache.Redis
import no.nav.dagpenger.objectMapper
import no.nav.dagpenger.oidc.OidcToken
import tools.jackson.module.kotlin.readValue

private const val KEY_PREFIX = "tilgangsmaskin"

class TilgangsmaskinCache(
    val redis: Redis,
) {
    private val prometheus =
        PrometheusMeterRegistry(
            PrometheusConfig.DEFAULT,
            PrometheusRegistry.defaultRegistry,
            Clock.SYSTEM,
        )

    suspend fun set(
        token: OidcToken,
        ident: String,
        value: TilgangsmaskinResponse,
    ) {
        redis.set(key(token, ident), objectMapper.writeValueAsBytes(value.toCached()))
    }

    suspend fun get(
        token: OidcToken,
        ident: String,
    ): TilgangsmaskinResponse? =
        redis[key(token, ident)]?.let {
            cacheHit()
            objectMapper.readValue<CachedTilgangsmaskinResponse>(it).toDomain()
        } ?: run {
            cacheMiss()
            null
        }

    private fun key(
        token: OidcToken,
        ident: String,
    ): Key = Key(prefix = KEY_PREFIX, value = "${token.navIdent()}_$ident")

    fun cacheHitCount(): Double = prometheus.counter("cache_hit", listOf(Tag.of("service", KEY_PREFIX))).count()

    fun cacheMissCount(): Double = prometheus.counter("cache_miss", listOf(Tag.of("service", KEY_PREFIX))).count()

    private fun cacheHit() =
        prometheus
            .counter(
                "cache_hit",
                listOf(Tag.of("service", KEY_PREFIX)),
            ).increment()

    private fun cacheMiss() =
        prometheus
            .counter(
                "cache_miss",
                listOf(Tag.of("service", KEY_PREFIX)),
            ).increment()
}
