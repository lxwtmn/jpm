# jpm

Kommandozeilenwerkzeug zur Verwaltung von Maven-Dependencies. Die Spezifikation steht in
[DESIGN.md](./DESIGN.md), das Glossar in [CONTEXT.md](./CONTEXT.md), die
Architekturentscheidungen unter [docs/adr/](./docs/adr/).

Stand: noch kein Code. Die Arbeit ist in GitHub Issues geschnitten, jedes eine vertikale
Scheibe mit expliziten Blocking-Kanten.

## Commit-Konventionen

- **Conventional Commits** (`feat:`, `fix:`, `docs:`, `chore:`, `refactor:`, `test:`,
  `build:`, `ci:`)
- **Niemals** ein `Co-Authored-By:`-Trailer

## Agent skills

### Issue tracker

GitHub Issues über die `gh`-CLI; Blocking über native Issue-Dependencies.
See `docs/agents/issue-tracker.md`.

### Triage labels

Das Standardvokabular der fünf kanonischen Rollen.
See `docs/agents/triage-labels.md`.

### Domain docs

Single-context: `CONTEXT.md` und `docs/adr/` im Wurzelverzeichnis.
See `docs/agents/domain.md`.
