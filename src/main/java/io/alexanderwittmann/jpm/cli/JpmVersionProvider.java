package io.alexanderwittmann.jpm.cli;

import java.io.IOException;
import java.util.Properties;
import picocli.CommandLine;

/**
 * Liefert die Version aus {@code jpm.properties}, das beim Bau aus {@code project.version}
 * gefiltert wird. Fehlt die Datei, ist das Artefakt fehlerhaft gebaut — dann soll der Aufruf
 * scheitern statt eine Ersatzangabe zu erfinden.
 */
final class JpmVersionProvider implements CommandLine.IVersionProvider {

  private static final String RESOURCE = "/jpm.properties";

  @Override
  public String[] getVersion() {
    try (var in = JpmVersionProvider.class.getResourceAsStream(RESOURCE)) {
      if (in == null) {
        throw new IllegalStateException(RESOURCE + " is missing from the artifact");
      }
      var properties = new Properties();
      properties.load(in);
      var version = properties.getProperty("version");
      if (version == null || version.isBlank()) {
        throw new IllegalStateException(RESOURCE + " contains no version");
      }
      return new String[] {"jpm " + version};
    } catch (IOException e) {
      throw new IllegalStateException(RESOURCE + " could not be read", e);
    }
  }
}
