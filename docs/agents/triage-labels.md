# Triage labels

The skills speak in five canonical triage roles. This table maps them to the label names
actually used on this repository's GitHub issues.

| Label in mattpocock/skills | Label in this repository | Meaning |
| --- | --- | --- |
| `needs-triage` | `needs-triage` | Still needs to be assessed |
| `needs-info` | `needs-info` | Waiting on the reporter for more information |
| `ready-for-agent` | `ready-for-agent` | Fully specified; an agent can carry it alone |
| `ready-for-human` | `ready-for-human` | Requires human implementation |
| `wontfix` | `wontfix` | Will not be actioned |

When a skill names a role ("apply the AFK-ready triage label"), use the name from the
right-hand column.

The labels exist on the repository and are applied with
`gh issue edit <n> --add-label "..."`. Exactly one triage label per issue.

The right-hand column can be adjusted if the repository later settles on a different
vocabulary; the skills then apply the existing labels instead of creating duplicates.
