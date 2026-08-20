# 09 — `jpm outdated`

**What to build:** Ein Nutzer sieht auf einen Blick, welche Dependencies veraltet sind — und
was davon überhaupt einzeln anhebbar ist, weil verwaltete Einträge am Maven-BOM hängen. Der
Befehl scheitert nie daran, dass er etwas gefunden hat.

**Blocked by:** 03, 06

**Status:** ready-for-agent

- [ ] Tabelle mit Koordinate, aktueller und neuester stabiler Version
- [ ] Verwaltete Dependencies werden als solche ausgewiesen, samt verwaltendem Artefakt
- [ ] Metadaten werden parallel abgerufen und aus dem Cache bedient
- [ ] Exit 0 auch dann, wenn Updates gefunden wurden
- [ ] `--fail-on-outdated` liefert Exit 1 bei Treffern
- [ ] Im Reaktor werden alle Maven-Module berücksichtigt und in der Ausgabe zugeordnet
- [ ] `--offline` arbeitet aus dem Cache und kennzeichnet, was nicht ermittelt werden konnte
