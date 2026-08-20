# Domain documentation

How the engineering skills should consume this repository's domain documentation before
exploring the code.

This repository is **single-context**: one `CONTEXT.md` and one `docs/adr/` at the root.

## Read before exploring

- **[`CONTEXT.md`](../../CONTEXT.md)** at the root — the glossary
- **[`docs/adr/`](../adr/)** — the ADRs touching the area being worked on

There is no `CONTEXT-MAP.md`; should one appear, the repository has become multi-context and
the structure changes accordingly.

## File structure

```
/
├── CONTEXT.md
├── DESIGN.md
├── docs/
│   ├── adr/
│   └── agents/
└── src/
```

## Use the glossary's vocabulary

When output names a domain concept — in an issue title, a refactoring proposal, a hypothesis, a
test name — the term is used as `CONTEXT.md` defines it. No drifting into synonyms the glossary
lists under `_Avoid_`.

**Particularly important in this repository:** three words carry two meanings and are **never**
used unqualified, not even in identifiers or commit messages:

- **BOM** → either *Maven BOM* or *byte order mark*
- **Module** → either *Maven module* (in the user's reactor) or *jpm module* (in jpm's code)
- **Repository** → either *artifact repository* or *Git repository*

If a needed term is missing from the glossary, that is a signal: either language is being
invented that the project does not use (reconsider), or there is a genuine gap (note it for
`/domain-modeling`).

## Surface contradictions with ADRs

If output contradicts an existing ADR, say so explicitly rather than overriding it silently:

> _Contradicts ADR-0003 (decisions are made against the effective model) — but worth reopening
> because…_
