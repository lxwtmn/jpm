package io.alexanderwittmann.jpm.cli;

import io.alexanderwittmann.jpm.domain.Coordinate;
import io.alexanderwittmann.jpm.domain.Dependency;
import io.alexanderwittmann.jpm.metadata.Resolution;
import io.alexanderwittmann.jpm.metadata.Selector;
import io.alexanderwittmann.jpm.metadata.VersionResolver;
import io.alexanderwittmann.jpm.metadata.VersionSourceFactory;
import io.alexanderwittmann.jpm.pom.AddResult;
import io.alexanderwittmann.jpm.pom.PomEditor;
import io.alexanderwittmann.jpm.pom.PomFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/** Adds a dependency to the project's POM, resolving the version when none was given. */
@Command(
    name = "add",
    description = "Add a dependency to the project's pom.xml.",
    mixinStandardHelpOptions = true)
final class AddCommand implements Callable<Integer> {

  @Parameters(
      index = "0",
      paramLabel = "<groupId:artifactId[@selector]>",
      description =
          "The dependency to add. Without a selector the latest stable version is used; "
              + "a selector such as @2.15 picks the newest version in that line. "
              + "Naming a version in full also allows a pre-release without --pre.")
  private String target;

  @Option(names = "--pre", description = "Allow pre-releases such as milestones and candidates.")
  private boolean allowPreReleases;



  @Option(names = "--dry-run", description = "Print the resulting POM instead of writing it.")
  private boolean dryRun;

  @Spec private CommandSpec spec;

  @ParentCommand private JpmCommand parent;

  private final Path workingDirectory;
  private final VersionSourceFactory versionSources;

  AddCommand(Path workingDirectory, VersionSourceFactory versionSources) {
    this.workingDirectory = workingDirectory;
    this.versionSources = versionSources;
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

    var request = Request.parse(target);
    var resolver = new VersionResolver(versionSources.create(parent.offline(), parent.refresh()));
    var resolution = resolver.resolve(request.coordinate(), request.selector(), allowPreReleases);

    if (!(resolution instanceof Resolution.Resolved resolved)) {
      err.printf("jpm: %s%n", failureMessage(request, resolution));
      return ExitCode.FAILURE;
    }

    var dependency = new Dependency(request.coordinate(), resolved.version());
    var result = PomEditor.addDependency(PomFile.read(pomPath), dependency);
    var displayPath = workingDirectory.relativize(pomPath);

    if (result instanceof AddResult.AlreadyPresent present) {
      // Reaching the state the user asked for is not a failure, even when nothing was written:
      // running `jpm add` twice must not break a provisioning script.
      out.printf(
          "%s is already declared in %s%s; nothing changed.%n",
          request.coordinate(),
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
        "Added %s %s (scope compile, %s) to %s%n",
        request.coordinate(), dependency.version(), request.origin(), displayPath);
    return ExitCode.SUCCESS;
  }

  private static String failureMessage(Request request, Resolution resolution) {
    if (resolution instanceof Resolution.NoSuchCoordinate) {
      // Do not suggest --pre here: someone who mistyped a groupId is not helped by being sent
      // after milestones.
      // Plain ASCII on purpose: an em dash arrives as a replacement character in the Windows
      // console, and a message the user cannot read is a message that failed.
      return "unknown coordinate "
          + request.coordinate()
          + ": the configured artifact repositories list no versions for it";
    }

    var advice =
        resolution instanceof Resolution.NoMatchingVersion missing && missing.preReleasesOnly()
            ? " (only pre-releases match; try --pre)"
            : "";
    if (request.selector() instanceof Selector.Prefix prefix) {
      return "no version matching '" + prefix.prefix() + "' for " + request.coordinate() + advice;
    }
    return "no stable version for " + request.coordinate() + advice;
  }

  /** What the user typed, split into the coordinate and the selector that follows the {@code @}. */
  private record Request(Coordinate coordinate, Selector selector, String origin) {

    static Request parse(String target) {
      int at = target.lastIndexOf('@');
      var coordinateText = at < 0 ? target : target.substring(0, at);
      var selectorText = at < 0 ? null : target.substring(at + 1);

      int colon = coordinateText.indexOf(':');
      if (colon < 0 || coordinateText.length() == colon + 1 || (at >= 0 && selectorText.isBlank())) {
        throw new IllegalArgumentException(
            "expected groupId:artifactId[@selector], but got '" + target + "'");
      }

      var coordinate =
          new Coordinate(coordinateText.substring(0, colon), coordinateText.substring(colon + 1));
      return selectorText == null
          ? new Request(coordinate, new Selector.Latest(), "latest stable")
          : new Request(coordinate, new Selector.Prefix(selectorText), "matching " + selectorText);
    }
  }
}
