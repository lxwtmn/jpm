# jpm — Design

Stand: 2026-08-20. Ergebnis einer strukturierten Design-Befragung in vier Runden.

Dieses Dokument ist die **Spezifikation**: was jpm tut, wie es aufgebaut ist, was es
ausdrücklich nicht tut. Das **Warum** der schwer umkehrbaren Entscheidungen steht in
[docs/adr/](./docs/adr/) und wird hier nicht wiederholt — jede Begründung soll genau einen
Ort haben. Die Begriffe folgen [CONTEXT.md](./CONTEXT.md).

---

## 1. Was jpm ist

Ein CLI, das Maven-Dependencies verwaltet, ohne dass man `pom.xml` von Hand editiert.
Vorbild ist npm — **nur in der Bedienung, nicht im Datenmodell**.

v1 löst zwei Schmerzpunkte:

1. Koordinate und aktuelle Version finden (kein mvnrepository.com mehr)
2. Den pom-Eintrag korrekt setzen (kein XML-Editieren von Hand)

Updates (`outdated` / `update`) folgen unmittelbar danach.

Zielgruppe: eigene Firma/Team. Ein späterer OSS-Release wird offengehalten, treibt aber
keine v1-Entscheidung. Das erzwingt früh private Artefakt-Repositories, Auth, Multi-Modul-
und Parent-Pom-Fälle — also genau das, was ein OSS-Release ohnehin bräuchte, ohne schon
Support-Last zu tragen.

---

## 2. Grundprinzipien

| | Prinzip | Begründung |
|---|---|---|
| **P1** | Die pom gehört dem Nutzer. jpm ist Editor, nicht Besitzer. | [ADR-0001](./docs/adr/0001-pom-bleibt-kanonisch.md) |
| **P2** | In der pom stehen immer exakte Versionen. | [ADR-0002](./docs/adr/0002-exakte-versionen-keine-ranges.md) |
| **P3** | Maßgeblich ist das effektive Modell, nicht der Dateitext. | [ADR-0003](./docs/adr/0003-effektives-modell-statt-dateitext.md) |
| **P4** | Nichts still tun, was der Nutzer nicht bemerken würde. | siehe unten |
| **P5** | Fremder Zustand wird nicht verändert. | siehe unten |
| **P6** | Skriptbarkeit ist ein Vertrag, kein Nachgedanke. | [ADR-0007](./docs/adr/0007-exit-code-vertrag.md) |

**Zu P4:** Scopes werden vorgeschlagen, nie stillschweigend gesetzt. Verwaltete
Dependencies werden nie stillschweigend übersteuert. Jeder Schreibvorgang berichtet
danach präzise, was wohin geschrieben wurde und woher die Version stammt.

**Zu P5:** jpm liest `~/.m2` und `settings.xml`, schreibt aber niemals hinein. Eigener
Cache, eigenes Verzeichnis. Maven-Zustand bleibt unangetastet.

---

## 3. Nicht-Ziele

Bewusst ausgeschlossen. Diese Liste ist so wichtig wie der Funktionsumfang.

| Nicht-Ziel | Warum |
|---|---|
| Lockfile / Reproduzierbarkeitsgarantie | Exakte Versionen (P2) leisten das bereits ausreichend |
| Konfliktauflösung, `<exclusion>`-Verwaltung, `why` / Dependency-Tree | Eigenes Produkt; `mvn dependency:tree` existiert |
| Task-Runner (`jpm run`) | Anderes Tool, das nur zufällig denselben Namen tragen könnte |
| Artefakte herunterladen | Nur optional per `--fetch`; der nächste `mvn`-Lauf tut es ohnehin |
| Schreiben in `~/.m2` | P5 |
| Eigene Konfigurationsdatei | `settings.xml` ist die einzige Quelle für Artefakt-Repositories, Auth, Mirrors, Proxy. Eine zweite Konfigurationsquelle ist eine zweite Fehlerquelle |
| Projekt-Scaffolding (`jpm init`) | Maven-Archetypes existieren |
| Gradle in v1 | [ADR-0005](./docs/adr/0005-maven-zuerst-gradle-ueber-version-catalogs.md) |
| `pom.xml` generieren | [ADR-0001](./docs/adr/0001-pom-bleibt-kanonisch.md) |

