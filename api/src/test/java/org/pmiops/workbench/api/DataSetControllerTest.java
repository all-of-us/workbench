package org.pmiops.workbench.api;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.gson.Gson;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.pmiops.workbench.cdr.CdrVersionService;
import org.pmiops.workbench.cdr.ConceptBigQueryService;
import org.pmiops.workbench.cdr.dao.ConceptDao;
import org.pmiops.workbench.cohortbuilder.ParticipantCounter;
import org.pmiops.workbench.cohorts.CohortFactory;
import org.pmiops.workbench.cohorts.CohortFactoryImpl;
import org.pmiops.workbench.cohorts.CohortMaterializationService;
import org.pmiops.workbench.compliance.ComplianceService;
import org.pmiops.workbench.config.CdrBigQuerySchemaConfig;
import org.pmiops.workbench.config.CdrBigQuerySchemaConfigService;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.CohortCloningService;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.CohortReviewDao;
import org.pmiops.workbench.db.dao.ConceptSetDao;
import org.pmiops.workbench.db.dao.ConceptSetService;
import org.pmiops.workbench.db.dao.DataSetService;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserRecentResourceService;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.dao.WorkspaceService;
import org.pmiops.workbench.db.dao.WorkspaceServiceImpl;
import org.pmiops.workbench.db.model.CdrVersion;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.google.CloudStorageService;
import org.pmiops.workbench.model.Cohort;
import org.pmiops.workbench.model.Concept;
import org.pmiops.workbench.model.ConceptSet;
import org.pmiops.workbench.model.CreateConceptSetRequest;
import org.pmiops.workbench.model.DataSetRequest;
import org.pmiops.workbench.model.DataSetQueryList;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.DomainValuePair;
import org.pmiops.workbench.model.EmailVerificationStatus;
import org.pmiops.workbench.model.SearchRequest;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.model.DataAccessLevel;
import org.pmiops.workbench.model.ResearchPurpose;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.test.FakeLongRandom;
import org.pmiops.workbench.test.SearchRequests;
import org.pmiops.workbench.test.TestBigQueryCdrSchemaConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Provider;

