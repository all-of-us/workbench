package org.pmiops.workbench.cdr.cache;

import java.util.List;

public class MySQLStopWords {
  private List<String> stopWords;

  public MySQLStopWords(List<String> stopWords) {
    this.stopWords = stopWords;
  }

  public List<String> getStopWords() {
    return this.stopWords;
  }
}
