package io.alexanderwittmann.jpm.metadata;

import static org.assertj.core.api.Assertions.assertThat;

import io.alexanderwittmann.jpm.domain.Coordinate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Resolution logic against a stand-in source. The wiring to the real Maven Resolver is proven
 * separately by a live integration test — a stand-in can prove the logic but never the plumbing.
 */
class VersionResolverTest {

  private static final Coordinate JACKSON =
      new Coordinate("com.fasterxml.jackson.core", "jackson-databind");

  /** Serves fixed version lists, and can be told which of them actually resolve. */
  private record FakeSource(Map<Coordinate, List<String>> versions, Set<String> missing)
      implements VersionSource {

    @Override
    public List<String> versionsOf(Coordinate coordinate) {
      return versions.getOrDefault(coordinate, List.of());
    }

    @Override
    public boolean exists(Coordinate coordinate, String version) {
      return !missing.contains(version);
    }
  }

  private static VersionResolver resolverWith(List<String> versions, String... missing) {
    return new VersionResolver(new FakeSource(Map.of(JACKSON, versions), Set.of(missing)));
  }

  @Test
  @DisplayName("Resolves the latest stable version")
  void resolvesLatestStable() {
    var resolver = resolverWith(List.of("2.16.0", "2.17.1", "6.0.0-M1"));

    assertThat(resolver.resolve(JACKSON, new Selector.Latest(), false))
        .isEqualTo(new Resolution.Resolved("2.17.1"));
  }

  @Test
  @DisplayName("Falls back to the next lower version when the newest is not actually there")
  void fallsBackWhenTheNewestIsMissing() {
    // Metadata lists versions that were deleted or never uploaded; the listing is not proof.
    var resolver = resolverWith(List.of("2.16.0", "2.17.0", "2.17.1"), "2.17.1");

    assertThat(resolver.resolve(JACKSON, new Selector.Latest(), false))
        .isEqualTo(new Resolution.Resolved("2.17.0"));
  }

  @Test
  @DisplayName("Reports an unknown coordinate as such, not as a version problem")
  void unknownCoordinateIsItsOwnOutcome() {
    var resolver = resolverWith(List.of());

    // Distinct from "no matching version": someone who mistyped a groupId must not be sent after
    // milestones with --pre.
    assertThat(resolver.resolve(new Coordinate("no.such", "artifact"), new Selector.Latest(), false))
        .isEqualTo(new Resolution.NoSuchCoordinate());
  }

  @Test
  @DisplayName("Says so when only pre-releases exist, because then --pre is the right advice")
  void reportsWhenOnlyPreReleasesExist() {
    var resolver = resolverWith(List.of("1.0.0-M1", "1.0.0-RC1"));

    assertThat(resolver.resolve(JACKSON, new Selector.Latest(), false))
        .isEqualTo(new Resolution.NoMatchingVersion(true));
  }

  @Test
  @DisplayName("Does not advertise --pre when the selector simply matches nothing")
  void doesNotAdvertisePreWhenSelectorMatchesNothing() {
    var resolver = resolverWith(List.of("1.0.0", "2.0.0"));

    assertThat(resolver.resolve(JACKSON, new Selector.Prefix("9.9"), false))
        .isEqualTo(new Resolution.NoMatchingVersion(false));
  }

  @Test
  @DisplayName("Reports no match when every candidate turns out to be missing")
  void nothingWhenEveryCandidateIsMissing() {
    var resolver = resolverWith(List.of("1.0.0", "1.0.1"), "1.0.0", "1.0.1");

    assertThat(resolver.resolve(JACKSON, new Selector.Latest(), false))
        .isEqualTo(new Resolution.NoMatchingVersion(false));
  }
}
