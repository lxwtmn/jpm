# 11 — `jpm search`

**What to build:** Ein Nutzer, der nur „irgendwas mit jackson" weiß, findet die Koordinate,
ohne den Browser zu öffnen — und kann direkt aus der Trefferliste heraus hinzufügen. Das
ist der Moment, in dem sich das Werkzeug wirklich wie npm anfühlt.

**Blocked by:** 03, 05

**Status:** ready-for-agent

- [ ] Freitextsuche gegen Maven Central; Tabelle mit Koordinate, neuester stabiler Version und Versionsanzahl
- [ ] Die angezeigte Version folgt derselben Stabilitätsregel wie `add`
- [ ] Reihenfolge unverändert von Central übernommen, Default 20 Treffer, Anzahl per Flag änderbar
- [ ] Im Terminal nummerierte Auswahl, die unmittelbar in `add` übergeht, inklusive Scope-Rückfrage
- [ ] Ohne Terminal nur die Tabelle, keine Auswahl
- [ ] Hilfe und Ausgabe sagen ausdrücklich, dass nur Central durchsucht wird und interne Artefakte nicht erscheinen
- [ ] `--offline` meldet klar, dass `search` Netzzugriff braucht, Exit 1
- [ ] Keine Treffer ist kein Fehler: Exit 0 mit Hinweis
