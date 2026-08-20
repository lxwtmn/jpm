# Schreibende Befehle fragen nicht und legen kein Backup an

`jpm add` und `jpm remove` schreiben sofort und berichten danach; es gibt kein
Bestätigungsprompt und keine `.bak`-Datei. `--dry-run` liefert die Vorschau für alle, die
sie wollen. Ein Prompt bei jedem Aufruf machte jpm langsamer als das Copy-Paste, das es
ersetzen soll, und kauft nichts: der Schreibvorgang ist atomar und wird vor dem
abschließenden Move neu geparst, und jedes Java-Projekt liegt in Git, wo `git diff` das
bessere Backup ist als eine Datei, die später versehentlich mitcommittet wird.
Bestätigungsprompts sind nur dort wertvoll, wo es kein Undo gibt.

## Consequences

Die Meldung *nach* dem Schreiben trägt die ganze Last und muss entsprechend gut sein: was,
in welche Datei, welcher Scope, welche Version — und woher die Version stammt.

Davon strikt zu trennen sind Rückfragen bei echter **Mehrdeutigkeit** (Zielmodul im
Reaktor, Scope, ein Versionswunsch abweichend von einem Maven-BOM). Die klären eine offene
Frage; sie bestätigen nicht eine bereits getroffene Entscheidung. Diese Rückfragen bleiben.

Aus demselben Grund prüft jpm auch nicht, ob das Arbeitsverzeichnis in Git schmutzig ist —
das wäre bevormundend und störte legitime Nutzung in Skripten.
