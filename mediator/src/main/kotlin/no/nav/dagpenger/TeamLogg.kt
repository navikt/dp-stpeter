package no.nav.dagpenger

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.coroutines.withLoggingContextAsync
import io.github.oshai.kotlinlogging.withLoggingContext
import kotlin.reflect.KClass

class TeamLogg(
    forClass: KClass<*>? = null,
) {
    private val logger =
        KotlinLogging.logger(
            forClass?.let { "tjenestekall.${it.simpleName}" } ?: "tjenestekall",
        )

    fun info(
        vararg context: Pair<String, String>,
        message: () -> String,
    ) = log(context, message, logger::info)

    fun warn(
        vararg context: Pair<String, String>,
        message: () -> String,
    ) = log(context, message, logger::warn)

    fun error(
        vararg context: Pair<String, String>,
        message: () -> String,
    ) = log(context, message, logger::error)

    private fun log(
        context: Array<out Pair<String, String>>,
        message: () -> String,
        emit: ((() -> Any?)) -> Unit,
    ) = withLoggingContext(*context) { emit { message() } }

    inline fun <T> withContext(
        vararg context: Pair<String, String>,
        block: TeamLogg.() -> T,
    ): T = withLoggingContext(*context) { block() }

    suspend inline fun <T> withContextAsync(
        vararg context: Pair<String, String>,
        crossinline block: suspend TeamLogg.() -> T,
    ): T = withLoggingContextAsync(*context) { block() }
}
