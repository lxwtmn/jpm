package io.alexanderwittmann.jpm.metadata;

/**
 * What the user asked for after the {@code @} in a command. A selector decides which version is
 * chosen at the moment the command runs; it never reaches the POM, where only the exact chosen
 * version is written (ADR-0002).
 */
public sealed interface Selector {

  /** No selector given: take the latest stable version. */
  record Latest() implements Selector {}

  /**
   * A version named in full. Naming a version explicitly is intent, so an exact selector is
   * honoured even when the version is a pre-release.
   */
  record Exact(String version) implements Selector {}

  /**
   * A leading part of a version, such as {@code 2.15}. Matching is by segment, not by characters:
   * {@code 2.1} selects from the {@code 2.1} line and does not reach {@code 2.10}.
   */
  record Prefix(String prefix) implements Selector {}
}
