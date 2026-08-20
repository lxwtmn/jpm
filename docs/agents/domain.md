# Domain-Dokumentation

Wie die Engineering-Skills die Domänendokumentation dieses Repos lesen sollen, bevor sie
den Code erkunden.

Dieses Repo ist **single-context**: eine `CONTEXT.md` und ein `docs/adr/` im
Wurzelverzeichnis.

## Vor dem Erkunden lesen

- **[`CONTEXT.md`](../../CONTEXT.md)** im Wurzelverzeichnis — das Glossar
- **[`docs/adr/`](../adr/)** — die ADRs, die den Bereich berühren, in dem gearbeitet wird

Es gibt kein `CONTEXT-MAP.md`; sollte eines entstehen, ist das Repo multi-context geworden
und die Struktur ändert sich entsprechend.

## Dateistruktur

```
/
├── CONTEXT.md
├── DESIGN.md
├── docs/
│   ├── adr/
│   └── agents/
└── .scratch/<effort>/issues/
```

## Das Vokabular des Glossars verwenden

Benennt eine Ausgabe ein Domänenkonzept — in einem Tickettitel, einem Refactoring-Vorschlag,
einer Hypothese, einem Testnamen —, gilt der Begriff so, wie `CONTEXT.md` ihn definiert.
Kein Abdriften in Synonyme, die das Glossar unter `_Avoid_` ausschließt.

**Besonders wichtig in diesem Repo:** Drei Begriffe sind doppelt belegt und werden **nie
unqualifiziert** verwendet, auch nicht in Bezeichnern oder Commit-Nachrichten:

- **BOM** → entweder *Maven-BOM* oder *Byte Order Mark*
- **Modul** → entweder *Maven-Modul* (im Reaktor des Nutzers) oder *jpm-Modul* (in jpms Code)
- **Repository** → entweder *Artefakt-Repository* oder *Git-Repository*

Fehlt ein benötigter Begriff im Glossar, ist das ein Signal: entweder wird Sprache
erfunden, die das Projekt nicht benutzt (überdenken), oder es gibt eine echte Lücke (für
`/domain-modeling` notieren).

## Widersprüche zu ADRs benennen

Widerspricht eine Ausgabe einem bestehenden ADR, muss das ausdrücklich gesagt werden statt
still übergangen:

> _Widerspricht ADR-0003 (Entscheidungen fallen gegen das effektive Modell) — aber
> wiederaufzumachen, weil…_
