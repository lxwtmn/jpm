package io.alexanderwittmann.jpm.cli;

import picocli.CommandLine;
import picocli.CommandLine.ParseResult;

/**
 * Catches failures that only surface while a command runs — unlike {@link
 * ParameterErrorHandler}, which covers the parse path alone.
 *
 * <p>Without this class an exception would escape {@link JpmCli#run} and the user would be shown
 * a Java stack trace, while {@code System.exit} in {@link Main} would never be reached — the
 * exit code would then come from the JVM default instead of from ADR-0007. That is precisely
 * what would undermine P6 ("scriptability is a contract").
 *
 * <p>The stack trace stays suppressed on purpose: it is not a user-facing message. A switch that
 * reveals it belongs to the diagnostic tooling of later tickets.
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
