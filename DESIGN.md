# jpm — Design

The result of a structured design interrogation across four rounds.

This document is the **specification**: what jpm does, how it is built, what it deliberately
does not do. The **why** behind the hard-to-reverse decisions lives in [docs/adr/](./docs/adr/)
and is not repeated here — every rationale should have exactly one home. Terminology follows
[CONTEXT.md](./CONTEXT.md).

---

## 1. What jpm is

A CLI that manages Maven dependencies without anybody editing `pom.xml` by hand. npm is the
model — **for the interface only, not for the data model**.

v1 solves two pain points:

1. Finding the coordinate and the current version (no more mvnrepository.com)
2. Writing the POM entry correctly (no more hand-edited XML)

Updates (`outdated` / `update`) follow immediately after.

Audience: our own company and team. A later open-source release is kept open but drives no v1
decision. That focus forces private artifact repositories, auth, multi-module and parent-POM
cases early — exactly what an open-source release would need anyway, without yet carrying the
support burden.

---

## 2. Guiding principles

Everything else follows from these six sentences.

| | Principle | Rationale |
|---|---|---|
| **P1** | The POM belongs to the user. jpm is an editor, not an owner. | [ADR-0001](./docs/adr/0001-pom-stays-canonical.md) |
| **P2** | The POM always contains exact versions. | [ADR-0002](./docs/adr/0002-exact-versions-no-ranges.md) |
| **P3** | The effective model decides, not the file text. | [ADR-0003](./docs/adr/0003-effective-model-over-file-text.md) |
| **P4** | Never do silently what the user would not notice. | see below |
| **P5** | Foreign state is not modified. | see below |
| **P6** | Scriptability is a contract, not an afterthought. | [ADR-0007](./docs/adr/0007-exit-code-contract.md) |

**On P4:** Scopes are suggested, never set silently. Managed dependencies are never silently
overridden. Every write reports afterwards exactly what went where and where the version came
from.

**On P5:** jpm reads `~/.m2` and `settings.xml` but never writes into them. Its own cache, its
own directory. Maven's state stays untouched.

---

## 3. Non-goals

Deliberately excluded. This list matters as much as the feature set.

| Non-goal | Why |
|---|---|
| Lock file / reproducibility guarantee | Exact versions (P2) already provide enough |
| Conflict resolution, `<exclusion>` management, `why` / dependency tree | A separate product; `mvn dependency:tree` exists |
| Task runner (`jpm run`) | A different tool that would merely share the name |
| Downloading artifacts | Only optionally via `--fetch`; the next `mvn` run does it anyway |
| Writing into `~/.m2` | P5 |
| A configuration file of its own | `settings.xml` is the single source for artifact repositories, auth, mirrors, proxy. A second configuration source is a second source of error |
| Project scaffolding (`jpm init`) | Maven archetypes exist |
| Gradle in v1 | [ADR-0005](./docs/adr/0005-maven-first-gradle-via-version-catalogs.md) |
| Generating `pom.xml` | [ADR-0001](./docs/adr/0001-pom-stays-canonical.md) |

---

## 4. Decisions

### B — Data model

- **B-1** `pom.xml` stays canonical; jpm edits it in place, preserving formatting.
  → [ADR-0001](./docs/adr/0001-pom-stays-canonical.md)

- **B-2 Version placement.** Inline `<version>` or `<properties>`: **the project's own
  convention is detected and continued** (majority vote over existing entries), overridable
  with `--property` / `--inline`.
  Property naming: `<artifactId>.version` by default — **but** if a property already exists
  whose value is the version of an artifact with the same groupId (typically
  `<jackson.version>`), that one is reused instead of adding a second.
  Reason: a tool that breaks the project's scheme produces diff noise and review friction —
  and that is what kills adoption in a team.

- **B-3 Target selection in a reactor.** The target is the nearest `pom.xml` walking up from
  the working directory. At a reactor root with several Maven modules jpm **asks instead of
  guessing** — a dependency in the aggregator POM is visible to every Maven module and is
  almost never what was meant. Without a TTY and without `--module`: **fail with a clear
  message, never guess.** Deliberately writing into the parent POM's `<dependencyManagement>`:
  `--managed`.

