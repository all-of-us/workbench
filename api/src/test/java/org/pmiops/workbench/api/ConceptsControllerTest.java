package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.inject.Provider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.pmiops.workbench.cdr.ConceptBigQueryService;
import org.pmiops.workbench.cdr.dao.CBCriteriaDao;
import org.pmiops.workbench.cdr.dao.ConceptDao;
import org.pmiops.workbench.cdr.dao.DomainInfoDao;
import org.pmiops.workbench.cdr.dao.SurveyModuleDao;
import org.pmiops.workbench.cdr.model.DbConcept;
import org.pmiops.workbench.cdr.model.DbCriteria;
import org.pmiops.workbench.cdr.model.DbDomainInfo;
import org.pmiops.workbench.cdr.model.DbSurveyModule;
import org.pmiops.workbench.cohorts.CohortCloningService;
import org.pmiops.workbench.concept.ConceptService;
import org.pmiops.workbench.conceptset.ConceptSetService;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.DataSetService;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceACL;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceAccessEntry;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceResponse;
import org.pmiops.workbench.model.Concept;
import org.pmiops.workbench.model.ConceptListResponse;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.DomainCount;
import org.pmiops.workbench.model.DomainCountsListResponse;
import org.pmiops.workbench.model.DomainCountsRequest;
import org.pmiops.workbench.model.DomainInfo;
import org.pmiops.workbench.model.EmailVerificationStatus;
import org.pmiops.workbench.model.SearchConceptsRequest;
import org.pmiops.workbench.model.StandardConceptFilter;
import org.pmiops.workbench.model.SurveyModule;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.pmiops.workbench.workspaces.WorkspaceServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import({BigQueryService.class})
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class ConceptsControllerTest {

  private static final Concept CLIENT_CONCEPT_1 =
      new Concept()
          .conceptId(123L)
          .conceptName("a concept")
          .standardConcept(true)
          .conceptCode("conceptA")
          .conceptClassId("classId")
          .vocabularyId("V1")
          .domainId("Condition")
          .countValue(123L)
          .prevalence(0.2F)
          .conceptSynonyms(new ArrayList<>());

  private static final Concept CLIENT_CONCEPT_2 =
      new Concept()
          .conceptId(456L)
          .standardConcept(false)
          .conceptName("b concept")
          .conceptCode("conceptB")
          .conceptClassId("classId2")
          .vocabularyId("V2")
          .domainId("Measurement")
          .countValue(456L)
          .prevalence(0.3F)
          .conceptSynonyms(new ArrayList<>());

  private static final Concept CLIENT_CONCEPT_3 =
      new Concept()
          .conceptId(789L)
          .standardConcept(false)
          .conceptName("multi word concept")
          .conceptCode("conceptC")
          .conceptClassId("classId3")
          .vocabularyId("V3")
          .domainId("Condition")
          .countValue(789L)
          .prevalence(0.4F)
          .conceptSynonyms(new ArrayList<>());

  private static final Concept CLIENT_CONCEPT_4 =
      new Concept()
          .conceptId(1234L)
          .conceptName("sample test con to test the multi word search")
          .standardConcept(true)
          .conceptCode("conceptD")
          .conceptClassId("classId4")
          .vocabularyId("V456")
          .domainId("Observation")
          .countValue(1250L)
          .prevalence(0.5F)
          .conceptSynonyms(new ArrayList<>());

  private static final Concept CLIENT_CONCEPT_5 =
      new Concept()
          .conceptId(7890L)
          .conceptName("conceptD test concept")
          .standardConcept(true)
          .conceptCode("conceptE")
          .conceptClassId("classId5")
          .vocabularyId("V5")
          .domainId("Condition")
          .countValue(7890L)
          .prevalence(0.9F)
          .conceptSynonyms(new ArrayList<>());

  private static final Concept CLIENT_CONCEPT_6 =
      new Concept()
          .conceptId(7891L)
          .conceptName("conceptD test concept 2")
          .standardConcept(false)
          .conceptCode("conceptD")
          .conceptClassId("classId6")
          .vocabularyId("V5")
          .domainId("Condition")
          .countValue(7891L)
          .prevalence(0.1F)
          .conceptSynonyms(new ArrayList<>());

  private static final DbConcept CONCEPT_1 = makeConcept(CLIENT_CONCEPT_1);
  private static final DbConcept CONCEPT_2 = makeConcept(CLIENT_CONCEPT_2);
  private static final DbConcept CONCEPT_3 = makeConcept(CLIENT_CONCEPT_3);
  private static final DbConcept CONCEPT_4 = makeConcept(CLIENT_CONCEPT_4);
  private static final DbConcept CONCEPT_5 = makeConcept(CLIENT_CONCEPT_5);
  private static final DbConcept CONCEPT_6 = makeConcept(CLIENT_CONCEPT_6);

  private static final DbDomainInfo MEASUREMENT_DOMAIN =
      new DbDomainInfo()
          .domainEnum(Domain.MEASUREMENT)
          .domainId("Measurement")
          .name("Measurement!")
          .description("Measurements!!!")
          .conceptId(CONCEPT_1.getConceptId())
          .participantCount(123)
          .standardConceptCount(3)
          .allConceptCount(5);

  private static final DbDomainInfo CONDITION_DOMAIN =
      new DbDomainInfo()
          .domainEnum(Domain.CONDITION)
          .domainId("Condition")
          .name("Condition!")
          .description("Conditions!")
          .conceptId(CONCEPT_2.getConceptId())
          .participantCount(456)
          .standardConceptCount(4)
          .allConceptCount(6);

  private static final DbDomainInfo PROCEDURE_DOMAIN =
      new DbDomainInfo()
          .domainEnum(Domain.PROCEDURE)
          .domainId("Procedure")
          .name("Procedure!!!")
          .description("Procedures!!!")
          .conceptId(CONCEPT_3.getConceptId())
          .participantCount(789)
          .standardConceptCount(1)
          .allConceptCount(2);

  private static final DbDomainInfo DRUG_DOMAIN =
      new DbDomainInfo()
          .domainEnum(Domain.DRUG)
          .domainId("Drug")
          .name("Drug!")
          .description("Drugs!")
          .conceptId(CONCEPT_4.getConceptId())
          .participantCount(3)
          .standardConceptCount(3)
          .allConceptCount(4);

  private static final DbDomainInfo OBSERVATION_DOMAIN =
      new DbDomainInfo()
          .domainEnum(Domain.OBSERVATION)
          .domainId("Observation")
          .name("Observation")
          .description("Observation")
          .conceptId(CONCEPT_4.getConceptId())
          .participantCount(33)
          .standardConceptCount(56)
          .allConceptCount(90);

  private static final DbDomainInfo SURVEY_DOMAIN =
      new DbDomainInfo()
          .domainEnum(Domain.SURVEY)
          .domainId("Survey")
          .name("Surveys")
          .description("SURVEY")
          .conceptId(CONCEPT_4.getConceptId())
          .participantCount(45)
          .standardConceptCount(0)
          .allConceptCount(2);

  private static final DbSurveyModule BASICS_SURVEY_MODULE =
      new DbSurveyModule()
          .conceptId(1L)
          .description("The Basics")
          .name("The Basics")
          .orderNumber(1)
          .participantCount(200)
          .questionCount(16);

  private static final DbSurveyModule LIFESTYLE_SURVEY_MODULE =
      new DbSurveyModule()
          .conceptId(2L)
          .description("Lifestyle")
          .name("Lifestyle")
          .orderNumber(3)
          .participantCount(2000)
          .questionCount(24);

  private static final DbSurveyModule OVERALL_HEALTH_SURVEY_MODULE =
      new DbSurveyModule()
          .conceptId(3L)
          .description("Overall Health")
          .name("Overall Health")
          .orderNumber(2)
          .participantCount(2000)
          .questionCount(24);

  private static final String WORKSPACE_NAMESPACE = "ns";
  private static final String WORKSPACE_NAME = "name";
  private static final String USER_EMAIL = "bob@gmail.com";
  private static DbUser currentUser;

  @TestConfiguration
  @Import({WorkspaceServiceImpl.class})
  @MockBean({
    BigQueryService.class,
    FireCloudService.class,
    CohortCloningService.class,
    ConceptSetService.class,
    ConceptBigQueryService.class,
    Clock.class,
    DataSetService.class
  })
  static class Configuration {
    @Bean
    @Scope("prototype")
    DbUser user() {
      return currentUser;
    }
  }

  @Autowired private ConceptDao conceptDao;
  @Autowired private WorkspaceService workspaceService;
  @Autowired private WorkspaceDao workspaceDao;
  @Autowired private CdrVersionDao cdrVersionDao;
  @Autowired private DomainInfoDao domainInfoDao;
  @Autowired private SurveyModuleDao surveyModuleDao;
  @Autowired private CBCriteriaDao cbCriteriaDao;
  @Autowired FireCloudService fireCloudService;
  @Autowired UserDao userDao;
  @Mock Provider<DbUser> userProvider;

  private ConceptsController conceptsController;

  @Before
  public void setUp() {
    // Injecting ConceptsController and ConceptService doesn't work well without using
    // SpringBootTest, which causes problems with CdrDbConfig. Just construct the service and
    // controller directly.
    ConceptService conceptService =
        new ConceptService(conceptDao, domainInfoDao, surveyModuleDao, cbCriteriaDao);
    conceptsController = new ConceptsController(conceptService, workspaceService);

    DbUser user = new DbUser();
    user.setUsername(USER_EMAIL);
    user.setUserId(123L);
    user.setDisabled(false);
    user.setEmailVerificationStatusEnum(EmailVerificationStatus.SUBSCRIBED);
    user = userDao.save(user);
    currentUser = user;
    when(userProvider.get()).thenReturn(user);

    DbCdrVersion cdrVersion = new DbCdrVersion();
    cdrVersion.setName("1");
    // set the db name to be empty since test cases currently
    // run in the workbench schema only.
    cdrVersion.setCdrDbName("");
    cdrVersion = cdrVersionDao.save(cdrVersion);

    DbWorkspace workspace = new DbWorkspace();
    workspace.setWorkspaceId(1L);
    workspace.setName("name");
    workspace.setFirecloudName("name");
    workspace.setWorkspaceNamespace("ns");
    workspace.setCdrVersion(cdrVersion);
    workspaceDao.save(workspace);
    FirecloudWorkspaceResponse fcResponse = new FirecloudWorkspaceResponse();
    fcResponse.setAccessLevel(WorkspaceAccessLevel.OWNER.name());
    when(fireCloudService.getWorkspace(WORKSPACE_NAMESPACE, WORKSPACE_NAME)).thenReturn(fcResponse);
    FirecloudWorkspaceACL workspaceAccessLevelResponse = new FirecloudWorkspaceACL();
    FirecloudWorkspaceAccessEntry accessLevelEntry =
        new FirecloudWorkspaceAccessEntry().accessLevel(WorkspaceAccessLevel.WRITER.toString());
    Map<String, FirecloudWorkspaceAccessEntry> userEmailToAccessEntry =
        ImmutableMap.of(USER_EMAIL, accessLevelEntry);
    workspaceAccessLevelResponse.setAcl(userEmailToAccessEntry);
    when(fireCloudService.getWorkspaceAcl(WORKSPACE_NAMESPACE, WORKSPACE_NAME))
        .thenReturn(workspaceAccessLevelResponse);

    DbCriteria parentSurvey =
        cbCriteriaDao.save(
            new DbCriteria()
                .ancestorData(false)
                .attribute(false)
                .code("c")
                .conceptId("1")
                .count("20")
                .domainId("SURVEY")
                .group(true)
                .hierarchy(true)
                .name("The Basics")
                .parentId(0L)
                .selectable(true)
                .standard(true)
                .subtype("QUESTION")
                .type("PPI"));

    cbCriteriaDao.save(
        new DbCriteria()
            .ancestorData(false)
            .attribute(false)
            .code("c")
            .conceptId("1")
            .count("20")
            .domainId("SURVEY")
            .group(true)
            .hierarchy(true)
            .name("question")
            .parentId(parentSurvey.getId())
            .selectable(true)
            .standard(true)
            .subtype("QUESTION")
            .type("PPI")
            .synonyms("test"));
  }

  @Test
  public void testSearchConceptsBlankQuery() {
    assertResults(
        conceptsController.searchConcepts(
            "ns",
            "name",
            new SearchConceptsRequest()
                .query(" ")
                .domain(Domain.CONDITION)
                .standardConceptFilter(StandardConceptFilter.ALL_CONCEPTS)));
  }

  @Test
  public void testSearchConceptsBlankQueryWithResults() {
    saveConcepts();
    assertResults(
        conceptsController.searchConcepts(
            "ns",
            "name",
            new SearchConceptsRequest()
                .query(" ")
                .domain(Domain.CONDITION)
                .standardConceptFilter(StandardConceptFilter.ALL_CONCEPTS)),
        CLIENT_CONCEPT_6,
        CLIENT_CONCEPT_5,
        CLIENT_CONCEPT_3,
        CLIENT_CONCEPT_1);
  }

  @Test
  public void testDomainCountSourceAndStandardTotalCount() {
    saveConcepts();
    saveDomains();
    ResponseEntity<DomainCountsListResponse> response =
        conceptsController.domainCounts(
            "ns",
            "name",
            new DomainCountsRequest().standardConceptFilter(StandardConceptFilter.ALL_CONCEPTS));
    assertCounts(
        response,
        ImmutableList.of(
            toDomainCount(CONDITION_DOMAIN, false),
            toDomainCount(DRUG_DOMAIN, false),
            toDomainCount(MEASUREMENT_DOMAIN, false),
            toDomainCount(OBSERVATION_DOMAIN, false),
            toDomainCount(PROCEDURE_DOMAIN, false),
            toDomainCount(SURVEY_DOMAIN, false)));
  }

  @Test
  public void testDomainCountSourceAndStandardWithSearchTerm() {
    saveConcepts();
    saveDomains();
    ResponseEntity<DomainCountsListResponse> response =
        conceptsController.domainCounts(
            "ns",
            "name",
            new DomainCountsRequest()
                .query("conceptA")
                .standardConceptFilter(StandardConceptFilter.ALL_CONCEPTS));
    assertCounts(
        response,
        ImmutableList.of(
            toDomainCount(CONDITION_DOMAIN, 1),
            toDomainCount(DRUG_DOMAIN, 0),
            toDomainCount(MEASUREMENT_DOMAIN, 0),
            toDomainCount(OBSERVATION_DOMAIN, 0),
            toDomainCount(PROCEDURE_DOMAIN, 0),
            toDomainCount(SURVEY_DOMAIN, 0)));
  }

  @Test
  public void testDomainCountStandardTotalCount() {
    saveConcepts();
    saveDomains();
    ResponseEntity<DomainCountsListResponse> response =
        conceptsController.domainCounts(
            "ns",
            "name",
            new DomainCountsRequest()
                .standardConceptFilter(StandardConceptFilter.STANDARD_CONCEPTS));
    assertCounts(
        response,
        ImmutableList.of(
            toDomainCount(CONDITION_DOMAIN, true),
            toDomainCount(DRUG_DOMAIN, true),
            toDomainCount(MEASUREMENT_DOMAIN, true),
            toDomainCount(OBSERVATION_DOMAIN, true),
            toDomainCount(PROCEDURE_DOMAIN, true),
            toDomainCount(SURVEY_DOMAIN, true)));
  }

  @Test
  public void testDomainCountStandardWithSearchTerm() {
    saveConcepts();
    saveDomains();
    ResponseEntity<DomainCountsListResponse> response =
        conceptsController.domainCounts(
            "ns",
            "name",
            new DomainCountsRequest()
                .query("conceptD")
                .standardConceptFilter(StandardConceptFilter.STANDARD_CONCEPTS));
    assertCounts(
        response,
        ImmutableList.of(
            toDomainCount(CONDITION_DOMAIN, 1),
            toDomainCount(DRUG_DOMAIN, 0),
            toDomainCount(MEASUREMENT_DOMAIN, 0),
            toDomainCount(OBSERVATION_DOMAIN, 1),
            toDomainCount(PROCEDURE_DOMAIN, 0),
            toDomainCount(SURVEY_DOMAIN, 0)));
  }

  @Test
  public void testSurveyCountSourceAndStandardWithSearchTerm() {
    saveConcepts();
    saveDomains();
    ResponseEntity<DomainCountsListResponse> response =
        conceptsController.domainCounts(
            "ns",
            "name",
            new DomainCountsRequest()
                .query("test")
                .standardConceptFilter(StandardConceptFilter.ALL_CONCEPTS));
    assertCounts(
        response,
        ImmutableList.of(
            toDomainCount(CONDITION_DOMAIN, 2),
            toDomainCount(DRUG_DOMAIN, 0),
            toDomainCount(MEASUREMENT_DOMAIN, 0),
            toDomainCount(OBSERVATION_DOMAIN, 1),
            toDomainCount(PROCEDURE_DOMAIN, 0),
            toDomainCount(SURVEY_DOMAIN, 1)));
  }

  @Test
  public void testSurveyCountSourceAndStandardWithSurveyName() {
    saveConcepts();
    saveDomains();
    ResponseEntity<DomainCountsListResponse> response =
        conceptsController.domainCounts(
            "ns",
            "name",
            new DomainCountsRequest()
                .surveyName("The Basics")
                .standardConceptFilter(StandardConceptFilter.ALL_CONCEPTS));
    assertCounts(
        response,
        ImmutableList.of(
            toDomainCount(CONDITION_DOMAIN, false),
            toDomainCount(DRUG_DOMAIN, false),
            toDomainCount(MEASUREMENT_DOMAIN, false),
            toDomainCount(OBSERVATION_DOMAIN, false),
            toDomainCount(PROCEDURE_DOMAIN, false),
            toDomainCount(SURVEY_DOMAIN, 1)));
  }

  @Test
  public void testSearchNoConcepts() {
    assertResults(
        conceptsController.searchConcepts(
            "ns",
            "name",
            new SearchConceptsRequest()
                .query("a")
                .domain(Domain.CONDITION)
                .standardConceptFilter(StandardConceptFilter.ALL_CONCEPTS)));
  }

  @Test
  public void testSearchConceptsNameNoMatches() {
    saveConcepts();
    assertResults(
        conceptsController.searchConcepts(
            "ns",
            "name",
            new SearchConceptsRequest()
                .query("x")
                .domain(Domain.CONDITION)
                .standardConceptFilter(StandardConceptFilter.ALL_CONCEPTS)));
  }

  @Test
  public void testSearchConceptsCodeMatch() {
    saveConcepts();
    assertResults(
        conceptsController.searchConcepts(
            "ns",
            "name",
            new SearchConceptsRequest()
                .query("conceptA")
                .domain(Domain.CONDITION)
                .standardConceptFilter(StandardConceptFilter.STANDARD_CONCEPTS)),
        CLIENT_CONCEPT_1);
  }

  @Test
  public void testSearchConceptsConceptIdMatch() {
    saveConcepts();
    // ID matching currently includes substrings.
    assertResults(
        conceptsController.searchConcepts(
            "ns",
            "name",
            new SearchConceptsRequest()
                .query("123")
                .domain(Domain.OBSERVATION)
                .standardConceptFilter(StandardConceptFilter.STANDARD_CONCEPTS)),
        CLIENT_CONCEPT_4);
  }

  @Test
  public void testSearchConceptsVocabIdMatch() {
    saveConcepts();
    assertResults(
        conceptsController.searchConcepts(
            "ns",
            "name",
            new SearchConceptsRequest()
                .query("V456")
                .domain(Domain.OBSERVATION)
                .standardConceptFilter(StandardConceptFilter.STANDARD_CONCEPTS)),
        CLIENT_CONCEPT_4);
  }

  @Test
  public void testSearchConceptsMatchOrder() {
    saveConcepts();
    assertResults(
        conceptsController.searchConcepts(
            "ns",
            "name",
            new SearchConceptsRequest()
                .query("conceptD")
                .domain(Domain.CONDITION)
                .standardConceptFilter(StandardConceptFilter.ALL_CONCEPTS)),
        CLIENT_CONCEPT_6,
        CLIENT_CONCEPT_5);
  }

  @Test
  public void testSearchConceptsWithVocabularyStandard() {
    saveConcepts();
    assertResults(
        conceptsController.searchConcepts(
            "ns",
            "name",
            new SearchConceptsRequest()
                .query("conceptD")
                .domain(Domain.CONDITION)
                .standardConceptFilter(StandardConceptFilter.STANDARD_CONCEPTS)),
        CLIENT_CONCEPT_5);
  }

  @Test
  public void testSearchConceptsNonStandard() {
    saveConcepts();
    assertResults(
        conceptsController.searchConcepts(
            "ns",
            "name",
            new SearchConceptsRequest()
                .domain(Domain.MEASUREMENT)
                .standardConceptFilter(StandardConceptFilter.ALL_CONCEPTS)
                .query("conceptB")),
        CLIENT_CONCEPT_2);
  }

  @Test
  public void testSearchConceptsStandardConcept() {
    saveConcepts();
    assertResults(
        conceptsController.searchConcepts(
            "ns",
            "name",
            new SearchConceptsRequest()
                .domain(Domain.CONDITION)
                .query("conceptA")
                .standardConceptFilter(StandardConceptFilter.STANDARD_CONCEPTS)),
        CLIENT_CONCEPT_1);
  }

  @Test
  public void testSearchConceptsDomainIdNoMatch() {
    saveConcepts();
    assertResults(
        conceptsController.searchConcepts(
            "ns",
            "name",
            new SearchConceptsRequest()
                .query("zzz")
                .domain(Domain.OBSERVATION)
                .standardConceptFilter(StandardConceptFilter.ALL_CONCEPTS)));
  }

  @Test
  public void testSearchConceptsDomainIdMatch() {
    saveConcepts();
    assertResults(
        conceptsController.searchConcepts(
            "ns",
            "name",
            new SearchConceptsRequest()
                .query("conceptA")
                .domain(Domain.CONDITION)
                .standardConceptFilter(StandardConceptFilter.STANDARD_CONCEPTS)),
        CLIENT_CONCEPT_1);
  }

  @Test
  public void testSearchConceptsMultipleMatch() {
    saveConcepts();
    assertResults(
        conceptsController.searchConcepts(
            "ns",
            "name",
            new SearchConceptsRequest()
                .query("con")
                .standardConceptFilter(StandardConceptFilter.STANDARD_CONCEPTS)
                .domain(Domain.CONDITION)),
        CLIENT_CONCEPT_5,
        CLIENT_CONCEPT_1);
  }

  @Test
  public void testSearchConceptsOneResult() {
    saveConcepts();
    assertResults(
        conceptsController.searchConcepts(
            "ns",
            "name",
            new SearchConceptsRequest()
                .query("conceptC")
                .domain(Domain.CONDITION)
                .standardConceptFilter(StandardConceptFilter.ALL_CONCEPTS)
                .maxResults(1)),
        CLIENT_CONCEPT_3);
  }

  @Test
  public void testSearchConceptsSubstring() {
    saveConcepts();
    assertResults(
        conceptsController.searchConcepts(
            "ns",
            "name",
            new SearchConceptsRequest()
                .query("est")
                .domain(Domain.CONDITION)
                .standardConceptFilter(StandardConceptFilter.ALL_CONCEPTS)
                .maxResults(1000)),
        CLIENT_CONCEPT_6,
        CLIENT_CONCEPT_5);
  }

  @Test
  public void testGetDomainInfo() {
    saveConcepts();
    saveDomains();
    List<DomainInfo> domainInfos =
        conceptsController.getDomainInfo("ns", "name").getBody().getItems();
    assertThat(domainInfos)
        .containsExactly(
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
                .domain(OBSERVATION_DOMAIN.getDomainEnum())
                .name(OBSERVATION_DOMAIN.getName())
                .description(OBSERVATION_DOMAIN.getDescription())
                .participantCount(OBSERVATION_DOMAIN.getParticipantCount())
                .allConceptCount(OBSERVATION_DOMAIN.getAllConceptCount())
                .standardConceptCount(OBSERVATION_DOMAIN.getStandardConceptCount()),
            new DomainInfo()
                .domain(PROCEDURE_DOMAIN.getDomainEnum())
                .name(PROCEDURE_DOMAIN.getName())
                .description(PROCEDURE_DOMAIN.getDescription())
                .participantCount(PROCEDURE_DOMAIN.getParticipantCount())
                .allConceptCount(PROCEDURE_DOMAIN.getAllConceptCount())
                .standardConceptCount(PROCEDURE_DOMAIN.getStandardConceptCount()))
        .inOrder();
  }

  @Test
  public void testGetSurveyInfo() {
    saveSurveyInfo();
    List<SurveyModule> surveyModules =
        conceptsController.getSurveyInfo("ns", "name").getBody().getItems();
    assertThat(surveyModules)
        .containsExactly(
            new SurveyModule()
                .conceptId(BASICS_SURVEY_MODULE.getConceptId())
                .description(BASICS_SURVEY_MODULE.getDescription())
                .name(BASICS_SURVEY_MODULE.getName())
                .orderNumber(BASICS_SURVEY_MODULE.getOrderNumber())
                .participantCount(BASICS_SURVEY_MODULE.getParticipantCount())
                .questionCount(BASICS_SURVEY_MODULE.getQuestionCount()),
            new SurveyModule()
                .conceptId(OVERALL_HEALTH_SURVEY_MODULE.getConceptId())
                .description(OVERALL_HEALTH_SURVEY_MODULE.getDescription())
                .name(OVERALL_HEALTH_SURVEY_MODULE.getName())
                .orderNumber(OVERALL_HEALTH_SURVEY_MODULE.getOrderNumber())
                .participantCount(OVERALL_HEALTH_SURVEY_MODULE.getParticipantCount())
                .questionCount(OVERALL_HEALTH_SURVEY_MODULE.getQuestionCount()),
            new SurveyModule()
                .conceptId(LIFESTYLE_SURVEY_MODULE.getConceptId())
                .description(LIFESTYLE_SURVEY_MODULE.getDescription())
                .name(LIFESTYLE_SURVEY_MODULE.getName())
                .orderNumber(LIFESTYLE_SURVEY_MODULE.getOrderNumber())
                .participantCount(LIFESTYLE_SURVEY_MODULE.getParticipantCount())
                .questionCount(LIFESTYLE_SURVEY_MODULE.getQuestionCount()))
        .inOrder();
  }

  public static DbConcept makeConcept(Concept concept) {
    DbConcept result = new DbConcept();
    result.setConceptId(concept.getConceptId());
    result.setConceptName(concept.getConceptName());
    result.setStandardConcept(
        concept.getStandardConcept() == null ? "" : (concept.getStandardConcept() ? "S" : ""));
    result.setConceptCode(concept.getConceptCode());
    result.setConceptClassId(concept.getConceptClassId());
    result.setVocabularyId(concept.getVocabularyId());
    result.setDomainId(concept.getDomainId());
    result.setCountValue(concept.getCountValue());
    result.setPrevalence(concept.getPrevalence());
    result.setSynonymsStr(
        String.valueOf(concept.getConceptId())
            + '|'
            + Joiner.on("|").join(concept.getConceptSynonyms()));
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
    domainInfoDao.save(OBSERVATION_DOMAIN);
  }

  private void saveSurveyInfo() {
    surveyModuleDao.save(BASICS_SURVEY_MODULE);
    surveyModuleDao.save(LIFESTYLE_SURVEY_MODULE);
    surveyModuleDao.save(OVERALL_HEALTH_SURVEY_MODULE);
  }

  private DomainCount toDomainCount(DbDomainInfo domainInfo, boolean standardCount) {
    return toDomainCount(
        domainInfo,
        standardCount ? domainInfo.getStandardConceptCount() : domainInfo.getAllConceptCount());
  }

  private DomainCount toDomainCount(DbDomainInfo domainInfo, long conceptCount) {
    return new DomainCount()
        .name(domainInfo.getName())
        .conceptCount(conceptCount)
        .domain(domainInfo.getDomainEnum());
  }

  private void assertResults(
      ResponseEntity<ConceptListResponse> response, Concept... expectedConcepts) {
    assertThat(response.getBody().getItems()).isEqualTo(ImmutableList.copyOf(expectedConcepts));
  }

  private void assertCounts(
      ResponseEntity<DomainCountsListResponse> response, ImmutableList<DomainCount> domainCounts) {
    assertThat(response.getBody().getDomainCounts()).isEqualTo(domainCounts);
  }
}
