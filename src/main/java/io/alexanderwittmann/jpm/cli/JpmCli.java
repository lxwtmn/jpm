package io.alexanderwittmann.jpm.cli;

import io.alexanderwittmann.jpm.metadata.VersionSourceFactory;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.file.Path;
import picocli.CommandLine;

/**
 * The boundary the launcher script calls: a working directory, arguments and output streams in,
 * exit code out. All of them are passed in rather than read from process state, so that behaviour
 * stays observable without a test having to change the process it runs in — and the same reasoning
 * applies to the version source, which is injected so the command line can be exercised offline.
 */
public final class JpmCli {

  private final VersionSourceFactory versionSources;

  public JpmCli() {
    this(VersionSourceFactory.live());
  }

  public JpmCli(VersionSourceFactory versionSources) {
    this.versionSources = versionSources;
  }

  public int run(Path workingDirectory, String[] args, PrintStream out, PrintStream err) {
    var command =
        new CommandLine(new JpmCommand())
            .addSubcommand("add", new AddCommand(workingDirectory, versionSources), "install", "i");

    return command
        .setOut(new PrintWriter(out, true))
        .setErr(new PrintWriter(err, true))
        .setParameterExceptionHandler(new ParameterErrorHandler())
        .setExecutionExceptionHandler(new ExecutionErrorHandler())
        .execute(args);
  }
}
