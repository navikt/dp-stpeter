---
name: conventional-commit
description: Generer conventional commit-meldinger med Nav-relevante scopes og breaking change-format
license: MIT
metadata:
  domain: general
  tags: git commit conventional-commits changelog
---

# Conventional Commit Skill

Generate Norwegian Bokmål commit messages following the Conventional Commits specification, adapted for Nav projects.

## Format

```
<type>(<scope>): <description>

[optional body]

[optional footer]
```

## Types

| Type | Usage |
|---|---|
| `feat` | New functionality |
| `fix` | Bug fix |
| `docs` | Documentation-only changes |
| `style` | Formatting, semicolons, etc. (no code change) |
| `refactor` | Code that neither fixes a bug nor adds a feature |
| `perf` | Performance changes |
| `test` | Adding or fixing tests |
| `build` | Build system or dependency changes |
| `ci` | CI configuration changes |
| `chore` | Other changes that don't affect code |

## Nav-relevant scopes

```
feat(vedtak): støtt klagevedtak
fix(auth): valider TokenX-token
docs(api): oppdater OpenAPI-spesifikasjonen
refactor(repository): bruk CTE for tydeligere spørringer
test(controller): legg til integrasjonstest med MockOAuth2Server
build(deps): oppgrader Spring Boot
ci(deploy): legg til prod-deploy
perf(db): legg til indeks på bruker_id
chore(nais): oppdater ressursgrenser
```

## Breaking Changes

```
feat(api)!: endre svarformat for vedtaksendepunktet

BREAKING CHANGE: Feltet `vedtakDato` er endret til `opprettetDato`.
Konsumenter må oppdatere innlesingen.
```

## Rules

- First line: max 72 characters
- Use Norwegian Bokmål for the description and body
- Keep the Conventional Commits type and scope in English
- Use imperative form in the description: "legg til", not "la til"
- Don't end with a period
- First line should be at most 72 characters
- Keep the standard `BREAKING CHANGE:` footer in English
- Reference Jira/GitHub issues in the footer when appropriate: `Refs NAV-1234`

## Examples

```bash
# Simple feature
git commit -m "feat(søknad): valider fødselsnummer"

# Bugfix with reference
git commit -m "fix(auth): forny utløpt refresh-token

Forny tokenet når det utløper, slik at brukeren ikke mister
tilgangen uten varsel.

Refs #456"

# Dependency update
git commit -m "build(deps): oppgrader PostgreSQL-driveren til 42.7.4"

# Breaking change
git commit -m "feat(api)!: fjern det utgåtte /api/v1/vedtak-endepunktet

BREAKING CHANGE: /api/v1/vedtak er fjernet. Bruk /api/v2/vedtak."
```

## Analyzing Staged Changes

To generate a commit message, analyze staged changes:

```bash
git diff --cached --stat        # Overview of changed files
git diff --cached               # Detailed diff
```

Based on the diff:
1. Identify **type** (feat/fix/refactor/etc.)
2. Identify **scope** (which module/domain)
3. Write short, precise description
4. Add body if the change needs explanation
5. Add `BREAKING CHANGE` footer if the API changes
