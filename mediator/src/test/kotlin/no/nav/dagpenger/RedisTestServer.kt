package no.nav.dagpenger

import com.redis.testcontainers.RedisContainer
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.cache.Redis
import java.net.URI
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class RedisTestServer : AutoCloseable {
    private val started = AtomicBoolean(false)
    private val redisContainer by lazy { RedisContainer(RedisContainer.DEFAULT_IMAGE_NAME.withTag(RedisContainer.DEFAULT_TAG)) }

    private val uri: URI
        get() = URI(redisContainer.redisURI)

    internal val server: Redis by lazy { Redis.from(uri) }

    fun start() {
        if (!started.compareAndSet(false, true)) return

        redisContainer.start()

        val timeout = System.currentTimeMillis() + 10.seconds.inWholeMilliseconds
        while (System.currentTimeMillis() < timeout) {
            try {
                runBlocking { server.ready() }
                break
            } catch (_: Throwable) {
                Thread.sleep(500.milliseconds.inWholeMilliseconds)
            }
        }
    }

    override fun close() {
        if (!started.get()) return
        server.close()
        redisContainer.stop()
    }
}
