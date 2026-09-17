package no.dagpenger.stpeter.plugin.test

import com.auth0.jwt.JWT
import io.ktor.http.HttpStatusCode
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import java.net.InetAddress

class StPeterMockServer {
    private val defaultHttpStatusCode: HttpStatusCode = HttpStatusCode(418, "I'm a teapot")
    private var responseStatus: HttpStatusCode = defaultHttpStatusCode

    val scope = "test.teamdagpenger.dp-stpeter"
    private val mockStPeterServer: MockWebServer by lazy {
        MockWebServer().apply {
            dispatcher =
                object : Dispatcher() {
                    override fun dispatch(request: RecordedRequest): MockResponse {
                        request.headers["Authorization"]?.let { authHeader ->
                            if (!authHeader.startsWith("Bearer ")) {
                                return MockResponse().setResponseCode(HttpStatusCode.Unauthorized.value)
                            }

                            val token = JWT.decode(authHeader.removePrefix("Bearer ").trim())
                            token.audience?.firstOrNull { it == scope }
                                ?: return MockResponse().setResponseCode(HttpStatusCode.Unauthorized.value)
                        } ?: return MockResponse().setResponseCode(HttpStatusCode.Unauthorized.value)
                        return when (responseStatus) {
                            HttpStatusCode.NoContent -> {
                                MockResponse().setResponseCode(HttpStatusCode.NoContent.value)
                            }

                            HttpStatusCode.Forbidden -> {
                                MockResponse()
                                    .setResponseCode(HttpStatusCode.Forbidden.value)
                                    .setHeader("Content-Type", "application/problem+json")
                                    .setBody(
                                        tilgangResponse(responseStatus),
                                    )
                            }

                            HttpStatusCode.NotFound -> {
                                MockResponse()
                                    .setResponseCode(HttpStatusCode.NotFound.value)
                                    .setHeader("Content-Type", "application/problem+json")
                                    .setBody(
                                        tilgangResponse(responseStatus),
                                    )
                            }

                            else -> {
                                MockResponse()
                                    .setResponseCode(responseStatus.value)
                                    .setHeader("Content-Type", "application/problem+json")
                                    .setBody(tilgangResponse(defaultHttpStatusCode))
                            }
                        }
                    }
                }
        }
    }

    fun url(): String = mockStPeterServer.url("/").toString().removeSuffix("/")

    fun start(
        inetAddress: InetAddress = InetAddress.getByName("localhost"),
        port: Int = 0,
    ) {
        mockStPeterServer.start(inetAddress, port)
    }

    fun shutdown() {
        mockStPeterServer.shutdown()
    }

    suspend fun withStPeterAllowAccessToPerson(block: suspend () -> Unit) {
        responseStatus = HttpStatusCode.NoContent
        block()
        resetResponse()
    }

    suspend fun withStPeterDenyAccessToPerson(block: suspend () -> Unit) {
        responseStatus = HttpStatusCode.Forbidden
        block()
        resetResponse()
    }

    suspend fun withStPeterSaksbehandlerNotFound(block: suspend () -> Unit) {
        responseStatus = HttpStatusCode.NotFound
        block()
        resetResponse()
    }

    suspend fun withStPeterResponse(
        status: HttpStatusCode,
        block: suspend () -> Unit,
    ) {
        responseStatus = status
        block()
        resetResponse()
    }

    private fun resetResponse() {
        responseStatus = defaultHttpStatusCode
    }

    private fun tilgangResponse(status: HttpStatusCode): String =
        when (status) {
            HttpStatusCode.Forbidden -> {
                // language=json
                """
                {
                  "title": "Ingen tilgang",
                  "status": 403,
                  "type": "urn:error:forbidden",
                  "detail": "Du har ikke tilgang til personen.",
                  "instance": "http://localhost"
                }
                """.trimIndent()
            }

            HttpStatusCode.NotFound -> {
                // language=json
                """
                {
                  "title": "Person ikke funnet",
                  "status": 404,
                  "type": "urn:error:not_found",
                  "detail": "Personen ble ikke funnet.",
                  "instance": "http://localhost"
                }
                """.trimIndent()
            }

            defaultHttpStatusCode -> {
                // language=json
                """
                {
                  "title": "I'm a teapot",
                  "status": 418,
                  "type": "urn:error:im_a_teapot",
                  "detail": "Bob's not your uncle.",
                  "instance": "http://localhost"
                }
                """.trimIndent()
            }

            else -> {
                ""
            }
        }
}
