package io.alexanderwittmann.jpm.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests at the boundary the launcher script calls: arguments and output streams in, exit code
 * out. Only what the user sees and what the shell receives is observed — never picocli
 * internals.
 *
 * <p>The expected exit codes appear as literal numbers rather than {@code ExitCode} constants on
 * purpose: ADR-0007 fixes those numbers, so the test must not derive them from the very code it
 * checks — otherwise it would stay green if somebody changed the constant.
 */
class JpmCliTest {

  record Result(int exitCode, String out, String err) {}

  private Result run(String... args) {
    var out = new ByteArrayOutputStream();
    var err = new ByteArrayOutputStream();
    int exitCode;
    try (var outStream = new PrintStream(out, true, StandardCharsets.UTF_8);
        var errStream = new PrintStream(err, true, StandardCharsets.UTF_8)) {
      exitCode = new JpmCli().run(Path.of("."), args, outStream, errStream);
    }
    return new Result(
        exitCode, out.toString(StandardCharsets.UTF_8), err.toString(StandardCharsets.UTF_8));
  }

  @Test
  @DisplayName("--version states the name and the version and exits with 0")
  void versionPrintsNameAndVersion() {
    var result = run("--version");

    assertThat(result.exitCode()).isEqualTo(0);
    assertThat(result.out()).containsPattern("jpm \\d+\\.\\d+\\.\\d+");
  }

  @Test
  @DisplayName("--help shows the usage line, the purpose and the available options")
  void helpShowsUsageAndOptions() {
    var result = run("--help");

    assertThat(result.exitCode()).isEqualTo(0);
    assertThat(result.out())
        .contains("Usage:")
        .contains("jpm")
        .contains("pom.xml")
        .contains("--version")
        .contains("--help");
  }

  @Test
  @DisplayName("An unknown command is reported on stderr and exits with 1")
  void unknownCommandFailsWithExitCodeOne() {
    var result = run("isntall");

    assertThat(result.exitCode()).isEqualTo(1);
    assertThat(result.err()).contains("isntall");
    // Failures belong on stderr: stdout has to stay parseable (--json from issue #12 onwards).
    assertThat(result.out()).isEmpty();
  }

  @Test
  @DisplayName("Without arguments jpm shows the help and exits with 0")
  void noArgumentsShowsHelp() {
    var result = run();

    // No command is not malformed input but the question "what can you do?".
    assertThat(result.exitCode()).isEqualTo(0);
    assertThat(result.out()).contains("Usage:").contains("jpm");
    assertThat(result.err()).isEmpty();
  }
}
