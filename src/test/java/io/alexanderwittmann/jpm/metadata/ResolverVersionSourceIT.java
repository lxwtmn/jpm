package io.alexanderwittmann.jpm.metadata;

import static org.assertj.core.api.Assertions.assertThat;

import io.alexanderwittmann.jpm.domain.Coordinate;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The one test that really talks to Maven Central. A stand-in source can prove the resolution
 * logic but never the wiring, and the wiring is what is new and risky in this ticket.
 *
 * <p>Assertions are deliberately about facts that cannot change: versions already published cannot
 * be unpublished from Central, whereas "the newest version" changes without notice.
 */
class ResolverVersionSourceIT {

  private static final Coordinate SLF4J = new Coordinate("org.slf4j", "slf4j-api");

  @TempDir Path cache;

  @Test
  @DisplayName("Reads real versions from Maven Central and resolves a stable one")
  void readsVersionsFromCentral() {
    var source = ResolverVersionSource.create(cache, false, false);

    var versions = source.versionsOf(SLF4J);

    assertThat(versions).contains("2.0.13", "1.7.36");

    var resolution = new VersionResolver(source).resolve(SLF4J, new Selector.Latest(), false);

    assertThat(resolution).isInstanceOf(Resolution.Resolved.class);
    var chosen = ((Resolution.Resolved) resolution).version();
    assertThat(VersionSelector.isPreRelease(chosen))
        .as("whatever Central's newest slf4j-api is today, it must not be a pre-release")
        .isFalse();
  }

  @Test
  @DisplayName("Confirms a real artifact exists and a made-up version does not")
  void checksActualAvailability() {
    var source = ResolverVersionSource.create(cache, false, false);

    assertThat(source.exists(SLF4J, "2.0.13")).isTrue();
    assertThat(source.exists(SLF4J, "99.99.99")).isFalse();
  }

  @Test
  @DisplayName("Caches into jpm's own directory and never into Maven's local repository")
  void cachesIntoOwnDirectoryOnly() throws Exception {
    var mavenLocalBefore = mavenLocalRepositoryFingerprint();

    ResolverVersionSource.create(cache, false, false).versionsOf(SLF4J);

    // P5: jpm reads Maven's state but never writes into it. A resolver handed ~/.m2 would start
    // populating it on the very first lookup, so this is not a theoretical guarantee.
    try (var entries = Files.walk(cache)) {
      assertThat(entries.filter(Files::isRegularFile).findAny())
          .as("jpm's own cache directory received the metadata")
          .isPresent();
    }
    assertThat(mavenLocalRepositoryFingerprint())
        .as("~/.m2/repository/org/slf4j was not touched")
        .isEqualTo(mavenLocalBefore);
  }

  /** A cheap stand-in for "did anything appear under ~/.m2 for this coordinate". */
  private static long mavenLocalRepositoryFingerprint() throws Exception {
    var directory =
        Path.of(System.getProperty("user.home"), ".m2", "repository", "org", "slf4j", "slf4j-api");
    if (!Files.isDirectory(directory)) {
      return -1;
    }
    try (var entries = Files.walk(directory)) {
      return entries.filter(Files::isRegularFile).count();
    }
  }
}
