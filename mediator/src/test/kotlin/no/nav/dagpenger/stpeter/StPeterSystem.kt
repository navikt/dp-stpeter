package no.nav.dagpenger.stpeter

import com.natpryce.konfig.ConfigurationMap
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.jackson3.JacksonConverter
import io.ktor.server.application.Application
import no.nav.dagpenger.TestApplication.AZURE_AD_ISSUER_ID
import no.nav.dagpenger.TestApplication.CLIENT_ID
import no.nav.dagpenger.TestApplication.SAKSBEHANDLER_GRUPPE
import no.nav.dagpenger.TestApplication.mockOAuth2Server
import no.nav.dagpenger.TestApplication.tokenProvider
import no.nav.dagpenger.api.auth.AuthFactory
import no.nav.dagpenger.api.auth.AuthFactory.azure_app
import no.nav.dagpenger.konfigurasjon.Configuration
import no.nav.dagpenger.objectMapper
import no.nav.dagpenger.tilgangsmaskin.TilgangsmaskinClient

class StPeterSystem(
    val oppsett: ScenarioOptions,
) {
    companion object {
        fun godkjentScenario() =
            ScenarioOptions(
                content = "",
                status = HttpStatusCode.NoContent,
            )

        fun avvisScenario() =
            ScenarioOptions(
                // language=json
                content =
                    """
                    {
                      "type": "https://confluence.adeo.no/display/TM/Tilgangsmaskin+API+og+regelsett",
                      "title": "AVVIST_STRENGT_FORTROLIG_ADRESSE",
                      "status": 403,
                      "instance": "Z990883/03508331575",
                      "brukerIdent": "03508331575",
                      "navIdent": "Z990883",
                      "traceId": "444290be30ed4fdd9a849654bad9dc1b",
                      "begrunnelse": "Du har ikke tilgang til brukere med strengt fortrolig adresse",
                      "kanOverstyres": false
                    }
                    """.trimIndent(),
                status = HttpStatusCode.Unauthorized,
            )

        fun navIdentIkkeFunnetScenario() =
            ScenarioOptions(
                // language=json
                content =
                    """
                    {
                      "detail": "Fant ingen oid for navident A222222, er den fremdeles gyldig?",
                      "instance": "/api/v1/ccf/komplett/A222222",
                      "status": 404,
                      "title": "Uventet respons fra Entra",
                      "navident": "A222222"
                    }
                    """.trimIndent(),
                status = HttpStatusCode.NotFound,
            )
    }

    private val mockEngine =
        MockEngine { _ ->
            respond(
                content = oppsett.content,
                status = oppsett.status,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

    private val httpClient =
        HttpClient(mockEngine) {
            install(ContentNegotiation) {
                register(ContentType.Application.Json, JacksonConverter(objectMapper))
            }
        }

    private val authFactory =
        AuthFactory(
            ConfigurationMap(
                mapOf(
                    Configuration.Grupper.saksbehandler.name to SAKSBEHANDLER_GRUPPE,
                    azure_app.client_id.name to CLIENT_ID,
                    azure_app.well_known_url.name to "${
                        mockOAuth2Server.wellKnownUrl(
                            AZURE_AD_ISSUER_ID,
                        )
                    }",
                ),
            ),
        )

    private val tilgangsmaskinClient =
        TilgangsmaskinClient(
            tilgangsMaskinApiUrl = "http://localhost",
            tokenProvider = tokenProvider.oboExchanger,
            httpClient = httpClient,
        )

    val api: Application.() -> Unit = { stpeterApi(authFactory, tilgangsmaskinClient) }

    class ScenarioOptions(
        var content: String = "",
        var status: HttpStatusCode = HttpStatusCode.NoContent,
    ) {
        inline fun test(crossinline block: StPeterSystem.() -> Unit) {
            val test = StPeterSystem(this@ScenarioOptions)
            test.block()
        }
    }
}
