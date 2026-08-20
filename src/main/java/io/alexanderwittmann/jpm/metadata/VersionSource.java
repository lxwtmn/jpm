package io.alexanderwittmann.jpm.metadata;

import io.alexanderwittmann.jpm.domain.Coordinate;
import java.util.List;

/**
 * Where version information comes from. An interface rather than a class so that resolution logic
 * can be exercised without a network, and so the live wiring is the only thing an integration test
 * has to cover.
 */
public interface VersionSource {

  /** Every version the configured artifact repositories list for the coordinate, in any order. */
  List<String> versionsOf(Coordinate coordinate);

  /**
   * Whether the version can actually be fetched. Metadata also lists versions that were deleted or
   * never uploaded, so a listing is a claim rather than proof (DESIGN D-2).
   */
  boolean exists(Coordinate coordinate, String version);
}
