package org.pmiops.workbench.cdr.model;

/**
 * A count of concepts in a vocabulary.
 */
public interface VocabularyCount {

  public String getVocabularyId();

  public long getConceptCount();
}
