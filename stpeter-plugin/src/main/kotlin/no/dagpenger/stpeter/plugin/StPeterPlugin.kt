package no.dagpenger.stpeter.plugin

import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class StPeterPlugin(
    private val oboExchanger: (String) -> String,
    private val config: StPeterConfig = StPeterConfig(),
) {
    private val client: HttpClient =
        HttpClient
            .newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build()

    private val url by lazy { config.url }

    suspend fun vedTilgangTilPerson(
        ident: String,
        token: String,
        vedTilgangBlock: suspend () -> Unit,
    ) {
        val oboToken = oboExchanger(token)
        val request =
            HttpRequest
                .newBuilder()
                .uri(URI.create("$url/api/v1/person"))
                .header("Authorization", "Bearer $oboToken")
                .header("Accept", "application/problem+json")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("""{"ident":"$ident"}"""))
                .build()

        val response =
            withContext(Dispatchers.IO) {
                client.send(request, HttpResponse.BodyHandlers.ofString())
            }

        when (response.statusCode()) {
            HttpStatusCode.Forbidden.value -> {
                throw response.body().tilgangAvvist()
            }

            HttpStatusCode.NoContent.value -> {
                vedTilgangBlock()
            }

            HttpStatusCode.NotFound.value -> {
                throw response.body().tilgangAvvist()
            }

            else -> {
                throw RuntimeException("💣")
            }
        }
    }

    private fun String.tilgangAvvist(): TilgangAvvistException {
        val problem = objectMapper.readValue(this, StPeterProblem::class.java)
        return TilgangAvvistException(
            title = problem.title,
            status = HttpStatusCode.fromValue(problem.status),
            type = URI(problem.type),
            detail = problem.detail,
            instance = URI(problem.instance),
            navIdent = problem.navIdent,
            traceId = problem.traceId,
            kanOverstyres = problem.kanOverstyres,
        )
    }
}
