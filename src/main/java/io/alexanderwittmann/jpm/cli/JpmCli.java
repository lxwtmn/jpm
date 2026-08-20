package io.alexanderwittmann.jpm.cli;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.file.Path;
import picocli.CommandLine;

/**
 * The boundary the launcher script calls: a working directory, arguments and output streams in,
 * exit code out. All three are passed in rather than read from process state, so that behaviour
 * stays observable without a test having to change the process it runs in.
 */
public final class JpmCli {

  public int run(Path workingDirectory, String[] args, PrintStream out, PrintStream err) {
    var command =
        new CommandLine(new JpmCommand())
            .addSubcommand("add", new AddCommand(workingDirectory), "install", "i");

    return command
        .setOut(new PrintWriter(out, true))
        .setErr(new PrintWriter(err, true))
        .setParameterExceptionHandler(new ParameterErrorHandler())
        .setExecutionExceptionHandler(new ExecutionErrorHandler())
        .execute(args);
  }
}
