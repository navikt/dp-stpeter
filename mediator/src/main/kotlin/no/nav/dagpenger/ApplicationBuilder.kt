package no.nav.dagpenger

import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.EmbeddedServer
import no.nav.dagpenger.api.apiConfig
import no.nav.dagpenger.api.auth.AuthFactory
import no.nav.dagpenger.cache.Redis
import no.nav.dagpenger.cache.RedisConfig
import no.nav.dagpenger.konfigurasjon.Configuration
import no.nav.dagpenger.konfigurasjon.Configuration.tilgangsMaskinApiUrl
import no.nav.dagpenger.tilgangsmaskin.TilgangsmaskinCache
import no.nav.dagpenger.tilgangsmaskin.TilgangsmaskinClient
import no.nav.dagpenger.tilgangsmaskin.TokenProvider

internal class ApplicationBuilder(
    config: Map<String, String>,
) {
    private val tokenProvider = TokenProvider(config)
    private val redis = Redis.from(RedisConfig())
    private val tilgangsmaskinCache = TilgangsmaskinCache(redis)
    private val tilgangsmaskinClient =
        TilgangsmaskinClient(
            tilgangsMaskinApiUrl = tilgangsMaskinApiUrl,
            tokenProvider = tokenProvider.oboExchanger,
            httpClient =
                createHttpClient(
                    metricsBaseName = "dp_stpeter_tilgangsmaskin_client",
                ),
            cache = tilgangsmaskinCache,
        )

    private val server: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration> =
        ktorApplication(
            preStopHook = {},
            aliveCheck = { true },
            readyCheck = { true },
            cioConfiguration = {
            },
            configuration = {
                apiConfig(AuthFactory(Configuration.properties), tilgangsmaskinClient)
            },
        )

    fun start() {
        server.start(wait = true)
    }
}
