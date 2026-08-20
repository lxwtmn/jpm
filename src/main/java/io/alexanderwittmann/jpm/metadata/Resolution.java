package io.alexanderwittmann.jpm.metadata;

/**
 * The outcome of resolving a version. Three distinct cases rather than an empty optional, because
 * they call for three different messages: "that coordinate does not exist" is a different problem
 * from "it exists but nothing matches what you asked for", and suggesting {@code --pre} to someone
 * who mistyped a groupId sends them the wrong way.
 */
public sealed interface Resolution {

  /** A version was found and verified as fetchable. */
  record Resolved(String version) implements Resolution {}

  /** The repositories list no versions at all for the coordinate. */
  record NoSuchCoordinate() implements Resolution {}

  /**
   * Versions exist, but none satisfies the selector and the stability rule. {@code preReleasesOnly}
   * distinguishes "there are only milestones" — where {@code --pre} is the right advice — from
   * "your selector matches nothing", where it is not.
   */
  record NoMatchingVersion(boolean preReleasesOnly) implements Resolution {}
}
