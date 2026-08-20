package io.alexanderwittmann.jpm.cli;

import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

/** Der Wurzelbefehl. Unterbefehle kommen mit den folgenden Tickets hinzu. */
@Command(
    name = "jpm",
    description = "Manage Maven dependencies without editing pom.xml by hand.",
    mixinStandardHelpOptions = true,
    versionProvider = JpmVersionProvider.class)
final class JpmCommand implements Callable<Integer> {

  @Spec private CommandSpec spec;

  @Override
  public Integer call() {
    // Ein Aufruf ohne Befehl ist keine Fehleingabe, sondern die Frage „was kannst du?" —
    // also die Hilfe auf stdout, nicht auf stderr, und Exit 0.
    spec.commandLine().usage(spec.commandLine().getOut());
    return ExitCode.SUCCESS;
  }
}
