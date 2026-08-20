package io.alexanderwittmann.jpm.metadata;

import java.nio.file.Path;
import java.util.Locale;

/**
 * Where jpm keeps the metadata it has fetched. Deliberately not {@code ~/.m2}: P5 says jpm reads
 * Maven's state but never writes into it (DESIGN D-4).
 */
public final class CacheDirectory {

  private CacheDirectory() {}

  public static Path resolve() {
    var home = Path.of(System.getProperty("user.home"));

    if (System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("win")) {
      var localAppData = System.getenv("LOCALAPPDATA");
      var base = localAppData == null || localAppData.isBlank() ? home : Path.of(localAppData);
      return base.resolve("jpm").resolve("cache");
    }

    var xdg = System.getenv("XDG_CACHE_HOME");
    var base = xdg == null || xdg.isBlank() ? home.resolve(".cache") : Path.of(xdg);
    return base.resolve("jpm");
  }
}