---

## 4. Entscheidungen

### B — Datenmodell

- **B-1** `pom.xml` bleibt kanonisch; jpm editiert formaterhaltend in-place.
  → [ADR-0001](./docs/adr/0001-pom-bleibt-kanonisch.md)

- **B-2 Ablageform.** Inline-`<version>` oder `<properties>`: **die Konvention des
  Projekts wird erkannt und fortgeführt** (Mehrheitsentscheid über bestehende Einträge),
  übersteuerbar per `--property` / `--inline`.
  Property-Benennung: `<artifactId>.version` als Default — **aber** existiert bereits eine
  Property, deren Wert die Version eines Artefakts derselben groupId ist (typisch
  `<jackson.version>`), wird diese wiederverwendet statt eine zweite anzulegen.
  Grund: Ein Werkzeug, das das Schema des Projekts bricht, erzeugt Diff-Rauschen und
  Review-Reibung — und daran scheitert die Akzeptanz im Team.

- **B-3 Zielbestimmung im Reaktor.** Ziel ist die nächste `pom.xml` vom
  Arbeitsverzeichnis aufwärts. Im Reaktor-Root mit mehreren Maven-Modulen wird
  **interaktiv gefragt statt geraten** — eine Dependency im Aggregator-Pom ist für alle
  Maven-Module sichtbar und fast nie gewollt. Ohne TTY und ohne `--module`: **Fehler mit
  klarer Meldung, niemals raten.** Bewusstes Eintragen ins `<dependencyManagement>` des
  Parent-Poms: `--managed`.

- **B-4 Scope.** Die Flags `--test` / `--provided` / `--runtime` sind die Wahrheit. Eine
  Heuristik (junit\*, mockito\*, testcontainers\* → `test`; lombok → `provided`) darf
  **vorschlagen**, nie entscheiden. Ein still falsch gesetzter Scope bricht nichts sofort,
  sondern erst beim Packaging oder zur Laufzeit — die unangenehmste Fehlerklasse.

### C — Build-Tool-Abdeckung

- **C-1** Maven zuerst, vollständig. Gradle ist Phase 2 und beginnt bei Version Catalogs.
  → [ADR-0005](./docs/adr/0005-maven-zuerst-gradle-ueber-version-catalogs.md)

### D — Auflösung & Metadaten

- **D-1 Metadatenquelle.** Versionsinformationen kommen aus `maven-metadata.xml` der im
  Projekt konfigurierten Artefakt-Repositories — funktioniert für Central **und** internes
  Nexus/Artifactory, ohne externe API-Abhängigkeit und ohne Rate-Limit. Die
  Central-Search-API wird **ausschließlich** für Freitextsuche benutzt.
  Konsequenz, die die Hilfe klar sagen muss: `search` findet keine internen Artefakte.

- **D-2 Neueste stabile Version.** Höchste Version, deren Qualifier stabil ist
  (`ComparableVersion` aus `maven-artifact` kennt die Ordnung
  alpha < beta < milestone < rc < snapshot < *leer*); `--pre` erlaubt Vorabversionen.
  Ein einfaches `<release>` aus den Metadaten genügt **nicht**: JUnit, Spring und Jackson
  veröffentlichen regelmäßig `-M1`/`-RC1` als reguläre Releases, die damit still in der
  Produktions-pom landen würden.
  Zusätzlich ein HTTP-HEAD auf die gewählte `.pom` als Existenzprüfung — Metadaten listen
  auch gelöschte oder nie hochgeladene Artefakte; schlägt der Check fehl, wird die
  nächstniedrigere Version genommen.
  Zur Selektor-Syntax → [ADR-0002](./docs/adr/0002-exakte-versionen-keine-ranges.md)

- **D-3 Maven-BOM-Awareness.** Ist die Koordinate im effektiven Modell bereits verwaltet,
  wird die Dependency ohne `<version>` eingefügt.
  → [ADR-0003](./docs/adr/0003-effektives-modell-statt-dateitext.md)

