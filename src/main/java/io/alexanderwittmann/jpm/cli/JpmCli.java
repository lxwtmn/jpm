package io.alexanderwittmann.jpm.cli;

import java.io.PrintStream;
import java.io.PrintWriter;
import picocli.CommandLine;

/**
 * The boundary the launcher script calls: arguments and output streams in, exit code out. The
 * streams are passed in rather than taken from {@code System.out} so that behaviour stays
 * observable without touching global state.
 */
public final class JpmCli {

  public int run(String[] args, PrintStream out, PrintStream err) {
    return new CommandLine(new JpmCommand())
        .setOut(new PrintWriter(out, true))
        .setErr(new PrintWriter(err, true))
        .setParameterExceptionHandler(new ParameterErrorHandler())
        .setExecutionExceptionHandler(new ExecutionErrorHandler())
        .execute(args);
  }
}
