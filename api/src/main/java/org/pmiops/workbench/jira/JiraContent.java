package org.pmiops.workbench.jira;

import java.math.BigDecimal;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.pmiops.workbench.jira.model.AtlassianContent;
import org.pmiops.workbench.jira.model.AtlassianDocument;
import org.pmiops.workbench.jira.model.AtlassianMark;
import org.pmiops.workbench.jira.model.AtlassianMarkAttributes;

/**
 * Several fields in the Jira API expect content in the Atlassian Document format, which is fairly
 * verbose. This class provides utilities for interacting with this document format.
 * https://developer.atlassian.com/cloud/jira/platform/apis/document/structure/
 */
public class JiraContent {

  private JiraContent() {}

  public static AtlassianDocument contentAsMinimalAtlassianDocument(
      Stream<AtlassianContent> content) {
    return new AtlassianDocument()
        .type("doc")
        .version(BigDecimal.ONE)
        .addContentItem(
            new AtlassianContent().type("paragraph").content(content.collect(Collectors.toList())));
  }

  /**
   * Converts an Atlassian document to a plain string; discarding markup / annotations such as
   * links. This is intended for debugging / logging / testing purposes.
   */
  public static String documentToString(AtlassianDocument document) {
    return document.getContent().stream()
        .flatMap(c -> c.getContent().stream())
        .map(AtlassianContent::getText)
        .collect(Collectors.joining());
  }

  public static AtlassianContent text(String text) {
    return new AtlassianContent().type("text").text(text);
  }

  public static AtlassianContent link(String href) {
    return link(href, href);
  }

  public static AtlassianContent link(String text, String href) {
    return text(text)
        .addMarksItem(
            new AtlassianMark().type("link").attrs(new AtlassianMarkAttributes().href(href)));
  }
}
