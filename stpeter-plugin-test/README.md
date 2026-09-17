# stpeter-plugin-test

Testverktøy for konsumenter av [`stpeter-plugin`](../stpeter-plugin). Gir en mock av
`dp-stpeter` (via MockWebServer) og en mock av Azure AD (via `MockOAuth2Server`), slik
at man kan teste tilgangssjekker uten å kalle en ekte `dp-stpeter`-instans.

## Installasjon

```kotlin
dependencies {
    testImplementation("no.nav.dagpenger:stpeter-plugin-test:{version}")
}
```

## Bruk

```kotlin
class MinTjenesteSpec : StringSpec({
    val stPeterMock = StPeterWithOAuthMock()
    lateinit var stPeter: StPeterPlugin

    beforeSpec {
        stPeterMock.start()
        stPeter = StPeterPlugin(
            config = StPeterConfig(config = stPeterMock.config()),
        )
    }

    afterSpec {
        stPeterMock.shutdown()
    }

    "skal gi tilgang når saksbehandler har tilgang" {
        stPeterMock.withStPeterAllowAccessToPerson {
            stPeter.vedTilgangTilPerson(
                ident = "12345678901",
                token = stPeterMock.issueToken(),
            ) {
                // forventet oppførsel ved tilgang
            }
        }
    }
})
```

## API

### `StPeterWithOAuthMock`

| Metode | Beskrivelse |
|--------|-------------|
| `start()` / `shutdown()` | Starter/stopper mock-serverne (`MockWebServer` + `MockOAuth2Server`) |
| `config()` | `Map<String, String>` med `STPETER_URL`, `STPETER_SCOPE` og Azure AD-konfigurasjon (klient-ID/-hemmelighet, well-known-URL osv.), til bruk i `StPeterConfig` |
| `issueToken(issuerId, audience, claims)` | Utsteder et gyldig testtoken |
| `withStPeterAllowAccessToPerson { }` | Simulerer at `dp-stpeter` svarer med tilgang (204) |
| `withStPeterDenyAccessToPerson { }` | Simulerer avvist tilgang (403) |
| `withStPeterSaksbehandlerNotFound { }` | Simulerer at NAVident ikke finnes (404) |
| `withStPeterResponse(status) { }` | Simulerer en vilkårlig statuskode fra `dp-stpeter`, f.eks. for å teste uventede svar |

Mock-serveren validerer at kall til `dp-stpeter` faktisk har en `Bearer`-prefikset
`Authorization`-header med riktig `audience`, slik at tester fanger opp feil i
OBO-token-bytte hos konsumenten.

## Team

* **Team:** #team-dagpenger-bjoa-aam på Slack
