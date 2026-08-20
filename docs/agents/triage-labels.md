# Triage-Labels

Die Skills sprechen in fünf kanonischen Triage-Rollen. Diese Tabelle bildet sie auf die
Label-Bezeichner ab, die in den GitHub Issues dieses Repos tatsächlich verwendet werden.

| Label in mattpocock/skills | Label in diesem Repo | Bedeutung |
| --- | --- | --- |
| `needs-triage` | `needs-triage` | Muss noch bewertet werden |
| `needs-info` | `needs-info` | Wartet auf Rückfrage beim Melder |
| `ready-for-agent` | `ready-for-agent` | Vollständig spezifiziert, ein Agent kann es allein umsetzen |
| `ready-for-human` | `ready-for-human` | Braucht menschliche Umsetzung |
| `wontfix` | `wontfix` | Wird nicht bearbeitet |

Nennt eine Skill eine Rolle („apply the AFK-ready triage label"), gilt der Bezeichner aus
der rechten Spalte.

Die Labels existieren im Repo und werden über `gh issue edit <n> --add-label "..."`
gesetzt. Genau ein Triage-Label pro Issue.

Die rechte Spalte kann angepasst werden, falls das Repo später ein abweichendes Vokabular
etabliert; dann wenden die Skills die dort existierenden Labels an, statt Duplikate
anzulegen.
