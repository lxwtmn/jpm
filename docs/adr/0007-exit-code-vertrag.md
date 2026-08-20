# Exit-Codes: Informationsbefehle scheitern nie an ihrem eigenen Inhalt

Der Vertrag lautet: `0` Erfolg, `1` Fehler, `2` Abbruch wegen fehlender Eingabe (kein TTY,
aber eine Rückfrage wäre nötig gewesen). Insbesondere liefert `jpm outdated` **immer 0**,
wenn es technisch funktioniert hat — auch wenn es Updates gefunden hat. Wer CI daran
scheitern lassen will, nimmt `--fail-on-outdated`.

`npm outdated` macht es umgekehrt, und das Ergebnis ist bekannt: Skriptautoren hängen
`|| true` an und schalten damit die echte Fehlererkennung gleich mit ab. „Es gibt Updates"
ist eine Information, kein Fehler.

## Consequences

Der eigene Code `2` erlaubt Skripten, „ich hätte fragen müssen" von „es ging kaputt" zu
unterscheiden — nötig, weil mehrere Befehle bei Mehrdeutigkeit ohne TTY bewusst abbrechen
statt zu raten.
