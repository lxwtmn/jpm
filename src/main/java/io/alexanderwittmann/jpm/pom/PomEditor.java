package io.alexanderwittmann.jpm.pom;

import io.alexanderwittmann.jpm.domain.Coordinate;
import io.alexanderwittmann.jpm.domain.Dependency;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Adds dependencies to a POM while preserving its formatting byte for byte.
 *
 * <p>The parser is used only to say <em>which</em> elements matter; every exact position is then
 * found by searching the source text. Nothing outside the inserted block is ever re-serialised,
 * which makes preservation structural rather than a property that has to be defended against a
 * serialiser's conventions.
 *
 * <p>Positions deliberately do not come from {@code Location.getCharacterOffset()}. That value is
 * the scanner's buffer position, which runs ahead of the event by an amount that depends on the
 * document's byte layout — measured, it drifts for POMs without an XML declaration. It is used
 * only as an upper bound for a backwards search, never as the position itself. This is the same
 * reason {@code versions-maven-plugin} tracks its own offsets instead of trusting StAX locations.
 */
public final class PomEditor {

  /**
   * The direct children of {@code <project>} that the Maven 4.0.0 schema orders <em>after</em>
   * {@code <dependencies>}. A freshly created section goes in front of the first of these, so the
   * result validates and not merely parses.
   */
  private static final Set<String> AFTER_DEPENDENCIES =
      Set.of("repositories", "pluginRepositories", "build", "reporting", "profiles");

  private PomEditor() {}

  public static AddResult addDependency(String source, Dependency dependency) {
    // The byte order mark is split off before scanning and put back afterwards, so every offset
    // in between refers to the document proper. See PomParser for why it has to go.
    var mark = source.startsWith(PomParser.BYTE_ORDER_MARK) ? PomParser.BYTE_ORDER_MARK : "";
    var body = PomParser.stripByteOrderMark(source);

    var pom = scan(body);
    if (pom.present.containsKey(dependency.coordinate())) {
      return new AddResult.AlreadyPresent(pom.present.get(dependency.coordinate()));
    }

    var edited =
        pom.dependenciesOpen >= 0
            ? insertIntoSection(body, pom, dependency)
            : insertNewSection(body, pom, dependency);

    // Verify the outcome, not merely that the result is well formed. Well-formedness would have
    // accepted a <dependency> that landed *beside* a self-closing <dependencies/> — a POM that
    // parses, breaks the user's build, and reported success.
    requireDeclared(edited, dependency.coordinate());
    return new AddResult.Added(mark + edited);
  }

  private static void requireDeclared(String edited, Coordinate coordinate) {
    if (!scan(edited).present.containsKey(coordinate)) {
      throw new MalformedPomException(
          "the edit did not place " + coordinate + " inside <dependencies>; nothing was written",
          null);
    }
  }

  // ---------------------------------------------------------------- insertion

  private static String insertIntoSection(String source, Scan pom, Dependency dependency) {
    var unit = indentUnit(pom.dependenciesIndent, pom.dependencyIndent);

    if (pom.dependenciesSelfClosing) {
      // <dependencies/> cannot be appended to. It is replaced by an open/close pair holding the
      // new entry, which is the only edit that stays valid.
      var indent = pom.dependenciesIndent;
      var replacement =
          "<" + pom.dependenciesQName + ">"
              + renderDependency(dependency, pom.projectPrefix, indent + unit, unit, pom.eol)
              + pom.eol
              + indent
              + "</" + pom.dependenciesQName + ">";
      return source.substring(0, pom.dependenciesOpen)
          + replacement
          + source.substring(pom.dependenciesOpenEnd);
    }

    var indent =
        pom.dependencyIndent != null ? pom.dependencyIndent : pom.dependenciesIndent + unit;
    var at = pom.lastDependencyEnd >= 0 ? pom.lastDependencyEnd : pom.dependenciesOpenEnd;
    return splice(source, at, renderDependency(dependency, pom.projectPrefix, indent, unit, pom.eol));
  }

