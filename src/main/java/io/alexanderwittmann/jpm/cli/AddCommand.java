package io.alexanderwittmann.jpm.cli;

import io.alexanderwittmann.jpm.pom.AddResult;
import io.alexanderwittmann.jpm.pom.Coordinate;
import io.alexanderwittmann.jpm.pom.Dependency;
import io.alexanderwittmann.jpm.pom.PomEditor;
import io.alexanderwittmann.jpm.pom.PomFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/** Adds a dependency to the project's POM. */
@Command(
    name = "add",
    description = "Add a dependency to the project's pom.xml.",
    mixinStandardHelpOptions = true)
final class AddCommand implements Callable<Integer> {

  @Parameters(
      index = "0",
      paramLabel = "<groupId:artifactId@version>",
      description = "The dependency to add, for example com.google.guava:guava@33.2.1-jre")
  private String target;

  @Option(names = "--dry-run", description = "Print the resulting POM instead of writing it.")
  private boolean dryRun;

  @Spec private CommandSpec spec;

  private final Path workingDirectory;

  AddCommand(Path workingDirectory) {
    this.workingDirectory = workingDirectory;
  }

  @Override
  public Integer call() throws Exception {
    var out = spec.commandLine().getOut();
    var err = spec.commandLine().getErr();

    var pomPath = workingDirectory.resolve("pom.xml");
    if (!Files.isRegularFile(pomPath)) {
      err.printf("jpm: no pom.xml in %s%n", workingDirectory);
      return ExitCode.FAILURE;
    }

    var dependency = parse(target);
    var result = PomEditor.addDependency(PomFile.read(pomPath), dependency);
    var displayPath = workingDirectory.relativize(pomPath);

    if (result instanceof AddResult.AlreadyPresent present) {
      // Reaching the state the user asked for is not a failure, even when nothing was written:
      // running `jpm add` twice must not break a provisioning script.
      out.printf(
          "%s is already declared in %s%s; nothing changed.%n",
          dependency.coordinate(),
          displayPath,
          present.presentVersion() == null ? "" : " at " + present.presentVersion());
      return ExitCode.SUCCESS;
    }

    var added = (AddResult.Added) result;
    if (dryRun) {
      out.print(added.pom());
      out.flush();
      return ExitCode.SUCCESS;
    }

    PomFile.write(pomPath, added.pom());
    out.printf(
        "Added %s %s (scope compile) to %s%n",
        dependency.coordinate(), dependency.version(), displayPath);
    return ExitCode.SUCCESS;
  }

  /**
   * Parses {@code groupId:artifactId@version}. Resolving an omitted version is issue #3; until
   * then saying so plainly beats guessing.
   */
  private static Dependency parse(String target) {
    int at = target.lastIndexOf('@');
    if (at < 0) {
      throw new IllegalArgumentException(
          "a version is required for now: use groupId:artifactId@version");
    }
    var coordinate = target.substring(0, at);
    var version = target.substring(at + 1);
    int colon = coordinate.indexOf(':');
    if (colon < 0 || version.isBlank()) {
      throw new IllegalArgumentException(
          "expected groupId:artifactId@version, but got '" + target + "'");
    }
    return new Dependency(
        new Coordinate(coordinate.substring(0, colon), coordinate.substring(colon + 1)), version);
  }
}
