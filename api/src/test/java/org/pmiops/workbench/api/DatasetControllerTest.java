package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.pmiops.workbench.billing.GoogleApisConfig.END_USER_CLOUD_BILLING;
import static org.pmiops.workbench.billing.GoogleApisConfig.SERVICE_ACCOUNT_CLOUD_BILLING;

import com.google.api.services.cloudbilling.Cloudbilling;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.FieldList;
import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.FieldValue.Attribute;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.LegacySQLTypeName;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.StandardSQLTypeName;
import com.google.cloud.bigquery.TableResult;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import java.io.FileReader;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;
import org.hibernate.engine.jdbc.internal.BasicFormatterImpl;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.invocation.InvocationOnMock;
import org.pmiops.workbench.actionaudit.auditors.BillingProjectAuditor;
import org.pmiops.workbench.actionaudit.auditors.UserServiceAuditor;
import org.pmiops.workbench.actionaudit.auditors.WorkspaceAuditor;
import org.pmiops.workbench.billing.BillingProjectBufferService;
import org.pmiops.workbench.billing.FreeTierBillingService;
import org.pmiops.workbench.cdr.CdrVersionService;
import org.pmiops.workbench.cdr.ConceptBigQueryService;
import org.pmiops.workbench.cdrselector.WorkspaceResourcesServiceImpl;
import org.pmiops.workbench.cohortbuilder.CohortBuilderService;
import org.pmiops.workbench.cohortbuilder.CohortQueryBuilder;
import org.pmiops.workbench.cohortreview.CohortReviewServiceImpl;
import org.pmiops.workbench.cohortreview.mapper.CohortReviewMapperImpl;
import org.pmiops.workbench.cohortreview.mapper.ParticipantCohortAnnotationMapper;
import org.pmiops.workbench.cohortreview.mapper.ParticipantCohortStatusMapper;
import org.pmiops.workbench.cohorts.CohortCloningService;
import org.pmiops.workbench.cohorts.CohortFactoryImpl;
import org.pmiops.workbench.cohorts.CohortMapperImpl;
import org.pmiops.workbench.cohorts.CohortMaterializationService;
import org.pmiops.workbench.cohorts.CohortService;
import org.pmiops.workbench.compliance.ComplianceService;
import org.pmiops.workbench.concept.ConceptService;
import org.pmiops.workbench.conceptset.ConceptSetService;
import org.pmiops.workbench.conceptset.mapper.ConceptSetMapperImpl;
import org.pmiops.workbench.config.CdrBigQuerySchemaConfig;
import org.pmiops.workbench.config.CdrBigQuerySchemaConfigService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.dataset.DatasetConfig;
import org.pmiops.workbench.dataset.DatasetServiceImpl;
import org.pmiops.workbench.dataset.mapper.DatasetMapperImpl;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserRecentResourceService;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbBillingProjectBufferEntry;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.FailedPreconditionException;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspace;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceACL;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceAccessEntry;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceResponse;
import org.pmiops.workbench.google.CloudStorageService;
import org.pmiops.workbench.google.DirectoryService;
import org.pmiops.workbench.model.BillingStatus;
import org.pmiops.workbench.model.Cohort;
import org.pmiops.workbench.model.Concept;
import org.pmiops.workbench.model.ConceptSet;
import org.pmiops.workbench.model.CreateConceptSetRequest;
import org.pmiops.workbench.model.DataAccessLevel;
import org.pmiops.workbench.model.DatasetCodeResponse;
import org.pmiops.workbench.model.DatasetExportRequest;
import org.pmiops.workbench.model.DatasetExportRequest.GenomicsAnalysisToolEnum;
import org.pmiops.workbench.model.DatasetExportRequest.GenomicsDataTypeEnum;
import org.pmiops.workbench.model.DatasetPreviewValueList;
import org.pmiops.workbench.model.DatasetRequest;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.DomainValue;
import org.pmiops.workbench.model.DomainValuePair;
import org.pmiops.workbench.model.EmailVerificationStatus;
import org.pmiops.workbench.model.KernelTypeEnum;
import org.pmiops.workbench.model.PrePackagedConceptSetEnum;
import org.pmiops.workbench.model.ResearchPurpose;
import org.pmiops.workbench.model.SearchRequest;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.model.WorkspaceActiveStatus;
import org.pmiops.workbench.monitoring.LogsBasedMetricServiceFakeImpl;
import org.pmiops.workbench.notebooks.NotebooksService;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.test.FakeLongRandom;
import org.pmiops.workbench.test.SearchRequests;
import org.pmiops.workbench.test.TestBigQueryCdrSchemaConfig;
import org.pmiops.workbench.testconfig.UserServiceTestConfiguration;
import org.pmiops.workbench.utils.TestMockFactory;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.utils.mappers.FirecloudMapperImpl;
import org.pmiops.workbench.utils.mappers.UserMapperImpl;
import org.pmiops.workbench.utils.mappers.WorkspaceMapperImpl;
import org.pmiops.workbench.workspaces.WorkspaceServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// TODO(jaycarlton): many of the tests here are testing DatasetServiceImpl more than
//   DatasetControllerImpl, so move those tests and setup stuff into DatasetServiceTest
//   and mock out DatasetService here.
@RunWith(SpringRunner.class)
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class DatasetControllerTest {

  private static final String COHORT_ONE_NAME = "cohort";
  private static final String COHORT_TWO_NAME = "cohort two";
  private static final String CONCEPT_SET_ONE_NAME = "concept set";
  private static final String CONCEPT_SET_TWO_NAME = "concept set two";
  private static final String CONCEPT_SET_SURVEY_NAME = "concept survey set";
  private static final String WORKSPACE_NAME = "name";
  private static final String WORKSPACE_BUCKET_NAME = "fc://bucket-hash";
  private static final String USER_EMAIL = "bob@gmail.com";
  private static final String TEST_CDR_PROJECT_ID = "all-of-us-ehr-dev";
  private static final String TEST_CDR_DATA_SET_ID = "synthetic_cdr20180606";
  private static final String TEST_CDR_TABLE = TEST_CDR_PROJECT_ID + "." + TEST_CDR_DATA_SET_ID;
  private static final String NAMED_PARAMETER_NAME = "p1_1";
  private static final QueryParameterValue NAMED_PARAMETER_VALUE =
      QueryParameterValue.string("concept_id");
  private static final String NAMED_PARAMETER_ARRAY_NAME = "p2_1";
  private static final QueryParameterValue NAMED_PARAMETER_ARRAY_VALUE =
      QueryParameterValue.array(new Integer[] {2, 5}, StandardSQLTypeName.INT64);

  private Long COHORT_ONE_ID;
  private Long COHORT_TWO_ID;
  private Long CONCEPT_SET_ONE_ID;
  private Long CONCEPT_SET_TWO_ID;
  private Long CONCEPT_SET_SURVEY_ID;

  private static final Instant NOW = Instant.now();
  private static final FakeClock CLOCK = new FakeClock(NOW, ZoneId.systemDefault());
  private static final BasicFormatterImpl BASIC_FORMATTER = new BasicFormatterImpl();
  private static final String PREFIX = "00000000";
  private static final String FULL_PREFIX = String.format("dataset_%s_condition_", PREFIX);

  private static DbUser currentUser;
  private Workspace workspace;

  @Autowired private CdrVersionDao cdrVersionDao;
  @Autowired private CohortsController cohortsController;
  @Autowired private ConceptSetsController conceptSetsController;
  @Autowired private DatasetController datasetController;
  @Autowired private UserDao userDao;
  @Autowired private WorkspaceDao workspaceDao;
  @Autowired private WorkspacesController workspacesController;

  @MockBean private CdrBigQuerySchemaConfigService mockCdrBigQuerySchemaConfigService;
  @MockBean private BillingProjectBufferService mockBillingProjectBufferService;
  @MockBean private BigQueryService mockBigQueryService;
  @MockBean private CohortQueryBuilder mockCohortQueryBuilder;
  @MockBean private FireCloudService fireCloudService;
  @MockBean private NotebooksService mockNotebooksService;

  @Captor ArgumentCaptor<JSONObject> notebookContentsCaptor;

  @TestConfiguration
  @Import({
    CohortFactoryImpl.class,
    CohortMapperImpl.class,
    CohortReviewMapperImpl.class,
    CohortReviewServiceImpl.class,
    CohortService.class,
    CohortsController.class,
    CommonMappers.class,
    ConceptService.class,
    ConceptSetMapperImpl.class,
    ConceptSetService.class,
    ConceptSetsController.class,
    DatasetController.class,
    DatasetMapperImpl.class,
    DatasetServiceImpl.class,
    FirecloudMapperImpl.class,
    LogsBasedMetricServiceFakeImpl.class,
    TestBigQueryCdrSchemaConfig.class,
    UserMapperImpl.class,
    UserServiceTestConfiguration.class,
    WorkspaceMapperImpl.class,
    WorkspaceResourcesServiceImpl.class,
    WorkspaceServiceImpl.class,
    WorkspacesController.class,
  })
  @MockBean({
    BillingProjectAuditor.class,
    CdrBigQuerySchemaConfigService.class,
    CdrVersionService.class,
    CloudStorageService.class,
    CohortBuilderService.class,
    CohortCloningService.class,
    CohortMaterializationService.class,
    ComplianceService.class,
    ConceptBigQueryService.class,
    DirectoryService.class,
    FreeTierBillingService.class,
    ParticipantCohortAnnotationMapper.class,
    ParticipantCohortStatusMapper.class,
    UserRecentResourceService.class,
    UserServiceAuditor.class,
    WorkspaceAuditor.class,
  })
  static class Configuration {

    @Bean(END_USER_CLOUD_BILLING)
    Cloudbilling endUserCloudbilling() {
      return TestMockFactory.createMockedCloudbilling();
    }

    @Bean(SERVICE_ACCOUNT_CLOUD_BILLING)
    Cloudbilling serviceAccountCloudbilling() {
      return TestMockFactory.createMockedCloudbilling();
    }

    @Bean
    Clock clock() {
      return CLOCK;
    }

    @Bean
    Random random() {
      return new FakeLongRandom(123);
    }

    @Bean
    @Scope("prototype")
    DbUser user() {
      return currentUser;
    }

    @Bean
    WorkbenchConfig workbenchConfig() {
      WorkbenchConfig workbenchConfig = WorkbenchConfig.createEmptyConfig();
      workbenchConfig.featureFlags = new WorkbenchConfig.FeatureFlagsConfig();
      workbenchConfig.featureFlags.enableBillingLockout = true;
      workbenchConfig.billing = new WorkbenchConfig.BillingConfig();
      workbenchConfig.billing.accountId = "free-tier";
      return workbenchConfig;
    }

    @Bean
    @Qualifier(DatasetConfig.DATASET_PREFIX_CODE)
    String prefixCode() {
      return "00000000";
    }
  }

  @Before
  public void setUp() throws Exception {
    DbBillingProjectBufferEntry entry = new DbBillingProjectBufferEntry();
    entry.setFireCloudProjectName(UUID.randomUUID().toString());

    doReturn(entry).when(mockBillingProjectBufferService).assignBillingProject(any());
    TestMockFactory.stubCreateFcWorkspace(fireCloudService);

    Gson gson = new Gson();
    CdrBigQuerySchemaConfig cdrBigQuerySchemaConfig =
        gson.fromJson(new FileReader("config/cdm/cdm_5_2.json"), CdrBigQuerySchemaConfig.class);

    doReturn(cdrBigQuerySchemaConfig).when(mockCdrBigQuerySchemaConfigService).getConfig();

    DbUser user = new DbUser();
    user.setUsername(USER_EMAIL);
    user.setUserId(123L);
    user.setDisabled(false);
    user.setEmailVerificationStatusEnum(EmailVerificationStatus.SUBSCRIBED);
    user = userDao.save(user);
    currentUser = user;

    DbCdrVersion cdrVersion = new DbCdrVersion();
    cdrVersion.setName("1");
    // set the db name to be empty since test cases currently
    // run in the workbench schema only.
    cdrVersion.setCdrDbName("");
    cdrVersion.setMicroarrayBigqueryDataset("microarray");
    cdrVersion = cdrVersionDao.save(cdrVersion);

    workspace = new Workspace();
    workspace.setName(WORKSPACE_NAME);
    workspace.setDataAccessLevel(DataAccessLevel.PROTECTED);
    workspace.setResearchPurpose(new ResearchPurpose());
    workspace.setCdrVersionId(String.valueOf(cdrVersion.getCdrVersionId()));
    workspace.setBillingAccountName("billing-account");

    workspace = workspacesController.createWorkspace(workspace).getBody();
    stubGetWorkspace(workspace.getNamespace(), workspace.getName());
    stubGetWorkspaceAcl(workspace.getNamespace());

    SearchRequest searchRequest = SearchRequests.males();

    String cohortCriteria = new Gson().toJson(searchRequest);

    Cohort cohort = new Cohort().name(COHORT_ONE_NAME).criteria(cohortCriteria);
    cohort =
        cohortsController.createCohort(workspace.getNamespace(), WORKSPACE_NAME, cohort).getBody();
    COHORT_ONE_ID = cohort.getId();

    Cohort cohortTwo = new Cohort().name(COHORT_TWO_NAME).criteria(cohortCriteria);
    cohortTwo =
        cohortsController
            .createCohort(workspace.getNamespace(), WORKSPACE_NAME, cohortTwo)
            .getBody();
    COHORT_TWO_ID = cohortTwo.getId();

    List<Concept> conceptList = new ArrayList<>();

    conceptList.add(
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
            .conceptSynonyms(Collections.emptyList()));

    ConceptSet conceptSet =
        new ConceptSet()
            .id(CONCEPT_SET_ONE_ID)
            .name(CONCEPT_SET_ONE_NAME)
            .domain(Domain.CONDITION)
            .concepts(conceptList);

    CreateConceptSetRequest conceptSetRequest =
        new CreateConceptSetRequest()
            .conceptSet(conceptSet)
            .addedIds(conceptList.stream().map(Concept::getConceptId).collect(Collectors.toList()));

    conceptSet =
        conceptSetsController
            .createConceptSet(workspace.getNamespace(), WORKSPACE_NAME, conceptSetRequest)
            .getBody();
    CONCEPT_SET_ONE_ID = conceptSet.getId();

    conceptList = new ArrayList<>();

    conceptList.add(
        new Concept()
            .conceptId(456L)
            .conceptName("a concept of type survey")
            .standardConcept(true)
            .conceptCode("conceptA")
            .conceptClassId("classId")
            .vocabularyId("V1")
            .domainId("Observation")
            .countValue(123L)
            .prevalence(0.2F)
            .conceptSynonyms(new ArrayList<>()));

    ConceptSet conceptSurveySet =
        new ConceptSet()
            .id(CONCEPT_SET_SURVEY_ID)
            .name(CONCEPT_SET_SURVEY_NAME)
            .domain(Domain.OBSERVATION)
            .concepts(conceptList);

    CreateConceptSetRequest conceptSetRequest1 =
        new CreateConceptSetRequest()
            .conceptSet(conceptSurveySet)
            .addedIds(conceptList.stream().map(Concept::getConceptId).collect(Collectors.toList()));

    conceptSurveySet =
        conceptSetsController
            .createConceptSet(workspace.getNamespace(), WORKSPACE_NAME, conceptSetRequest1)
            .getBody();
    CONCEPT_SET_SURVEY_ID = conceptSurveySet.getId();

    ConceptSet conceptSetTwo =
        new ConceptSet()
            .id(CONCEPT_SET_TWO_ID)
            .name(CONCEPT_SET_TWO_NAME)
            .domain(Domain.DRUG)
            .concepts(conceptList);

    CreateConceptSetRequest conceptSetTwoRequest =
        new CreateConceptSetRequest()
            .conceptSet(conceptSetTwo)
            .addedIds(conceptList.stream().map(Concept::getConceptId).collect(Collectors.toList()));

    conceptSetTwo =
        conceptSetsController
            .createConceptSet(workspace.getNamespace(), WORKSPACE_NAME, conceptSetTwoRequest)
            .getBody();
    CONCEPT_SET_TWO_ID = conceptSetTwo.getId();

    when(mockCohortQueryBuilder.buildParticipantIdQuery(any()))
        .thenReturn(
            QueryJobConfiguration.newBuilder(
                    "SELECT * FROM person_id from `${projectId}.${datasetId}.person` person WHERE @"
                        + NAMED_PARAMETER_NAME
                        + " IN unnest(@"
                        + NAMED_PARAMETER_ARRAY_NAME
                        + ")")
                .addNamedParameter(NAMED_PARAMETER_NAME, NAMED_PARAMETER_VALUE)
                .addNamedParameter(NAMED_PARAMETER_ARRAY_NAME, NAMED_PARAMETER_ARRAY_VALUE)
                .build());
    // This is not great, but due to the interaction of mocks and bigquery, it is
    // exceptionally hard to fix it so that it calls the real filterBitQueryConfig
    // but _does not_ call the real methods in the rest of the bigQueryService.
    // I tried .thenCallRealMethod() which ended up giving a null pointer from the mock,
    // as opposed to calling through.
    when(mockBigQueryService.filterBigQueryConfig(any()))
        .thenAnswer(
            (InvocationOnMock invocation) -> {
              Object[] args = invocation.getArguments();
              QueryJobConfiguration queryJobConfiguration = (QueryJobConfiguration) args[0];

              String returnSql =
                  queryJobConfiguration.getQuery().replace("${projectId}", TEST_CDR_PROJECT_ID);
              returnSql = returnSql.replace("${datasetId}", TEST_CDR_DATA_SET_ID);
              return queryJobConfiguration.toBuilder().setQuery(returnSql).build();
            });
  }

  private DatasetRequest buildEmptyDatasetRequest() {
    return new DatasetRequest()
        .conceptSetIds(new ArrayList<>())
        .cohortIds(new ArrayList<>())
        .domainValuePairs(new ArrayList<>())
        .name("blah")
        .prePackagedConceptSet(PrePackagedConceptSetEnum.NONE);
  }

  private void stubGetWorkspace(String ns, String name) {
    FirecloudWorkspace fcWorkspace = new FirecloudWorkspace();
    fcWorkspace.setNamespace(ns);
    fcWorkspace.setName(name);
    fcWorkspace.setCreatedBy(DatasetControllerTest.USER_EMAIL);
    fcWorkspace.setBucketName(WORKSPACE_BUCKET_NAME);
    FirecloudWorkspaceResponse fcResponse = new FirecloudWorkspaceResponse();
    fcResponse.setWorkspace(fcWorkspace);
    fcResponse.setAccessLevel(WorkspaceAccessLevel.OWNER.toString());
    when(fireCloudService.getWorkspace(ns, name)).thenReturn(fcResponse);
  }

  private void stubGetWorkspaceAcl(String ns) {
    FirecloudWorkspaceACL workspaceAccessLevelResponse = new FirecloudWorkspaceACL();
    FirecloudWorkspaceAccessEntry accessLevelEntry =
        new FirecloudWorkspaceAccessEntry().accessLevel(WorkspaceAccessLevel.OWNER.toString());
    Map<String, FirecloudWorkspaceAccessEntry> userEmailToAccessEntry =
        ImmutableMap.of(DatasetControllerTest.USER_EMAIL, accessLevelEntry);
    workspaceAccessLevelResponse.setAcl(userEmailToAccessEntry);
    when(fireCloudService.getWorkspaceAclAsService(ns, DatasetControllerTest.WORKSPACE_NAME))
        .thenReturn(workspaceAccessLevelResponse);
  }

  private List<DomainValuePair> mockDomainValuePair() {
    List<DomainValuePair> domainValues = new ArrayList<>();
    domainValues.add(new DomainValuePair().domain(Domain.CONDITION).value("PERSON_ID"));
    return domainValues;
  }

  private List<DomainValuePair> mockDomainValuePairWithPerson() {
    List<DomainValuePair> domainValues = new ArrayList<>();
    domainValues.add(new DomainValuePair().domain(Domain.PERSON).value("PERSON_ID"));
    return domainValues;
  }

  private List<DomainValuePair> mockSurveyDomainValuePair() {
    List<DomainValuePair> domainValues = new ArrayList<>();
    DomainValuePair domainValuePair = new DomainValuePair();
    domainValuePair.setDomain(Domain.OBSERVATION);
    domainValuePair.setValue("PERSON_ID");
    domainValues.add(domainValuePair);
    return domainValues;
  }

  private void mockLinkingTableQuery(Collection<String> domainBaseTables) {
    final TableResult tableResultMock = mock(TableResult.class);

    final FieldList schema =
        FieldList.of(
            ImmutableList.of(
                Field.of("OMOP_SQL", LegacySQLTypeName.STRING),
                Field.of("JOIN_VALUE", LegacySQLTypeName.STRING)));

    doReturn(
            domainBaseTables.stream()
                .map(
                    domainBaseTable -> {
                      ArrayList<FieldValue> rows = new ArrayList<>();
                      rows.add(FieldValue.of(Attribute.PRIMITIVE, "PERSON_ID"));
                      rows.add(FieldValue.of(Attribute.PRIMITIVE, domainBaseTable));
                      return FieldValueList.of(rows, schema);
                    })
                .collect(ImmutableList.toImmutableList()))
        .when(tableResultMock)
        .getValues();

    doReturn(tableResultMock).when(mockBigQueryService).executeQuery(any());
  }

  @Test
  public void testAddFieldValuesFromBigQueryToPreviewListWorksWithNullValues() {
    DatasetPreviewValueList datasetPreviewValueList = new DatasetPreviewValueList();
    List<DatasetPreviewValueList> valuePreviewList = ImmutableList.of(datasetPreviewValueList);
    List<FieldValue> fieldValueListRows =
        ImmutableList.of(FieldValue.of(FieldValue.Attribute.PRIMITIVE, null));
    FieldValueList fieldValueList = FieldValueList.of(fieldValueListRows);
    datasetController.addFieldValuesFromBigQueryToPreviewList(valuePreviewList, fieldValueList);
    assertThat(valuePreviewList.get(0).getQueryValue().get(0))
        .isEqualTo(DatasetController.EMPTY_CELL_MARKER);
  }

  @Test(expected = BadRequestException.class)
  public void testGetQueryFailsWithNoCohort() {
    DatasetRequest dataset = buildEmptyDatasetRequest();
    dataset =
        dataset
            .addConceptSetIdsItem(CONCEPT_SET_ONE_ID)
            .domainValuePairs(ImmutableList.of(new DomainValuePair().domain(Domain.CONDITION)));

    datasetController.generateCode(
        workspace.getNamespace(), WORKSPACE_NAME, KernelTypeEnum.PYTHON.toString(), dataset);
  }

  @Test(expected = BadRequestException.class)
  public void testGetQueryFailsWithNoConceptSet() {
    DatasetRequest dataset = buildEmptyDatasetRequest();
    dataset =
        dataset
            .addCohortIdsItem(COHORT_ONE_ID)
            .domainValuePairs(ImmutableList.of(new DomainValuePair().domain(Domain.CONDITION)));

    datasetController.generateCode(
        workspace.getNamespace(), WORKSPACE_NAME, KernelTypeEnum.PYTHON.toString(), dataset);
  }

  @Rule public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void testGetQueryDropsQueriesWithNoValue() {
    final DatasetRequest dataset =
        buildEmptyDatasetRequest()
            .datasetId(1l)
            .addCohortIdsItem(COHORT_ONE_ID)
            .addConceptSetIdsItem(CONCEPT_SET_ONE_ID);

    expectedException.expect(BadRequestException.class);

    DatasetCodeResponse response =
        datasetController
            .generateCode(
                workspace.getNamespace(), WORKSPACE_NAME, KernelTypeEnum.PYTHON.toString(), dataset)
            .getBody();
  }

  @Test
  public void createDatasetMissingArguments() {
    DatasetRequest dataset = buildEmptyDatasetRequest().name(null);

    List<Long> cohortIds = new ArrayList<>();
    cohortIds.add(1L);

    List<Long> conceptIds = new ArrayList<>();
    conceptIds.add(1L);

    List<DomainValuePair> valuePairList = new ArrayList<>();
    DomainValuePair domainValue = new DomainValuePair();
    domainValue.setDomain(Domain.DRUG);
    domainValue.setValue("DRUGS_VALUE");

    valuePairList.add(domainValue);

    dataset.setDomainValuePairs(valuePairList);
    dataset.setConceptSetIds(conceptIds);
    dataset.setCohortIds(cohortIds);

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Missing name");

    datasetController.createDataset(workspace.getNamespace(), WORKSPACE_NAME, dataset);

    dataset.setName("dataset");
    dataset.setCohortIds(null);

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Missing cohort ids");

    datasetController.createDataset(workspace.getNamespace(), WORKSPACE_NAME, dataset);

    dataset.setCohortIds(cohortIds);
    dataset.setConceptSetIds(null);

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Missing concept set ids");

    datasetController.createDataset(workspace.getNamespace(), WORKSPACE_NAME, dataset);

    dataset.setConceptSetIds(conceptIds);
    dataset.setDomainValuePairs(null);

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Missing values");

    datasetController.createDataset(workspace.getNamespace(), WORKSPACE_NAME, dataset);
  }

  @Test
  public void exportToNewNotebook() {
    DatasetExportRequest request = setUpValidDatasetExportRequest();

    datasetController.exportToNotebook(workspace.getNamespace(), WORKSPACE_NAME, request).getBody();
    verify(mockNotebooksService, never()).getNotebookContents(any(), any());
    // I tried to have this verify against the actual expected contents of the json object, but
    // java equivalence didn't handle it well.
    verify(mockNotebooksService, times(1))
        .saveNotebook(
            eq(WORKSPACE_BUCKET_NAME), eq(request.getNotebookName()), any(JSONObject.class));
  }

  @Test(expected = ForbiddenException.class)
  public void exportToNotebook_requiresActiveBilling() {
    DbWorkspace dbWorkspace =
        workspaceDao.findByWorkspaceNamespaceAndFirecloudNameAndActiveStatus(
            workspace.getNamespace(),
            WORKSPACE_NAME,
            DbStorageEnums.workspaceActiveStatusToStorage(WorkspaceActiveStatus.ACTIVE));
    dbWorkspace.setBillingStatus(BillingStatus.INACTIVE);
    workspaceDao.save(dbWorkspace);

    DatasetExportRequest request = new DatasetExportRequest();
    datasetController.exportToNotebook(workspace.getNamespace(), WORKSPACE_NAME, request);
  }

  @Test
  public void exportToExistingNotebook() {
    DatasetRequest dataset = buildEmptyDatasetRequest();
    dataset = dataset.addCohortIdsItem(COHORT_ONE_ID);
    dataset = dataset.addConceptSetIdsItem(CONCEPT_SET_ONE_ID);
    List<DomainValuePair> domainValuePairs = mockDomainValuePair();
    dataset.setDomainValuePairs(domainValuePairs);

    ArrayList<String> tables = new ArrayList<>();
    tables.add("FROM `" + TEST_CDR_TABLE + ".condition_occurrence` c_occurrence");

    mockLinkingTableQuery(tables);

    String notebookName = "Hello World";

    when(mockNotebooksService.getNotebookContents(WORKSPACE_BUCKET_NAME, notebookName))
        .thenReturn(
            new JSONObject()
                .put("cells", new JSONArray())
                .put("metadata", new JSONObject())
                .put("nbformat", 4)
                .put("nbformat_minor", 2));

    DatasetExportRequest request =
        new DatasetExportRequest()
            .datasetRequest(dataset)
            .newNotebook(false)
            .notebookName(notebookName);

    datasetController.exportToNotebook(workspace.getNamespace(), WORKSPACE_NAME, request).getBody();
    verify(mockNotebooksService, times(1)).getNotebookContents(WORKSPACE_BUCKET_NAME, notebookName);
    // I tried to have this verify against the actual expected contents of the json object, but
    // java equivalence didn't handle it well.
    verify(mockNotebooksService, times(1))
        .saveNotebook(eq(WORKSPACE_BUCKET_NAME), eq(notebookName), any(JSONObject.class));
  }

  @Test
  public void testGetValuesFromDomain() {
    when(mockBigQueryService.getTableFieldsFromDomain(Domain.CONDITION))
        .thenReturn(
            FieldList.of(
                Field.of("FIELD_ONE", LegacySQLTypeName.STRING),
                Field.of("FIELD_TWO", LegacySQLTypeName.STRING)));
    List<DomainValue> domainValues =
        datasetController
            .getValuesFromDomain(
                workspace.getNamespace(), WORKSPACE_NAME, Domain.CONDITION.toString())
            .getBody()
            .getItems();
    verify(mockBigQueryService).getTableFieldsFromDomain(Domain.CONDITION);

    assertThat(domainValues)
        .containsExactly(
            new DomainValue().value("field_one"), new DomainValue().value("field_two"));
  }

  @Test
  public void exportToNotebook_microarrayCodegen_cdrCheck() {
    DbCdrVersion cdrVersion =
        cdrVersionDao.findByCdrVersionId(Long.parseLong(workspace.getCdrVersionId()));
    cdrVersion.setMicroarrayBigqueryDataset(null);
    cdrVersionDao.save(cdrVersion);

    DatasetExportRequest request =
        setUpValidDatasetExportRequest().genomicsDataType(GenomicsDataTypeEnum.MICROARRAY);

    FailedPreconditionException e =
        assertThrows(
            FailedPreconditionException.class,
            () ->
                datasetController.exportToNotebook(
                    workspace.getNamespace(), WORKSPACE_NAME, request));
    assertThat(e)
        .hasMessageThat()
        .contains("The workspace CDR version does not have microarray data");
  }

  @Test
  public void exportToNotebook_microarrayCodegen_kernelCheck() {
    DatasetExportRequest request =
        setUpValidDatasetExportRequest()
            .kernelType(KernelTypeEnum.R)
            .genomicsDataType(GenomicsDataTypeEnum.MICROARRAY);

    BadRequestException e =
        assertThrows(
            BadRequestException.class,
            () ->
                datasetController.exportToNotebook(
                    workspace.getNamespace(), WORKSPACE_NAME, request));
    assertThat(e).hasMessageThat().contains("Genomics code generation is only supported in Python");
  }

  @Test
  public void exportToNotebook_microarrayCodegen_noGenomicsTool() {
    DatasetExportRequest request =
        setUpValidDatasetExportRequest()
            .genomicsDataType(GenomicsDataTypeEnum.MICROARRAY)
            .genomicsAnalysisTool(GenomicsAnalysisToolEnum.NONE);

    datasetController.exportToNotebook(workspace.getNamespace(), WORKSPACE_NAME, request);

    verify(mockNotebooksService, times(1))
        .saveNotebook(
            eq(WORKSPACE_BUCKET_NAME),
            eq(request.getNotebookName()),
            notebookContentsCaptor.capture());

    List<String> codeCells = notebookContentsToStrings(notebookContentsCaptor.getValue());

    assertThat(codeCells.size()).isEqualTo(5);
    assertThat(codeCells.get(2)).contains("raw_array_cohort_extract.py");
    assertThat(codeCells.get(3)).contains("ArrayExtractCohort");
    assertThat(codeCells.get(4)).contains("gsutil cp");
  }

  @Test
  public void exportToNotebook_microarrayCodegen_plink() {
    DatasetExportRequest request =
        setUpValidDatasetExportRequest()
            .genomicsDataType(GenomicsDataTypeEnum.MICROARRAY)
            .genomicsAnalysisTool(GenomicsAnalysisToolEnum.PLINK);

    datasetController.exportToNotebook(workspace.getNamespace(), WORKSPACE_NAME, request);

    verify(mockNotebooksService, times(1))
        .saveNotebook(
            eq(WORKSPACE_BUCKET_NAME),
            eq(request.getNotebookName()),
            notebookContentsCaptor.capture());

    List<String> codeCells = notebookContentsToStrings(notebookContentsCaptor.getValue());

    assertThat(codeCells.size()).isEqualTo(7);
    assertThat(codeCells.get(3)).contains("ArrayExtractCohort");
    assertThat(codeCells.get(5)).contains("cohort_phenotypes.to_csv");
    assertThat(codeCells.get(5)).contains(".phe");
    assertThat(codeCells.get(6)).contains("plink");
  }

  List<String> notebookContentsToStrings(JSONObject notebookContents) {
    List<String> codeCellStrings = new ArrayList<>();

    JSONArray cells = notebookContents.getJSONArray("cells");
    for (int i = 0; i < cells.length(); i++) {
      String cellString = "";
      JSONArray innerCells = cells.getJSONObject(i).getJSONArray("source");

      for (int j = 0; j < innerCells.length(); j++) {
        cellString += innerCells.getString(j);
      }

      codeCellStrings.add(cellString);
    }

    return codeCellStrings;
  }

  @Test
  public void exportToNotebook_microarrayCodegen_hail() {
    DatasetExportRequest request =
        setUpValidDatasetExportRequest()
            .genomicsDataType(GenomicsDataTypeEnum.MICROARRAY)
            .genomicsAnalysisTool(GenomicsAnalysisToolEnum.HAIL);

    datasetController.exportToNotebook(workspace.getNamespace(), WORKSPACE_NAME, request);

    verify(mockNotebooksService, times(1))
        .saveNotebook(
            eq(WORKSPACE_BUCKET_NAME),
            eq(request.getNotebookName()),
            notebookContentsCaptor.capture());

    List<String> codeCells = notebookContentsToStrings(notebookContentsCaptor.getValue());

    assertThat(codeCells.size()).isEqualTo(7);
    assertThat(codeCells.get(3)).contains("ArrayExtractCohort");
    assertThat(codeCells.get(5)).contains("cohort_phenotypes.to_csv");
    assertThat(codeCells.get(5)).contains(".tsv");
    assertThat(codeCells.get(6)).contains("import hail as hl");
  }

  @Test
  public void exportToNotebook_microarrayCodegen_noneGenomicsDataType() {
    DatasetExportRequest request =
        setUpValidDatasetExportRequest()
            .genomicsDataType(GenomicsDataTypeEnum.NONE)
            .genomicsAnalysisTool(GenomicsAnalysisToolEnum.HAIL);

    datasetController.exportToNotebook(workspace.getNamespace(), WORKSPACE_NAME, request);

    verify(mockNotebooksService, times(1))
        .saveNotebook(
            eq(WORKSPACE_BUCKET_NAME),
            eq(request.getNotebookName()),
            notebookContentsCaptor.capture());

    List<String> codeCells = notebookContentsToStrings(notebookContentsCaptor.getValue());
    assertThat(codeCells.size()).isEqualTo(1);
  }

  DatasetExportRequest setUpValidDatasetExportRequest() {
    DatasetRequest dataset = buildEmptyDatasetRequest().name("blah");
    dataset = dataset.addCohortIdsItem(COHORT_ONE_ID);
    dataset = dataset.addConceptSetIdsItem(CONCEPT_SET_ONE_ID);
    List<DomainValuePair> domainValuePairs = mockDomainValuePair();
    dataset.setDomainValuePairs(domainValuePairs);

    ArrayList<String> tables = new ArrayList<>();
    tables.add("FROM `" + TEST_CDR_TABLE + ".condition_occurrence` c_occurrence");

    mockLinkingTableQuery(tables);
    String notebookName = "Hello World";

    return new DatasetExportRequest()
        .datasetRequest(dataset)
        .newNotebook(true)
        .notebookName(notebookName)
        .kernelType(KernelTypeEnum.PYTHON);
  }
}
