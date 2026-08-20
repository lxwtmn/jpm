package io.alexanderwittmann.jpm.cli;

import static org.assertj.core.api.Assertions.assertThat;

import io.alexanderwittmann.jpm.domain.Coordinate;
import io.alexanderwittmann.jpm.metadata.VersionSource;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * End-to-end tests for {@code jpm add} at the CLI boundary: what the user types goes in, what the
 * shell and the file system end up with comes out.
 *
 * <p>The version source is a stand-in so these stay offline and fast. That the real resolver
 * behaves is proven separately by {@code ResolverVersionSourceIT}.
 */
class AddCommandTest {

  private static final String POM =
      """
      <?xml version="1.0" encoding="UTF-8"?>
      <project xmlns="http://maven.apache.org/POM/4.0.0">
        <modelVersion>4.0.0</modelVersion>
        <groupId>com.example</groupId>
        <artifactId>demo</artifactId>
        <version>1.0.0</version>

        <dependencies>
          <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
            <version>2.0.13</version>
          </dependency>
        </dependencies>
      </project>
      """;

  private static final Coordinate JACKSON =
      new Coordinate("com.fasterxml.jackson.core", "jackson-databind");
  private static final Coordinate SLF4J = new Coordinate("org.slf4j", "slf4j-api");

  /** Lists fixed versions; everything it lists is treated as actually fetchable. */
  private record FakeSource(Map<Coordinate, List<String>> versions) implements VersionSource {

    @Override
    public List<String> versionsOf(Coordinate coordinate) {
      return versions.getOrDefault(coordinate, List.of());
    }

    @Override
    public boolean exists(Coordinate coordinate, String version) {
      return versionsOf(coordinate).contains(version);
    }
  }

  private static final Map<Coordinate, List<String>> CATALOGUE =
      Map.of(
          JACKSON, List.of("2.15.4", "2.16.2", "2.17.1", "2.18.0-RC1", "3.0.0-M1"),
          SLF4J, List.of("2.0.13", "2.0.16"));

  @TempDir Path project;

  /** What the command line actually handed to the version source on the last run. */
  private boolean requestedOffline;

  private boolean requestedRefresh;

  record Result(int exitCode, String out, String err) {}

  private Result run(String... args) {
    var out = new ByteArrayOutputStream();
    var err = new ByteArrayOutputStream();
    int exitCode;
    try (var outStream = new PrintStream(out, true, StandardCharsets.UTF_8);
        var errStream = new PrintStream(err, true, StandardCharsets.UTF_8)) {
      var cli =
          new JpmCli(
              (offline, refresh) -> {
                requestedOffline = offline;
                requestedRefresh = refresh;
                return new FakeSource(CATALOGUE);
              });
      exitCode = cli.run(project, args, outStream, errStream);
    }
    return new Result(
        exitCode, out.toString(StandardCharsets.UTF_8), err.toString(StandardCharsets.UTF_8));
  }

  private void givenPom() throws Exception {
    Files.writeString(project.resolve("pom.xml"), POM, StandardCharsets.UTF_8);
  }

  private String pom() throws Exception {
    return Files.readString(project.resolve("pom.xml"), StandardCharsets.UTF_8);
  }

  @Test
  @DisplayName("Without a selector, resolves and writes the latest stable version")
  void resolvesLatestStableWhenNoSelectorGiven() throws Exception {
    givenPom();

    var result = run("add", "com.fasterxml.jackson.core:jackson-databind");

    assertThat(result.exitCode()).as("stderr was: %s", result.err()).isEqualTo(0);
    // 2.18.0-RC1 and 3.0.0-M1 are newer and must be skipped.
    assertThat(pom()).contains("<version>2.17.1</version>");
    assertThat(result.out()).contains("2.17.1").contains("latest stable").contains("compile");
  }

  @Test
  @DisplayName("A selector picks the newest version in that line")
  void selectorPicksNewestInLine() throws Exception {
    givenPom();

    var result = run("add", "com.fasterxml.jackson.core:jackson-databind@2.16");

    assertThat(result.exitCode()).isEqualTo(0);
    assertThat(pom()).contains("<version>2.16.2</version>");
  }

  @Test
  @DisplayName("--pre lets a milestone win")
  void preReleasesOnRequest() throws Exception {
    givenPom();

    var result = run("add", "com.fasterxml.jackson.core:jackson-databind", "--pre");

    assertThat(result.exitCode()).isEqualTo(0);
    assertThat(pom()).contains("<version>3.0.0-M1</version>");
  }

  @Test
  @DisplayName("A version written out in full is honoured even when it is a pre-release")
  void exactPreReleaseIsHonoured() throws Exception {
    givenPom();

    var result = run("add", "com.fasterxml.jackson.core:jackson-databind@2.18.0-RC1");

    assertThat(result.exitCode()).as("stderr was: %s", result.err()).isEqualTo(0);
    assertThat(pom()).contains("<version>2.18.0-RC1</version>");
  }

