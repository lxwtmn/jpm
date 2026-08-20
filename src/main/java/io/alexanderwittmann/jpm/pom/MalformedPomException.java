package io.alexanderwittmann.jpm.pom;

/**
 * Raised when the source POM cannot be parsed. jpm then aborts without writing anything: a file
 * it cannot read is a file it must not touch.
 */
public final class MalformedPomException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public MalformedPomException(String message, Throwable cause) {
    super(message, cause);
  }
}
