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
 * Exercises the built fat JAR as a file — that is, everything the unit tests structurally
 * cannot see: that the artifact really starts, that the version it reports is the version that
 * was built, that the licence notices travel with it (Apache-2.0 §4), and that the exit code
 * contract holds even when the artifact itself is broken.
 */
class JpmDistributionIT {

  private static final Path JAR = Path.of("target", "jpm.jar");

  /** Passed in by the build so the test need not derive the version from the code. */
  private static final String EXPECTED_VERSION = System.getProperty("jpm.expected.version");

  record Result(int exitCode, String out, String err) {}

  @BeforeAll
  static void preconditions() {
    assertThat(JAR).as("fat JAR from the packaging phase").exists();
    assertThat(EXPECTED_VERSION)
        .as("system property jpm.expected.version from the Failsafe configuration")
        .isNotBlank();
  }

  @Test
  @DisplayName("The fat JAR starts and reports exactly the version that was built")
  void jarReportsBuiltVersion() throws Exception {
    var java = Path.of(System.getProperty("java.home"), "bin", "java").toString();

    var result = execute(List.of(java, "-jar", JAR.toString(), "--version"));

    assertThat(result.exitCode()).as("stderr was: %s", result.err()).isEqualTo(0);
    // Not merely "some version-shaped string", but the one filtered from project.version.
    assertThat(result.out().strip()).isEqualTo("jpm " + EXPECTED_VERSION);
  }

  @Test
  @DisplayName("The fat JAR carries licence and notices, produced by the shade transformer")
  void jarCarriesLicenseAndNotice() throws IOException {
    try (var jar = new JarFile(JAR.toFile())) {
      assertThat(readEntry(jar, "META-INF/LICENSE"))
          .as("Apache-2.0 §4 requires the licence inside the artifact")
          .contains("Apache License")
          .contains("Version 2.0")
          .contains("Alexander Wittmann Consulting GmbH");

      var notice = readEntry(jar, "META-INF/NOTICE");
      // This header can only come from the ApacheNoticeResourceTransformer. Without asserting
      // it, the test would stay green if the transformer were removed — the file would then
      // silently come from the <resources> block alone.
      assertThat(notice)
          .as("proves the shade transformer actually runs")
          .contains("section 4d of The Apache License");
      assertThat(notice)
          .as("our own notice text survives the merge")
          .contains("jpm")
          .contains("picocli");
      assertThat(notice)
          .as("the notices of the bundled Apache components are merged in, not dropped")
          .contains("The Apache Software Foundation");

      // Not every bundled component is Apache-2.0, and neither of these ships its licence text
      // inside its own JAR. Redistribution requires the text, so jpm has to carry it.
      assertThat(readEntry(jar, "META-INF/licenses/slf4j-MIT.txt"))
          .contains("QOS.ch")
          .contains("DEALINGS IN THE SOFTWARE");
      assertThat(readEntry(jar, "META-INF/licenses/asm-BSD-3-Clause.txt"))
          .contains("INRIA")
          .contains("Neither the name")
          .contains("POSSIBILITY OF SUCH DAMAGE");
      assertThat(notice)
          .as("NOTICE has to name the non-Apache licences, or the texts are unfindable")
          .contains("MIT License")
          .contains("BSD 3-Clause");
    }
  }

  @Test
  @DisplayName("A broken artifact reports the failure and honours the exit code contract")
  void brokenArtifactReportsErrorInsteadOfStackTrace(@TempDir Path tempDir) throws Exception {
    var broken = tempDir.resolve("jpm-broken.jar");
    copyJarWithout(JAR, broken, "jpm.properties");
    var java = Path.of(System.getProperty("java.home"), "bin", "java").toString();

    var result = execute(List.of(java, "-jar", broken.toString(), "--version"));

    assertThat(result.exitCode()).as("ADR-0007: failure is 1, not the JVM default").isEqualTo(1);
    assertThat(result.err()).contains("jpm.properties");
    assertThat(result.err()).as("a stack trace is not a user message").doesNotContain("\tat ");
    assertThat(result.out()).isEmpty();
  }

  @Test
  @DisplayName("The launcher starts jpm and passes the exit code through")
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
    assertThat(ok.exitCode()).as("stderr was: %s", ok.err()).isEqualTo(0);
    assertThat(ok.out()).contains("jpm " + EXPECTED_VERSION);

    var failure = execute(unknown);
    assertThat(failure.exitCode()).as("the launcher must not swallow the code").isEqualTo(1);
  }

  private static Result execute(List<String> command) throws Exception {
    var process = new ProcessBuilder(command).start();
    var out = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    var err = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
    return new Result(process.waitFor(), out, err);
  }

  private static String readEntry(JarFile jar, String name) throws IOException {
    var entry = jar.getEntry(name);
    assertThat(entry).as("entry %s inside the fat JAR", name).isNotNull();
    try (var in = jar.getInputStream(entry)) {
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  /** Copies the JAR without one entry, to reproduce an incorrectly built artifact. */
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
