# 01 — Lauffähiges Skelett mit `jpm --version`

**What to build:** Ein Nutzer kann jpm bauen und aufrufen. `jpm --version` gibt die Version
aus und beendet sauber. Die ganze Kette von Quelltext über Fat-JAR bis Launcher steht,
bevor ein einziges Feature existiert — das Skelett läuft, damit alle folgenden Tickets
etwas haben, an das sie andocken können.

**Blocked by:** None — kann sofort starten.

**Status:** ready-for-agent

- [ ] Maven-Projekt baut mit Java 17 als Zielversion
- [ ] picocli ist eingebunden; ein unbekannter Befehl erzeugt eine verständliche Meldung und Exit 1
- [ ] Der Build erzeugt ein ausführbares Fat-JAR
- [ ] Launcher für Windows und Unix rufen das JAR auf
- [ ] `jpm --version` gibt die Version aus, Exit 0
- [ ] `jpm --help` zeigt die Befehlsübersicht
