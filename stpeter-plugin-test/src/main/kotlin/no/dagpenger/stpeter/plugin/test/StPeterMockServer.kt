package no.dagpenger.stpeter.plugin.test

import com.auth0.jwt.JWT
import io.ktor.http.HttpStatusCode
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import java.net.InetAddress

class StPeterMockServer {
    private var stPeterResponseStatus: HttpStatusCode = HttpStatusCode.NoContent

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
                        return when (stPeterResponseStatus) {
                            HttpStatusCode.NoContent -> {
                                MockResponse().setResponseCode(HttpStatusCode.NoContent.value)
                            }

                            HttpStatusCode.Forbidden -> {
                                MockResponse()
                                    .setResponseCode(HttpStatusCode.Forbidden.value)
                                    .setHeader("Content-Type", "application/problem+json")
                                    .setBody(
                                        tilgangResponse(stPeterResponseStatus),
                                    )
                            }

                            HttpStatusCode.NotFound -> {
                                MockResponse()
                                    .setResponseCode(HttpStatusCode.NotFound.value)
                                    .setHeader("Content-Type", "application/problem+json")
                                    .setBody(
                                        tilgangResponse(stPeterResponseStatus),
                                    )
                            }

                            else -> {
                                MockResponse().setResponseCode(HttpStatusCode.InternalServerError.value)
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
        setStPeterResponse(status = HttpStatusCode.NoContent)
        block()
    }

    suspend fun withStPeterDenyAccessToPerson(block: suspend () -> Unit) {
        setStPeterResponse(status = HttpStatusCode.Forbidden)
        block()
    }

    suspend fun withStPeterSaksbehandlerNotFound(block: suspend () -> Unit) {
        setStPeterResponse(status = HttpStatusCode.NotFound)
        block()
    }

    fun setStPeterResponse(status: HttpStatusCode = HttpStatusCode.NoContent) {
        stPeterResponseStatus = status
    }

    private fun tilgangResponse(status: HttpStatusCode): String =
        when (status) {
            HttpStatusCode.Forbidden -> {
                """{"title":"Ingen tilgang","status":403,"type":"urn:error:forbidden","detail":"Du har ikke tilgang til personen.","instance":"http://localhost"}"""
            }

            HttpStatusCode.NotFound -> {
                """{"title":"Person ikke funnet","status":404,"type":"urn:error:not_found","detail":"Personen ble ikke funnet.","instance":"http://localhost"}"""
            }

            else -> {
                ""
            }
        }
}
