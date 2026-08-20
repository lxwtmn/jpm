package io.alexanderwittmann.jpm.metadata;

import io.alexanderwittmann.jpm.domain.Coordinate;
import java.util.List;

/**
 * Turns "what the user asked for" into one concrete version: filter by the selector, apply the
 * stability rule, then take the highest candidate that is actually fetchable.
 */
public final class VersionResolver {

  private final VersionSource source;

  public VersionResolver(VersionSource source) {
    this.source = source;
  }

  public Resolution resolve(Coordinate coordinate, Selector selector, boolean allowPreReleases) {
    List<String> available = source.versionsOf(coordinate);
    if (available.isEmpty()) {
      return new Resolution.NoSuchCoordinate();
    }

    List<String> candidates = VersionSelector.candidates(available, selector, allowPreReleases);
    if (candidates.isEmpty()) {
      // Distinguish "only milestones exist" from "your selector matches nothing": only the first
      // is a case where --pre helps.
      boolean preReleasesOnly =
          !allowPreReleases
              && !VersionSelector.candidates(available, selector, true).isEmpty();
      return new Resolution.NoMatchingVersion(preReleasesOnly);
    }

    // Walk down rather than trusting the top entry: metadata lists versions that cannot be
    // fetched, and silently writing one of those into a POM breaks the next build.
    for (String candidate : candidates) {
      if (source.exists(coordinate, candidate)) {
        return new Resolution.Resolved(candidate);
      }
    }
    return new Resolution.NoMatchingVersion(false);
  }
}
