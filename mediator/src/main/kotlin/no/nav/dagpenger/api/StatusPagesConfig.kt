package no.nav.dagpenger.api

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpStatusCode.Companion.allStatusCodes
import io.ktor.http.content.OutgoingContent
import io.ktor.http.isSuccess
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.log
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.plugins.statuspages.StatusPagesConfig
import io.ktor.server.request.uri
import io.ktor.server.response.header
import io.ktor.server.response.respond
import no.nav.dagpenger.api.models.HttpProblem
import no.nav.dagpenger.tilgangsmaskin.NavIdentIkkeFunnetException
import no.nav.dagpenger.tilgangsmaskin.TilgangAvvistException
import java.net.URI

fun StatusPagesConfig.statusPagesConfig() {
    exception<BehandlingException> { call, cause ->
        call.application.log.warn("domenefeil: ${cause.message}. svarer med ${cause.httpStatus} og HttpProblem", cause)
        call.response.header("Content-Type", ContentType.Application.ProblemJson.toString())
        call.respond(
            cause.httpStatus,
            HttpProblem(
                type = cause.type,
                title = cause.title,
                status = cause.httpStatus.value,
                detail = cause.message,
                instance = URI(call.request.uri),
                properties =
                    cause.extensions
                        .filterValues { it != null }
                        .mapValues { it.value!! }
                        .toMutableMap(),
            ),
        )
    }
    exception<NavIdentIkkeFunnetException> { call, cause ->
        call.application.log.info("tilgangsfeil: ${cause.message}. svarer med ${cause.status} og HttpProblem", cause)
        call.response.header("Content-Type", ContentType.Application.ProblemJson.toString())
        call.respond(
            cause.status,
            HttpProblem(
                type = URI("urn:error:not_found"),
                title = cause.title,
                status = cause.status.value,
                detail = cause.message,
                instance = URI(call.request.uri),
                properties =
                    mutableMapOf(
                        "navident" to cause.navident,
                    ),
            ),
        )
    }

    exception<TilgangAvvistException> { call, cause ->
        call.application.log.info("tilgangsfeil: ${cause.message}. svarer med ${cause.status} og HttpProblem", cause)

        call.response.header("Content-Type", ContentType.Application.ProblemJson.toString())
        call.respond(
            cause.status,
            HttpProblem(
                title = cause.title,
                status = cause.status.value,
                type = cause.type,
                detail = cause.message,
                instance = URI(call.request.uri),
                properties =
                    mutableMapOf(
                        "traceId" to cause.traceId,
                    ),
            ),
        )
    }

    exception<BadRequestException> { call, cause ->
        call.application.log.warn(
            "bad request: ${cause.message}. svarer med BadRequest og en feilmelding i JSON",
            cause,
        )
        call.response.header("Content-Type", ContentType.Application.ProblemJson.toString())
        call.respond(
            HttpStatusCode.BadRequest,
            HttpProblem(
                title = "Ugyldig forespørsel",
                status = HttpStatusCode.BadRequest.value,
                type = URI("urn:error:bad_request"),
                detail = cause.message,
                instance = URI(call.request.uri),
            ),
        )
    }
    exception<NotFoundException> { call, cause ->
        call.response.header("Content-Type", ContentType.Application.ProblemJson.toString())
        call.respond(
            HttpStatusCode.NotFound,
            HttpProblem(
                status = HttpStatusCode.NotFound.value,
                title = "Ressurs ikke funnet",
                type = URI("urn:error:not_found"),
                detail = cause.message,
                instance = URI(call.request.uri),
            ),
        )
    }
    exception<Throwable> { call, cause ->
        call.application.log.error(
            "ukjent feil: ${cause.message}. svarer med InternalServerError og en feilmelding i JSON",
            cause,
        )
        call.response.header("Content-Type", ContentType.Application.ProblemJson.toString())
        call.respond(
            HttpStatusCode.InternalServerError,
            HttpProblem(
                status = HttpStatusCode.InternalServerError.value,
                title = "Uventet feil",
                type = URI("urn:error:internal_error"),
                detail = "Uventet feil: ${cause.message}",
                instance = URI(call.request.uri),
            ),
        )
    }
    status(*allStatusCodes.filterNot { code -> code.isSuccess() }.toTypedArray()) { statusCode ->
        // exhaustive when-block so it will be compiler error if new types are added
        when (content) {
            is OutgoingContent.NoContent -> {
                call.response.header("Content-Type", ContentType.Application.ProblemJson.toString())
                call.respond(
                    statusCode,
                    HttpProblem(
                        status = statusCode.value,
                        title = statusCode.description,
                        type = statusCode.toURI(call),
                        detail = statusCode.description,
                        instance = URI(call.request.uri),
                    ),
                )
            }

            is OutgoingContent.ByteArrayContent,
            is OutgoingContent.ContentWrapper,
            is OutgoingContent.ProtocolUpgrade,
            is OutgoingContent.ReadChannelContent,
            is OutgoingContent.WriteChannelContent,
            -> {
                // do nothing
            }
        }
    }
}

private fun HttpStatusCode.toURI(call: ApplicationCall): URI {
    val type =
        try {
            description.lowercase().replace("\\s+".toRegex(), "_")
        } catch (_: Exception) {
            call.application.log.error("klarte ikke lage uri fra httpstatuscode=$this")
            "unknown_error"
        }
    return URI("urn:error:$type")
}
