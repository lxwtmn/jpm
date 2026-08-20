# 07 — Zielbestimmung im Reaktor

**What to build:** In einem Multi-Modul-Projekt landet die Dependency im richtigen
Maven-Modul. Vom Arbeitsverzeichnis aus ist das die nächstgelegene pom; im Aggregator-Root
fragt jpm, statt zu raten — ein Eintrag im Aggregator-Pom wäre für alle Maven-Module
sichtbar und ist fast nie gewollt.

**Blocked by:** 05, 06

**Status:** ready-for-agent

- [ ] Der Reaktor wird erkannt: Aggregator-Pom, seine Maven-Module, die Parent-Pom-Beziehung
- [ ] Ziel ist die nächste pom vom Arbeitsverzeichnis aufwärts
- [ ] Im Aggregator-Root mit mehreren Maven-Modulen erscheint eine Auswahl
- [ ] Ohne Terminal und ohne `--module` bricht jpm mit Exit 2 ab und nennt die möglichen Ziele
- [ ] `--module` wählt das Ziel ausdrücklich; ein unbekannter Name führt zu Exit 1
- [ ] `--managed` trägt in das `<dependencyManagement>` des Parent-Poms ein
- [ ] Golden-File-Korpus um Aggregator-Pom und Parent-Pom erweitert