- **B-4 Scope.** The flags `--test` / `--provided` / `--runtime` are the truth. A heuristic
  (junit\*, mockito\*, testcontainers\* → `test`; lombok → `provided`) may **suggest**, never
  decide. A silently wrong scope breaks nothing immediately — it breaks at packaging time or
  at runtime, which is the nastiest class of failure.

### C — Build tool coverage

- **C-1** Maven first, completely. Gradle is phase 2 and starts at version catalogs.
  → [ADR-0005](./docs/adr/0005-maven-first-gradle-via-version-catalogs.md)

### D — Resolution and metadata

- **D-1 Metadata source.** Version information comes from the `maven-metadata.xml` of the
  artifact repositories configured for the project — which works for Central **and** an
  internal Nexus/Artifactory, without an external API dependency and without a rate limit. The
  Central search API is used **exclusively** for free-text search.
  A consequence the help text must state plainly: `search` does not find internal artifacts.

- **D-2 Latest stable version.** The highest version whose qualifier is stable
  (`ComparableVersion` from `maven-artifact` knows the ordering
  alpha < beta < milestone < rc < snapshot < *none*); `--pre` allows pre-releases.
  A plain `<release>` from the metadata is **not** enough: JUnit, Spring and Jackson regularly
  publish `-M1`/`-RC1` as ordinary releases, which would silently land in a production POM.
  Additionally an HTTP HEAD against the chosen `.pom` as an existence check — metadata also
  lists deleted or never-uploaded artifacts; if the check fails, the next lower version is
  taken.
  On selector syntax → [ADR-0002](./docs/adr/0002-exact-versions-no-ranges.md)

- **D-3 Maven BOM awareness.** If the coordinate is already managed in the effective model,
  the dependency is inserted without a `<version>`.
  → [ADR-0003](./docs/adr/0003-effective-model-over-file-text.md)

- **D-4 Cache.** A metadata cache with roughly a 1 h TTL, `--refresh` forces a reload,
  `--offline` forbids network access. Maven's `updatePolicy: daily` is right for a build but
  wrong for an interactive tool — you want to see a ten-minute-old release immediately.
  Conversely, `outdated` across 60 dependencies means 60 requests, which is noticeably slow.
  Cache location: `%LOCALAPPDATA%\jpm\cache` on Windows, `$XDG_CACHE_HOME/jpm` elsewhere.

- **D-5 Configuration.** Artifact repositories, mirrors, auth and proxy come exclusively from
  `settings.xml`, read through the embedded resolver.

### E — Implementation and distribution

- **E-1/E-2** Java (bytecode target 17) with an embedded Maven Resolver and
  `maven-model-builder`.
  → [ADR-0004](./docs/adr/0004-java-with-embedded-maven-resolver.md)

- **E-3 CLI framework: picocli** — the standard in the Java ecosystem, and its annotation
  processor already emits the GraalVM configuration.

- **E-4 Distribution.** A fat JAR first, with launcher scripts — `bin/jpm` (POSIX sh) and
  `bin/jpm.cmd` (Windows) — and native-image as the goal rather than the first step. Later a
  GitHub Actions matrix and a Scoop bucket (Windows is the primary platform); SDKMAN! is out
  while that holds.

- **E-5 Write safety.** Produce the result in memory → re-parse and verify → temp file →
  atomic move. If verification fails the original is never touched. If the source POM does not
  parse: abort immediately without writing anything. No backup, no confirmation prompt, no
  Git-dirty check → [ADR-0006](./docs/adr/0006-write-without-confirmation.md)

### F — Command-line surface

- **F-1 Naming.** `add` is canonical; `install` and `i` are aliases (likewise `remove` / `rm` /
  `uninstall`). Names are promises: `install` promises that something is installed afterwards,
  which would be false here. The alias resolves the conflict — truth in the canonical name,
  familiarity at the surface.

- **F-2 Interactivity.** TTY detection governs prompts and colour; `--yes` accepts every
  suggestion, `--no-input` forbids questions. `NO_COLOR` is honoured.

- **F-3 Exit codes.** `0` success, `1` failure, `2` aborted for want of input.
  → [ADR-0007](./docs/adr/0007-exit-code-contract.md)

