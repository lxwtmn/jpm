package io.alexanderwittmann.jpm.pom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The writer gets a seam of its own because its most important guarantee is unreachable from the
 * command line: a correct editor never produces invalid XML, so the "verification failed, keep
 * the original" path can only be exercised by handing the writer broken content directly.
 */
class PomFileTest {

  private static final String VALID =
      "<project><modelVersion>4.0.0</modelVersion></project>\n";

  @Test
  @DisplayName("Leaves the original untouched when the new content does not parse")
  void keepsOriginalWhenNewContentIsUnparsable(@TempDir Path dir) throws Exception {
    var pom = dir.resolve("pom.xml");
    Files.writeString(pom, VALID, StandardCharsets.UTF_8);

    assertThatThrownBy(() -> PomFile.write(pom, "<project><oops>"))
        .isInstanceOf(MalformedPomException.class);

    assertThat(Files.readString(pom, StandardCharsets.UTF_8)).isEqualTo(VALID);
  }

  @Test
  @DisplayName("Round-trips content byte for byte, including CRLF and a byte order mark")
  void roundTripsBytesExactly(@TempDir Path dir) throws Exception {
    var pom = dir.resolve("pom.xml");
    var content = "﻿<project>\r\n  <modelVersion>4.0.0</modelVersion>\r\n</project>\r\n";

    PomFile.write(pom, content);

    assertThat(PomFile.read(pom)).isEqualTo(content);
    assertThat(Files.readAllBytes(pom)).startsWith((byte) 0xEF, (byte) 0xBB, (byte) 0xBF);
  }

  @Test
  @DisplayName("Leaves no temporary files behind after a failed write")
  void leavesNoTemporaryFilesBehind(@TempDir Path dir) throws Exception {
    var pom = dir.resolve("pom.xml");
    Files.writeString(pom, VALID, StandardCharsets.UTF_8);

    assertThatThrownBy(() -> PomFile.write(pom, "<project><oops>"))
        .isInstanceOf(MalformedPomException.class);

    try (var entries = Files.list(dir)) {
      assertThat(entries).containsExactly(pom);
    }
  }
}
