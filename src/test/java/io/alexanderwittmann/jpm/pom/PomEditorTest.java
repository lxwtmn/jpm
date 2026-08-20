package io.alexanderwittmann.jpm.pom;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Golden-file tests for the POM editor. Each case is a directory under
 * {@code src/test/resources/pom-corpus} holding an {@code input.xml} and the
 * {@code expected.xml} it must turn into.
 *
 * <p>The comparison is deliberately over the entire file rather than over the inserted block:
 * the failure this editor exists to avoid is a *formatting* failure — shifted indentation, a
 * swallowed comment, a rewritten line ending. Only a whole-file comparison can see it.
 */
class PomEditorTest {

  private static final Dependency JACKSON =
      new Dependency(
          new Coordinate("com.fasterxml.jackson.core", "jackson-databind"), "2.17.1");

  @Test
  @DisplayName("Appends to an existing dependencies section, leaving everything else untouched")
  void appendsToExistingDependenciesSection() {
    assertCase("existing-dependencies", JACKSON);
  }

  @Test
  @DisplayName("Fills an empty dependencies section, deriving the indent from the document")
  void fillsEmptyDependenciesSection() {
    assertCase("empty-dependencies", JACKSON);
  }

  @Test
  @DisplayName("Creates a missing dependencies section at the position the POM schema requires")
  void createsMissingDependenciesSectionInSchemaOrder() {
    // <project> is a sequence in the Maven 4.0.0 schema: <dependencies> has to precede <build>,
    // so appending at the end of the document would produce a POM that parses but does not
    // validate.
    assertCase("no-dependencies-section", JACKSON);
  }

  @Test
  @DisplayName("Falls back to just before </project> when no later section exists to anchor on")
  void createsMissingDependenciesSectionBeforeProjectEnd() {
    assertCase("minimal-pom", JACKSON);
  }

  @Test
  @DisplayName("Leaves comments, blank lines and the licence header exactly where they were")
  void preservesComments() {
    assertCase("comments-between-entries", JACKSON);
  }

  @Test
  @DisplayName("Writes CRLF line endings into a CRLF file, without converting the rest")
  void preservesCrlfLineEndings() {
    // The default on Windows, and the classic way a format-preserving editor betrays itself:
    // the inserted block arrives with LF while every other line keeps CRLF.
    assertCase("crlf-line-endings", JACKSON);
  }

  @Test
  @DisplayName("Keeps a leading byte order mark instead of silently dropping it")
  void preservesByteOrderMark() {
    assertCase("byte-order-mark", JACKSON);
  }

  @Test
  @DisplayName("Reports a coordinate that is already present instead of adding it twice")
  void reportsCoordinateAlreadyPresent() {
    var input = corpusFile("existing-dependencies", "input.xml");
    // A different version on purpose: presence is decided by coordinate, not by coordinate plus
    // version — which is why Coordinate is a type of its own.
    var slf4j = new Dependency(new Coordinate("org.slf4j", "slf4j-api"), "2.0.16");

    var result = PomEditor.addDependency(input, slf4j);

    assertThat(result)
        .isInstanceOf(AddResult.AlreadyPresent.class)
        .extracting(r -> ((AddResult.AlreadyPresent) r).presentVersion())
        .isEqualTo("2.0.13");
  }

  @Test
  @DisplayName("Turns a self-closing <dependencies/> into a real section instead of a sibling")
  void expandsSelfClosingDependenciesSection() {
    // Appending after <dependencies/> produces a <dependency> that is a sibling of the section.
    // That is well formed, so a parse check accepts it — and Maven then refuses the POM.
    assertCase("self-closing-dependencies", JACKSON);
  }

  @Test
  @DisplayName("Writes prefixed elements into a namespace-prefixed POM")
  void handlesNamespacePrefixedPom() {
    assertCase("namespace-prefixed", JACKSON);
  }

  @Test
  @DisplayName("Locates positions correctly in a POM without an XML declaration")
  void handlesPomWithoutXmlDeclaration() {
    // The parser's reported offset drifts with the document's byte layout; this shape is where
    // trusting it as an exact position went wrong.
    assertCase("no-xml-declaration", JACKSON);
  }

  @Test
  @DisplayName("On a real 1000-line POM, everything outside one inserted block stays identical")
  void realWorldPomChangesNothingElse() {
    var input = corpusFile("real-world", "input.xml");

    var result = PomEditor.addDependency(input, JACKSON);

    var output = ((AddResult.Added) result).pom();
    PomParser.requireParsable(output);

    // Deliberately not a golden file: an expected.xml produced by running this very editor would
    // agree with it by construction. Instead the promise itself is checked — strip the longest
    // common prefix and suffix, and whatever remains is the entire change.
    int prefix = commonPrefixLength(input, output);
    int suffix = commonSuffixLength(input, output, prefix);
    var inserted = output.substring(prefix, output.length() - suffix);

    assertThat(inserted)
        .as("the whole change is one dependency block and nothing else")
        .contains("<dependency>")
        .contains("<artifactId>jackson-databind</artifactId>")
        .contains("</dependency>");
    assertThat(inserted.lines().count())
        .as("a change spanning more than one block would mean something else moved")
        .isLessThanOrEqualTo(7);
  }

  private static int commonPrefixLength(String a, String b) {
    int limit = Math.min(a.length(), b.length());
    int index = 0;
    while (index < limit && a.charAt(index) == b.charAt(index)) {
      index++;
    }
    return index;
  }

  private static int commonSuffixLength(String a, String b, int prefix) {
    int limit = Math.min(a.length(), b.length()) - prefix;
    int index = 0;
    while (index < limit
        && a.charAt(a.length() - 1 - index) == b.charAt(b.length() - 1 - index)) {
      index++;
    }
    return index;
  }

  private static void assertCase(String name, Dependency dependency) {
    var input = corpusFile(name, "input.xml");
    var expected = corpusFile(name, "expected.xml");

    var result = PomEditor.addDependency(input, dependency);

    assertThat(result).isInstanceOf(AddResult.Added.class);
    assertThat(((AddResult.Added) result).pom()).isEqualTo(expected);
  }

  private static String corpusFile(String name, String file) {
    var resource = "/pom-corpus/" + name + "/" + file;
    try (var in = PomEditorTest.class.getResourceAsStream(resource)) {
      assertThat(in).as("corpus file %s", resource).isNotNull();
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
