# Issue tracker: GitHub

Issues für dieses Repo leben als GitHub Issues. Alle Operationen laufen über die
`gh`-CLI, die das Repo aus `git remote -v` ableitet, wenn sie im Klon ausgeführt wird.

## Konventionen

- **Issue anlegen**: `gh issue create --title "..." --body "..."` (Heredoc für mehrzeilige Rümpfe)
- **Issue lesen**: `gh issue view <nummer> --comments`
- **Issues auflisten**: `gh issue list --state open --json number,title,body,labels,comments`
- **Kommentieren**: `gh issue comment <nummer> --body "..."`
- **Labels setzen/entfernen**: `gh issue edit <nummer> --add-label "..."` / `--remove-label "..."`
- **Schließen**: `gh issue close <nummer> --comment "..."`

## Blockierende Kanten

Blocking wird über **GitHubs native Issue-Dependencies** ausgedrückt — das ist die
kanonische, in der Oberfläche sichtbare Darstellung:

```
gh api --method POST repos/<owner>/<repo>/issues/<kind>/dependencies/blocked_by \
  -F issue_id=<datenbank-id-des-blockers>
```

Die `issue_id` ist die numerische **Datenbank-ID** des Blockers
(`gh api repos/<owner>/<repo>/issues/<n> --jq .id`), **nicht** die `#nummer` und nicht die
`node_id`. GitHub meldet offene Blocker unter `issue_dependencies_summary.blocked_by`.

Zusätzlich trägt jeder Issue-Rumpf einen `## Blocked by`-Abschnitt mit `#`-Referenzen —
redundant zur API, aber im Rumpf lesbar, auch wenn Dependencies einmal nicht verfügbar sind.

## Pull requests als Triage-Fläche

**PRs as a request surface: no.** _(Auf `yes` setzen, wenn externe PRs wie Feature Requests
behandelt werden sollen; `/triage` liest dieses Flag.)_

## Wo die Spezifikation liegt

**Abweichung von der Standardkonvention.** Die Vorlage sieht Specs als Issues vor. In
diesem Repo ist die Spezifikation [`DESIGN.md`](../../DESIGN.md) im Wurzelverzeichnis,
ergänzt um die Architekturentscheidungen unter [`docs/adr/`](../adr/).

Lege **keine** zweite Spezifikation als Issue an — sie würde von `DESIGN.md` wegdriften.
Issues verweisen stattdessen auf `DESIGN.md` und die passenden ADRs.

## Nummerierung und Reihenfolge

Tickets werden in Abhängigkeitsreihenfolge angelegt: **jeder Blocker bekommt eine kleinere
Issue-Nummer** als das Ticket, das er blockiert. Das ist nicht nur Kosmetik — die
Nummerierung ist damit eine topologische Sortierung, und ein Zyklus im Abhängigkeitsgraph
fällt beim Anlegen auf.

## Wenn eine Skill sagt „publish to the issue tracker"

Ein GitHub Issue anlegen.

## Wenn eine Skill sagt „fetch the relevant ticket"

`gh issue view <nummer> --comments` ausführen.

## Wayfinding-Operationen

Genutzt von `/wayfinder`. Die **Map** ist ein einzelnes Issue mit **Kind**-Issues als
Tickets.

- **Map**: ein Issue mit Label `wayfinder:map`, Rumpf enthält Notes / Decisions-so-far / Fog
- **Kind-Ticket**: als GitHub-Sub-Issue an die Map gehängt; wo Sub-Issues nicht aktiv sind,
  Task-Liste im Map-Rumpf plus `Part of #<map>` oben im Kind. Labels: `wayfinder:<typ>`
  (`research`/`prototype`/`grilling`/`task`).
- **Blocking**: wie oben, native Dependencies.
- **Frontier**: offene Kinder ohne offenen Blocker und ohne Assignee; erstes in Map-Reihenfolge gewinnt.
- **Claim**: `gh issue edit <n> --add-assignee @me` — der erste Schreibvorgang der Sitzung.
- **Resolve**: `gh issue comment`, dann `gh issue close`, dann Kontextzeiger in die
  Decisions-so-far der Map.
