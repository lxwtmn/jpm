package io.alexanderwittmann.jpm.metadata;

/**
 * Raised when version information could not be obtained at all — the network is unreachable, a
 * repository rejected the request, or {@code settings.xml} cannot be read. Distinct from "the
 * coordinate has no matching version", which is an ordinary empty answer rather than a failure.
 */
public final class MetadataUnavailableException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public MetadataUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
