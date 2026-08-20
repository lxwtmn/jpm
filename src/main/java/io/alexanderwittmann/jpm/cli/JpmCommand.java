package io.alexanderwittmann.jpm.cli;

import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ScopeType;
import picocli.CommandLine.Spec;

/** The root command. Subcommands arrive with the following tickets. */
@Command(
    name = "jpm",
    description = "Manage Maven dependencies without editing pom.xml by hand.",
    mixinStandardHelpOptions = true,
    versionProvider = JpmVersionProvider.class)
final class JpmCommand implements Callable<Integer> {

  // DESIGN section 5 lists these as global flags. Declaring them here with INHERIT scope means
  // `jpm --offline add ...` works and every future subcommand gets them for free, instead of each
  // one redeclaring the same two options.
  @Option(
      names = "--offline",
      scope = ScopeType.INHERIT,
      description = "Work from the cache only; never touch the network.")
  private boolean offline;

  @Option(
      names = "--refresh",
      scope = ScopeType.INHERIT,
      description = "Ignore cached metadata and ask the repositories again.")
  private boolean refresh;

  @Spec private CommandSpec spec;

  boolean offline() {
    return offline;
  }

  boolean refresh() {
    return refresh;
  }

  @Override
  public Integer call() {
    // Invoking jpm without a command is not a malformed input but the question "what can you
    // do?" — so the help goes to stdout, not stderr, and the exit code is 0.
    spec.commandLine().usage(spec.commandLine().getOut());
    return ExitCode.SUCCESS;
  }
}
