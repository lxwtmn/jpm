# 05 — Scope-Flags und interaktiver Vertrag

**What to build:** Ein Nutzer kann den Scope einer Dependency bestimmen. Bei bekannten
Testartefakten schlägt jpm im Terminal `test` vor und lässt bestätigen; ohne Terminal wird
nie gefragt und nie geraten. Mit diesem Ticket steht zugleich das Verhalten, auf das alle
späteren Rückfragen aufbauen — Prompts, Farbe, Exit-Codes.

**Blocked by:** 02

**Status:** ready-for-agent

- [ ] `--test`, `--provided` und `--runtime` setzen den Scope
- [ ] Ohne Flag ist der Scope `compile`
- [ ] Bei bekannten Testartefakten erscheint im Terminal ein Vorschlag mit Rückfrage; wird er abgelehnt, bleibt es bei `compile`
- [ ] Ohne Terminal wird nie gefragt, nichts geraten und keine Farbe ausgegeben
- [ ] `--yes` nimmt Vorschläge an, `--no-input` verbietet Rückfragen
- [ ] `NO_COLOR` wird respektiert
- [ ] Exit-Codes: 0 Erfolg, 1 Fehler, 2 Abbruch wegen fehlender Eingabe
