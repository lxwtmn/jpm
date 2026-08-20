# 03 — Neueste stabile Version auflösen

**What to build:** Ein Nutzer kann die Version weglassen. jpm ermittelt die neueste stabile
Version aus den im Projekt konfigurierten Artefakt-Repositories und trägt sie exakt ein.
Vorabversionen werden übersprungen, sofern sie nicht ausdrücklich verlangt werden. Damit
entfällt der Weg über mvnrepository.com.

**Blocked by:** 02

**Status:** ready-for-agent

- [ ] `add` ohne Versionsangabe trägt die neueste stabile Version ein
- [ ] Qualifier wie alpha, beta, milestone, rc und SNAPSHOT gelten als instabil und werden übersprungen
- [ ] `--pre` erlaubt Vorabversionen
- [ ] Ein Präfix-Selektor wie `@2.15` wählt die höchste passende Version
- [ ] Die gewählte Version wird auf tatsächliche Verfügbarkeit geprüft; schlägt das fehl, wird die nächstniedrigere genommen
- [ ] Artefakt-Repositories, Mirrors, Proxy und Authentifizierung stammen aus `settings.xml` und funktionieren gegen eine private Instanz
- [ ] Metadaten werden zwischengespeichert; `--refresh` erzwingt Neuladen, `--offline` verbietet Netzzugriff
- [ ] Eine unbekannte Koordinate erzeugt eine klare Meldung und Exit 1
