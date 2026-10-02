package no.nav.dagpenger

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import kotlin.reflect.KClass

class TeamLogg private constructor(
    name: String,
    private val context: Map<String, String>,
) {
    constructor(forClass: KClass<*>? = null) : this(
        name = forClass?.let { "tjenestekall.${it.simpleName}" } ?: "tjenestekall",
        context = emptyMap(),
    )

    private val logger = KotlinLogging.logger(name)

    fun info(
        vararg context: Pair<String, String>,
        message: () -> String,
    ) = log(context, message, logger::info)

    fun warn(
        vararg context: Pair<String, String>,
        cause: Throwable? = null,
        message: () -> String,
    ) = log(context, message) { logger.warn(cause, it) }

    fun error(
        vararg context: Pair<String, String>,
        cause: Throwable? = null,
        message: () -> String,
    ) = log(context, message) { logger.error(cause, it) }

    private fun log(
        callContext: Array<out Pair<String, String>>,
        message: () -> String,
        emit: (() -> Any?) -> Unit,
    ) {
        // MDC settes kun rundt selve loggkallet, slik at konteksten til TeamLogg
        // ikke lekker inn i andre loggere (f.eks. KotlinLogging.logger {}) som
        // tilfeldigvis logger mens en withContext/withContextAsync-blokk pågår.
        withLoggingContext(context + callContext) { emit { message() } }
    }

    /**
     * Returnerer en ny TeamLogg der [context] er lagt til eksisterende kontekst,
     * og kjører [block] på den. Konteksten lever kun på denne instansen og settes
     * i MDC først når et loggkall faktisk skjer, slik at den ikke lekker ut i
     * andre loggere.
     */
    fun <T> withContext(
        vararg context: Pair<String, String>,
        block: TeamLogg.() -> T,
    ): T = TeamLogg(logger.name, this.context + context).block()

    suspend fun <T> withContextAsync(
        vararg context: Pair<String, String>,
        block: suspend TeamLogg.() -> T,
    ): T = TeamLogg(logger.name, this.context + context).block()
}
