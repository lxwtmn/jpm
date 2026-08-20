package io.alexanderwittmann.jpm.pom;

import java.io.StringReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * The single place that knows how this project parses XML, so the editor and the writer cannot
 * drift apart in their idea of what counts as a valid POM.
 */
final class PomParser {

  /**
   * A byte order mark is a signal at the <em>byte</em> level. Once the file has been decoded it
   * survives as a stray U+FEFF ahead of the prolog, which XML forbids and the parser rejects.
   */
  static final String BYTE_ORDER_MARK = "﻿";

  private PomParser() {}

  static String stripByteOrderMark(String source) {
    return source.startsWith(BYTE_ORDER_MARK) ? source.substring(BYTE_ORDER_MARK.length()) : source;
  }

  static XMLStreamReader readerOver(String source) throws XMLStreamException {
    var factory = XMLInputFactory.newInstance();
    factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
    factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
    return factory.createXMLStreamReader(new StringReader(source));
  }

  /** Throws {@link MalformedPomException} unless the whole document parses. */
  static void requireParsable(String source) {
    try {
      var reader = readerOver(stripByteOrderMark(source));
      while (reader.hasNext()) {
        reader.next();
      }
    } catch (XMLStreamException e) {
      throw new MalformedPomException(e.getMessage(), e);
    }
  }
}