- **F-4 Write behaviour.** Write immediately, report precisely afterwards; `--dry-run` for the
  preview. → [ADR-0006](./docs/adr/0006-write-without-confirmation.md)

- **F-5 Machine-readable output.** `--json` for `outdated` and `search`. The output model is
  kept separate from the text formatting so that retrofitting is an adapter, not a rebuild.

---

## 5. Command set for v1

```
jpm add <groupId:artifactId>[@<selector>]  # aliases: install, i
    --test | --provided | --runtime        # scope (B-4)
    --module <name>                        # target Maven module (B-3)
    --managed                              # into the parent POM's <dependencyManagement>
    --property | --inline                  # override version placement (B-2)
    --fetch                                # resolve artifacts afterwards
    --dry-run

jpm remove <groupId:artifactId>            # aliases: rm, uninstall
jpm search <text>                          # Maven Central only (D-1)
jpm outdated                               # always exit 0 (ADR-0007)
jpm update [<groupId:artifactId>]
    --patch | --minor | --major            # default: --minor
    --all                                  # required without a TTY
```

Global flags: `--yes`, `--no-input`, `--offline`, `--refresh`, `--json`, `--dry-run`.

Later: `info`, `list`.

### Behaviour in detail

**`add`** — resolve the version (D-2) → consult the effective model (D-3) → determine the
target Maven module (B-3) → settle the scope (B-4) → determine version placement (B-2) → write
atomically (E-5) → report: what, where, which scope, which version, and **where the version
came from**.

**`remove`** — removes the `<dependency>` block. A property orphaned by that removal is removed
along with it, but **only when sole use across the reactor has been proven**; in doubt it stays,
with a note. The burden of proof lies with jpm, not with the user.
If the coordinate is not in this POM at all, that is stated plainly rather than passed over in
silence — for example "it arrives transitively via spring-boot-starter-web; to exclude it you
need an `<exclusion>`, which jpm does not manage". A bare "not found" looks broken even though
it would be correct.

**`update`** — with no argument, an interactive selection from the `outdated` list on a TTY;
without a TTY only with an explicit `--all`. The default jump limit is `--minor`, `--major` is
opt-in, with an honest note about the SemVer heuristic (ADR-0002). If the version hangs off a
shared property, everyone else affected is shown before the bump.
Managed dependencies are never bumped individually — instead jpm reports the managing Maven BOM
as the thing that would have to move (ADR-0003).

**`search`** — a table of coordinate, latest stable version and version count (a crude maturity
signal; the public API offers no download ranking). On a TTY the entries are numbered and a
selection flows straight into `add` — the moment the tool genuinely feels like npm. Ranking is
taken from Central unchanged: any re-sorting of our own without download figures would be
guesswork wearing a confident face. 20 hits by default.

---

## 6. Architecture

The most important seam runs between **bytes** and **semantics**:

| jpm module | Responsibility |
|---|---|
| `domain` | The vocabulary of CONTEXT.md as types — `Coordinate`, `Dependency`. Depends on nothing; everything else depends on it |
| `cli` | picocli commands, TTY detection, formatting, exit codes |
| `pom` | The format-preserving XML editor. Knows files and bytes, no Maven semantics |
| `model` | The effective model via `maven-model-builder`: parent POM, Maven BOMs, `dependencyManagement`, properties, reactor detection. Read-only |
| `metadata` | `maven-metadata.xml` through the resolver, cache, stability filter |
| `search` | The Central search API. The only jpm module with a backend of its own |
| `commands` | Orchestrates the above into `add` / `remove` / `outdated` / `update` / `search` |

`pom` writes and knows nothing about Maven; `model` understands Maven and writes nothing. That
separation is why the risky part — format preservation — stays isolated and testable byte for
byte, without mocking Maven.

`domain` exists because `Coordinate` is not the property of any one module: `pom` writes them,
`metadata` looks them up, `cli` parses them. Keeping the shared vocabulary in the module that
happened to need it first would have made every other module depend on `pom` for a word the
glossary owns.

---

## 7. Testing strategy

