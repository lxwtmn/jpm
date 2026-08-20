# jpm

A command-line tool for managing Maven dependencies without editing `pom.xml` by hand:

```
jpm add com.fasterxml.jackson.core:jackson-databind
```

instead of a browser, mvnrepository.com, copy-paste and XML.

The **interface** takes after npm; the **data model** deliberately does not. `pom.xml` remains
the single source of truth, and jpm edits it in place while preserving its formatting. There is
no `jpm.json`, no lock file and no equivalent of `node_modules`.

**Status:** the CLI skeleton is in place — `jpm --version` and `jpm --help` work. The dependency
commands follow along the [issues](https://github.com/lxwtmn/jpm/issues).

```
mvn verify
bin/jpm --version
```

## Documentation

- **[DESIGN.md](./DESIGN.md)** — the full specification: command set, architecture, testing
  strategy, non-goals, implementation order
- **[CONTEXT.md](./CONTEXT.md)** — glossary of domain terms
- **[docs/adr/](./docs/adr/)** — architecture decisions with their rationale

## Licence

[Apache-2.0](./LICENSE) — Copyright 2026 Alexander Wittmann Consulting GmbH.
Bundled dependencies and their notices are listed in [NOTICE](./NOTICE).

## Architecture decisions

| ADR | Decision |
|---|---|
| [0001](./docs/adr/0001-pom-stays-canonical.md) | `pom.xml` stays canonical — jpm is an editor, not an owner |
| [0002](./docs/adr/0002-exact-versions-no-ranges.md) | Exact versions in the POM — no ranges, no `^`/`~` |
| [0003](./docs/adr/0003-effective-model-over-file-text.md) | Decisions are made against the effective model |
| [0004](./docs/adr/0004-java-with-embedded-maven-resolver.md) | Java with an embedded Maven Resolver |
| [0005](./docs/adr/0005-maven-first-gradle-via-version-catalogs.md) | Maven first; Gradle starts at version catalogs |
| [0006](./docs/adr/0006-write-without-confirmation.md) | Writing commands do not ask and leave no backup |
| [0007](./docs/adr/0007-exit-code-contract.md) | Informational commands never fail over their own content |
