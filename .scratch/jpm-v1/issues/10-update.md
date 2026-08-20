# 10 — `jpm update`

**What to build:** Ein Nutzer kann Versionen anheben — einzeln oder ausgewählt aus der Liste
der veralteten. Sprünge über eine Major-Grenze passieren nur auf Ansage, geteilte
Properties werden nicht heimlich für andere Dependencies mitverändert, und verwaltete
Einträge werden nicht einzeln übersteuert.

**Blocked by:** 04, 09

**Status:** ready-for-agent

- [ ] `update` mit Koordinate hebt diese eine Dependency an
- [ ] Ohne Argument erscheint im Terminal eine Auswahl aus der `outdated`-Liste
- [ ] Ohne Terminal ist `--all` erforderlich, sonst Exit 2
- [ ] Default-Sprunggrenze ist `--minor`; `--major` ist ausdrücklich zu verlangen
- [ ] Die Ausgabe benennt, dass die Patch/Minor/Major-Einordnung eine Heuristik über den Versions-String ist
- [ ] Hängt die Version an einer geteilten Property, werden vor dem Anheben alle betroffenen Dependencies genannt
- [ ] Verwaltete Dependencies werden nicht angehoben; stattdessen wird das verwaltende Maven-BOM als Ziel genannt
- [ ] `--dry-run` zeigt das Ergebnis, ohne zu schreiben
