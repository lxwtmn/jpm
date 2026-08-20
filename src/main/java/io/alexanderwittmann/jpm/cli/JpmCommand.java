package io.alexanderwittmann.jpm.cli;

import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

/** The root command. Subcommands arrive with the following tickets. */
@Command(
    name = "jpm",
    description = "Manage Maven dependencies without editing pom.xml by hand.",
    mixinStandardHelpOptions = true,
    versionProvider = JpmVersionProvider.class)
final class JpmCommand implements Callable<Integer> {

  @Spec private CommandSpec spec;

  @Override
  public Integer call() {
    // Invoking jpm without a command is not a malformed input but the question "what can you
    // do?" — so the help goes to stdout, not stderr, and the exit code is 0.
    spec.commandLine().usage(spec.commandLine().getOut());
    return ExitCode.SUCCESS;
  }
}
