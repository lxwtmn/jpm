# jpm

Ein Kommandozeilenwerkzeug, das Maven-Dependencies verwaltet, ohne dass man `pom.xml` von
Hand editiert:

```
jpm add com.fasterxml.jackson.core:jackson-databind
```

statt Browser, mvnrepository.com, Copy-Paste und XML.

Die **Bedienung** ist an npm angelehnt, das **Datenmodell** ausdrücklich nicht: die
`pom.xml` bleibt die einzige Wahrheit, jpm editiert sie formaterhaltend in-place. Es gibt
kein `jpm.json`, kein Lockfile und kein `node_modules`-Äquivalent.

**Status:** Das CLI-Skelett steht — `jpm --version` und `jpm --help` laufen. Die
Dependency-Befehle folgen entlang der [Issues](https://github.com/lxwtmn/jpm/issues).

```
mvn verify
bin/jpm --version
```

## Dokumentation

- **[DESIGN.md](./DESIGN.md)** — vollständige Spezifikation: Befehlssatz, Architektur,
  Teststrategie, Nicht-Ziele, Umsetzungsreihenfolge
- **[CONTEXT.md](./CONTEXT.md)** — Glossar der Domänenbegriffe
- **[docs/adr/](./docs/adr/)** — Architekturentscheidungen mit Begründung

## Lizenz

[Apache-2.0](./LICENSE) — Copyright 2026 Alexander Wittmann Consulting GmbH.
Gebündelte Abhängigkeiten und ihre Hinweise siehe [NOTICE](./NOTICE).

## Architekturentscheidungen

| ADR | Entscheidung |
|---|---|
| [0001](./docs/adr/0001-pom-bleibt-kanonisch.md) | `pom.xml` bleibt kanonisch — jpm ist Editor, nicht Besitzer |
| [0002](./docs/adr/0002-exakte-versionen-keine-ranges.md) | Exakte Versionen in der pom — keine Ranges, kein `^`/`~` |
| [0003](./docs/adr/0003-effektives-modell-statt-dateitext.md) | Entscheidungen fallen gegen das effektive Modell |
| [0004](./docs/adr/0004-java-mit-eingebettetem-maven-resolver.md) | Java mit eingebettetem Maven Resolver |
| [0005](./docs/adr/0005-maven-zuerst-gradle-ueber-version-catalogs.md) | Maven zuerst; Gradle beginnt bei Version Catalogs |
| [0006](./docs/adr/0006-schreiben-ohne-bestaetigung.md) | Schreibende Befehle fragen nicht und legen kein Backup an |
| [0007](./docs/adr/0007-exit-code-vertrag.md) | Informationsbefehle scheitern nie an ihrem eigenen Inhalt |