  private static String insertNewSection(String source, Scan pom, Dependency dependency) {
    int at = pom.sectionAnchorLineStart >= 0 ? pom.sectionAnchorLineStart : pom.projectEndLineStart;
    if (at < 0) {
      throw new MalformedPomException("the POM has no <project> element to write into", null);
    }
    // The indentation comes from the section's siblings, never from </project>: that closing tag
    // sits at column zero, which would put the new section flush left.
    var indent =
        pom.sectionAnchorIndent != null
            ? pom.sectionAnchorIndent
            : (pom.projectChildIndent != null ? pom.projectChildIndent : "  ");
    var unit = indent.isEmpty() ? "  " : indent;
    var name = pom.projectPrefix + "dependencies";

    var section =
        indent
            + "<" + name + ">"
            + renderDependency(dependency, pom.projectPrefix, indent + unit, unit, pom.eol)
            + pom.eol
            + indent
            + "</" + name + ">"
            + pom.eol
            // Mirror the document's own spacing: if sections here are separated by a blank line,
            // the new one is too.
            + (blankLineBefore(source, at, pom.eol) ? pom.eol : "");
    return splice(source, at, section);
  }

  private static String splice(String source, int at, String text) {
    return source.substring(0, at) + text + source.substring(at);
  }

  // ---------------------------------------------------------------- scanning

  private static final class Scan {
    String eol = "\n";
    String projectPrefix = "";
    String dependenciesQName = "dependencies";
    String dependenciesIndent = "";
    String dependencyIndent;
    boolean dependenciesSelfClosing;
    int dependenciesOpen = -1;
    int dependenciesOpenEnd = -1;
    int lastDependencyEnd = -1;
    int sectionAnchorLineStart = -1;
    String sectionAnchorIndent;
    String projectChildIndent;
    int projectEndLineStart = -1;
    final Map<Coordinate, String> present = new LinkedHashMap<>();
  }

  private static Scan scan(String source) {
    var pom = new Scan();
    pom.eol = source.contains("\r\n") ? "\r\n" : "\n";
    Deque<String> path = new ArrayDeque<>();
    String groupId = null;
    String artifactId = null;
    String version = null;

    try {
      var reader = PomParser.readerOver(source);
      while (reader.hasNext()) {
        int event = reader.next();
        if (event == XMLStreamConstants.START_ELEMENT) {
          var name = reader.getLocalName();
          path.addLast(name);

          if (matches(path, "project")) {
            pom.projectPrefix = prefixOf(reader);
          } else if (matches(path, "project", name) && pom.projectChildIndent == null) {
            pom.projectChildIndent = indentBefore(source, startTagAt(source, reader));
          }

          if (matches(path, "project", "dependencies")) {
            int open = startTagAt(source, reader);
            int openEnd = source.indexOf('>', open) + 1;
            pom.dependenciesQName = qName(reader);
            pom.dependenciesOpen = open;
            pom.dependenciesOpenEnd = openEnd;
            pom.dependenciesSelfClosing = source.charAt(openEnd - 2) == '/';
            pom.dependenciesIndent = indentBefore(source, open);
          } else if (matches(path, "project", "dependencies", "dependency")) {
            pom.dependencyIndent = indentBefore(source, startTagAt(source, reader));
            groupId = null;
            artifactId = null;
            version = null;
          } else if (matches(path, "project", "dependencies", "dependency", "groupId")
              || matches(path, "project", "dependencies", "dependency", "artifactId")
              || matches(path, "project", "dependencies", "dependency", "version")) {
            var text = reader.getElementText();
            switch (name) {
              case "groupId" -> groupId = text;
              case "artifactId" -> artifactId = text;
              default -> version = text;
            }
            // getElementText consumed this element's END_ELEMENT, so the loop will never see it.
            path.removeLast();
          } else if (matches(path, "project", name)
              && AFTER_DEPENDENCIES.contains(name)
              && pom.sectionAnchorLineStart < 0) {
            int start = startTagAt(source, reader);
            pom.sectionAnchorIndent = indentBefore(source, start);
            pom.sectionAnchorLineStart = start - pom.sectionAnchorIndent.length();
          }
        } else if (event == XMLStreamConstants.END_ELEMENT) {
          if (matches(path, "project", "dependencies", "dependency")) {
            pom.lastDependencyEnd = endTagEnd(source, reader);
            if (groupId != null && artifactId != null) {
              pom.present.put(new Coordinate(groupId, artifactId), version);
            }
          } else if (matches(path, "project")) {
            int start = endTagStart(source, reader);
            var indent = indentBefore(source, start);
            pom.projectEndLineStart = start - indent.length();
          }
          path.removeLast();
        }
      }
    } catch (XMLStreamException e) {
      throw new MalformedPomException(e.getMessage(), e);
    }
    return pom;
  }

