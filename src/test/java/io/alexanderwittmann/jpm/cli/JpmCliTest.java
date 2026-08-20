package io.alexanderwittmann.jpm.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests an der Grenze, die das Launcher-Skript aufruft: Argumente und Ausgabeströme hinein,
 * Exit-Code heraus. Beobachtet wird ausschließlich, was der Nutzer sieht und was die Shell
 * bekommt — nie picocli-Interna.
 *
 * <p>Die erwarteten Exit-Codes stehen bewusst als Zahlen im Test und nicht als {@code ExitCode}
 * -Konstanten: ADR-0007 legt die Zahlen fest, also darf der Test sie nicht aus demselben Code
 * ableiten, den er prüft — sonst bliebe er grün, wenn jemand die Konstante ändert.
 */
class JpmCliTest {

  record Result(int exitCode, String out, String err) {}

  private Result run(String... args) {
    var out = new ByteArrayOutputStream();
    var err = new ByteArrayOutputStream();
    int exitCode;
    try (var outStream = new PrintStream(out, true, StandardCharsets.UTF_8);
        var errStream = new PrintStream(err, true, StandardCharsets.UTF_8)) {
      exitCode = new JpmCli().run(args, outStream, errStream);
    }
    return new Result(
        exitCode, out.toString(StandardCharsets.UTF_8), err.toString(StandardCharsets.UTF_8));
  }

  @Test
  @DisplayName("--version nennt Name und Version und beendet mit 0")
  void versionPrintsNameAndVersion() {
    var result = run("--version");

    assertThat(result.exitCode()).isEqualTo(0);
    assertThat(result.out()).containsPattern("jpm \\d+\\.\\d+\\.\\d+");
  }

  @Test
  @DisplayName("--help zeigt Aufrufzeile, Zweck und die verfügbaren Optionen")
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
  @DisplayName("Ein unbekannter Befehl meldet ihn auf stderr und beendet mit 1")
  void unknownCommandFailsWithExitCodeOne() {
    var result = run("isntall");

    assertThat(result.exitCode()).isEqualTo(1);
    assertThat(result.err()).contains("isntall");
    // Fehler gehören nach stderr: stdout muss auswertbar bleiben (--json ab Issue #12).
    assertThat(result.out()).isEmpty();
  }

  @Test
  @DisplayName("Ohne Argumente zeigt jpm die Hilfe und beendet mit 0")
  void noArgumentsShowsHelp() {
    var result = run();

    // Kein Befehl ist keine Fehleingabe, sondern die Frage „was kannst du?".
    assertThat(result.exitCode()).isEqualTo(0);
    assertThat(result.out()).contains("Usage:").contains("jpm");
    assertThat(result.err()).isEmpty();
  }
}
