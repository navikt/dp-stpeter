# Instruksjoner for commit-meldinger

Skriv commit-meldinger på norsk bokmål. Bruk imperativ i beskrivelsen, for eksempel «legg til», «rett» eller «oppdater». Behold Conventional Commits-type og scope på engelsk og med små bokstaver.

Bruk formatet `<type>(<scope>): <beskrivelse>` når det passer. Hold første linje under 72 tegn, og avslutt den ikke med punktum. Skriv brødtekst og forklaring på norsk. Behold den faste footeren `BREAKING CHANGE:` på engelsk.

Eksempler:

```text
feat(auth): krev saksbehandlergruppe
fix(cache): skill cache-nøkler per endepunkt
docs(api): beskriv personoppslaget
```

Lag meldingen ut fra endringene som faktisk skal committes. Ikke gjett på scope eller legg til detaljer som ikke framgår av diffen.
