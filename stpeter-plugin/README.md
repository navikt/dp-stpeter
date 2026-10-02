# stpeter-plugin

Klientbibliotek for å sjekke om innlogget saksbehandler har tilgang til en person, via `dp-stpeter`-APIet.

## Installasjon

```kotlin
dependencies {
    implementation("no.nav.dagpenger:stpeter-plugin:{version}")
}
```

Pakken publiseres til GitHub Packages: `https://maven.pkg.github.com/navikt/dp-stpeter`.
Se [Bruk hos konsument](#bruk-hos-konsument) for oppsett av repository og autentisering.

## Bruk

```kotlin
val stPeter = StPeterPlugin(
    config = StPeterConfig(), // leser STPETER_URL, STPETER_SCOPE og Azure AD-oppsett fra miljøvariabler
)

stPeter.vedTilgangTilPerson(
    ident = fødselsnummer,
    token = call.request.headers["Authorization"]!!.removePrefix("Bearer "),
) {
    // Kjøres kun hvis saksbehandler har tilgang
    call.respond(HttpStatusCode.OK, sak)
}
```

`StPeterPlugin` gjør selv OBO-bytte av det innkommende tokenet til et token med riktig
`audience`/scope for `dp-stpeter`, før kallet sendes. Konsumenten trenger altså ikke
gjøre token-bytte selv eller risikere å sende feil (ubyttet) token videre.

Hvis saksbehandler ikke har tilgang, eller ikke finnes i tilgangsmaskin, kaster
`vedTilgangTilPerson` en `TilgangAvvistException` med `status`, `title`, `detail`
og (der relevant) `traceId`/`kanOverstyres` fra `dp-stpeter`.

🔴 Rød sone: OBO-bytte og videresending av `Authorization`-header er sikkerhetskritisk
kode — forstå hvordan `StPeterPlugin` bygger og bruker tokenet før du endrer det.

### Feilhåndtering med StatusPages

`TilgangAvvistException` bør mappes til et `application/problem+json`-svar. Legg til
en `exception<TilgangAvvistException>`-handler i `StatusPages`-konfigurasjonen. `HttpProblem`
er typisk en type generert fra appens egen OpenAPI-spesifikasjon (RFC 7807), ikke noe
`stpeter-plugin` leverer selv:

```kotlin
install(StatusPages) {
    exception<TilgangAvvistException> { call, cause ->
        call.response.header("Content-Type", ContentType.Application.ProblemJson.toString())
        call.respond(
            cause.status,
            HttpProblem(
                status = cause.status.value,
                title = cause.title,
                type = cause.type,
                detail = cause.detail,
                instance = cause.instance,
                properties =
                    mutableMapOf<String, Any>().apply {
                        cause.traceId?.let { put("traceId", it) }
                        cause.kanOverstyres?.let { put("kanOverstyres", it) }
                    },
            ),
        )
    }
}
```

## API

### `StPeterPlugin(config)`

| Parameter | Type | Beskrivelse |
|-----------|------|-------------|
| `config` | `StPeterConfig` | URL/scope til `dp-stpeter` og Azure AD-oppsett brukt til OBO-bytte internt i pluginet. Default leser alt fra miljøvariabler eller systemegenskaper |

### `suspend fun vedTilgangTilPerson(ident, token, vedTilgangBlock)`

| Parameter | Type | Beskrivelse |
|-----------|------|-------------|
| `ident` | `String` | Fødselsnummer eller D-nummer, 11 siffer |
| `token` | `String` | Det innkommende Azure AD-tokenet til saksbehandleren (byttes internt til et OBO-token før kall til `dp-stpeter`) |
| `vedTilgangBlock` | `suspend () -> Unit` | Kjøres kun hvis saksbehandler har tilgang til personen |

Kaster `TilgangAvvistException` ved manglende tilgang (403), ukjent NAVident (404),
eller uventet svar fra `dp-stpeter` (annen statuskode).

## Konfigurasjon

| Variabel | Beskrivelse | Påkrevd |
|----------|-------------|---------|
| `STPETER_URL` | Base-URL til `dp-stpeter`, f.eks. `http://dp-stpeter.teamdagpenger` | Ja |
| `STPETER_SCOPE` | Scope brukt ved OBO-bytte, f.eks. `api://[cluster].teamdagpenger.dp-stpeter/.default` | Ja |
| `AZURE_APP_CLIENT_ID` | Klient-ID til appens egen Azure AD-registrering | Ja |
| `AZURE_APP_CLIENT_SECRET` | Klienthemmelighet til Azure AD-registreringen. Behandles som sensitivt: `StPeterConfig.toString()` maskerer denne | Ja |
| `AZURE_APP_JWK` | Privat JWK for appen (krevd av `StPeterConfig`, brukes ikke av klientsekret-flyten pluginet bruker i dag) | Ja |
| `AZURE_APP_WELL_KNOWN_URL` | Well-known-URL til Azure AD | Ja |
| `AZURE_OPENID_CONFIG_TOKEN_ENDPOINT` | Token-endepunkt til Azure AD | Ja |

I NAIS-miljø settes Azure AD-variablene automatisk når appen har `azure: { application: { enabled: true } }` konfigurert.

## Bruk hos konsument

Legg til GitHub Packages som maven-kilde og autentiser med et token som har `read:packages`:

```kotlin
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/navikt/dp-stpeter")
        credentials {
            username = providers.gradleProperty("githubUser").orNull
            password = providers.gradleProperty("githubPassword").orNull
        }
    }
}
```

## Testing

For å skrive tester mot `StPeterPlugin` uten å kalle en ekte `dp-stpeter`, bruk
[`stpeter-plugin-test`](../stpeter-plugin-test/README.md).

## Team

* **Team:** #team-dagpenger-bjoa-aam på Slack
