# Repository guide

## Build and test

Use the Gradle wrapper from the repository root:

```bash
./gradlew build
./gradlew test
./gradlew :mediator:test --tests 'no.nav.dagpenger.stpeter.StPeterApiSpec'
./gradlew :mediator:ktlintCheck
```

The `--tests` example runs one test spec. Replace the module and class to target another spec. Run `ktlintCheck` on the
module you changed. The shared `common` Gradle plugin applies ktlint to Kotlin modules, and Kotlin compilation depends
on `ktlintFormat`, which can reformat source files.

## Architecture

This is a Gradle multi-project Kotlin service. `mediator` is the Ktor application and wires together HTTP routes, Azure
AD authentication, Redis caching, and calls to tilgangsmaskin. `tilgangsmaskin` holds the access-check response types,
client interface, and response handling. `konfigurasjon`, `oidc`, and `logging` provide shared configuration, token, and
logging code. `openapi` contains the API contract and shared API types.

`stpeter-plugin` is the consumer-facing Kotlin client. `stpeter-plugin-test` provides test helpers, including a mock
server. The plugin has a separate publish workflow.

## Project-specific conventions

- The service endpoint is `POST /api/v1/person`. Its authenticated request is checked against the configured
  Saksbehandler group before `TilgangsmaskinResponseService` maps tilgangsmaskin responses to API results. The plugin,
  OpenAPI contract, and mediator tests share this contract; keep them aligned when changing request fields, status
  codes, or access behavior.
- The plugin exchanges the caller's Azure AD token for a downstream token before calling the service. Keep token
  validation, group authorization, request metadata validation, and access decisions consistent with the existing auth
  and API tests. Treat these as security-sensitive changes.
- Outbound HTTP clients use `createHttpClient`, which installs JSON handling, metrics, and request/connect/socket
  timeouts. Follow this pattern for service-to-service calls.
- Cache entries are scoped by the caller's Nav ident, endpoint, and person identifier. Redis entries expire after 60
  minutes. Preserve these key dimensions when changing caching behavior.
- API tests use Kotest `StringSpec` and the test application/auth helpers. Access-machine scenarios are set up through
  `StPeterSystem`; cache tests use the Redis test server. Prefer these helpers when extending integration coverage.
- Use structured logging through the logging module. Do not add raw tokens, secrets, or person identifiers to logs.

For Kotlin test patterns, see `.github/instructions/testing-kotlin.instructions.md`. For security-sensitive Kotlin
changes, see `.github/instructions/security-owasp.instructions.md`.

## Commit messages

When asked to create a commit message, inspect the staged diff and follow `.github/commit-instructions.md`. Write the description and body in Norwegian Bokmål. Keep Conventional Commits types, scopes, and the `BREAKING CHANGE:` footer in their standard English form.

## Purpose of StPeter

StPeter er en proxy/tilgangskontroll-tjeneste som håndterer tilgangssjekk mot tilgangsmaskin for saksbehandlere som
ønsker å se persondata i Arena. Den er laget for å beskytte personopplysninger og sikre at bare autoriserte
saksbehandlere får tilgang til sensitive data. Den håndterer også caching av tilgangssjekkresultater for å redusere belastningen på tilgangsmaskin og forbedre
responstiden for saksbehandlere. StPeter logger også til oppslagslogg (ArcSight) by default.

App'er som ønsker å bruke StPeter gjør dette typisk i API-rutene hvor de skal hente og vise persondata til en
saksbehandler. App'en kan da bruker stpeter-plugin eller implementer kall til StPeter direkte. StPeter håndterer da autentisering, autorisasjon og caching av tilgangssjekkresultater.
