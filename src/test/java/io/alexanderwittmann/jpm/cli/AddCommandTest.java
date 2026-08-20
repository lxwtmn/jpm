package io.alexanderwittmann.jpm.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * End-to-end tests for {@code jpm add} at the CLI boundary: what the user types goes in, what the
 * shell and the file system end up with comes out.
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

  @TempDir Path project;

  record Result(int exitCode, String out, String err) {}

  private Result run(String... args) {
    var out = new ByteArrayOutputStream();
    var err = new ByteArrayOutputStream();
    int exitCode;
    try (var outStream = new PrintStream(out, true, StandardCharsets.UTF_8);
        var errStream = new PrintStream(err, true, StandardCharsets.UTF_8)) {
      exitCode = new JpmCli().run(project, args, outStream, errStream);
    }
    return new Result(
        exitCode, out.toString(StandardCharsets.UTF_8), err.toString(StandardCharsets.UTF_8));
  }

  private String pom() throws Exception {
    return Files.readString(project.resolve("pom.xml"), StandardCharsets.UTF_8);
  }

  @Test
  @DisplayName("Adds the dependency to the POM and reports what it wrote where")
  void addsDependencyAndReportsIt() throws Exception {
    Files.writeString(project.resolve("pom.xml"), POM, StandardCharsets.UTF_8);

    var result = run("add", "com.fasterxml.jackson.core:jackson-databind@2.17.1");

    assertThat(result.exitCode()).as("stderr was: %s", result.err()).isEqualTo(0);
    assertThat(pom()).contains("<artifactId>jackson-databind</artifactId>").contains("2.17.1");
    assertThat(result.out())
        .contains("pom.xml")
        .contains("com.fasterxml.jackson.core:jackson-databind")
        .contains("2.17.1")
        .contains("compile");
  }

  @Test
  @DisplayName("--dry-run prints the result and leaves the file alone")
  void dryRunLeavesTheFileAlone() throws Exception {
    Files.writeString(project.resolve("pom.xml"), POM, StandardCharsets.UTF_8);

    var result = run("add", "com.fasterxml.jackson.core:jackson-databind@2.17.1", "--dry-run");

    assertThat(result.exitCode()).isEqualTo(0);
    assertThat(result.out()).contains("jackson-databind");
    assertThat(pom()).isEqualTo(POM);
  }

  @Test
  @DisplayName("An already declared coordinate is reported, and succeeds without changing anything")
  void alreadyDeclaredCoordinateIsReported() throws Exception {
    Files.writeString(project.resolve("pom.xml"), POM, StandardCharsets.UTF_8);

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
  @DisplayName("The install alias behaves the same as add")
  void installAliasWorks() throws Exception {
    Files.writeString(project.resolve("pom.xml"), POM, StandardCharsets.UTF_8);

    var result = run("install", "com.fasterxml.jackson.core:jackson-databind@2.17.1");

    assertThat(result.exitCode()).as("stderr was: %s", result.err()).isEqualTo(0);
    assertThat(pom()).contains("jackson-databind");
  }
}
