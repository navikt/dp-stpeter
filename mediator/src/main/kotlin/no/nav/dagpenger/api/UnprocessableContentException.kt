package no.nav.dagpenger.api

import io.ktor.http.HttpStatusCode
import java.net.URI

class UnprocessableContentException(
    message: String,
) : BehandlingException(
        httpStatus = HttpStatusCode.UnprocessableEntity,
        type = URI("urn:error:unprocessable_content"),
        title = "Ugyldig forespørsel",
        message = message,
    )
