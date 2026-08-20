# jpm

Kommandozeilenwerkzeug zur Verwaltung von Maven-Dependencies. Die Spezifikation steht in
[DESIGN.md](./DESIGN.md), das Glossar in [CONTEXT.md](./CONTEXT.md), die
Architekturentscheidungen unter [docs/adr/](./docs/adr/).

Stand: das CLI-Skelett steht (Issue #1). Die weitere Arbeit ist in GitHub Issues
geschnitten, jedes eine vertikale Scheibe mit expliziten Blocking-Kanten.

## Bauen und testen

```
mvn verify
```

Unit-Tests laufen über Surefire, die Integrationstests (`*IT`) über Failsafe **nach** dem
Packaging — sie prüfen das gebaute Fat-JAR und die Launcher, was vor dem Packaging nicht
möglich wäre. `mvn test` allein lässt diese Hälfte aus.

Gebaut wird mit einem aktuellen JDK, das Bytecode-Ziel ist 17 (DESIGN E-1).

## Sprache

- **Äußere Oberfläche englisch:** alles, was ein Nutzer im Terminal zu sehen bekommt —
  Hilfetexte, Fehlermeldungen, Launcher-Ausgaben, `--help`.
- **Interne Artefakte deutsch:** `DESIGN.md`, `CONTEXT.md`, ADRs, Javadoc, Code-Kommentare,
  Testnamen (`@DisplayName`) und Commit-Nachrichten. Der Typ-Präfix bleibt englisch, weil
  Conventional Commits ihn festlegt (`feat:`, `fix:`, …).

Das ist Absicht, kein Versehen: das Werkzeug richtet sich an ein internationales
Java-Publikum, die Entwurfsdokumentation an das Team, das es baut. Die Trennlinie ist
„sieht das ein Nutzer im Terminal?". Kein ADR dafür — es ist eine Konvention, keine
Architekturentscheidung.

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