The riskiest part is the POM editor: it mutates somebody else's source files and must preserve
formatting, comments, indentation and line endings.

**Golden-file tests are the backbone**: a corpus of `input.xml` + `expected.xml`, compared byte
for byte after the operation. The reasoning: the failure you fear is a *byte* failure — shifted
indentation, a swallowed comment, a rewritten encoding. Unit tests at method level never see it
and shine green while the POM looks ransacked in the diff. Test what you are afraid of, not what
is easy to test.

The corpus explicitly needs the nasty cases:

- comments between `<dependency>` blocks
- **CRLF line endings** (the default on Windows and a classic format-preservation killer)
- a file with a **byte order mark** at the start
- an empty `<dependencies>` / no `<dependencies>` at all
- a multi-module aggregator
- a Maven-BOM-managed Spring Boot project
- mixed version-placement conventions (property and inline side by side)

Corpus source: real open-source POMs (Spring Boot, Quarkus, Camel) as a regression net.
Property-based testing and fuzzing later, once the editor stands.

---

## 8. Implementation order

The work is cut into 13 tickets, each a **vertical slice**: a narrow but complete path through
every layer, demonstrable on its own. The jpm modules from section 6 emerge along the way —
they are not a build phase of their own, because a finished `model` jpm module with no command
above it could not be verified.

The tickets are [GitHub issues](https://github.com/lxwtmn/jpm/issues), numbered so that every
blocker carries a lower number. The blocking edges also exist as native GitHub issue
dependencies, so they are visible in the UI and queryable.

| Issue | Ticket | Blocked by |
|---|---|---|
| [#1](https://github.com/lxwtmn/jpm/issues/1) | Runnable skeleton with `jpm --version` | — |
| [#2](https://github.com/lxwtmn/jpm/issues/2) | `jpm add` with an exact version into a single-module POM | #1 |
| [#3](https://github.com/lxwtmn/jpm/issues/3) | Resolve the latest stable version | #2 |
| [#4](https://github.com/lxwtmn/jpm/issues/4) | Match the project's version-placement convention | #2 |
| [#5](https://github.com/lxwtmn/jpm/issues/5) | Scope flags and the interactive contract | #2 |
| [#6](https://github.com/lxwtmn/jpm/issues/6) | Effective model and Maven BOM awareness | #3 |
| [#7](https://github.com/lxwtmn/jpm/issues/7) | Target selection in a reactor | #5, #6 |
| [#8](https://github.com/lxwtmn/jpm/issues/8) | `jpm remove` | #4, #7 |
| [#9](https://github.com/lxwtmn/jpm/issues/9) | `jpm outdated` | #3, #6 |
| [#10](https://github.com/lxwtmn/jpm/issues/10) | `jpm update` | #4, #9 |
| [#11](https://github.com/lxwtmn/jpm/issues/11) | `jpm search` | #3, #5 |
| [#12](https://github.com/lxwtmn/jpm/issues/12) | `--json` for `outdated` and `search` | #9, #11 |
| [#13](https://github.com/lxwtmn/jpm/issues/13) | native-image instead of a fat JAR | #12 |

The riskiest part stays up front: issue #2 contains the format-preserving POM editor together
with the golden-file corpus (section 7), just end to end rather than in isolation. The corpus
then grows with every ticket that introduces a new POM situation — properties in #4, Maven BOMs
in #6, aggregator POMs in #7.

---

## 9. Open points

- Gradle phase 2 deserves a design interrogation of its own before it starts

### Settled

- **Licence: Apache-2.0.** It matches the bundled dependencies (Maven Resolver,
  `maven-model-builder` and picocli are all Apache-2.0), carries an explicit patent grant for
  the corporate use in A-2, and is the unsurprising choice in the Java ecosystem.
  Consequence for the build: Apache-2.0 §4 requires that the `LICENSE` and `NOTICE` entries of
  bundled artifacts survive the shading rather than overwrite one another. Note that
  `ApacheLicenseResourceTransformer` is unsuitable for this — measured, it discards *all*
  LICENSE entries, including our own.
- **Name check `jpm`.** Both historical collisions (jpm4j, the npm package `jpm`) are dead;
  `lxwtmn/jpm` was free.
