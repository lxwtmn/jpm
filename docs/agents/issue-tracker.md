# Issue tracker: GitHub

Issues for this repository live as GitHub issues. Every operation goes through the `gh` CLI,
which infers the repository from `git remote -v` when run inside the clone.

## Conventions

- **Create an issue**: `gh issue create --title "..." --body "..."` (use `--body-file` for
  multi-line bodies)
- **Read an issue**: `gh issue view <number> --comments`
- **List issues**: `gh issue list --state open --json number,title,body,labels,comments`
- **Comment**: `gh issue comment <number> --body "..."`
- **Add/remove labels**: `gh issue edit <number> --add-label "..."` / `--remove-label "..."`
- **Close**: `gh issue close <number> --comment "..."`

## Blocking edges

Blocking is expressed through **GitHub's native issue dependencies** — the canonical
representation, visible in the UI:

```
gh api --method POST repos/<owner>/<repo>/issues/<child>/dependencies/blocked_by \
  -F issue_id=<database-id-of-the-blocker>
```

The `issue_id` is the blocker's numeric **database id**
(`gh api repos/<owner>/<repo>/issues/<n> --jq .id`) — **not** the `#number` and not the
`node_id`. GitHub reports open blockers under `issue_dependencies_summary.blocked_by`.

Every issue body additionally carries a `## Blocked by` section with `#` references —
redundant with the API, but readable in the body even where dependencies are unavailable.

## Pull requests as a triage surface

**PRs as a request surface: no.** _(Set to `yes` if external PRs should be treated like feature
requests; `/triage` reads this flag.)_

## Where the specification lives

**A deviation from the default convention.** The template expects specs to live as issues. In
this repository the specification is [`DESIGN.md`](../../DESIGN.md) at the root, together with
the architecture decisions under [`docs/adr/`](../adr/).

Do **not** create a second specification as an issue — it would drift away from `DESIGN.md`.
Issues reference `DESIGN.md` and the relevant ADRs instead.

## Numbering and order

Tickets are created in dependency order: **every blocker receives a lower issue number** than
the ticket it blocks. This is not cosmetic — the numbering is a topological sort, so a cycle in
the dependency graph shows up while the issues are being created.

## When a skill says "publish to the issue tracker"

Create a GitHub issue.

## When a skill says "fetch the relevant ticket"

Run `gh issue view <number> --comments`.

## Wayfinding operations

Used by `/wayfinder`. The **map** is a single issue with **child** issues as tickets.

- **Map**: an issue labelled `wayfinder:map`, whose body holds Notes / Decisions-so-far / Fog
- **Child ticket**: attached to the map as a GitHub sub-issue; where sub-issues are disabled,
  a task list in the map body plus `Part of #<map>` at the top of the child. Labels:
  `wayfinder:<type>` (`research`/`prototype`/`grilling`/`task`).
- **Blocking**: native dependencies, as above.
- **Frontier**: open children with no open blocker and no assignee; first in map order wins.
- **Claim**: `gh issue edit <n> --add-assignee @me` — the session's first write.
- **Resolve**: `gh issue comment`, then `gh issue close`, then append a context pointer to the
  map's Decisions-so-far.
