package no.nav.dagpenger.tilgangsmaskin

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.RedisTestServer
import no.nav.dagpenger.oidc.OidcToken
import java.net.URI
import java.util.Date

class TilgangsmaskinCacheSpec :
    StringSpec({
        val redis = RedisTestServer()
        lateinit var cache: TilgangsmaskinCache

        beforeSpec {
            redis.start()
            cache = TilgangsmaskinCache(redis.server)
        }

        afterSpec {
            redis.close()
        }

        "skal kunne sette og hente alle tilgangsmaskin responser fra cache" {
            val token = OidcToken(createTestOAuthToken())
            val responses =
                listOf<TilgangsmaskinResponse>(
                    TilgangsmaskinResponse.TilgangGodkjent(
                        harTilgang = true,
                    ),
                    TilgangsmaskinResponse.TilgangAvvist(
                        type = URI("urn:dp:test:tilgangskontroll"),
                        title = "test",
                        status = 403,
                        navIdent = "test",
                        begrunnelse = "test",
                        traceId = "test",
                        kanOverstyres = false,
                    ),
                    TilgangsmaskinResponse.NavIdentIkkeFunnet(
                        detail = "ikke funnet",
                        instance = "/api/v1/ccf/komplett/A222222",
                        status = 404,
                        title = "Uventet respons fra Entra",
                        navident = "A222222",
                    ),
                )

            responses.forEachIndexed { index, expected ->
                val ident = "123$index"
                cache.set(token, ident, expected)

                val response = cache.get(token, ident)

                response shouldBe expected
            }

            cache.cacheHitCount() shouldBe responses.size.toDouble()
        }
    })

fun createTestOAuthToken(
    subject: String = "user-123",
    issuer: String = "test-issuer",
    audience: String = "test-audience",
    secret: String = "super-secret-test-key-minimum-256-bits-long!!",
): String =
    JWT
        .create()
        .withIssuer(issuer)
        .withAudience(audience)
        .withSubject(subject)
        .withClaim("NAVident", "navident") // OAuth2 scopes
        .withIssuedAt(Date())
        .withExpiresAt(Date(System.currentTimeMillis() + 3600000)) // 1 hour expiry
        .sign(Algorithm.HMAC256(secret))
