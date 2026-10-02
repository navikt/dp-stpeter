package no.dagpenger.stpeter.plugin.test

import com.nimbusds.oauth2.sdk.ErrorObject

@Suppress("unused")
class StPeterException(
    val errorObject: ErrorObject?,
    msg: String,
    throwable: Throwable?,
) : RuntimeException(msg, throwable) {
    constructor(msg: String) : this(null, msg, null)
    constructor(msg: String, throwable: Throwable?) : this(null, msg, throwable)
    constructor(errorObject: ErrorObject?, msg: String) : this(errorObject, msg, null)
}
