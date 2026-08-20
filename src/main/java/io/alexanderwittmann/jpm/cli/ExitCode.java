package io.alexanderwittmann.jpm.cli;

/**
 * The exit code contract from ADR-0007. Informational commands never fail over their own
 * content — {@code outdated} returns {@link #SUCCESS} even when it found updates.
 */
final class ExitCode {

  /** Success. */
  static final int SUCCESS = 0;

  /** Failure. */
  static final int FAILURE = 1;

  /** Aborted because a question would have been necessary but no terminal was available. */
  static final int NEEDS_INPUT = 2;

  private ExitCode() {}
}
