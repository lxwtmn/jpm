package io.alexanderwittmann.jpm.cli;

import java.io.IOException;
import java.util.Properties;
import picocli.CommandLine;

/**
 * Supplies the version from {@code jpm.properties}, which the build filters from
 * {@code project.version}. If the file is missing the artifact was built incorrectly, and the
 * invocation should fail rather than invent a substitute.
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
