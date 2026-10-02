# dp-stpeter

Applikasjon for å sjekke om saksbehandler har tilgang til en person.

## Komme i gang

Gradle brukes som byggverktøy og er bundlet inn.

`./gradlew build`

## Struktur

| Modul                                        | Beskrivelse                                                                    |
|----------------------------------------------|--------------------------------------------------------------------------------|
| [`mediator`](mediator)                       | Ktor-applikasjonen `dp-stpeter`, deployes til Nais                             |
| `konfigurasjon`                              | Delt konfigurasjonslasting                                                     |
| `oidc`                                       | Hjelpetype for å tolke Azure AD-token                                          |
| `tilgangsmaskin`                             | Typer og feil for integrasjon mot tilgangsmaskin (populasjonstilgangskontroll) |
| `openapi`                                    | OpenAPI-spesifikasjon for `dp-stpeter`-APIet                                   |
| [`stpeter-plugin`](stpeter-plugin)           | Klientbibliotek  å kalle `dp-stpeter`                                          |
| [`stpeter-plugin-test`](stpeter-plugin-test) | Testverktøy for konsumenter av `stpeter-plugin`                                |

## Henvendelser

Spørsmål knyttet til koden kan rettes til:

* #team-dagpenger-bjoa-aam på Slack

## Lisens

[MIT](LICENSE.md)
