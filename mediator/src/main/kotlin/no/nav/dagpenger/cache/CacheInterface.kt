package no.nav.dagpenger.cache

import io.micrometer.core.instrument.Clock
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.prometheus.metrics.model.registry.PrometheusRegistry
import no.nav.dagpenger.objectMapper
import tools.jackson.core.type.TypeReference

interface CacheInterface {
    val redis: Redis
    val keyPrefix: String

    val prometheus: MeterRegistry
        get() =
            PrometheusMeterRegistry(
                PrometheusConfig.DEFAULT,
                PrometheusRegistry.defaultRegistry,
                Clock.SYSTEM,
            )

    suspend fun <T> set(
        key: String,
        value: T,
    ) {
        redis.set(Key(prefix = keyPrefix, value = key), objectMapper.writeValueAsBytes(value))
    }
}

suspend inline fun <reified T> CacheInterface.get(key: String): T? {
    val tr = object : TypeReference<T>() {}

    return redis[Key(prefix = keyPrefix, value = key)]?.let { objectMapper.readValue(it, tr) }
}
