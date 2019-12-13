package org.pmiops.workbench.cdr.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.cdr.model.DbConcept;
import org.pmiops.workbench.model.CriteriaType;
import org.pmiops.workbench.model.DomainType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class ConceptDaoTest {

  @Autowired ConceptDao conceptDao;
  private DbConcept concept1;
  private DbConcept concept2;

  @Before
  public void setUp() {

    concept1 =
        conceptDao.save(
            new DbConcept()
                .conceptId(1L)
                .domainId(DomainType.CONDITION.toString())
                .conceptName("Personal history of malignant neoplasm of breast")
                .vocabularyId(CriteriaType.ICD9CM.toString())
                .conceptClassId("4-char billing code")
                .conceptCode("Z85")
                .count(3094L)
                .prevalence(0.0F)
                .sourceCountValue(3094L)
                .synonyms(
                    Arrays.asList(
                        "35225339",
                        "Personal history of malignant neoplasm of breast|Personal history of malignant neoplasm of breast")));
    concept2 =
        conceptDao.save(
            new DbConcept()
                .conceptId(2L)
                .domainId(DomainType.CONDITION.toString())
                .conceptName("Personal history of malignant neoplasm")
                .vocabularyId(CriteriaType.SNOMED.toString())
                .conceptClassId("4-char billing code")
                .conceptCode("Z86")
                .standardConcept("S")
                .count(3094L)
                .prevalence(0.0F)
                .sourceCountValue(3094L)
                .synonyms(
                    Arrays.asList(
                        "35225339",
                        "Personal history of malignant neoplasm of breast|Personal history of malignant neoplasm of breast")));
  }

  @Test
  public void findAll() throws Exception {
    List<DbConcept> concepts = (List<DbConcept>) conceptDao.findAll();
    assertEquals(2, concepts.size());
    assertTrue(concepts.contains(concept1));
    assertTrue(concepts.contains(concept2));
  }
}