- **D-4 Cache.** Metadaten-Cache mit ca. 1 h TTL, `--refresh` erzwingt Neuladen,
  `--offline` verbietet Netzzugriff. Mavens `updatePolicy: daily` ist für einen Build
  richtig, für ein interaktives Werkzeug aber falsch — man will ein zehn Minuten altes
  Release sofort sehen. Umgekehrt sind bei `outdated` über 60 Dependencies 60 Requests
  spürbar träge. Cache-Ort: `%LOCALAPPDATA%\jpm\cache` (Windows) bzw. `$XDG_CACHE_HOME/jpm`.

- **D-5 Konfiguration.** Artefakt-Repositories, Mirrors, Auth und Proxy kommen
  ausschließlich aus `settings.xml`, gelesen über den eingebetteten Resolver.

### E — Implementierung & Auslieferung

- **E-1/E-2** Java (Bytecode-Ziel 17) mit eingebettetem Maven Resolver und
  `maven-model-builder`.
  → [ADR-0004](./docs/adr/0004-java-mit-eingebettetem-maven-resolver.md)

- **E-3 CLI-Framework: picocli** — Standard im Java-Ökosystem, und sein Annotation
  Processor erzeugt die GraalVM-Konfiguration bereits mit.

- **E-4 Auslieferung.** Zuerst Fat-JAR mit Launcher-Skripten — `bin/jpm` (POSIX sh) und
  `bin/jpm.cmd` (Windows) —, native-image als Ziel, nicht als erster Schritt. Später GitHub-Actions-Matrix und Scoop-Bucket (Windows ist die
  Hauptplattform); SDKMAN! scheidet aus, solange das so ist.

- **E-5 Schreibsicherheit.** Ergebnis im Speicher erzeugen → neu parsen und verifizieren →
  Temp-Datei → atomarer Move. Schlägt die Verifikation fehl, wird das Original nie
  angefasst. Ist die Ausgangs-pom nicht parsebar: sofortiger Abbruch, ohne irgendetwas zu
  schreiben. Kein Backup, kein Bestätigungsprompt, kein Git-Dirty-Check
  → [ADR-0006](./docs/adr/0006-schreiben-ohne-bestaetigung.md)

### F — CLI-Oberfläche

- **F-1 Benennung.** `add` ist kanonisch, `install` und `i` sind Aliase (analog `remove` /
  `rm` / `uninstall`). Namen sind Versprechen: `install` verspricht, dass danach etwas
  installiert ist — das wäre hier falsch. Der Alias löst den Zielkonflikt: Wahrheit im
  kanonischen Namen, Vertrautheit an der Oberfläche.

- **F-2 Interaktivität.** TTY-Erkennung steuert Rückfragen und Farbe; `--yes` nimmt alle
  Vorschläge an, `--no-input` verbietet Rückfragen. `NO_COLOR` wird respektiert.

- **F-3 Exit-Codes.** `0` Erfolg, `1` Fehler, `2` Abbruch wegen fehlender Eingabe.
  → [ADR-0007](./docs/adr/0007-exit-code-vertrag.md)

- **F-4 Schreibverhalten.** Sofort schreiben, danach präzise berichten; `--dry-run` für
  die Vorschau. → [ADR-0006](./docs/adr/0006-schreiben-ohne-bestaetigung.md)

- **F-5 Maschinenlesbare Ausgabe.** `--json` für `outdated` und `search`. Ausgabemodell
  intern von der Formatierung getrennt, damit Nachrüsten ein Adapter ist und kein Umbau.

---

## 5. Befehlssatz v1

```
jpm add <groupId:artifactId>[@<selektor>]  # Alias: install, i
    --test | --provided | --runtime        # Scope (B-4)
    --module <name>                        # Ziel-Maven-Modul (B-3)
    --managed                              # ins <dependencyManagement> des Parent-Poms
    --property | --inline                  # Ablageform übersteuern (B-2)
    --fetch                                # Artefakte anschließend auflösen
    --dry-run

jpm remove <groupId:artifactId>            # Alias: rm, uninstall
jpm search <text>                          # nur Maven Central (D-1)
jpm outdated                               # immer Exit 0 (ADR-0007)
jpm update [<groupId:artifactId>]
    --patch | --minor | --major            # Default: --minor
    --all                                  # nötig ohne TTY
```

Globale Flags: `--yes`, `--no-input`, `--offline`, `--refresh`, `--json`, `--dry-run`.

Später: `info`, `list`.

### Verhalten im Detail

