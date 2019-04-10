package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.FieldList;
import com.google.cloud.bigquery.LegacySQLTypeName;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.cdr.dao.ConceptDao;
import org.pmiops.workbench.cdr.dao.ConceptService;
import org.pmiops.workbench.cdr.dao.DomainInfoDao;
import org.pmiops.workbench.cdr.dao.DomainVocabularyInfoDao;
import org.pmiops.workbench.cdr.model.DomainVocabularyInfo.DomainVocabularyInfoId;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.CohortCloningService;
import org.pmiops.workbench.db.dao.ConceptSetService;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.dao.WorkspaceService;
import org.pmiops.workbench.db.dao.WorkspaceServiceImpl;
import org.pmiops.workbench.db.model.CdrVersion;
import org.pmiops.workbench.db.model.Workspace;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.model.Concept;
import org.pmiops.workbench.model.ConceptListResponse;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.DomainCount;
import org.pmiops.workbench.model.DomainInfo;
import org.pmiops.workbench.model.DomainValue;
import org.pmiops.workbench.model.SearchConceptsRequest;
import org.pmiops.workbench.model.StandardConceptFilter;
import org.pmiops.workbench.model.VocabularyCount;
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
@Import({LiquibaseAutoConfiguration.class, BigQueryService.class})
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
          .prevalence(0.2F)
          .conceptSynonyms(new ArrayList<String>());

  private static final Concept CLIENT_CONCEPT_2 = new Concept()
          .conceptId(456L)
          .standardConcept(false)
          .conceptName("b concept")
          .conceptCode("conceptB")
          .conceptClassId("classId2")
          .vocabularyId("V2")
          .domainId("Measurement")
          .countValue(456L)
          .prevalence(0.3F)
          .conceptSynonyms(new ArrayList<String>());

  private static final Concept CLIENT_CONCEPT_3 = new Concept()
          .conceptId(789L)
          .standardConcept(false)
          .conceptName("multi word concept")
          .conceptCode("conceptC")
          .conceptClassId("classId3")
          .vocabularyId("V3")
          .domainId("Condition")
          .countValue(789L)
          .prevalence(0.4F)
          .conceptSynonyms(new ArrayList<String>());

  private static final Concept CLIENT_CONCEPT_4 = new Concept()
          .conceptId(1234L)
          .conceptName("sample test con to test the multi word search")
          .standardConcept(true)
          .conceptCode("conceptD")
          .conceptClassId("classId4")
          .vocabularyId("V456")
          .domainId("Observation")
          .countValue(1250L)
          .prevalence(0.5F)
          .conceptSynonyms(new ArrayList<String>());

  private static final Concept CLIENT_CONCEPT_5 = new Concept()
          .conceptId(7890L)
          .conceptName("conceptD test concept")
          .standardConcept(true)
          .conceptCode("conceptE")
          .conceptClassId("classId5")
          .vocabularyId("V5")
          .domainId("Condition")
          .countValue(7890L)
          .prevalence(0.9F)
          .conceptSynonyms(new ArrayList<String>());

  private static final Concept CLIENT_CONCEPT_6 = new Concept()
          .conceptId(7891L)
          .conceptName("conceptD test concept 2")
          .standardConcept(false)
          .conceptCode("conceptD")
          .conceptClassId("classId6")
          .vocabularyId("V5")
          .domainId("Condition")
          .countValue(7891L)
          .prevalence(0.1F)
          .conceptSynonyms(new ArrayList<String>());

  private static final org.pmiops.workbench.cdr.model.Concept CONCEPT_1 =
      makeConcept(CLIENT_CONCEPT_1);
  private static final org.pmiops.workbench.cdr.model.Concept CONCEPT_2 =
      makeConcept(CLIENT_CONCEPT_2);
  private static final org.pmiops.workbench.cdr.model.Concept CONCEPT_3 =
          makeConcept(CLIENT_CONCEPT_3);
  private static final org.pmiops.workbench.cdr.model.Concept CONCEPT_4 =
          makeConcept(CLIENT_CONCEPT_4);
  private static final org.pmiops.workbench.cdr.model.Concept CONCEPT_5 =
          makeConcept(CLIENT_CONCEPT_5);
  private static final org.pmiops.workbench.cdr.model.Concept CONCEPT_6 =
          makeConcept(CLIENT_CONCEPT_6);

  private static final org.pmiops.workbench.cdr.model.DomainInfo MEASUREMENT_DOMAIN =
      new org.pmiops.workbench.cdr.model.DomainInfo()
          .domainEnum(Domain.MEASUREMENT)
          .domainId("Measurement")
          .name("Measurement!")
          .description("Measurements!!!")
          .conceptId(CONCEPT_1.getConceptId())
          .participantCount(123)
          .standardConceptCount(3)
          .allConceptCount(5);

  private static final org.pmiops.workbench.cdr.model.DomainInfo CONDITION_DOMAIN =
      new org.pmiops.workbench.cdr.model.DomainInfo()
          .domainEnum(Domain.CONDITION)
          .domainId("Condition")
          .name("Condition!")
          .description("Conditions!")
          .conceptId(CONCEPT_2.getConceptId())
          .participantCount(456)
          .standardConceptCount(4)
          .allConceptCount(6);

  private static final org.pmiops.workbench.cdr.model.DomainInfo PROCEDURE_DOMAIN =
      new org.pmiops.workbench.cdr.model.DomainInfo()
          .domainEnum(Domain.PROCEDURE)
          .domainId("Procedure")
          .name("Procedure!!!")
          .description("Procedures!!!")
          .conceptId(CONCEPT_3.getConceptId())
          .participantCount(789)
          .standardConceptCount(1)
          .allConceptCount(2);

  private static final org.pmiops.workbench.cdr.model.DomainInfo DRUG_DOMAIN =
      new org.pmiops.workbench.cdr.model.DomainInfo()
          .domainEnum(Domain.DRUG)
          .domainId("Drug")
          .name("Drug!")
          .description("Drugs!")
          .conceptId(CONCEPT_4.getConceptId())
          .participantCount(3)
          .standardConceptCount(3)
          .allConceptCount(4);

  private static final org.pmiops.workbench.cdr.model.DomainVocabularyInfo CONDITION_V1_INFO =
      new org.pmiops.workbench.cdr.model.DomainVocabularyInfo()
          .id(new DomainVocabularyInfoId("Condition", "V1"))
          .standardConceptCount(1).allConceptCount(1);
  private static final org.pmiops.workbench.cdr.model.DomainVocabularyInfo CONDITION_V3_INFO =
      new org.pmiops.workbench.cdr.model.DomainVocabularyInfo()
          .id(new DomainVocabularyInfoId("Condition", "V3"))
          .allConceptCount(1);
  private static final org.pmiops.workbench.cdr.model.DomainVocabularyInfo CONDITION_V5_INFO =
      new org.pmiops.workbench.cdr.model.DomainVocabularyInfo()
          .id(new DomainVocabularyInfoId("Condition", "V5"))
          .allConceptCount(2).standardConceptCount(1);

  @TestConfiguration
  @Import({
      WorkspaceServiceImpl.class
  })
  @MockBean({
      BigQueryService.class,
      FireCloudService.class,
      CohortCloningService.class,
      ConceptSetService.class,
      Clock.class
  })
  static class Configuration {
  }

  @Autowired
  private BigQueryService bigQueryService;
  @Autowired
  private ConceptDao conceptDao;
  @Autowired
  private WorkspaceService workspaceService;
  @Autowired
  private WorkspaceDao workspaceDao;
  @Autowired
  private CdrVersionDao cdrVersionDao;
  @Autowired
  private DomainInfoDao domainInfoDao;
  @Autowired
  private DomainVocabularyInfoDao domainVocabularyInfoDao;
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
    ConceptService conceptService = new ConceptService(entityManager, conceptDao);
    conceptsController = new ConceptsController(bigQueryService, conceptService, workspaceService,
        domainInfoDao, domainVocabularyInfoDao, conceptDao);

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

  @Test
  public void testSearchConceptsBlankQuery() throws Exception {
    assertResults(
        conceptsController.searchConcepts("ns", "name",
            new SearchConceptsRequest().query(" ")));
  }

  @Test
  public void testSearchConceptsBlankQueryWithResults() throws Exception{
    saveConcepts();
    saveDomains();
    assertResults(
        conceptsController.searchConcepts("ns", "name",
            new SearchConceptsRequest().query(" ")),
        CLIENT_CONCEPT_6, CLIENT_CONCEPT_5, CLIENT_CONCEPT_4, CLIENT_CONCEPT_3, CLIENT_CONCEPT_2,
        CLIENT_CONCEPT_1);
  }

  @Test
  public void testSearchConceptsBlankQueryWithVocabAllCounts() throws Exception{
    saveConcepts();
    saveDomains();
    saveDomainVocabularyInfos();
    ResponseEntity<ConceptListResponse> response =
        conceptsController.searchConcepts("ns", "name",
            new SearchConceptsRequest().includeVocabularyCounts(true).domain(Domain.CONDITION));
    assertResultsWithCounts(response,
        null,
        ImmutableList.<VocabularyCount>of(
            new VocabularyCount().vocabularyId("V1").conceptCount(1L),
            new VocabularyCount().vocabularyId("V3").conceptCount(1L),
            new VocabularyCount().vocabularyId("V5").conceptCount(2L)
        ),
        ImmutableList.of(CLIENT_CONCEPT_6, CLIENT_CONCEPT_5, CLIENT_CONCEPT_3,
            CLIENT_CONCEPT_1));
  }

  @Test
  public void testSearchConceptsBlankQueryWithVocabStandardCounts() throws Exception{
    saveConcepts();
    saveDomains();
    saveDomainVocabularyInfos();
    ResponseEntity<ConceptListResponse> response =
        conceptsController.searchConcepts("ns", "name",
            new SearchConceptsRequest().includeVocabularyCounts(true).domain(Domain.CONDITION)
                .standardConceptFilter(StandardConceptFilter.STANDARD_CONCEPTS));
    assertResultsWithCounts(response,
        null,
        ImmutableList.<VocabularyCount>of(
            new VocabularyCount().vocabularyId("V1").conceptCount(1L),
            new VocabularyCount().vocabularyId("V5").conceptCount(1L)
        ),
        ImmutableList.of(CLIENT_CONCEPT_5, CLIENT_CONCEPT_1));
  }

  @Test
  public void testSearchConceptsBlankQueryWithDomainAllCounts() throws Exception{
    saveConcepts();
    saveDomains();
    // When no query is provided, domain concept counts come from domain info directly.
    ResponseEntity<ConceptListResponse> response =
        conceptsController.searchConcepts("ns", "name",
            new SearchConceptsRequest().includeDomainCounts(true));
    assertResultsWithCounts(response,
        ImmutableList.<DomainCount>of(
            toDomainCount(CONDITION_DOMAIN, false),
            toDomainCount(DRUG_DOMAIN, false),
            toDomainCount(MEASUREMENT_DOMAIN, false),
            toDomainCount(PROCEDURE_DOMAIN, false)),
        null,
        ImmutableList.of(CLIENT_CONCEPT_6, CLIENT_CONCEPT_5, CLIENT_CONCEPT_4, CLIENT_CONCEPT_3,
            CLIENT_CONCEPT_2, CLIENT_CONCEPT_1));
  }

  @Test
  public void testSearchConceptsBlankQueryWithDomainStandardCounts() throws Exception{
    saveConcepts();
    saveDomains();
    // When no query is provided, domain concept counts come from domain info directly.
    ResponseEntity<ConceptListResponse> response =
        conceptsController.searchConcepts("ns", "name",
            new SearchConceptsRequest().includeDomainCounts(true)
                .standardConceptFilter(StandardConceptFilter.STANDARD_CONCEPTS));
    assertResultsWithCounts(response,
        ImmutableList.<DomainCount>of(
            toDomainCount(CONDITION_DOMAIN, true),
            toDomainCount(DRUG_DOMAIN, true),
            toDomainCount(MEASUREMENT_DOMAIN, true),
            toDomainCount(PROCEDURE_DOMAIN, true)),
        null,
        ImmutableList.of(CLIENT_CONCEPT_5, CLIENT_CONCEPT_4, CLIENT_CONCEPT_1));
  }

  @Test
  public void testSearchConceptsBlankQueryWithDomainAndVocabStandardCounts() throws Exception{
    saveConcepts();
    saveDomains();
    saveDomainVocabularyInfos();
    // When no query is provided, domain concept counts come from domain info directly.
    ResponseEntity<ConceptListResponse> response =
        conceptsController.searchConcepts("ns", "name",
            new SearchConceptsRequest().includeDomainCounts(true).includeVocabularyCounts(true)
                .domain(Domain.CONDITION).standardConceptFilter(StandardConceptFilter.STANDARD_CONCEPTS));
    assertResultsWithCounts(response,
        ImmutableList.<DomainCount>of(
            toDomainCount(CONDITION_DOMAIN, true),
            toDomainCount(DRUG_DOMAIN, true),
            toDomainCount(MEASUREMENT_DOMAIN, true),
            toDomainCount(PROCEDURE_DOMAIN, true)),
        ImmutableList.<VocabularyCount>of(
            new VocabularyCount().vocabularyId("V1").conceptCount(1L),
            new VocabularyCount().vocabularyId("V5").conceptCount(1L)
        ),
        ImmutableList.of(CLIENT_CONCEPT_5, CLIENT_CONCEPT_1));
  }

  @Test
  public void testSearchConceptsBlankQueryInDomain() throws Exception{
    saveConcepts();
    assertResults(
        conceptsController.searchConcepts("ns", "name",
            new SearchConceptsRequest().domain(Domain.CONDITION).query(" ")),
        CLIENT_CONCEPT_6, CLIENT_CONCEPT_5, CLIENT_CONCEPT_3, CLIENT_CONCEPT_1);
  }

  @Test
  public void testSearchConceptsBlankQueryInDomainWithVocabularyIds() throws Exception{
    saveConcepts();
    saveDomainVocabularyInfos();
    assertResults(
        conceptsController.searchConcepts("ns", "name",
            new SearchConceptsRequest().domain(Domain.CONDITION).vocabularyIds(
                ImmutableList.of("V1", "V5")).query(" ")),
        CLIENT_CONCEPT_6, CLIENT_CONCEPT_5, CLIENT_CONCEPT_1);
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
    assertResults(conceptsController.searchConcepts("ns", "name",
            new SearchConceptsRequest().query("xyz")));
  }

  @Test
  public void testSearchConceptsNameTwoMatches() throws Exception {
    saveConcepts();
    assertResults(
        conceptsController.searchConcepts("ns", "name",
            new SearchConceptsRequest().query("word")), CLIENT_CONCEPT_4, CLIENT_CONCEPT_3);
  }

  @Test
  public void testSearchConceptsCodeMatch() throws Exception {
    saveConcepts();
    assertResults(conceptsController.searchConcepts("ns", "name",
            new SearchConceptsRequest().query("conceptA")), CLIENT_CONCEPT_1);
  }

  @Test
  public void testSearchConceptsConceptIdMatch() throws Exception {
    saveConcepts();
    // ID matching currently includes substrings.
    assertResults(conceptsController.searchConcepts("ns", "name",
        new SearchConceptsRequest().query("123")), CLIENT_CONCEPT_4, CLIENT_CONCEPT_1);
  }


  @Test
  public void testSearchConceptsVocabIdMatch() throws Exception {
    saveConcepts();
    assertResults(conceptsController.searchConcepts("ns", "name",
            new SearchConceptsRequest().query("V456")), CLIENT_CONCEPT_4);
  }

  @Test
  public void testSearchConceptsMatchOrder() throws Exception{
    saveConcepts();
    assertResults(conceptsController.searchConcepts("ns", "name",
            new SearchConceptsRequest().query("conceptD")),
        CLIENT_CONCEPT_6, CLIENT_CONCEPT_5, CLIENT_CONCEPT_4);
  }

  @Test
  public void testSearchConceptsWithDomainAllCounts() throws Exception{
    saveConcepts();
    saveDomains();
    assertResultsWithCounts(conceptsController.searchConcepts("ns", "name",
        new SearchConceptsRequest().query("conceptD").includeDomainCounts(true)),
        ImmutableList.of(
            toDomainCount(CONDITION_DOMAIN, 2),
            toDomainCount(DRUG_DOMAIN, 0),
            toDomainCount(MEASUREMENT_DOMAIN, 0),
            toDomainCount(PROCEDURE_DOMAIN, 0)),null,
        ImmutableList.of(CLIENT_CONCEPT_6, CLIENT_CONCEPT_5, CLIENT_CONCEPT_4));
  }

  @Test
  public void testSearchConceptsWithDomainStandardCounts() throws Exception{
    saveConcepts();
    saveDomains();
    assertResultsWithCounts(conceptsController.searchConcepts("ns", "name",
        new SearchConceptsRequest().query("conceptD").includeDomainCounts(true)
            .standardConceptFilter(StandardConceptFilter.STANDARD_CONCEPTS)),
        ImmutableList.of(
            toDomainCount(CONDITION_DOMAIN, 1),
            toDomainCount(DRUG_DOMAIN, 0),
            toDomainCount(MEASUREMENT_DOMAIN, 0),
            toDomainCount(PROCEDURE_DOMAIN, 0)),null,
        // There's no domain filtering so we return CLIENT_CONCEPT_4 here even though it doesn't
        // show up in the counts.
        ImmutableList.of(CLIENT_CONCEPT_5, CLIENT_CONCEPT_4));
  }

  @Test
  public void testSearchConceptsWithVocabularyAllCounts() throws Exception{
    saveConcepts();
    saveDomains();
    assertResultsWithCounts(conceptsController.searchConcepts("ns", "name",
        new SearchConceptsRequest().query("conceptD").domain(Domain.CONDITION).includeVocabularyCounts(true)),
        null,ImmutableList.of(new VocabularyCount().vocabularyId("V5").conceptCount(2L)),
        ImmutableList.of(CLIENT_CONCEPT_6, CLIENT_CONCEPT_5));
  }

  @Test
  public void testSearchConceptsWithVocabularyStandardCounts() throws Exception{
    saveConcepts();
    saveDomains();
    assertResultsWithCounts(conceptsController.searchConcepts("ns", "name",
        new SearchConceptsRequest().query("conceptD").domain(Domain.CONDITION).includeVocabularyCounts(true)
            .standardConceptFilter(StandardConceptFilter.STANDARD_CONCEPTS)),
        null,ImmutableList.of(new VocabularyCount().vocabularyId("V5").conceptCount(1L)),
        ImmutableList.of(CLIENT_CONCEPT_5));
  }

  @Test
  public void testSearchConceptsWithDomainAndVocabularyStandardCounts() throws Exception{
    saveConcepts();
    saveDomains();
    assertResultsWithCounts(conceptsController.searchConcepts("ns", "name",
        new SearchConceptsRequest().query("conceptD").domain(Domain.CONDITION).includeVocabularyCounts(true)
            .includeDomainCounts(true).standardConceptFilter(StandardConceptFilter.STANDARD_CONCEPTS)),
        ImmutableList.of(
            toDomainCount(CONDITION_DOMAIN, 1),
            toDomainCount(DRUG_DOMAIN, 0),
            toDomainCount(MEASUREMENT_DOMAIN, 0),
            toDomainCount(PROCEDURE_DOMAIN, 0)),
        ImmutableList.of(new VocabularyCount().vocabularyId("V5").conceptCount(1L)),
        ImmutableList.of(CLIENT_CONCEPT_5));
  }

  @Test
  public void testSearchConceptsWithDomainAndVocabularyAllCounts() throws Exception{
    saveConcepts();
    saveDomains();
    assertResultsWithCounts(conceptsController.searchConcepts("ns", "name",
        new SearchConceptsRequest().query("conceptD").domain(Domain.CONDITION).includeVocabularyCounts(true)
            .includeDomainCounts(true).standardConceptFilter(StandardConceptFilter.ALL_CONCEPTS)),
        ImmutableList.of(
            toDomainCount(CONDITION_DOMAIN, 2),
            toDomainCount(DRUG_DOMAIN, 0),
            toDomainCount(MEASUREMENT_DOMAIN, 0),
            toDomainCount(PROCEDURE_DOMAIN, 0)),
        ImmutableList.of(new VocabularyCount().vocabularyId("V5").conceptCount(2L)),
        ImmutableList.of(CLIENT_CONCEPT_6, CLIENT_CONCEPT_5));
  }

  @Test
  public void testSearchConceptsWithDomainAndVocabularyAllCountsMatchAllConditions() throws Exception{
    saveConcepts();
    saveDomains();
    assertResultsWithCounts(conceptsController.searchConcepts("ns", "name",
        new SearchConceptsRequest().query("con").domain(Domain.CONDITION).includeVocabularyCounts(true)
            .includeDomainCounts(true).standardConceptFilter(StandardConceptFilter.ALL_CONCEPTS)),
        ImmutableList.of(
            toDomainCount(CONDITION_DOMAIN, 4),
            toDomainCount(DRUG_DOMAIN, 0),
            // Although it doesn't match the domain filter, we still include the measurement concept
            // in domain counts
            toDomainCount(MEASUREMENT_DOMAIN, 1),
            toDomainCount(PROCEDURE_DOMAIN, 0)),
        ImmutableList.of(new VocabularyCount().vocabularyId("V1").conceptCount(1L),
            new VocabularyCount().vocabularyId("V3").conceptCount(1L),
            new VocabularyCount().vocabularyId("V5").conceptCount(2L)),
        ImmutableList.of(CLIENT_CONCEPT_6, CLIENT_CONCEPT_5, CLIENT_CONCEPT_3, CLIENT_CONCEPT_1));
  }

  @Test
  public void testSearchConceptsNonStandard() throws Exception{
    saveConcepts();
    ResponseEntity<ConceptListResponse> response = conceptsController.searchConcepts("ns", "name",
            new SearchConceptsRequest().query("conceptB"));
    assertResults(
            conceptsController.searchConcepts("ns", "name",
                    new SearchConceptsRequest().query("conceptB")), CLIENT_CONCEPT_2);
  }

  @Test
  public void testSearchConceptsStandardConcept() throws Exception {
    saveConcepts();
    assertResults(conceptsController.searchConcepts("ns", "name",
            new SearchConceptsRequest().query("conceptA")
                    .standardConceptFilter(StandardConceptFilter.STANDARD_CONCEPTS)),
        CLIENT_CONCEPT_1);
  }

  @Test
  public void testSearchConceptsNotStandardConcept() throws Exception {
    saveConcepts();
    ResponseEntity<ConceptListResponse> response = conceptsController.searchConcepts("ns", "name",
            new SearchConceptsRequest().query("conceptB")
                    .standardConceptFilter(StandardConceptFilter.NON_STANDARD_CONCEPTS));
    Concept concept = response.getBody().getItems().get(0);
    assertThat(concept.getConceptCode()).isEqualTo("conceptB");
  }

  @Test
  public void testSearchConceptsVocabularyIdNoMatch() throws Exception {
    saveConcepts();
    assertResults(conceptsController.searchConcepts("ns", "name",
            new SearchConceptsRequest().query("con").vocabularyIds(ImmutableList.of("x", "v"))));
  }

  @Test
  public void testSearchConceptsVocabularyIdMatch() throws Exception {
    saveConcepts();
    assertResults(conceptsController.searchConcepts("ns", "name",
            new SearchConceptsRequest().query("conceptB").vocabularyIds(ImmutableList.of("V3", "V2"))),
        CLIENT_CONCEPT_2);
  }

  @Test
  public void testSearchConceptsDomainIdNoMatch() throws Exception {
    saveConcepts();
    assertResults(conceptsController.searchConcepts("ns", "name",
            new SearchConceptsRequest().query("zzz").domain(Domain.OBSERVATION)));
  }

  @Test
  public void testSearchConceptsDomainIdMatch() throws Exception {
    saveConcepts();
    assertResults(conceptsController.searchConcepts("ns", "name",
        new SearchConceptsRequest().query("conceptA").domain(Domain.CONDITION)), CLIENT_CONCEPT_1);
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
    assertResults(conceptsController.searchConcepts("ns", "name",
            new SearchConceptsRequest().query("con")
                .standardConceptFilter(StandardConceptFilter.NON_STANDARD_CONCEPTS)
                .vocabularyIds(ImmutableList.of("V1"))
                .domain(Domain.CONDITION)));
  }

  public void testSearchConceptsOneResult() throws Exception {
    saveConcepts();
    assertResults(conceptsController.searchConcepts("ns", "name",
            new SearchConceptsRequest().query("conceptC").maxResults(1)),
        CLIENT_CONCEPT_3);
  }

  @Test
  public void testSearchConceptsSubstring() throws Exception {
    saveConcepts();
    assertResults(
        conceptsController.searchConcepts("ns", "name",
            new SearchConceptsRequest().query("est").maxResults(1000)),
        CLIENT_CONCEPT_6, CLIENT_CONCEPT_5, CLIENT_CONCEPT_4);
  }

  @Test
  public void testGetDomainInfo() throws Exception {
    saveConcepts();
    saveDomains();
    List<DomainInfo> domainInfos = conceptsController.getDomainInfo("ns", "name")
        .getBody().getItems();
    assertThat(domainInfos).containsExactly(
        new DomainInfo()
            .domain(CONDITION_DOMAIN.getDomainEnum())
            .name(CONDITION_DOMAIN.getName())
            .description(CONDITION_DOMAIN.getDescription())
            .participantCount(CONDITION_DOMAIN.getParticipantCount())
            .allConceptCount(CONDITION_DOMAIN.getAllConceptCount())
            .standardConceptCount(CONDITION_DOMAIN.getStandardConceptCount()),
        new DomainInfo()
            .domain(DRUG_DOMAIN.getDomainEnum())
            .name(DRUG_DOMAIN.getName())
            .description(DRUG_DOMAIN.getDescription())
            .participantCount(DRUG_DOMAIN.getParticipantCount())
            .allConceptCount(DRUG_DOMAIN.getAllConceptCount())
            .standardConceptCount(DRUG_DOMAIN.getStandardConceptCount()),
        new DomainInfo()
            .domain(MEASUREMENT_DOMAIN.getDomainEnum())
            .name(MEASUREMENT_DOMAIN.getName())
            .description(MEASUREMENT_DOMAIN.getDescription())
            .participantCount(MEASUREMENT_DOMAIN.getParticipantCount())
            .allConceptCount(MEASUREMENT_DOMAIN.getAllConceptCount())
            .standardConceptCount(MEASUREMENT_DOMAIN.getStandardConceptCount()),
        new DomainInfo()
            .domain(PROCEDURE_DOMAIN.getDomainEnum())
            .name(PROCEDURE_DOMAIN.getName())
            .description(PROCEDURE_DOMAIN.getDescription())
            .participantCount(PROCEDURE_DOMAIN.getParticipantCount())
            .allConceptCount(PROCEDURE_DOMAIN.getAllConceptCount())
            .standardConceptCount(PROCEDURE_DOMAIN.getStandardConceptCount())).inOrder();
  }

  @Test
  public void testGetValuesFromDomain() {
    when(bigQueryService.getTableFieldsFromDomain(Domain.CONDITION))
        .thenReturn(FieldList.of(
            Field.of("FIELD_ONE", LegacySQLTypeName.STRING),
            Field.of("FIELD_TWO", LegacySQLTypeName.STRING)
        ));
    List<DomainValue> domainValues =
        conceptsController.getValuesFromDomain("ns", "name", Domain.CONDITION.toString()).getBody().getItems();
    verify(bigQueryService).getTableFieldsFromDomain(Domain.CONDITION);

    assertThat(domainValues).containsExactly(
        new DomainValue().value("FIELD_ONE"),
        new DomainValue().value("FIELD_TWO"));
  }

  static org.pmiops.workbench.cdr.model.Concept makeConcept(Concept concept) {
    org.pmiops.workbench.cdr.model.Concept result = new org.pmiops.workbench.cdr.model.Concept();
    result.setConceptId(concept.getConceptId());
    result.setConceptName(concept.getConceptName());
    result.setStandardConcept(concept.getStandardConcept() == null ? null :
        (concept.getStandardConcept() ? "S" : null));
    result.setConceptCode(concept.getConceptCode());
    result.setConceptClassId(concept.getConceptClassId());
    result.setVocabularyId(concept.getVocabularyId());
    result.setDomainId(concept.getDomainId());
    result.setCountValue(concept.getCountValue());
    result.setPrevalence(concept.getPrevalence());
    result.setSynonymsStr(
        String.valueOf(concept.getConceptId()) + '|' +
            Joiner.on("|").join(concept.getConceptSynonyms()));
    return result;
  }

  private void saveConcepts() {
    conceptDao.save(CONCEPT_1);
    conceptDao.save(CONCEPT_2);
    conceptDao.save(CONCEPT_3);
    conceptDao.save(CONCEPT_4);
    conceptDao.save(CONCEPT_5);
    conceptDao.save(CONCEPT_6);
  }

  private void saveDomains() {
    domainInfoDao.save(MEASUREMENT_DOMAIN);
    domainInfoDao.save(PROCEDURE_DOMAIN);
    domainInfoDao.save(CONDITION_DOMAIN);
    domainInfoDao.save(DRUG_DOMAIN);
  }

  private void saveDomainVocabularyInfos() {
    domainVocabularyInfoDao.save(CONDITION_V1_INFO);
    domainVocabularyInfoDao.save(CONDITION_V3_INFO);
    domainVocabularyInfoDao.save(CONDITION_V5_INFO);
  }

  private DomainCount toDomainCount(org.pmiops.workbench.cdr.model.DomainInfo domainInfo, boolean standardCount) {
    return toDomainCount(domainInfo,
        standardCount ? domainInfo.getStandardConceptCount() : domainInfo.getAllConceptCount());
  }

  private DomainCount toDomainCount(org.pmiops.workbench.cdr.model.DomainInfo domainInfo, long conceptCount) {
    return new DomainCount().name(domainInfo.getName())
        .conceptCount(conceptCount)
        .domain(domainInfo.getDomainEnum());
  }

  private void assertResultsWithCounts(ResponseEntity<ConceptListResponse> response,
      ImmutableList<DomainCount> domainCounts, ImmutableList<VocabularyCount> vocabularyCounts,
      ImmutableList<Concept> concepts) {
    assertThat(response.getBody().getDomainCounts()).isEqualTo(domainCounts);
    assertThat(response.getBody().getVocabularyCounts()).isEqualTo(vocabularyCounts);
    assertThat(response.getBody().getItems()).isEqualTo(concepts);
  }

  private void assertResults(ResponseEntity<ConceptListResponse> response,
      Concept... expectedConcepts) {
    assertThat(response.getBody().getItems()).isEqualTo(ImmutableList.copyOf(expectedConcepts));
    assertThat(response.getBody().getDomainCounts()).isNull();
    assertThat(response.getBody().getVocabularyCounts()).isNull();
  }
}
