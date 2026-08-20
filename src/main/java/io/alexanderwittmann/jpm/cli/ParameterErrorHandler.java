package io.alexanderwittmann.jpm.cli;

import picocli.CommandLine;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.UnmatchedArgumentException;

/**
 * Decides what the user sees when the input could not be parsed — an unknown command
 * ({@code jpm isntall}), an unknown option, a missing value.
 *
 * <p>This class exists because picocli exits with {@code 2} here by default. In our contract
 * (ADR-0007) {@code 2} is reserved for "aborted for want of input": a script has to be able to
 * tell a typo from a question that needed asking. Malformed input is simply a failure, and thus
 * {@link ExitCode#FAILURE}.
 */
final class ParameterErrorHandler implements CommandLine.IParameterExceptionHandler {

  @Override
  public int handleParseException(ParameterException ex, String[] args) {
    CommandLine command = ex.getCommandLine();
    var err = command.getErr();

    err.println(ex.getMessage());
    // A correction suggestion helps more in a failure than the full help text does, and it
    // scales as the command set grows instead of degrading. When nothing is close enough, the
    // pointer to the help remains.
    if (!UnmatchedArgumentException.printSuggestions(ex, err)) {
      err.printf("Try '%s --help' for a list of commands.%n", command.getCommandName());
    }
    return ExitCode.FAILURE;
  }
}