**`add`** — Version auflösen (D-2) → effektives Modell prüfen (D-3) → Ziel-Maven-Modul
bestimmen (B-3) → Scope klären (B-4) → Ablageform bestimmen (B-2) → atomar schreiben
(E-5) → berichten: was, wohin, welcher Scope, welche Version, **woher die Version stammt**.

**`remove`** — Entfernt den `<dependency>`-Block. Eine dadurch verwaiste Property wird
mitentfernt, aber **nur bei reaktorweit nachgewiesener Alleinnutzung**; im Zweifel bleibt
sie stehen, mit Hinweis. Die Beweislast liegt bei jpm, nicht beim Nutzer.
Steht die Koordinate gar nicht in dieser pom, wird das klar benannt statt geschwiegen —
etwa „kommt transitiv über spring-boot-starter-web; zum Ausschließen brauchst du eine
`<exclusion>`, die macht jpm nicht". Ein bloßes „nicht gefunden" wirkt kaputt, obwohl es
korrekt wäre.

**`update`** — Ohne Argument im TTY interaktive Auswahl aus der `outdated`-Liste, ohne TTY
nur mit explizitem `--all`. Default-Sprunggrenze `--minor`, `--major` opt-in, mit
ehrlichem Hinweis auf die SemVer-Heuristik (ADR-0002). Hängt die Version an einer
geteilten Property, wird vor dem Anheben gezeigt, wer sonst noch betroffen ist.
Verwaltete Dependencies werden nie einzeln angehoben — jpm meldet stattdessen das
verwaltende Maven-BOM als das, was anzuheben wäre (ADR-0003).

**`search`** — Tabelle aus Koordinate, neuester stabiler Version und Versionsanzahl
(grober Reifeindikator; ein Download-Ranking gibt die öffentliche API nicht her).
Im TTY nummeriert mit direkter Auswahl, die unmittelbar in `add` übergeht — der Moment, in
dem sich das Werkzeug wirklich wie npm anfühlt. Ranking unverändert von Central übernehmen:
jede eigene Umsortierung ohne Download-Zahlen wäre Raterei mit selbstbewusster Fassade.
Default 20 Treffer.

---

## 6. Architektur

Der wichtigste Schnitt liegt zwischen **Bytes** und **Semantik**:

| jpm-Modul | Verantwortung |
|---|---|
| `cli` | picocli-Befehle, TTY-Erkennung, Formatierung, Exit-Codes |
| `pom` | Formaterhaltender XML-Editor. Kennt nur Dateien und Bytes, keine Maven-Semantik |
| `model` | Effektives Modell über `maven-model-builder`: Parent-Pom, Maven-BOMs, `dependencyManagement`, Properties, Reaktor-Erkennung. Liest nur |
| `metadata` | `maven-metadata.xml` über den Resolver, Cache, Stabilitätsfilter |
| `search` | Central-Search-API. Einziges jpm-Modul mit eigenem Backend |
| `commands` | Orchestriert die obigen zu `add` / `remove` / `outdated` / `update` / `search` |

`pom` schreibt und weiß nichts über Maven; `model` versteht Maven und schreibt nichts.
Diese Trennung ist der Grund, warum der riskante Teil — die Formaterhaltung — isoliert und
byteweise testbar bleibt, ohne Maven mocken zu müssen.

---

## 7. Teststrategie

Der riskanteste Teil ist der pom-Editor: er mutiert fremde Quelldateien und muss
Formatierung, Kommentare, Einrückung und Zeilenenden erhalten.

**Golden-File-Tests sind das Rückgrat**: Korpus aus `input.xml` + `expected.xml`,
byteweiser Vergleich nach der Operation. Begründung: Der gefürchtete Fehler ist ein
*Byte*-Fehler — verschobene Einrückung, geschluckter Kommentar, umgeschriebene Kodierung.
Unit-Tests auf Methodenebene sehen den nie und leuchten grün, während die pom im Diff
verwüstet aussieht. Man testet, wovor man Angst hat, nicht was leicht zu testen ist.

Der Korpus braucht ausdrücklich die fiesen Fälle:

