# Sikkerhetsgjennomgang før push

Gjennomgangen omfattet endringene i `dp-stpeter` per 7. oktober 2026. `.nais/alerts.yaml` var untracked og kommentert
ut, og inngikk ikke i gjennomgangen.

## Funn

| # | Alvorlighet | Fil                                                                                                                                    | Linjer       | Funn                                                                                                                                                                              | Status                                                                                                                                                                        |
|---|-------------|----------------------------------------------------------------------------------------------------------------------------------------|--------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1 | 🟠 HIGH     | `openapi/src/main/kotlin/no/nav/dagpenger/IdentForesporselUtils.kt`                                                                    | 1-2          | Den staged versjonen manglet hjelpefunksjonene som API-et importerer.                                                                                                             | Fikset: hele fila er staged.                                                                                                                                                  |
| 2 | 🟠 HIGH     | `openapi/src/main/kotlin/no/nav/dagpenger/IdentForesporselUtils.kt`                                                                    | 23-34        | Rutemalvalideringen godtok konkrete tallbaserte segmenter som kunne havne i loggene.                                                                                              | Begrenset: segmenter tillater nå bokstaver, versjonssegmenter som `v1` og `{parameter}`. Bokstavbaserte personopplysninger kan ikke skilles sikkert fra statiske rutetekster. |
| 3 | 🟡 MEDIUM   | `mediator/src/main/kotlin/no/nav/dagpenger/stpeter/StPeterApi.kt`; `mediator/src/main/kotlin/no/nav/dagpenger/api/auth/AuthFactory.kt` | 43-44; 39-45 | `application` valideres for format, men sammenlignes ikke med klientidentiteten i tokenet. En klient kan oppgi et annet appnavn, så oversikten over klienter er ikke autoritativ. | Åpent: bruk som driftsmetadata, eller knytt feltet til identiteten i tokenet.                                                                                                 |

## Avgrensninger

Gjennomgangen leste staged, ustaged og untracked endringer, med unntak av `.nais/alerts.yaml`. Produksjonslogger, koden
til den eksterne konsumenten og deployrekkefølgen ble ikke undersøkt. Automatiske sikkerhetsskannere ble ikke kjørt.

Utvikleren opplyser at tester kjører og at appen bygger. Dette ble ikke verifisert i gjennomgangsmiljøet, der Gradle
ikke fikk koblet til daemonen.

## Vurdering

De to første funnene er rettet eller begrenset etter gjennomgangen. Appnavnet kan fortsatt oppgis feil, og
rutemalvalideringen kan ikke oppdage bokstavbaserte personopplysninger. Konsumenten må sende faste rutemaler uten
personopplysninger. Vurder om det er tilstrekkelig før push.
