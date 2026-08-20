# jpm

A command-line tool for managing Maven dependencies. The specification lives in
[DESIGN.md](./DESIGN.md), the glossary in [CONTEXT.md](./CONTEXT.md), the architecture
decisions under [docs/adr/](./docs/adr/).

Status: the CLI skeleton is in place (issue #1). The remaining work is cut into GitHub
issues, each a vertical slice with explicit blocking edges.

## Language

**Everything in this repository is written in English** — user-facing output, documentation,
glossary, ADRs, Javadoc, code comments, test display names and commit messages alike. There
is no split between an outward surface and internal artefacts.

## Build and test

```
mvn verify
```

Unit tests run under Surefire; the integration tests (`*IT`) run under Failsafe **after**
packaging, because they exercise the built fat JAR and the launcher scripts, which does not
exist earlier. `mvn test` alone skips that half.

Built with a current JDK; the bytecode target is 17 (DESIGN E-1).

## Commit conventions

- **Conventional Commits** (`feat:`, `fix:`, `docs:`, `chore:`, `refactor:`, `test:`,
  `build:`, `ci:`)
- **Never** a `Co-Authored-By:` trailer

## Agent skills

### Issue tracker

GitHub Issues via the `gh` CLI; blocking edges use native issue dependencies.
See `docs/agents/issue-tracker.md`.

### Triage labels

The standard vocabulary of the five canonical roles.
See `docs/agents/triage-labels.md`.

### Domain docs

Single context: `CONTEXT.md` and `docs/adr/` at the repository root.
See `docs/agents/domain.md`.
