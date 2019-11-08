package org.pmiops.workbench.cdr.dao;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.cdr.model.Concept;
import org.pmiops.workbench.cdr.model.VocabularyCount;
import org.pmiops.workbench.model.CriteriaType;
import org.pmiops.workbench.model.DomainType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
public class ConceptDaoTest {

  @Autowired ConceptDao conceptDao;

  @Before
  public void setUp() {

    conceptDao.save(
        new Concept()
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
    conceptDao.save(
        new Concept()
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
  public void findVocabularyAllConceptCounts() throws Exception {
    List<VocabularyCount> counts =
        conceptDao.findVocabularyAllConceptCounts("+Z85", DomainType.CONDITION.toString());
    assertEquals(CriteriaType.ICD9CM.toString(), counts.get(0).getVocabularyId());
    assertEquals(1, counts.get(0).getConceptCount());
  }

  @Test
  public void findVocabularyStandardConceptCounts() throws Exception {
    List<VocabularyCount> counts =
        conceptDao.findVocabularyStandardConceptCounts("+Z86", DomainType.CONDITION.toString());
    assertEquals(CriteriaType.SNOMED.toString(), counts.get(0).getVocabularyId());
    assertEquals(1, counts.get(0).getConceptCount());
  }
}
