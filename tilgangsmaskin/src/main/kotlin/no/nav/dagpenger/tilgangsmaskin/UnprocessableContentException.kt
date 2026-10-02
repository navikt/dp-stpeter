package no.nav.dagpenger.tilgangsmaskin

import io.ktor.http.HttpStatusCode
import java.net.URI

class UnprocessableContentException(
    message: String,
) : RequestException(
        httpStatus = HttpStatusCode.UnprocessableEntity,
        type = URI("urn:error:unprocessable_content"),
        title = "Ugyldig forespørsel",
        message = message,
    )
