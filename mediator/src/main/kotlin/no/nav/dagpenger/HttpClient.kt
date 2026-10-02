package no.nav.dagpenger

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.serialization.jackson3.JacksonConverter
import io.prometheus.metrics.model.registry.PrometheusRegistry
import no.nav.dagpenger.ktor.client.metrics.PrometheusMetricsPlugin
import kotlin.time.Duration.Companion.seconds

fun createHttpClient(
    prometheusRegistry: PrometheusRegistry = PrometheusRegistry.defaultRegistry,
    metricsBaseName: String,
    engine: HttpClientEngine = CIO.create {},
    expectSuccess: Boolean = false,
    configure: HttpClientConfig<*>.() -> Unit = {},
) = HttpClient(engine) {
    this.expectSuccess = expectSuccess
    install(PrometheusMetricsPlugin) {
        this.baseName = metricsBaseName
        this.registry = prometheusRegistry
    }
    install(ContentNegotiation) {
        register(ContentType.Application.Json, JacksonConverter(objectMapper))
        register(ContentType.Application.ProblemJson, JacksonConverter(objectMapper))
    }
    install(HttpTimeout) {
        requestTimeoutMillis = 5.seconds.inWholeMilliseconds
        connectTimeoutMillis = 2.seconds.inWholeMilliseconds
        socketTimeoutMillis = 3.seconds.inWholeMilliseconds
    }
    configure()
}
