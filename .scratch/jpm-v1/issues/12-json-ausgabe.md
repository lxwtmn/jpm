# 12 — `--json` für `outdated` und `search`

**What to build:** Ein CI-Skript kann die Ergebnisse beider Befehle maschinell
weiterverarbeiten, ohne Text zu parsen. Das sind die einzigen zwei Befehle mit einem realen
Weiterverarbeitungsfall — die schreibenden ziehen später nach, wenn Bedarf entsteht.

**Blocked by:** 09, 11

**Status:** ready-for-agent

- [ ] `--json` liefert bei `outdated` und `search` strukturierte Ausgabe
- [ ] Beide nutzen dieselbe Hüllenstruktur
- [ ] Mit `--json` geht keine Fortschritts- oder Farbausgabe nach stdout
- [ ] `--json` impliziert nicht-interaktiv
- [ ] Fehler werden im JSON-Modus ebenfalls strukturiert gemeldet, bei unveränderten Exit-Codes
- [ ] Das Ausgabemodell ist von der Textformatierung getrennt, sodass weitere Befehle ohne Umbau nachziehen können
