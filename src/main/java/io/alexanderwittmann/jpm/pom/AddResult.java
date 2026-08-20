package io.alexanderwittmann.jpm.pom;

/** What happened when a dependency was offered to a POM. */
public sealed interface AddResult {

  /** The dependency was inserted; {@code pom} is the edited source. */
  record Added(String pom) implements AddResult {}

  /**
   * The coordinate was already declared, so nothing was written. A POM carrying the same
   * coordinate twice is a defect, and the state the caller asked for has already been reached.
   *
   * <p>{@code presentVersion} is {@code null} when the existing entry carries no {@code
   * <version>} — that is a managed dependency, which issue #6 gives its own treatment.
   */
  record AlreadyPresent(String presentVersion) implements AddResult {}
}
