package org.pmiops.workbench.cdr.model;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class ConceptTest {

  @Test
  public void testSetSynonymStrIdAndCodeOnly() {
    DbConcept concept = new DbConcept();
    concept.setSynonymsStr("123|");
    assertThat(concept.getSynonyms()).isEmpty();
  }

  @Test
  public void testSetSynonymStrIdAndCodeOneSynonym() {
    DbConcept concept = new DbConcept();
    concept.setSynonymsStr("123|foo bar");
    assertThat(concept.getSynonyms()).containsExactly("foo bar").inOrder();
  }

  @Test
  public void testSetSynonymStrIdAndCodeTwoSynonymsOneEscaped() {
    DbConcept concept = new DbConcept();
    concept.setSynonymsStr("123|foo bar|baz || blah");
    assertThat(concept.getSynonyms()).containsExactly("foo bar", "baz | blah").inOrder();
  }
}
