# 08 — `jpm remove`

**What to build:** Ein Nutzer kann eine Dependency entfernen. Wird dadurch eine Property zur
Leiche, verschwindet sie mit — aber nur, wenn nachweislich kein anderes Maven-Modul im
Reaktor sie nutzt. Steht die Koordinate gar nicht in dieser pom, erklärt jpm, woher sie
stattdessen kommt, statt nur „nicht gefunden" zu melden.

**Blocked by:** 04, 07

**Status:** ready-for-agent

- [ ] `remove` entfernt den Dependency-Block formaterhaltend
- [ ] Die Aliase `rm` und `uninstall` funktionieren
- [ ] Eine allein von diesem Eintrag genutzte Property wird mitentfernt
- [ ] Wird die Property anderswo im Reaktor referenziert, bleibt sie stehen, mit Hinweis
- [ ] Im Zweifel bleibt die Property stehen — die Beweislast liegt bei jpm
- [ ] Steht die Koordinate nicht in dieser pom, nennt die Meldung die tatsächliche Herkunft (transitiv oder Parent-Pom) und dass jpm keine `<exclusion>` verwaltet
- [ ] `--dry-run` zeigt das Ergebnis, ohne zu schreiben
