# 02 — `jpm add` mit exakter Version in eine Single-Modul-pom

**What to build:** Ein Nutzer kann in einem Projekt mit einer einzelnen pom eine Dependency
mit ausdrücklich genannter Version hinzufügen. Die Datei behält Formatierung, Kommentare,
Einrückung, Zeilenenden und eine eventuelle Byte-Order-Mark-Signatur unverändert bei — nur
der neue Block kommt hinzu. Danach berichtet jpm, was wohin geschrieben wurde.

Das ist der schmalste vollständige Pfad durch das System und zugleich sein riskantester
Teil. Die Golden-File-Testinfrastruktur entsteht hier und wächst mit jedem späteren Ticket,
das neue pom-Situationen einführt.

**Blocked by:** 01

**Status:** ready-for-agent

- [ ] `add` mit Koordinate und exakter Version fügt einen Dependency-Block ein, Scope `compile`, Version inline
- [ ] Fehlt der `<dependencies>`-Abschnitt, wird er angelegt
- [ ] Ist die Koordinate bereits vorhanden, wird das gemeldet statt doppelt eingetragen
- [ ] Der Schreibvorgang ist atomar; das Ergebnis wird vor dem Übernehmen neu geparst, andernfalls bleibt das Original unangetastet
- [ ] Eine nicht parsebare Ausgangs-pom führt zum Abbruch, ohne dass geschrieben wird
- [ ] `--dry-run` zeigt das Ergebnis, ohne zu schreiben
- [ ] Golden-File-Korpus deckt ab: Kommentare zwischen Blöcken, CRLF-Zeilenenden, Byte Order Mark, leerer und fehlender `<dependencies>`-Abschnitt
- [ ] Die Meldung nach dem Schreiben nennt Datei, Koordinate, Version und Scope
