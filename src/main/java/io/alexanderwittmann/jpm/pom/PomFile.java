package io.alexanderwittmann.jpm.pom;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Reads and writes POM files without going through a character-normalising layer.
 *
 * <p>Writing follows DESIGN E-5: produce the result, verify it by parsing, write it to a
 * temporary file beside the target and only then move it into place. If verification fails the
 * original is never touched — a half-written POM is worse than no edit at all. There is
 * deliberately no backup file: every Java project lives in Git, where {@code git diff} is the
 * better record and does not end up committed by accident (ADR-0006).
 */
public final class PomFile {

  private PomFile() {}

  public static String read(Path path) throws IOException {
    return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
  }

  public static void write(Path path, String content) throws IOException {
    PomParser.requireParsable(content);

    var directory = path.toAbsolutePath().getParent();
    // The temporary file has to share a filesystem with the target, otherwise the move cannot be
    // atomic.
    var temporary = Files.createTempFile(directory, ".jpm-", ".xml");
    try {
      Files.write(temporary, content.getBytes(StandardCharsets.UTF_8));
      moveIntoPlace(temporary, path);
    } finally {
      Files.deleteIfExists(temporary);
    }
  }

  private static void moveIntoPlace(Path temporary, Path target) throws IOException {
    try {
      Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
    } catch (AtomicMoveNotSupportedException e) {
      // Rare, but real on some network filesystems. A plain replace is still far better than
      // writing into the target directly.
      Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
    }
  }
}
