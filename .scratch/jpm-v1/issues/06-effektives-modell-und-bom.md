# 06 — Effektives Modell und Maven-BOM-Awareness

**What to build:** In einem Projekt, dessen Versionen von einem Maven-BOM oder Parent-Pom
verwaltet werden, fügt jpm die Dependency **ohne** `<version>` ein und sagt, woher die
Version kommt. Ein abweichender Versionswunsch übersteuert das BOM nie stillschweigend.

Vorführbar an einem echten Spring-Boot-Projekt. Das ist der Punkt, an dem jpm besser wird
als Copy-Paste — und die Grundlage, auf der `update` später konsistent bleibt.

**Blocked by:** 03

**Status:** ready-for-agent

- [ ] Das effektive Modell wird inklusive Parent-Pom und per Import eingebundener Maven-BOMs aufgelöst
- [ ] Eine dort verwaltete Koordinate wird ohne `<version>` eingetragen
- [ ] Die Meldung nennt das verwaltende Artefakt und die daraus resultierende Version
- [ ] Ein ausdrücklich abweichender Versionswunsch warnt und fragt im Terminal zurück; ohne Terminal Abbruch mit Exit 2
- [ ] Für verwaltete Koordinaten findet keine Versionsauflösung statt
- [ ] Golden-File-Korpus um ein BOM-verwaltetes Projekt erweitert
