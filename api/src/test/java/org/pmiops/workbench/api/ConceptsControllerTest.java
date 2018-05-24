package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;

import java.util.Arrays;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.cdr.dao.ConceptDao;
import org.pmiops.workbench.cdr.dao.ConceptService;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.model.Concept;
import org.pmiops.workbench.model.ConceptListResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration.class)
@AutoConfigureTestDatabase(replace= AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class ConceptsControllerTest {

  private static final Concept CLIENT_CONCEPT_1 = new Concept()
      .conceptId(123L)
      .conceptName("a concept")
      .standardConcept(true)
      .conceptCode("conceptA")
      .conceptClassId("classId")
      .vocabularyId("V1")
      .domainId("D1")
      .countValue(123L)
      .prevalence(0.2F);

  private static final Concept CLIENT_CONCEPT_2 = new Concept()
      .conceptId(456L)
      .conceptName("b concept")
      .conceptCode("conceptB")
      .conceptClassId("classId2")
      .vocabularyId("V2")
      .domainId("D2")
      .countValue(456L)
      .prevalence(0.3F);

  private static final org.pmiops.workbench.cdr.model.Concept CONCEPT_1 =
      makeConcept(CLIENT_CONCEPT_1);
  private static final org.pmiops.workbench.cdr.model.Concept CONCEPT_2 =
      makeConcept(CLIENT_CONCEPT_2);

  @Autowired
  private ConceptDao conceptDao;
  @PersistenceContext
  private EntityManager entityManager;

  private ConceptsController conceptsController;

  @Before
  public void setUp() {
    // Injecting ConceptsController and ConceptService doesn't work well without using
    // SpringBootTest, which causes problems with CdrDbConfig. Just construct the service and
    // controller directly.
    ConceptService conceptService = new ConceptService(entityManager);
    conceptsController = new ConceptsController(conceptService);
  }

  @Test(expected = BadRequestException.class)
  public void testSearchConceptsBlankQuery() throws Exception {
    assertResults(
        conceptsController.searchConcepts(" ", null, null,
            null, null));
  }

  @Test
  public void testSearchNoConcepts() throws Exception {
    assertResults(
        conceptsController.searchConcepts("a", null, null,
          null, null));
  }

  @Test
  public void testSearchConceptsNameNoMatches() throws Exception {
    saveConcepts();
    assertResults(
        conceptsController.searchConcepts("x", null, null,
            null, null));
  }

  @Test
  public void testSearchConceptsNameOneMatch() throws Exception {
    saveConcepts();
    assertResults(
        conceptsController.searchConcepts("a", null, null,
            null, null), CLIENT_CONCEPT_1);
  }

  @Test
  public void testSearchConceptsNameTwoMatches() throws Exception {
    saveConcepts();
    assertResults(
        conceptsController.searchConcepts("con", null, null,
            null, null), CLIENT_CONCEPT_2, CLIENT_CONCEPT_1);
  }

  @Test
  public void testSearchConceptsCodeMatch() throws Exception {
    saveConcepts();
    assertResults(
        conceptsController.searchConcepts("conceptb", null, null,
            null, null), CLIENT_CONCEPT_2);
  }

  @Test
  public void testSearchConceptsConceptIdMatch() throws Exception {
    saveConcepts();
    assertResults(
        conceptsController.searchConcepts("456", null, null,
            null, null), CLIENT_CONCEPT_2);
  }

  @Test
  public void testSearchConceptsStandardConcept() throws Exception {
    saveConcepts();
    assertResults(
        conceptsController.searchConcepts("con", true, null,
            null, null), CLIENT_CONCEPT_1);
  }

  @Test
  public void testSearchConceptsNotStandardConcept() throws Exception {
    saveConcepts();
    assertResults(
        conceptsController.searchConcepts("con", false, null,
            null, null), CLIENT_CONCEPT_2);
  }

  @Test
  public void testSearchConceptsVocabularyIdNoMatch() throws Exception {
    saveConcepts();
    assertResults(
        conceptsController.searchConcepts("con", null, "V",
            null, null));
  }

  @Test
  public void testSearchConceptsVocabularyIdMatch() throws Exception {
    saveConcepts();
    assertResults(
        conceptsController.searchConcepts("con", null, "V2",
            null, null), CLIENT_CONCEPT_2);
  }

  @Test
  public void testSearchConceptsDomainIdNoMatch() throws Exception {
    saveConcepts();
    assertResults(
        conceptsController.searchConcepts("con", null, null,
            "D", null));
  }

  @Test
  public void testSearchConceptsDomainIdMatch() throws Exception {
    saveConcepts();
    assertResults(
        conceptsController.searchConcepts("con", null, null,
            "D1", null), CLIENT_CONCEPT_1);
  }

  @Test
  public void testSearchConceptsMultipleMatch() throws Exception {
    saveConcepts();
    assertResults(
        conceptsController.searchConcepts("con", true, "V1",
            "D1", null), CLIENT_CONCEPT_1);
  }

  @Test
  public void testSearchConceptsMultipleNoMatch() throws Exception {
    saveConcepts();
    assertResults(
        conceptsController.searchConcepts("con", false, "V1",
            "D1", null));
  }

  @Test
  public void testSearchConceptsOneResult() throws Exception {
    saveConcepts();
    assertResults(
        conceptsController.searchConcepts("con", null, null,
            null, 1), CLIENT_CONCEPT_2);
  }

  @Test
  public void testSearchConceptsOneThousandResults() throws Exception {
    saveConcepts();
    assertResults(
        conceptsController.searchConcepts("con", null, null,
            null, 1000), CLIENT_CONCEPT_2, CLIENT_CONCEPT_1);
  }

  @Test(expected = BadRequestException.class)
  public void testSearchConceptsOneThousandOneResults() throws Exception {
    saveConcepts();
    conceptsController.searchConcepts("con", null, null,
       null, 1001);
  }

  private static org.pmiops.workbench.cdr.model.Concept makeConcept(Concept concept) {
    org.pmiops.workbench.cdr.model.Concept result = new org.pmiops.workbench.cdr.model.Concept();
    result.setConceptId(concept.getConceptId());
    result.setConceptName(concept.getConceptName());
    result.setStandardConcept(concept.getStandardConcept() == null ? null :
        (concept.getStandardConcept() ? "S" : "C"));
    result.setConceptCode(concept.getConceptCode());
    result.setConceptClassId(concept.getConceptClassId());
    result.setVocabularyId(concept.getVocabularyId());
    result.setDomainId(concept.getDomainId());
    result.setCountValue(concept.getCountValue());
    result.setPrevalence(concept.getPrevalence());
    return result;
  }

  private static Concept makeClientConcept(org.pmiops.workbench.cdr.model.Concept dbConcept) {
    Concept concept = new Concept();
    concept.setConceptId(dbConcept.getConceptId());
    concept.setConceptName(dbConcept.getConceptName());
    concept.setStandardConcept(ConceptService.STANDARD_CONCEPT_CODE.equals(
        dbConcept.getStandardConcept()));
    concept.setConceptCode(dbConcept.getConceptCode());
    concept.setConceptClassId(dbConcept.getConceptClassId());
    concept.setVocabularyId(dbConcept.getVocabularyId());
    concept.setDomainId(dbConcept.getDomainId());
    concept.setCountValue(dbConcept.getCountValue());
    concept.setPrevalence(dbConcept.getPrevalence());
    return concept;
  }


  private void saveConcepts() {
    conceptDao.save(CONCEPT_1);
    conceptDao.save(CONCEPT_2);
  }

  private void assertResults(ResponseEntity<ConceptListResponse> response,
      Concept... expectedConcepts) {
    assertThat(response.getBody().getItems().equals(Arrays.asList(expectedConcepts)));
  }
}
