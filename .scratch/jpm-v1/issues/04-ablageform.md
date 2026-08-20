# 04 — Ablageform der Version an die Projektkonvention anpassen

**What to build:** In einem Projekt, das Versionen über `<properties>` führt, legt jpm
ebenfalls eine Property an statt inline zu schreiben — und umgekehrt. Der Diff soll
aussehen, als hätte ein Mensch aus dem Team den Eintrag gemacht; genau daran entscheidet
sich, ob das Werkzeug im Team akzeptiert wird.

**Blocked by:** 02

**Status:** ready-for-agent

- [ ] jpm erkennt die überwiegende Ablageform des Projekts und folgt ihr
- [ ] Im Property-Modus lautet der Default-Name `artifactId.version`
- [ ] Existiert bereits eine Property mit der Version eines Artefakts derselben groupId, wird sie wiederverwendet statt eine zweite anzulegen
- [ ] `--property` und `--inline` übersteuern die Erkennung
- [ ] Ein Projekt ohne bestehende Dependencies fällt auf inline zurück
- [ ] Golden-File-Korpus um gemischte Konventionen erweitert