  @Test
  @DisplayName("An unknown coordinate fails with a message naming it")
  void unknownCoordinateIsReported() throws Exception {
    givenPom();

    var result = run("add", "no.such:artifact");

    assertThat(result.exitCode()).isEqualTo(1);
    assertThat(result.err()).contains("no.such:artifact");
    assertThat(pom()).isEqualTo(POM);
  }

  @Test
  @DisplayName("A selector that matches nothing fails without misleading advice")
  void unmatchedSelectorIsReported() throws Exception {
    givenPom();

    var result = run("add", "com.fasterxml.jackson.core:jackson-databind@9.9");

    assertThat(result.exitCode()).isEqualTo(1);
    assertThat(result.err()).contains("9.9");
    // Nothing in the 9.9 line exists at all, stable or not — suggesting --pre would send the
    // user down a road that leads nowhere.
    assertThat(result.err()).doesNotContain("--pre");
    assertThat(pom()).isEqualTo(POM);
  }

  @Test
  @DisplayName("--pre is advised exactly when only pre-releases match")
  void preIsAdvisedWhenItWouldHelp() throws Exception {
    givenPom();

    // The 2.18 line holds only 2.18.0-RC1, so here --pre genuinely is the way forward.
    var result = run("add", "com.fasterxml.jackson.core:jackson-databind@2.18");

    assertThat(result.exitCode()).isEqualTo(1);
    assertThat(result.err()).contains("2.18").contains("--pre");
  }

  @Test
  @DisplayName("An unknown coordinate is named as such, not as a version problem")
  void unknownCoordinateIsNotAVersionProblem() throws Exception {
    givenPom();

    var result = run("add", "no.such:artifact");

    assertThat(result.err()).contains("unknown coordinate");
    assertThat(result.err()).doesNotContain("--pre");
  }

  @Test
  @DisplayName("--dry-run prints the result and leaves the file alone")
  void dryRunLeavesTheFileAlone() throws Exception {
    givenPom();

    var result = run("add", "com.fasterxml.jackson.core:jackson-databind@2.17.1", "--dry-run");

    assertThat(result.exitCode()).isEqualTo(0);
    assertThat(result.out()).contains("jackson-databind");
    assertThat(pom()).isEqualTo(POM);
  }

  @Test
  @DisplayName("An already declared coordinate is reported, and succeeds without changing anything")
  void alreadyDeclaredCoordinateIsReported() throws Exception {
    givenPom();

    var result = run("add", "org.slf4j:slf4j-api@2.0.16");

    // Idempotent on purpose: running `jpm add` twice must not break a provisioning script.
    assertThat(result.exitCode()).isEqualTo(0);
    assertThat(result.out()).contains("org.slf4j:slf4j-api").contains("2.0.13");
    assertThat(pom()).isEqualTo(POM);
  }

  @Test
  @DisplayName("An unparsable POM aborts the run without writing anything")
  void unparsablePomAbortsWithoutWriting() throws Exception {
    var broken = "<project><dependencies>";
    Files.writeString(project.resolve("pom.xml"), broken, StandardCharsets.UTF_8);

    var result = run("add", "com.fasterxml.jackson.core:jackson-databind@2.17.1");

    assertThat(result.exitCode()).isEqualTo(1);
    assertThat(result.err()).isNotEmpty();
    assertThat(pom()).isEqualTo(broken);
  }

  @Test
  @DisplayName("A directory without a pom.xml fails with a message naming the directory")
  void missingPomIsReported() {
    var result = run("add", "com.fasterxml.jackson.core:jackson-databind@2.17.1");

    assertThat(result.exitCode()).isEqualTo(1);
    assertThat(result.err()).contains("pom.xml").contains(project.toString());
  }

  @Test
  @DisplayName("--offline and --refresh are global flags and reach the version source")
  void globalFlagsReachTheVersionSource() throws Exception {
    givenPom();

    // DESIGN section 5 lists these as global, so they must work before the subcommand as well.
    // Without this test the flags could be silently inert and every test would still pass.
    run("--offline", "add", "com.fasterxml.jackson.core:jackson-databind@2.17.1");
    assertThat(requestedOffline).isTrue();
    assertThat(requestedRefresh).isFalse();

    run("--refresh", "add", "com.fasterxml.jackson.core:jackson-databind@2.16.2");
    assertThat(requestedRefresh).isTrue();
    assertThat(requestedOffline).isFalse();
  }

  @Test
  @DisplayName("The install alias behaves the same as add")
  void installAliasWorks() throws Exception {
    givenPom();

    var result = run("install", "com.fasterxml.jackson.core:jackson-databind@2.17.1");

    assertThat(result.exitCode()).as("stderr was: %s", result.err()).isEqualTo(0);
    assertThat(pom()).contains("jackson-databind");
  }
}
