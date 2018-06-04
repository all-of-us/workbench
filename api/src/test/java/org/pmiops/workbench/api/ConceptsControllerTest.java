package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

import com.google.appengine.repackaged.com.google.common.collect.ImmutableList;
import java.time.Clock;
import java.util.Arrays;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.cdr.dao.ConceptDao;
import org.pmiops.workbench.cdr.dao.ConceptService;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.CohortService;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.dao.WorkspaceService;
import org.pmiops.workbench.db.dao.WorkspaceServiceImpl;
import org.pmiops.workbench.db.model.CdrVersion;
import org.pmiops.workbench.db.model.Workspace;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.model.Concept;
import org.pmiops.workbench.model.ConceptListResponse;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.SearchConceptsRequest;
import org.pmiops.workbench.model.StandardConceptFilter;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
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
      .domainId("Condition")
      .countValue(123L)
      .prevalence(0.2F);

  private static final Concept CLIENT_CONCEPT_2 = new Concept()
      .conceptId(456L)
      .conceptName("b concept")
      .conceptCode("conceptB")
      .conceptClassId("classId2")
      .vocabularyId("V2")
      .domainId("Measurement")
      .countValue(456L)
      .prevalence(0.3F);

  private static final Concept CLIENT_CONCEPT_3 = new Concept()
          .conceptId(789L)
          .conceptName("multi word concept")
          .conceptCode("conceptC")
          .conceptClassId("classId3")
          .vocabularyId("V3")
          .domainId("Condition")
          .countValue(789L)
          .prevalence(0.4F);

  private static final org.pmiops.workbench.cdr.model.Concept CONCEPT_1 =
      makeConcept(CLIENT_CONCEPT_1);
  private static final org.pmiops.workbench.cdr.model.Concept CONCEPT_2 =
      makeConcept(CLIENT_CONCEPT_2);
  private static final org.pmiops.workbench.cdr.model.Concept CONCEPT_3 =
          makeConcept(CLIENT_CONCEPT_3);

  @TestConfiguration
  @Import({
      WorkspaceServiceImpl.class
  })
  @MockBean({
      FireCloudService.class,
      CohortService.class,
      Clock.class
  })
  static class Configuration {
  }


  @Autowired
  private ConceptDao conceptDao;
  @Autowired
  private WorkspaceService workspaceService;
  @Autowired
  private WorkspaceDao workspaceDao;
  @Autowired
  private CdrVersionDao cdrVersionDao;
  @Autowired
  FireCloudService fireCloudService;

  @PersistenceContext
  private EntityManager entityManager;

  private ConceptsController conceptsController;

  @Before
  public void setUp() {
    // Injecting ConceptsController and ConceptService doesn't work well without using
    // SpringBootTest, which causes problems with CdrDbConfig. Just construct the service and
    // controller directly.
    ConceptService conceptService = new ConceptService(entityManager);
    conceptsController = new ConceptsController(conceptService, workspaceService);

    CdrVersion cdrVersion = new CdrVersion();
    cdrVersion.setName("1");
    //set the db name to be empty since test cases currently
    //run in the workbench schema only.
    cdrVersion.setCdrDbName("");
    cdrVersion = cdrVersionDao.save(cdrVersion);

    Workspace workspace = new Workspace();
    workspace.setWorkspaceId(1L);
    workspace.setName("name");
    workspace.setFirecloudName("name");
    workspace.setWorkspaceNamespace("ns");
    workspace.setCdrVersion(cdrVersion);
    workspaceDao.save(workspace);
    org.pmiops.workbench.firecloud.model.WorkspaceResponse fcResponse =
        new org.pmiops.workbench.firecloud.model.WorkspaceResponse();
    fcResponse.setAccessLevel(WorkspaceAccessLevel.OWNER.name());
    when(fireCloudService.getWorkspace("ns", "name"))
        .thenReturn(fcResponse);
  }

  @Test(expected = BadRequestException.class)
  public void testSearchConceptsBlankQuery() throws Exception {
    assertResults(
        conceptsController.searchConcepts("ns", "name",
            new SearchConceptsRequest().query(" ")));
  }

  @Test
  public void testSearchConceptsMultipleWordQuery(){
    saveConcepts();
    assertResults(
            conceptsController.searchConcepts("ns", "name",
                    new SearchConceptsRequest().query("multi word")));
  }

  @Test
  public void testSearchNoConcepts() throws Exception {
    assertResults(
        conceptsController.searchConcepts("ns", "name",
            new SearchConceptsRequest().query("a")));
  }

  @Test
  public void testSearchConceptsNameNoMatches() throws Exception {
    saveConcepts();
    assertResults(
        conceptsController.searchConcepts("ns", "name",
            new SearchConceptsRequest().query("x")));
  }

  @Test
  public void testSearchConceptsNameOneMatch() throws Exception {
    saveConcepts();
    assertResults(
        conceptsController.searchConcepts("ns", "name",
            new SearchConceptsRequest().query("a")), CLIENT_CONCEPT_1);
  }

  @Test
  public void testSearchConceptsNameTwoMatches() throws Exception {
    saveConcepts();
    assertResults(
        conceptsController.searchConcepts("ns", "name",
            new SearchConceptsRequest().query("con")), CLIENT_CONCEPT_2, CLIENT_CONCEPT_1);
  }

  @Test
  public void testSearchConceptsCodeMatch() throws Exception {
    saveConcepts();
    assertResults(
        conceptsController.searchConcepts("ns", "name",
            new SearchConceptsRequest().query("conceptb")), CLIENT_CONCEPT_2);
  }

  @Test
  public void testSearchConceptsConceptIdMatch() throws Exception {
    saveConcepts();
    assertResults(
        conceptsController.searchConcepts("ns", "name",
            new SearchConceptsRequest().query("456")), CLIENT_CONCEPT_2);
  }

  @Test
  public void testSearchConceptsStandardConcept() throws Exception {
    saveConcepts();
    assertResults(
        conceptsController.searchConcepts("ns", "name",
            new SearchConceptsRequest().query("con")
                .standardConceptFilter(StandardConceptFilter.STANDARD_CONCEPTS)), CLIENT_CONCEPT_1);
  }

  @Test
  public void testSearchConceptsNotStandardConcept() throws Exception {
    saveConcepts();
    assertResults(
        conceptsController.searchConcepts("ns", "name",
            new SearchConceptsRequest().query("con")
                .standardConceptFilter(StandardConceptFilter.NON_STANDARD_CONCEPTS)),
        CLIENT_CONCEPT_2);
  }

  @Test
  public void testSearchConceptsVocabularyIdNoMatch() throws Exception {
    saveConcepts();
    assertResults(
        conceptsController.searchConcepts("ns", "name",
            new SearchConceptsRequest().query("con").vocabularyIds(ImmutableList.of("x", "v"))));
  }

  @Test
  public void testSearchConceptsVocabularyIdMatch() throws Exception {
    saveConcepts();
    assertResults(
        conceptsController.searchConcepts("ns", "name",
            new SearchConceptsRequest().query("con").vocabularyIds(ImmutableList.of("V3", "V2"))),
        CLIENT_CONCEPT_2);
  }

  @Test
  public void testSearchConceptsDomainIdNoMatch() throws Exception {
    saveConcepts();
    assertResults(
        conceptsController.searchConcepts("ns", "name",
            new SearchConceptsRequest().query("con").domain(Domain.OBSERVATION)));
  }

  @Test
  public void testSearchConceptsDomainIdMatch() throws Exception {
    saveConcepts();
    assertResults(
        conceptsController.searchConcepts("ns", "name",
            new SearchConceptsRequest().query("con").domain(Domain.CONDITION)), CLIENT_CONCEPT_1);
  }

  @Test
  public void testSearchConceptsMultipleMatch() throws Exception {
    saveConcepts();
    assertResults(
        conceptsController.searchConcepts("ns", "name",
            new SearchConceptsRequest().query("con")
                .standardConceptFilter(StandardConceptFilter.STANDARD_CONCEPTS)
                .vocabularyIds(ImmutableList.of("V1"))
                .domain(Domain.CONDITION)), CLIENT_CONCEPT_1);
  }

  @Test
  public void testSearchConceptsMultipleNoMatch() throws Exception {
    saveConcepts();
    assertResults(
        conceptsController.searchConcepts("ns", "name",
            new SearchConceptsRequest().query("con")
                .standardConceptFilter(StandardConceptFilter.NON_STANDARD_CONCEPTS)
                .vocabularyIds(ImmutableList.of("V1"))
                .domain(Domain.CONDITION)));
  }

  @Test
  public void testSearchConceptsOneResult() throws Exception {
    saveConcepts();
    assertResults(
        conceptsController.searchConcepts("ns", "name",
            new SearchConceptsRequest().query("con").maxResults(1)), CLIENT_CONCEPT_2);
  }

  @Test
  public void testSearchConceptsOneThousandResults() throws Exception {
    saveConcepts();
    assertResults(
        conceptsController.searchConcepts("ns", "name",
            new SearchConceptsRequest().query("con").maxResults(1000)),
        CLIENT_CONCEPT_2, CLIENT_CONCEPT_1);
  }

  @Test
  public void testSearchConceptsOneThousandOneResults() throws Exception {
    saveConcepts();
    assertResults(
        conceptsController.searchConcepts("ns", "name",
            new SearchConceptsRequest().query("con").maxResults(1001)), CLIENT_CONCEPT_2,
        CLIENT_CONCEPT_1);
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

  private void saveConcepts() {
    conceptDao.save(CONCEPT_1);
    conceptDao.save(CONCEPT_2);
    conceptDao.save(CONCEPT_3);
  }

  private void assertResults(ResponseEntity<ConceptListResponse> response,
      Concept... expectedConcepts) {
    assertThat(response.getBody().getItems().equals(Arrays.asList(expectedConcepts)));
  }
}