  // ------------------------------------------------------- position resolving

  private static String prefixOf(XMLStreamReader reader) {
    var prefix = reader.getPrefix();
    return prefix == null || prefix.isEmpty() ? "" : prefix + ":";
  }

  private static String qName(XMLStreamReader reader) {
    return prefixOf(reader) + reader.getLocalName();
  }

  /**
   * The reported offset is only an upper bound, so the real tag is found by searching backwards
   * from it. The qualified name is used so that a namespace-prefixed POM resolves too.
   */
  private static int startTagAt(String source, XMLStreamReader reader) {
    var tag = "<" + qName(reader);
    int index = source.lastIndexOf(tag, upperBound(reader, source));
    if (index < 0) {
      throw new MalformedPomException("could not locate " + tag + "> in the source", null);
    }
    return index;
  }

  private static int endTagStart(String source, XMLStreamReader reader) {
    var tag = "</" + qName(reader) + ">";
    int index = source.lastIndexOf(tag, upperBound(reader, source));
    if (index < 0) {
      throw new MalformedPomException("could not locate " + tag + " in the source", null);
    }
    return index;
  }

  private static int endTagEnd(String source, XMLStreamReader reader) {
    return endTagStart(source, reader) + ("</" + qName(reader) + ">").length();
  }

  /** Clamps the parser's reported offset into the source, where it serves as a search bound. */
  private static int upperBound(XMLStreamReader reader, String source) {
    int offset = reader.getLocation().getCharacterOffset();
    return offset < 0 || offset > source.length() ? source.length() : offset;
  }

  /** The whitespace between the preceding line break and the given offset, if it is all blank. */
  private static String indentBefore(String source, int offset) {
    int lineStart = source.lastIndexOf('\n', offset - 1) + 1;
    var candidate = source.substring(lineStart, offset);
    return candidate.isBlank() ? candidate : "";
  }

  private static boolean blankLineBefore(String source, int lineStart, String eol) {
    return source.startsWith(eol + eol, lineStart - 2 * eol.length());
  }

  private static String indentUnit(String parentIndent, String childIndent) {
    if (childIndent != null
        && parentIndent != null
        && childIndent.startsWith(parentIndent)
        && childIndent.length() > parentIndent.length()) {
      return childIndent.substring(parentIndent.length());
    }
    // <dependencies> is a direct child of <project>, which sits at column zero — so its own
    // indentation is exactly one unit, tabs included.
    return parentIndent == null || parentIndent.isEmpty() ? "  " : parentIndent;
  }

  // ---------------------------------------------------------------- rendering

  private static String renderDependency(
      Dependency dependency, String prefix, String indent, String indentUnit, String eol) {
    var childIndent = indent + indentUnit;
    var coordinate = dependency.coordinate();
    return eol
        + indent
        + "<" + prefix + "dependency>"
        + element(childIndent, prefix + "groupId", coordinate.groupId(), eol)
        + element(childIndent, prefix + "artifactId", coordinate.artifactId(), eol)
        + element(childIndent, prefix + "version", dependency.version(), eol)
        + eol
        + indent
        + "</" + prefix + "dependency>";
  }

  private static String element(String indent, String name, String value, String eol) {
    return eol + indent + "<" + name + ">" + escape(value) + "</" + name + ">";
  }

  /**
   * Maven coordinates realistically never carry XML metacharacters, but writing unescaped input
   * into a document is a defect class rather than a judgement call.
   */
  private static String escape(String value) {
    return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
  }

  private static boolean matches(Deque<String> path, String... expected) {
    if (path.size() != expected.length) {
      return false;
    }
    int index = 0;
    for (String element : path) {
      if (!element.equals(expected[index++])) {
        return false;
      }
    }
    return true;
  }
}
