package no.nav.dagpenger.tilgangsmaskin

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.jackson3.JacksonConverter
import no.nav.dagpenger.objectMapper

class TilgangsmaskinSystem(
    val oppsett: ScenarioOptions,
) {
    companion object {
        fun nyttScenario(block: ScenarioOptions.() -> Unit = { }) = ScenarioOptions().apply(block)
    }

    init {
    }

    val mockEngine =
        MockEngine { request ->
            respond(
                content = oppsett.content,
                status = oppsett.status,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

    val httpClient =
        HttpClient(mockEngine) {
            install(ContentNegotiation) {
                register(ContentType.Application.Json, JacksonConverter(objectMapper))
            }
        }

    class ScenarioOptions(
        var content: String = "",
        var status: HttpStatusCode = HttpStatusCode.NoContent,
    ) {
        inline fun test(crossinline block: TilgangsmaskinSystem.() -> Unit) {
            val test = TilgangsmaskinSystem(this@ScenarioOptions)
            test.block()
        }
    }
}
