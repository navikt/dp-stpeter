# Dataflytdiagram for trusselmodellering

Diagrammet viser dataflyten for `POST /api/v1/person` i prod. `stpeter-plugin` kjører inne i konsumentappen. Stiplede rammer viser tillitsgrenser, og pilene beskriver dataene som krysser dem.

```mermaid
flowchart LR
    Saksbehandler["Saksbehandler"]

    subgraph Konsument["Konsumentens kjøreflate"]
        App["Konsumentapp"]
        Plugin("stpeter-plugin")
        App -->|"Ident, brukerens token, application, originRoute"| Plugin
    end

    subgraph Entra["Microsoft Entra ID / Azure AD"]
        EntraToken["Token-endepunkt"]
        EntraJwks["OpenID-konfigurasjon og JWKS"]
    end

    subgraph Nais["NAIS prod-gcp, namespace teamdagpenger"]
        Ingress["Intern ingress / Wonderwall"]
        API("dp-stpeter Ktor API<br/>JWT-validering og gruppesjekk")
        Cache[("Valkey / Redis<br/>cachet tilgangssvar")]
        Audit("Oppslagslogg<br/>CEF-audit-hendelse")
        Telemetry("NAIS logging<br/>Loki og Elastic")
    end

    subgraph Tilgangsflate["NAIS, namespace tilgangsmaskin"]
        Tilgang("populasjonstilgangskontroll")
    end

    Saksbehandler -->|"Starter personoppslag"| App

    Plugin -->|"OBO token exchange<br/>brukerens token, scope for dp-stpeter"| EntraToken
    EntraToken -->|"Token for dp-stpeter"| Plugin
    Plugin -->|"HTTPS: ident, metadata, bearer-token"| Ingress
    Ingress -->|"Videreformidler forespørsel"| API

    API -->|"Henter signeringsnøkler ved behov"| EntraJwks
    EntraJwks -->|"Issuer og JWKS"| API

    API -->|"Cacheoppslag: Nav-ident, endepunkt, ident"| Cache
    Cache -->|"Tilgangssvar ved cache-treff"| API
    API -->|"Kun ved cache-miss: lagrer tilgangssvar, TTL 60 min"| Cache

    API -->|"OBO token exchange<br/>brukerens token, scope for tilgangsmaskin"| EntraToken
    EntraToken -->|"Token for tilgangsmaskin"| API
    API -->|"HTTP internt i clusteret: ident, bearer-token"| Tilgang
    Tilgang -->|"Tilgangsbeslutning eller feil"| API

    API -->|"Ved godkjent tilgang og oppslagslogg=true:<br/>Nav-ident, ident, appnavn, callId, beslutning"| Audit
    Audit -->|"CEF til applikasjonslogg"| Telemetry
    API -->|"Applikasjonslogger med Nav-ident og metadata,<br/>målinger uten personidentifikator"| Telemetry

    API -->|"204 eller problem-svar"| Ingress
    Ingress -->|"HTTPS-svar"| Plugin
    Plugin -->|"Kjører blokk eller kaster tilgangsunntak"| App

    classDef sensitive fill:#ffe0e0,stroke:#a00,color:#111
    classDef identity fill:#e5e5ff,stroke:#336,color:#111
    classDef store fill:#fff2cc,stroke:#997300,color:#111
    class EntraToken,EntraJwks identity
    class Cache,Audit,Telemetry store
    class Plugin,API,Ingress,App,Saksbehandler sensitive
    style Konsument stroke:#a00,stroke-dasharray:5 5
    style Entra stroke:#336,stroke-dasharray:5 5
    style Nais stroke:#a00,stroke-dasharray:5 5
    style Tilgangsflate stroke:#997300,stroke-dasharray:5 5
```

## Data i flyten

| Data | Klassifisering | Hvor dataene går |
|---|---|---|
| Fødselsnummer eller annen personidentifikator | Strengt fortrolig | Konsumentapp, plugin, API, tilgangsmaskin og cache-nøkkel |
| Saksbehandlerens Nav-ident | Fortrolig | Token-claims, cache-nøkkel og oppslagslogg |
| Tilgangsbeslutning og svar fra tilgangsmaskin | Fortrolig | API, cache og konsumentapp som problem-svar ved avslag |
| Bearer-token og OBO-token | Hemmelig autentiseringsdata | Plugin/API, Azure AD og tilgangsmaskin; skal ikke logges eller caches |
| `application`, `originRoute` og `callId` | Intern metadata | Konsumentapp og API logger metadata; oppslagsloggen tar med appnavn og callId |

## Tillitsgrenser og merknader

- Konsumentappen må være eksplisitt tillatt av ingressens og NAIS `accessPolicy`-regler. Kontroller prod-reglene i `.nais/nais.yaml` før du bruker diagrammet som grunnlag for godkjenning.
- API-et validerer signatur, issuer, audience og saksbehandlergruppe før det behandler forespørselen.
- Kallet fra `dp-stpeter` til tilgangsmaskin bruker HTTP i clusteret, slik `TILGANGSMASKIN_API_URL` i manifestet angir. Behandle dette som en nettverkstillitsgrense, og bekreft transportbeskyttelsen mot gjeldende plattformkrav.
- Cache-nøkkelen inneholder Nav-ident og personidentifikator. Cacheinnholdet har tilgangssvar og utløper etter 60 minutter.
- Oppslagsloggen registrerer bare godkjente oppslag når konsumenten ber om det. CEF-hendelsen inneholder Nav-ident, personidentifikator, appnavn, callId og beslutningen «Permit». Applikasjonsloggene inneholder Nav-ident og forespørselsmetadata.
- `allowAllUsers: true` i Azure-oppsettet erstatter ikke API-ets saksbehandlergruppesjekk.
