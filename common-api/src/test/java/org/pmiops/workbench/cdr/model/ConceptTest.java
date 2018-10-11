package org.pmiops.workbench.cdr.model;

import static com.google.common.truth.Truth.assertThat;
import org.junit.Test;

public class ConceptTest {

  @Test
  public void testSetSynonymStrIdAndCodeOnly() {
    Concept concept = new Concept();
    concept.setSynonymsStr("123|code|");
    assertThat(concept.getSynonyms()).isEmpty();
  }

  @Test
  public void testSetSynonymStrIdAndCodeOneSynonym() {
    Concept concept = new Concept();
    concept.setSynonymsStr("123|code|foo bar");
    assertThat(concept.getSynonyms()).containsExactly(new ConceptSynonym().conceptSynonymName("foo bar"))
        .inOrder();
  }

  @Test
  public void testSetSynonymStrIdAndCodeTwoSynonymsOneEscaped() {
    Concept concept = new Concept();
    concept.setSynonymsStr("123|code|foo bar|baz || blah");
    assertThat(concept.getSynonyms()).containsExactly(new ConceptSynonym().conceptSynonymName("foo bar"),
        new ConceptSynonym().conceptSynonymName("baz | blah"))
        .inOrder();
  }
}
