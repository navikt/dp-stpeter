package no.nav.dagpenger.migrering.api

import io.ktor.http.HttpStatusCode
import no.nav.dagpenger.api.BehandlingException
import java.net.URI

class UnprocessableContentException(
    message: String,
) : BehandlingException(
        httpStatus = HttpStatusCode.UnprocessableEntity,
        type = URI("urn:error:unprocessable_content"),
        title = "Ugyldig forespørsel",
        message = message,
    )
