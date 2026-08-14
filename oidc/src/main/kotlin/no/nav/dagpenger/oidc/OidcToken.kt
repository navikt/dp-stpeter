package no.nav.dagpenger.oidc

import com.auth0.jwt.JWT
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Mer info om tilgjengelige claims i token:
 * https://docs.nais.io/auth/entra-id/reference/#claims
 **/
private const val OID = "oid"
private const val IDTYP = "idtyp"
private const val APP = "app"
private const val NAV_IDENT = "NAVident"

class OidcToken(
    accessToken: String,
) {
    private val accessToken = JWT.decode(accessToken)

    fun token(): String = accessToken.token

    fun expires(): LocalDateTime = LocalDateTime.ofInstant(accessToken.expiresAt.toInstant(), ZoneId.systemDefault())

    fun isNotExpired(): Boolean {
        val now = LocalDateTime.now().plusSeconds(30)
        return now.isBefore(expires())
    }

    /**
     * Sjekker om token er et systembruker-token (client credentials)
     **/
    fun isClientCredentials(): Boolean {
        val subject = accessToken.subject

        // Sjekker både gammel konvensjon (oid=sub) og nyere (idtyp="app")
        return subject == accessToken.getClaim(OID).asString() ||
            APP == accessToken.getClaim(IDTYP).asString()
    }

    /**
     * Returnerer NAVident for personbruker-token
     *
     * @throws IllegalStateException hvis token tilhører systembruker
     **/
    fun navIdent(): String {
        check(!isClientCredentials()) { "Kan kun hente NAVident for personbruker" }

        return accessToken.getClaim(NAV_IDENT).asString()
    }
}
