package io.alexanderwittmann.jpm.cli;

/**
 * Der Exit-Code-Vertrag aus ADR-0007. Informationsbefehle scheitern nie an ihrem eigenen
 * Inhalt — {@code outdated} liefert auch dann {@link #SUCCESS}, wenn es Updates gefunden hat.
 */
final class ExitCode {

  /** Erfolg. */
  static final int SUCCESS = 0;

  /** Fehler. */
  static final int FAILURE = 1;

  /** Abbruch, weil eine Rückfrage nötig gewesen wäre, aber kein Terminal zur Verfügung stand. */
  static final int NEEDS_INPUT = 2;

  private ExitCode() {}
}
