package io.alexanderwittmann.jpm.cli;

import picocli.CommandLine;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.UnmatchedArgumentException;

/**
 * Entscheidet, was der Nutzer sieht, wenn die Eingabe nicht geparst werden konnte — ein
 * unbekannter Befehl ({@code jpm isntall}), eine unbekannte Option, ein fehlender Wert.
 *
 * <p>Diese Klasse existiert, weil picocli hier standardmäßig mit {@code 2} beendet. In unserem
 * Vertrag (ADR-0007) ist {@code 2} aber für „Abbruch wegen fehlender Eingabe" reserviert: ein
 * Skript muss einen Tippfehler von einer nötigen Rückfrage unterscheiden können. Fehleingaben
 * sind schlicht Fehler und damit {@link ExitCode#FAILURE}.
 *
 * <p>Nützlich für die Umsetzung:
 *
 * <ul>
 *   <li>{@code ex.getMessage()} — picocli formuliert bereits eine Meldung, die das nicht
 *       erkannte Argument enthält, z. B. {@code Unmatched argument at index 0: 'isntall'}
 *   <li>{@code ex.getCommandLine().getErr()} — der {@code PrintWriter} für stderr; alles
 *       Fehlerhafte gehört dorthin, damit stdout auswertbar bleibt
 *   <li>{@code UnmatchedArgumentException.printSuggestions(ex, writer)} — picocli kann
 *       „Did you mean" von sich aus, ohne dass wir eine Ähnlichkeitssuche bauen
 *   <li>{@code ex.getCommandLine().usage(writer)} — die vollständige Hilfe
 * </ul>
 */
final class ParameterErrorHandler implements CommandLine.IParameterExceptionHandler {

  @Override
  public int handleParseException(ParameterException ex, String[] args) {
    CommandLine command = ex.getCommandLine();
    var err = command.getErr();

    err.println(ex.getMessage());
    // Ein Korrekturvorschlag hilft im Fehlerfall mehr als die vollständige Hilfe, und er
    // skaliert mit wachsendem Befehlssatz, statt dabei schlechter zu werden. Gibt es nichts
    // Ähnliches, bleibt der Verweis auf die Hilfe.
    if (!UnmatchedArgumentException.printSuggestions(ex, err)) {
      err.printf("Try '%s --help' for a list of commands.%n", command.getCommandName());
    }
    return ExitCode.FAILURE;
  }
}