- Kommentare zwischen `<dependency>`-Blöcken
- **CRLF-Zeilenenden** (auf Windows der Standardfall, klassischer Formaterhaltungs-Killer)
- Datei mit **Byte Order Mark** am Anfang
- leere `<dependencies>` / gar kein `<dependencies>`
- Multi-Modul-Aggregator
- Maven-BOM-verwaltetes Spring-Boot-Projekt
- gemischte Ablageform-Konventionen (Property und inline nebeneinander)

Quelle für den Korpus: echte OSS-poms (Spring Boot, Quarkus, Camel) als Regressionsnetz.
Property-based/Fuzzing später, wenn der Editor steht.

---

## 8. Umsetzungsreihenfolge

Die Arbeit ist in 13 Tickets geschnitten, jedes eine **vertikale Scheibe**: ein schmaler,
aber vollständiger Pfad durch alle Schichten, der für sich vorführbar ist. Die jpm-Module
aus Abschnitt 6 entstehen dabei nebenbei — sie sind kein eigener Bauabschnitt, weil ein
fertiges `model`-jpm-Modul ohne Befehl darüber nicht überprüfbar wäre.

Die Tickets sind [GitHub Issues](https://github.com/lxwtmn/jpm/issues), nummeriert so, dass
jeder Blocker eine kleinere Nummer hat. Die Blocking-Kanten liegen zusätzlich als native
GitHub-Issue-Dependencies vor, sind also in der Oberfläche sichtbar und maschinell
abfragbar.

| Issue | Ticket | Blocked by |
|---|---|---|
| [#1](https://github.com/lxwtmn/jpm/issues/1) | Lauffähiges Skelett mit `jpm --version` | — |
| [#2](https://github.com/lxwtmn/jpm/issues/2) | `jpm add` mit exakter Version in eine Single-Modul-pom | #1 |
| [#3](https://github.com/lxwtmn/jpm/issues/3) | Neueste stabile Version auflösen | #2 |
| [#4](https://github.com/lxwtmn/jpm/issues/4) | Ablageform an die Projektkonvention anpassen | #2 |
| [#5](https://github.com/lxwtmn/jpm/issues/5) | Scope-Flags und interaktiver Vertrag | #2 |
| [#6](https://github.com/lxwtmn/jpm/issues/6) | Effektives Modell und Maven-BOM-Awareness | #3 |
| [#7](https://github.com/lxwtmn/jpm/issues/7) | Zielbestimmung im Reaktor | #5, #6 |
| [#8](https://github.com/lxwtmn/jpm/issues/8) | `jpm remove` | #4, #7 |
| [#9](https://github.com/lxwtmn/jpm/issues/9) | `jpm outdated` | #3, #6 |
| [#10](https://github.com/lxwtmn/jpm/issues/10) | `jpm update` | #4, #9 |
| [#11](https://github.com/lxwtmn/jpm/issues/11) | `jpm search` | #3, #5 |
| [#12](https://github.com/lxwtmn/jpm/issues/12) | `--json` für `outdated` und `search` | #9, #11 |
| [#13](https://github.com/lxwtmn/jpm/issues/13) | native-image statt Fat-JAR | #12 |

Das Riskanteste bleibt vorn: Issue #2 enthält den formaterhaltenden pom-Editor samt
Golden-File-Korpus (Abschnitt 7), nur eben end-to-end statt isoliert. Der Korpus wächst
danach mit jedem Ticket, das eine neue pom-Situation einführt — Properties in #4,
Maven-BOMs in #6, Aggregator-Poms in #7.

---

## 9. Offene Punkte

- Gradle-Phase 2 verdient eine eigene Design-Befragung, bevor sie beginnt

### Erledigt

- **Lizenz: Apache-2.0.** Passend zu den gebündelten Abhängigkeiten (Maven Resolver,
  `maven-model-builder`, picocli sind sämtlich Apache-2.0), mit ausdrücklichem
  Patentgrant für den Firmeneinsatz aus A-2, und die im Java-Ökosystem unauffällige Wahl.
  Folge für den Build: Apache-2.0 §4 verlangt, dass `LICENSE`- und `NOTICE`-Einträge der
  eingebundenen Artefakte beim Zusammenschütten des Fat-JAR erhalten bleiben statt sich
  gegenseitig zu überschreiben — siehe Issue #1.
- **Namensprüfung `jpm`.** Beide historischen Kollisionen (jpm4j, npm-Paket `jpm`) sind
  tot; `lxwtmn/jpm` war frei.
