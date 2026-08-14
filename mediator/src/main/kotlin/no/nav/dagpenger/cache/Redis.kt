package no.nav.dagpenger.cache

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.SetArgs
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import io.lettuce.core.api.coroutines.RedisCoroutinesCommandsImpl
import io.lettuce.core.codec.ByteArrayCodec
import java.net.URI
import java.util.concurrent.TimeUnit

data class Key(
    val prefix: String,
    val value: String,
) {
    fun get(): ByteArray = "$prefix:$value".toByteArray()
}

@OptIn(ExperimentalLettuceCoroutinesApi::class)
class Redis private constructor(
    private val client: RedisClient,
    private val connection: StatefulRedisConnection<ByteArray, ByteArray>,
) : AutoCloseable {
    private val commands: RedisCoroutinesCommands<ByteArray, ByteArray> =
        RedisCoroutinesCommandsImpl(connection.reactive())

    suspend fun set(
        key: Key,
        value: ByteArray,
        expireSec: Long = TimeUnit.MINUTES.toSeconds(60),
    ) {
        commands.set(key.get(), value, SetArgs().ex(expireSec))
    }

    suspend operator fun get(key: Key): ByteArray? = commands.get(key.get())

    suspend fun ready(): Boolean = commands.ping() == "PONG"

    override fun close() {
        connection.close()
        client.shutdown()
    }

    companion object {
        fun from(uri: URI): Redis = buildClient(RedisURI.create(uri))

        fun from(config: RedisConfig): Redis =
            buildClient(
                RedisURI
                    .builder()
                    .withHost(config.uri.host)
                    .withPort(config.uri.port)
                    .withSsl(true)
                    .withAuthentication(config.username, config.password.toCharArray())
                    .build(),
            )

        private fun buildClient(uri: RedisURI): Redis =
            RedisClient.create(uri).let {
                Redis(it, it.connect(ByteArrayCodec.INSTANCE))
            }
    }
}
