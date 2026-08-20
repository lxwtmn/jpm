package io.alexanderwittmann.jpm.cli;

import picocli.CommandLine;
import picocli.CommandLine.ParseResult;

/**
 * Fängt Fehler ab, die erst beim Ausführen eines Befehls auftreten — im Gegensatz zu
 * {@link ParameterErrorHandler}, der nur den Parse-Pfad abdeckt.
 *
 * <p>Ohne diese Klasse verlässt eine Exception {@link JpmCli#run} und der Nutzer bekommt einen
 * Java-Stacktrace zu sehen, während {@code System.exit} in {@link Main} nie erreicht wird — der
 * Exit-Code käme dann vom JVM-Default statt aus ADR-0007. Genau das würde P6 („Skriptbarkeit
 * ist ein Vertrag") aushebeln.
 *
 * <p>Der Stacktrace bleibt bewusst unterdrückt: er ist keine Nutzermeldung. Ein Schalter, der
 * ihn sichtbar macht, gehört zur Diagnose-Ausstattung späterer Tickets.
 */
final class ExecutionErrorHandler implements CommandLine.IExecutionExceptionHandler {

  @Override
  public int handleExecutionException(Exception ex, CommandLine command, ParseResult parseResult) {
    var message = ex.getMessage();
    command
        .getErr()
        .printf(
            "jpm: %s%n",
            message == null || message.isBlank() ? ex.getClass().getSimpleName() : message);
    return ExitCode.FAILURE;
  }
}