import java.io.FileReader;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration.class)
@AutoConfigureTestDatabase(replace= AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class DataSetControllerTest {
  private static final String COHORT_ONE_NAME = "cohort";
  private static final String COHORT_TWO_NAME = "cohort two";
  private static final String CONCEPT_SET_ONE_NAME = "concept set";
  private static final String CONCEPT_SET_TWO_NAME = "concept set two";
  private static final String WORKSPACE_NAMESPACE = "ns";
  private static final String WORKSPACE_NAME = "name";
  private static final String USER_EMAIL = "bob@gmail.com";

  private Long COHORT_ONE_ID;
  private Long CONCEPT_SET_ONE_ID;
  private Long COHORT_TWO_ID;
  private Long CONCEPT_SET_TWO_ID;

  private static final Instant NOW = Instant.now();
  private static final FakeClock CLOCK = new FakeClock(NOW, ZoneId.systemDefault());

  String cohortCriteria;
  SearchRequest searchRequest;

  @Autowired
  BigQueryService bigQueryService;

  @Autowired
  CdrBigQuerySchemaConfigService cdrBigQuerySchemaConfigService;

  @Autowired
  CdrVersionDao cdrVersionDao;

  @Autowired
  CdrVersionService cdrVersionService;

  @Autowired
  CloudStorageService cloudStorageService;

  @Autowired
  CohortDao cohortDao;

  @Autowired
  CohortFactory cohortFactory;

  @Autowired
  CohortMaterializationService cohortMaterializationService;

  @Autowired
  CohortReviewDao cohortReviewDao;

  @Autowired
  ConceptBigQueryService conceptBigQueryService;

  @Autowired
  ConceptDao conceptDao;

  @Autowired
  ConceptSetDao conceptSetDao;

  @Autowired
  DataSetService dataSetService;

  @Autowired
  FireCloudService fireCloudService;

  @Autowired
  ParticipantCounter participantCounter;

  @Autowired
  TestBigQueryCdrSchemaConfig testBigQueryCdrSchemaConfig;

  @Autowired
  UserDao userDao;

  @Mock
  Provider<User> userProvider;

  @Autowired
  UserRecentResourceService userRecentResourceService;

  @Autowired
  UserService userService;

  @Autowired
  WorkspaceService workspaceService;

  @TestConfiguration
  @Import({CohortFactoryImpl.class, TestBigQueryCdrSchemaConfig.class,
      UserService.class, WorkspacesController.class, WorkspaceServiceImpl.class})
  @MockBean({BigQueryService.class, CdrBigQuerySchemaConfigService.class, CdrVersionService.class,
      CloudStorageService.class, CohortCloningService.class,
      CohortMaterializationService.class, ComplianceService.class,
      ConceptBigQueryService.class, ConceptSetService.class, DataSetService.class,
      FireCloudService.class,
      ParticipantCounter.class, UserRecentResourceService.class})
  static class Configuration {
    @Bean
    Clock clock() {
      return CLOCK;
    }

    @Bean
    Random random() {
      return new FakeLongRandom(123);
    }
  }

  private DataSetController dataSetController;

  @Before
  public void setUp() throws Exception {
    dataSetController = new DataSetController(bigQueryService, CLOCK,
        cohortDao, conceptDao, conceptSetDao, dataSetService, userProvider, workspaceService);
    WorkspacesController workspacesController =
        new WorkspacesController(workspaceService, cdrVersionDao, cohortDao, cohortFactory, conceptSetDao, userDao,
            userProvider, fireCloudService, cloudStorageService, CLOCK, userService,
            userRecentResourceService);
    CohortsController cohortsController = new CohortsController(workspaceService, cohortDao, cdrVersionDao, cohortFactory,
        cohortReviewDao, conceptSetDao, cohortMaterializationService, userProvider, CLOCK,
        cdrVersionService, userRecentResourceService);
    ConceptSetsController conceptSetsController = new ConceptSetsController(workspaceService, conceptSetDao,
        conceptDao, conceptBigQueryService, userRecentResourceService, userProvider, CLOCK);

    Gson gson = new Gson();
    CdrBigQuerySchemaConfig cdrBigQuerySchemaConfig =  gson.fromJson(new FileReader("config/cdm/cdm_5_2.json"),
        CdrBigQuerySchemaConfig.class);

    when(cdrBigQuerySchemaConfigService.getConfig()).thenReturn(cdrBigQuerySchemaConfig);

    User user = new User();
    user.setEmail(USER_EMAIL);
    user.setUserId(123L);
    user.setDisabled(false);
    user.setEmailVerificationStatusEnum(EmailVerificationStatus.SUBSCRIBED);
    user = userDao.save(user);
    when(userProvider.get()).thenReturn(user);

    CdrVersion cdrVersion = new CdrVersion();
    cdrVersion.setName("1");
    //set the db name to be empty since test cases currently
    //run in the workbench schema only.
    cdrVersion.setCdrDbName("");
    cdrVersion = cdrVersionDao.save(cdrVersion);

    Workspace workspace = new Workspace();
    workspace.setName(WORKSPACE_NAME);
    workspace.setNamespace(WORKSPACE_NAMESPACE);
    workspace.setDataAccessLevel(DataAccessLevel.PROTECTED);
    workspace.setResearchPurpose(new ResearchPurpose());
    workspace.setCdrVersionId(String.valueOf(cdrVersion.getCdrVersionId()));

    stubGetWorkspace(WORKSPACE_NAMESPACE, WORKSPACE_NAME, USER_EMAIL, WorkspaceAccessLevel.OWNER);
    workspacesController.createWorkspace(workspace);

    searchRequest = SearchRequests.males();
    cohortCriteria = new Gson().toJson(searchRequest);

    Cohort cohort = new Cohort()
        .name(COHORT_ONE_NAME)
        .criteria(cohortCriteria);
    cohort = cohortsController.createCohort(
        WORKSPACE_NAMESPACE, WORKSPACE_NAME, cohort).getBody();
    COHORT_ONE_ID = cohort.getId();

    Cohort cohortTwo = new Cohort()
        .name(COHORT_TWO_NAME)
        .criteria(cohortCriteria);
    cohortTwo = cohortsController.createCohort(
        WORKSPACE_NAMESPACE, WORKSPACE_NAME, cohortTwo).getBody();
    COHORT_TWO_ID = cohortTwo.getId();

    List<Concept> conceptList = new ArrayList<>();

    conceptList.add(new Concept()
        .conceptId(123L)
        .conceptName("a concept")
        .standardConcept(true)
        .conceptCode("conceptA")
        .conceptClassId("classId")
        .vocabularyId("V1")
        .domainId("Condition")
        .countValue(123L)
        .prevalence(0.2F)
        .conceptSynonyms(new ArrayList<String>()));

    ConceptSet conceptSet = new ConceptSet()
        .id(CONCEPT_SET_ONE_ID)
        .name(CONCEPT_SET_ONE_NAME)
        .domain(Domain.CONDITION)
        .concepts(conceptList);

    CreateConceptSetRequest conceptSetRequest = new CreateConceptSetRequest().conceptSet(conceptSet);

    conceptSet = conceptSetsController.createConceptSet(
        WORKSPACE_NAMESPACE, WORKSPACE_NAME, conceptSetRequest).getBody();
    CONCEPT_SET_ONE_ID = conceptSet.getId();

    ConceptSet conceptSetTwo = new ConceptSet()
        .id(CONCEPT_SET_TWO_ID)
        .name(CONCEPT_SET_TWO_NAME)
        .domain(Domain.DRUG)
        .concepts(conceptList);

    CreateConceptSetRequest conceptSetTwoRequest = new CreateConceptSetRequest().conceptSet(conceptSetTwo);

    conceptSetTwo = conceptSetsController.createConceptSet(
        WORKSPACE_NAMESPACE, WORKSPACE_NAME, conceptSetTwoRequest).getBody();
    CONCEPT_SET_TWO_ID = conceptSetTwo.getId();

    when(participantCounter.buildParticipantIdQuery(any())).thenReturn(
        QueryJobConfiguration.newBuilder("SELECT * FROM person_id from `${projectId}.${dataSetId}.person` person").build());
    when(bigQueryService.filterBigQueryConfig(any())).thenReturn(
        QueryJobConfiguration.newBuilder("SELECT * FROM person_id from `all-of-us-ehr-dev.synthetic_cdr20180606.person` person").build());
  }

  private DataSetRequest buildEmptyDataSet() {
    return new DataSetRequest()
        .conceptSetIds(new ArrayList<>())
        .cohortIds(new ArrayList<>())
        .values(new ArrayList<>());
  }

  private void stubGetWorkspace(String ns, String name, String creator,
                                WorkspaceAccessLevel access) throws Exception {
    org.pmiops.workbench.firecloud.model.Workspace fcWorkspace =
        new org.pmiops.workbench.firecloud.model.Workspace();
    fcWorkspace.setNamespace(ns);
    fcWorkspace.setName(name);
    fcWorkspace.setCreatedBy(creator);
    org.pmiops.workbench.firecloud.model.WorkspaceResponse fcResponse =
        new org.pmiops.workbench.firecloud.model.WorkspaceResponse();
    fcResponse.setWorkspace(fcWorkspace);
    fcResponse.setAccessLevel(access.toString());
    when(fireCloudService.getWorkspace(ns, name)).thenReturn(
        fcResponse
    );
  }



//  @Test(expected = BadRequestException.class)
//  public void testGetQueryFailsWithNoCohort() {
//    DataSetRequest dataSet = buildEmptyDataSet();
//    dataSet = dataSet.addConceptSetIdsItem(CONCEPT_SET_ONE_ID);
//
//    dataSetController.generateQuery(WORKSPACE_NAMESPACE, WORKSPACE_NAME, dataSet);
//  }
//
//  @Test(expected = BadRequestException.class)
//  public void testGetQueryFailsWithNoConceptSet() {
//    DataSetRequest dataSet = buildEmptyDataSet();
//    dataSet = dataSet.addCohortIdsItem(COHORT_ONE_ID);
//
//    dataSetController.generateQuery(WORKSPACE_NAMESPACE, WORKSPACE_NAME, dataSet);
//  }
//
//  @Test
//  public void testGetQueryDropsQueriesWithNoValue() {
//    DataSetRequest dataSet = buildEmptyDataSet();
//    dataSet = dataSet.addCohortIdsItem(COHORT_ONE_ID);
//    dataSet = dataSet.addConceptSetIdsItem(CONCEPT_SET_ONE_ID);
//
//    DataSetQueryList response = dataSetController.generateQuery(WORKSPACE_NAMESPACE, WORKSPACE_NAME, dataSet).getBody();
//    assertThat(response.getQueryList()).isEmpty();
//  }
//
//  private List<DomainValuePair> mockDomainValuePair() {
//    List<DomainValuePair> domainValues = new ArrayList<>();
//    DomainValuePair domainValuePair = new DomainValuePair();
//    domainValuePair.setDomain(Domain.CONDITION);
//    domainValuePair.setValue("PERSON_ID");
//    domainValues.add(domainValuePair);
//    return domainValues;
//  }
//
//  @Test
//  public void testGetQuery() {
//    DataSetRequest dataSet = buildEmptyDataSet();
//    dataSet = dataSet.addCohortIdsItem(COHORT_ONE_ID);
//    dataSet = dataSet.addConceptSetIdsItem(CONCEPT_SET_ONE_ID);
//    List<DomainValuePair> domainValues = mockDomainValuePair();
//    dataSet.setValues(domainValues);
//
//    List<String> selectStrings = new ArrayList<>();
//    selectStrings.add("PERSON_ID");
//    List<String> joinStrings = new ArrayList<>();
//    joinStrings.add("FROM `all-of-us-ehr-dev.synthetic_cdr20180606.condition_occurrence` c_occurrence");
//
//
//    dataSetController = spy(dataSetController);
//    dataSetService = spy(dataSetService);
//    doReturn(new ValuesLinkingPair(selectStrings, joinStrings)).when(dataSetService).getValueSelectsAndJoins(domainValues, Domain.CONDITION);
//
//    DataSetQueryList response = dataSetController.generateQuery(WORKSPACE_NAMESPACE, WORKSPACE_NAME, dataSet).getBody();
//    assertThat(response.getQueryList().size()).isEqualTo(1);
//    verify(dataSetService, times(1)).getValueSelectsAndJoins(domainValues, Domain.CONDITION);
//    assertThat(response.getQueryList().get(0).getQuery())
//        .isEqualTo("SELECT PERSON_ID FROM " +
//            "`all-of-us-ehr-dev.synthetic_cdr20180606.condition_occurrence` " +
//            "c_occurrence WHERE (condition_concept_id IN () OR condition_source_concept_id IN ()) " +
//            "AND (PERSON_ID IN (SELECT * FROM person_id from `all-of-us-ehr-dev.synthetic_cdr20180606.person` person))");
//  }
//
//  @Test
//  public void testGetQueryTwoDomains() {
//    DataSetRequest dataSet = buildEmptyDataSet();
//    dataSet = dataSet.addCohortIdsItem(COHORT_ONE_ID);
//    dataSet = dataSet.addConceptSetIdsItem(CONCEPT_SET_ONE_ID);
//    dataSet = dataSet.addConceptSetIdsItem(CONCEPT_SET_TWO_ID);
//    List<DomainValuePair> domainValues = new ArrayList<>();
//    domainValues.addAll(mockDomainValuePair());
//
//    DomainValuePair drugDomainValue = new DomainValuePair();
//    drugDomainValue.setDomain(Domain.DRUG);
//    domainValues.add(drugDomainValue);
//    dataSet.setValues(domainValues);
//
//    List<DomainValuePair> valueSet2 = new ArrayList<>();
//    valueSet2.add(drugDomainValue);
//
//    List<String> selectConditionStrings = new ArrayList<>();
//    selectConditionStrings.add("PERSON_ID");
//    List<String> joinConditionStrings = new ArrayList<>();
//    joinConditionStrings.add("FROM `all-of-us-ehr-dev.synthetic_cdr20180606.condition_occurrence` c_occurrence");
//
//    List<String> selectDrugStrings = new ArrayList<>();
//    selectDrugStrings.add("PERSON_ID");
//    List<String> joinDrugStrings = new ArrayList<>();
//    joinDrugStrings.add("FROM `all-of-us-ehr-dev.synthetic_cdr20180606.drug_exposure` d_exposure");
//
//    dataSetController = spy(dataSetController);
//    doReturn(new ValuesLinkingPair(selectConditionStrings, joinConditionStrings)).when(dataSetController).getValueSelectsAndJoins(mockDomainValuePair(), Domain.CONDITION);
//    doReturn(new ValuesLinkingPair(selectDrugStrings, joinDrugStrings)).when(dataSetController).getValueSelectsAndJoins(valueSet2, Domain.DRUG);
//
//    DataSetQueryList response = dataSetController.generateQuery(WORKSPACE_NAMESPACE, WORKSPACE_NAME, dataSet).getBody();
//    assertThat(response.getQueryList()).isNotEmpty();
//    verify(dataSetController, times(2)).getValueSelectsAndJoins(any(), any());
//    assertThat(response.getQueryList().size()).isEqualTo(2);
//  }
//
//  @Test
//  public void testGetQueryTwoCohorts() {
//    DataSetRequest dataSet = buildEmptyDataSet();
//    dataSet = dataSet.addCohortIdsItem(COHORT_ONE_ID);
//    dataSet = dataSet.addCohortIdsItem(COHORT_TWO_ID);
//    dataSet = dataSet.addConceptSetIdsItem(CONCEPT_SET_ONE_ID);
//    List<DomainValuePair> domainValuePairList = mockDomainValuePair();
//    dataSet.setValues(domainValuePairList);
//
//
//    List<String> selectStrings = new ArrayList<>();
//    selectStrings.add("PERSON_ID");
//    List<String> joinStrings = new ArrayList<>();
//    joinStrings.add("FROM `all-of-us-ehr-dev.synthetic_cdr20180606.condition_occurrence` c_occurrence");
//
//    dataSetController = spy(dataSetController);
//    doReturn(new ValuesLinkingPair(selectStrings, joinStrings)).when(dataSetController).getValueSelectsAndJoins(domainValuePairList, Domain.CONDITION);
//
//    DataSetQueryList response = dataSetController.generateQuery(WORKSPACE_NAMESPACE, WORKSPACE_NAME, dataSet).getBody();
//    assertThat(response.getQueryList().size()).isEqualTo(1);
//    assertThat(response.getQueryList().get(0).getQuery()).contains("OR PERSON_ID IN");
//  }

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void createDataSetMissingArguments() {
    DataSetRequest dataSet = buildEmptyDataSet();

    List<Long> cohortIds = new ArrayList<>();
    cohortIds.add(1l);

    List<Long> conceptIds = new ArrayList<>();
    conceptIds.add(1l);

    List<DomainValuePair> valuePairList = new ArrayList<>();
    DomainValuePair domainValue = new DomainValuePair();
    domainValue.setDomain(Domain.DRUG);
    domainValue.setValue("DRUGS_VALUE");

    valuePairList.add(domainValue);

    dataSet.setValues(valuePairList);
    dataSet.setConceptSetIds(conceptIds);
    dataSet.setCohortIds(cohortIds);

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Missing name");

    dataSetController.createDataSet(WORKSPACE_NAMESPACE, WORKSPACE_NAME, dataSet);

    dataSet.setName("dataSet");
    dataSet.setCohortIds(null);


    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Missing cohort ids");

    dataSetController.createDataSet(WORKSPACE_NAMESPACE, WORKSPACE_NAME, dataSet);

    dataSet.setCohortIds(cohortIds);
    dataSet.setConceptSetIds(null);

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Missing concept set ids");

    dataSetController.createDataSet(WORKSPACE_NAMESPACE, WORKSPACE_NAME, dataSet);


    dataSet.setConceptSetIds(conceptIds);
    dataSet.setValues(null);

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Missing values");

    dataSetController.createDataSet(WORKSPACE_NAMESPACE, WORKSPACE_NAME, dataSet);
  }
}
