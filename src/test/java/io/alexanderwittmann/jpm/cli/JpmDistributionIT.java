package io.alexanderwittmann.jpm.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Prüft das gebaute Fat-JAR als Datei — also das, was die Unit-Tests strukturell nicht sehen
 * können: dass das Artefakt wirklich startet, dass die gemeldete Version die gebaute ist, dass
 * die Lizenzhinweise mitreisen (Apache-2.0 §4) und dass der Exit-Code-Vertrag auch dann hält,
 * wenn das Artefakt selbst defekt ist.
 */
class JpmDistributionIT {

  private static final Path JAR = Path.of("target", "jpm.jar");

  /** Vom Build durchgereicht, damit der Test die Version nicht aus dem Code ableiten muss. */
  private static final String EXPECTED_VERSION = System.getProperty("jpm.expected.version");

  record Result(int exitCode, String out, String err) {}

  @BeforeAll
  static void preconditions() {
    assertThat(JAR).as("Fat-JAR aus dem Packaging").exists();
    assertThat(EXPECTED_VERSION)
        .as("Systemeigenschaft jpm.expected.version aus der Failsafe-Konfiguration")
        .isNotBlank();
  }

  @Test
  @DisplayName("Das Fat-JAR startet und meldet genau die gebaute Version")
  void jarReportsBuiltVersion() throws Exception {
    var java = Path.of(System.getProperty("java.home"), "bin", "java").toString();

    var result = execute(List.of(java, "-jar", JAR.toString(), "--version"));

    assertThat(result.exitCode()).as("stderr war: %s", result.err()).isEqualTo(0);
    // Nicht bloß „irgendeine Versionsform", sondern die aus project.version gefilterte.
    assertThat(result.out().strip()).isEqualTo("jpm " + EXPECTED_VERSION);
  }

  @Test
  @DisplayName("Das Fat-JAR führt Lizenz und Hinweise mit, erzeugt durch den Shade-Transformer")
  void jarCarriesLicenseAndNotice() throws IOException {
    try (var jar = new JarFile(JAR.toFile())) {
      assertThat(readEntry(jar, "META-INF/LICENSE"))
          .as("Apache-2.0 §4 verlangt die Lizenz im Artefakt")
          .contains("Apache License")
          .contains("Version 2.0")
          .contains("Alexander Wittmann Consulting GmbH");

      var notice = readEntry(jar, "META-INF/NOTICE");
      // Dieser Kopf stammt ausschließlich vom ApacheNoticeResourceTransformer. Ohne die
      // Zusicherung bliebe der Test grün, wenn der Transformer entfernt würde — die Datei
      // käme dann still allein aus dem <resources>-Block.
      assertThat(notice)
          .as("beweist, dass der Shade-Transformer tatsächlich läuft")
          .contains("section 4d of The Apache License");
      assertThat(notice)
          .as("der eigene Hinweistext überlebt das Zusammenführen")
          .contains("jpm")
          .contains("picocli");
    }
  }

  @Test
  @DisplayName("Ein defektes Artefakt meldet den Fehler und hält den Exit-Code-Vertrag")
  void brokenArtifactReportsErrorInsteadOfStackTrace(@TempDir Path tempDir) throws Exception {
    var broken = tempDir.resolve("jpm-broken.jar");
    copyJarWithout(JAR, broken, "jpm.properties");
    var java = Path.of(System.getProperty("java.home"), "bin", "java").toString();

    var result = execute(List.of(java, "-jar", broken.toString(), "--version"));

    assertThat(result.exitCode()).as("ADR-0007: Fehler ist 1, nicht der JVM-Default").isEqualTo(1);
    assertThat(result.err()).contains("jpm.properties");
    assertThat(result.err()).as("ein Stacktrace ist keine Nutzermeldung").doesNotContain("\tat ");
    assertThat(result.out()).isEmpty();
  }

  @Test
  @DisplayName("Der Launcher startet jpm und reicht den Exit-Code durch")
  void launcherRunsJpmAndPropagatesExitCode() throws Exception {
    var windows = System.getProperty("os.name").toLowerCase(Locale.ROOT).startsWith("win");
    List<String> version =
        windows
            ? List.of("cmd", "/c", "bin\\jpm.cmd", "--version")
            : List.of("sh", "bin/jpm", "--version");
    List<String> unknown =
        windows
            ? List.of("cmd", "/c", "bin\\jpm.cmd", "isntall")
            : List.of("sh", "bin/jpm", "isntall");

    var ok = execute(version);
    assertThat(ok.exitCode()).as("stderr war: %s", ok.err()).isEqualTo(0);
    assertThat(ok.out()).contains("jpm " + EXPECTED_VERSION);

    var failure = execute(unknown);
    assertThat(failure.exitCode()).as("der Launcher darf den Code nicht schlucken").isEqualTo(1);
  }

  private static Result execute(List<String> command) throws Exception {
    var process = new ProcessBuilder(command).start();
    var out = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    var err = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
    return new Result(process.waitFor(), out, err);
  }

  private static String readEntry(JarFile jar, String name) throws IOException {
    var entry = jar.getEntry(name);
    assertThat(entry).as("Eintrag %s im Fat-JAR", name).isNotNull();
    try (var in = jar.getInputStream(entry)) {
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  /** Kopiert das JAR ohne einen Eintrag, um ein fehlerhaft gebautes Artefakt nachzustellen. */
  private static void copyJarWithout(Path source, Path target, String entryToDrop)
      throws IOException {
    try (var in = new JarFile(source.toFile());
        var out = new JarOutputStream(Files.newOutputStream(target))) {
      var entries = in.entries();
      while (entries.hasMoreElements()) {
        var entry = entries.nextElement();
        if (entry.getName().equals(entryToDrop)) {
          continue;
        }
        out.putNextEntry(new JarEntry(entry.getName()));
        if (!entry.isDirectory()) {
          try (var content = in.getInputStream(entry)) {
            content.transferTo(out);
          }
        }
        out.closeEntry();
      }
    }
  }
}
