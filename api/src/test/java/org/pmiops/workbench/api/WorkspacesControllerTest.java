package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.pmiops.workbench.FakeClockConfiguration.NOW_TIME;
import static org.pmiops.workbench.exfiltration.ExfiltrationConstants.EGRESS_OBJECT_LENGTHS_SERVICE_QUALIFIER;
import static org.pmiops.workbench.utils.TestMockFactory.DEFAULT_GOOGLE_PROJECT;

import com.google.api.services.cloudbilling.model.ProjectBillingInfo;
import com.google.cloud.PageImpl;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.LegacySQLTypeName;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.TableResult;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.access.AccessTierService;
import org.pmiops.workbench.actionaudit.ActionAuditQueryService;
import org.pmiops.workbench.actionaudit.auditors.AdminAuditor;
import org.pmiops.workbench.actionaudit.auditors.BillingProjectAuditor;
import org.pmiops.workbench.actionaudit.auditors.LeonardoRuntimeAuditor;
import org.pmiops.workbench.actionaudit.auditors.WorkspaceAuditor;
import org.pmiops.workbench.actionaudit.bucket.BucketAuditQueryService;
import org.pmiops.workbench.actionaudit.bucket.BucketAuditQueryServiceImpl;
import org.pmiops.workbench.billing.FreeTierBillingService;
import org.pmiops.workbench.cdr.CdrVersionContext;
import org.pmiops.workbench.cdr.CdrVersionService;
import org.pmiops.workbench.cdr.ConceptBigQueryService;
import org.pmiops.workbench.cloudtasks.TaskQueueService;
import org.pmiops.workbench.cohortbuilder.CohortBuilderService;
import org.pmiops.workbench.cohortbuilder.CohortQueryBuilder;
import org.pmiops.workbench.cohortbuilder.chart.ChartQueryBuilder;
import org.pmiops.workbench.cohortbuilder.chart.ChartService;
import org.pmiops.workbench.cohortbuilder.mapper.CohortBuilderMapper;
import org.pmiops.workbench.cohortreview.CohortAnnotationDefinitionServiceImpl;
import org.pmiops.workbench.cohortreview.CohortReviewServiceImpl;
import org.pmiops.workbench.cohortreview.ReviewQueryBuilder;
import org.pmiops.workbench.cohortreview.mapper.CohortAnnotationDefinitionMapperImpl;
import org.pmiops.workbench.cohortreview.mapper.CohortReviewMapperImpl;
import org.pmiops.workbench.cohortreview.mapper.ParticipantCohortAnnotationMapperImpl;
import org.pmiops.workbench.cohortreview.mapper.ParticipantCohortStatusMapperImpl;
import org.pmiops.workbench.cohorts.CohortCloningService;
import org.pmiops.workbench.cohorts.CohortFactoryImpl;
import org.pmiops.workbench.cohorts.CohortMapperImpl;
import org.pmiops.workbench.cohorts.CohortService;
import org.pmiops.workbench.conceptset.ConceptSetService;
import org.pmiops.workbench.conceptset.mapper.ConceptSetMapperImpl;
import org.pmiops.workbench.config.CdrBigQuerySchemaConfigService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.dataset.DataSetService;
import org.pmiops.workbench.dataset.DataSetServiceImpl;
import org.pmiops.workbench.dataset.mapper.DataSetMapperImpl;
import org.pmiops.workbench.db.dao.AccessTierDao;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.CohortReviewDao;
import org.pmiops.workbench.db.dao.ConceptSetDao;
import org.pmiops.workbench.db.dao.DataSetDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserRecentWorkspaceDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.dao.WorkspaceFreeTierUsageDao;
import org.pmiops.workbench.db.dao.WorkspaceOperationDao;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.db.model.DbCohortReview;
import org.pmiops.workbench.db.model.DbConceptSet;
import org.pmiops.workbench.db.model.DbConceptSetConceptId;
import org.pmiops.workbench.db.model.DbDataset;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.db.model.DbWorkspaceOperation;
import org.pmiops.workbench.db.model.DbWorkspaceOperation.DbWorkspaceOperationStatus;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.exceptions.FailedPreconditionException;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.exfiltration.EgressRemediationService;
import org.pmiops.workbench.exfiltration.ObjectNameLengthService;
import org.pmiops.workbench.exfiltration.ObjectNameLengthServiceImpl;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.FirecloudTransforms;
import org.pmiops.workbench.firecloud.model.FirecloudManagedGroupWithMembers;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceACL;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceACLUpdate;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceACLUpdateResponseList;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceDetails;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceResponse;
import org.pmiops.workbench.genomics.GenomicExtractionService;
import org.pmiops.workbench.google.CloudBillingClient;
import org.pmiops.workbench.google.CloudMonitoringService;
import org.pmiops.workbench.google.CloudStorageClient;
import org.pmiops.workbench.iam.IamService;
import org.pmiops.workbench.leonardo.LeonardoApiClient;
import org.pmiops.workbench.mail.MailService;
import org.pmiops.workbench.model.AnnotationType;
import org.pmiops.workbench.model.ArchivalStatus;
import org.pmiops.workbench.model.BillingStatus;
import org.pmiops.workbench.model.CloneWorkspaceRequest;
import org.pmiops.workbench.model.Cohort;
import org.pmiops.workbench.model.CohortAnnotationDefinition;
import org.pmiops.workbench.model.CohortAnnotationDefinitionListResponse;
import org.pmiops.workbench.model.CohortReview;
import org.pmiops.workbench.model.Concept;
import org.pmiops.workbench.model.ConceptSet;
import org.pmiops.workbench.model.ConceptSetConceptId;
import org.pmiops.workbench.model.CreateConceptSetRequest;
import org.pmiops.workbench.model.CreateReviewRequest;
import org.pmiops.workbench.model.CreateWorkspaceTaskRequest;
import org.pmiops.workbench.model.DataSet;
import org.pmiops.workbench.model.DataSetRequest;
import org.pmiops.workbench.model.DisseminateResearchEnum;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.DomainValuePair;
import org.pmiops.workbench.model.DuplicateWorkspaceTaskRequest;
import org.pmiops.workbench.model.PageFilterRequest;
import org.pmiops.workbench.model.ParticipantCohortAnnotation;
import org.pmiops.workbench.model.ParticipantCohortAnnotationListResponse;
import org.pmiops.workbench.model.PrePackagedConceptSetEnum;
import org.pmiops.workbench.model.RecentWorkspace;
import org.pmiops.workbench.model.RecentWorkspaceResponse;
import org.pmiops.workbench.model.ResearchOutcomeEnum;
import org.pmiops.workbench.model.ResearchPurpose;
import org.pmiops.workbench.model.ResourceType;
import org.pmiops.workbench.model.ShareWorkspaceRequest;
import org.pmiops.workbench.model.SpecificPopulationEnum;
import org.pmiops.workbench.model.UpdateConceptSetRequest;
import org.pmiops.workbench.model.UpdateWorkspaceRequest;
import org.pmiops.workbench.model.UserRole;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.model.WorkspaceActiveStatus;
import org.pmiops.workbench.model.WorkspaceBillingUsageResponse;
import org.pmiops.workbench.model.WorkspaceOperation;
import org.pmiops.workbench.model.WorkspaceOperationStatus;
import org.pmiops.workbench.model.WorkspaceResource;
import org.pmiops.workbench.model.WorkspaceResourceResponse;
import org.pmiops.workbench.model.WorkspaceResponseListResponse;
import org.pmiops.workbench.model.WorkspaceUserRolesResponse;
import org.pmiops.workbench.monitoring.LogsBasedMetricServiceFakeImpl;
import org.pmiops.workbench.monitoring.MonitoringService;
import org.pmiops.workbench.notebooks.NotebooksService;
import org.pmiops.workbench.test.CohortDefinitions;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.utils.TestMockFactory;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.utils.mappers.FirecloudMapperImpl;
import org.pmiops.workbench.utils.mappers.LeonardoMapperImpl;
import org.pmiops.workbench.utils.mappers.UserMapperImpl;
import org.pmiops.workbench.utils.mappers.WorkspaceMapperImpl;
import org.pmiops.workbench.workspaceadmin.WorkspaceAdminService;
import org.pmiops.workbench.workspaceadmin.WorkspaceAdminServiceImpl;
import org.pmiops.workbench.workspaces.WorkspaceAuthService;
import org.pmiops.workbench.workspaces.WorkspaceOperationMapperImpl;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.pmiops.workbench.workspaces.WorkspaceServiceImpl;
import org.pmiops.workbench.workspaces.resources.UserRecentResourceService;
import org.pmiops.workbench.workspaces.resources.WorkspaceResourceMapperImpl;
import org.pmiops.workbench.workspaces.resources.WorkspaceResourcesServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class WorkspacesControllerTest {
  private static final String LOGGED_IN_USER_EMAIL = "bob@gmail.com";
  private static final String CLONE_GOOGLE_PROJECT_ID = "clone-project-id";

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
          .domainId("Condition")
          .countValue(456L)
          .prevalence(0.3F)
          .conceptSynonyms(new ArrayList<>());

  private static final Concept CLIENT_CONCEPT_3 =
      new Concept()
          .conceptId(256L)
          .standardConcept(true)
          .conceptName("c concept")
          .conceptCode("conceptC")
          .conceptClassId("classId2")
          .vocabularyId("V3")
          .domainId("Measurement")
          .countValue(256L)
          .prevalence(0.4F)
          .conceptSynonyms(new ArrayList<>());

  @Autowired AccessTierDao accessTierDao;
  @Autowired BigQueryService bigQueryService;
  @Autowired CdrVersionDao cdrVersionDao;
  @Autowired CloudStorageClient cloudStorageClient;
  @Autowired CohortAnnotationDefinitionController cohortAnnotationDefinitionController;
  @Autowired CohortDao cohortDao;
  @Autowired CohortReviewController cohortReviewController;
  @Autowired CohortReviewDao cohortReviewDao;
  @Autowired CohortsController cohortsController;
  @Autowired ConceptBigQueryService conceptBigQueryService;
  @Autowired ConceptSetDao conceptSetDao;
  @Autowired ConceptSetService conceptSetService;
  @Autowired ConceptSetsController conceptSetsController;
  @Autowired DataSetController dataSetController;
  @Autowired DataSetDao dataSetDao;
  @Autowired DataSetService dataSetService;
  @Autowired FakeClock fakeClock;
  @Autowired FireCloudService fireCloudService;
  @Autowired UserDao userDao;
  @Autowired UserRecentResourceService userRecentResourceService;
  @Autowired UserRecentWorkspaceDao userRecentWorkspaceDao;
  @Autowired WorkspaceAdminService workspaceAdminService;
  @Autowired WorkspaceAuditor mockWorkspaceAuditor;
  @Autowired WorkspaceFreeTierUsageDao workspaceFreeTierUsageDao;
  @Autowired WorkspaceOperationDao workspaceOperationDao;
  @Autowired WorkspaceService workspaceService;
  @Autowired WorkspacesController workspacesController;
  @Autowired ObjectNameLengthService objectNameLengthService;

  @SpyBean @Autowired WorkspaceDao workspaceDao;

  @MockBean CohortBuilderService cohortBuilderService;

  @MockBean AccessTierService accessTierService;
  @MockBean CloudBillingClient mockCloudBillingClient;
  @MockBean FreeTierBillingService mockFreeTierBillingService;
  @MockBean IamService mockIamService;
  @MockBean BucketAuditQueryService bucketAuditQueryService;

  @MockBean
  @Qualifier(EGRESS_OBJECT_LENGTHS_SERVICE_QUALIFIER)
  EgressRemediationService egressRemediationService;

  private static DbUser currentUser;
  private static WorkbenchConfig workbenchConfig;

  private DbAccessTier registeredTier;
  private DbCdrVersion cdrVersion;
  private String cdrVersionId;
  private String archivedCdrVersionId;

  @TestConfiguration
  @Import({
    CdrVersionService.class,
    CohortAnnotationDefinitionController.class,
    CohortAnnotationDefinitionMapperImpl.class,
    CohortAnnotationDefinitionServiceImpl.class,
    CohortCloningService.class,
    CohortFactoryImpl.class,
    CohortMapperImpl.class,
    CohortReviewController.class,
    CohortReviewMapperImpl.class,
    CohortReviewServiceImpl.class,
    CohortService.class,
    CohortsController.class,
    CommonMappers.class,
    ConceptSetMapperImpl.class,
    ConceptSetService.class,
    ConceptSetsController.class,
    DataSetController.class,
    DataSetMapperImpl.class,
    DataSetServiceImpl.class,
    FakeClockConfiguration.class,
    FirecloudMapperImpl.class,
    LeonardoMapperImpl.class,
    LogsBasedMetricServiceFakeImpl.class,
    ParticipantCohortAnnotationMapperImpl.class,
    ParticipantCohortStatusMapperImpl.class,
    ReviewQueryBuilder.class,
    UserMapperImpl.class,
    WorkspaceAdminServiceImpl.class,
    WorkspaceAuthService.class,
    WorkspaceMapperImpl.class,
    WorkspaceOperationMapperImpl.class,
    WorkspaceResourceMapperImpl.class,
    WorkspaceResourcesServiceImpl.class,
    WorkspaceServiceImpl.class,
    WorkspacesController.class,
    ObjectNameLengthServiceImpl.class,
    BucketAuditQueryServiceImpl.class,
  })
  @MockBean({
    AccessTierService.class,
    ActionAuditQueryService.class,
    AdminAuditor.class,
    BigQueryService.class,
    BillingProjectAuditor.class,
    CdrBigQuerySchemaConfigService.class,
    CdrVersionService.class,
    CloudMonitoringService.class,
    CloudStorageClient.class,
    CohortBuilderMapper.class,
    CohortBuilderService.class,
    CohortQueryBuilder.class,
    ChartService.class,
    ChartQueryBuilder.class,
    ConceptBigQueryService.class,
    FireCloudService.class,
    GenomicExtractionService.class,
    LeonardoApiClient.class,
    LeonardoRuntimeAuditor.class,
    MailService.class,
    MonitoringService.class,
    NotebooksService.class,
    TaskQueueService.class,
    UserRecentResourceService.class,
    UserService.class,
    WorkspaceAuditor.class,
  })
  static class Configuration {
    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    DbUser user() {
      return currentUser;
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    WorkbenchConfig workbenchConfig() {
      return workbenchConfig;
    }
  }

  @BeforeEach
  public void setUp() throws Exception {
    workbenchConfig = WorkbenchConfig.createEmptyConfig();
    workbenchConfig.billing.accountId = "free-tier";
    workbenchConfig.billing.projectNamePrefix = "aou-local";

    currentUser = createUser(LOGGED_IN_USER_EMAIL);
    registeredTier = TestMockFactory.createRegisteredTierForTests(accessTierDao);

    when(cohortBuilderService.findAllDemographicsMap()).thenReturn(HashBasedTable.create());

    when(accessTierService.getAccessTierShortNamesForUser(currentUser))
        .thenReturn(Arrays.asList(AccessTierService.REGISTERED_TIER_SHORT_NAME));
    when(accessTierService.getRegisteredTierOrThrow()).thenReturn(registeredTier);

    cdrVersion = TestMockFactory.createDefaultCdrVersion(cdrVersionDao, accessTierDao, 1);
    cdrVersion.setName("1");
    // set the db name to be empty since test cases currently
    // run in the workbench schema only.
    cdrVersion.setCdrDbName("");
    cdrVersion.setAccessTier(registeredTier);
    cdrVersion = cdrVersionDao.save(cdrVersion);
    cdrVersionId = Long.toString(cdrVersion.getCdrVersionId());

    DbCdrVersion archivedCdrVersion =
        TestMockFactory.createDefaultCdrVersion(cdrVersionDao, accessTierDao, 2);
    archivedCdrVersion.setName("archived");
    archivedCdrVersion.setCdrDbName("");
    archivedCdrVersion.setArchivalStatusEnum(ArchivalStatus.ARCHIVED);
    archivedCdrVersion = cdrVersionDao.save(archivedCdrVersion);
    archivedCdrVersionId = Long.toString(archivedCdrVersion.getCdrVersionId());

    TestMockFactory.stubCreateBillingProject(fireCloudService);
    TestMockFactory.stubCreateFcWorkspace(fireCloudService);

    when(mockCloudBillingClient.pollUntilBillingAccountLinked(any(), any()))
        .thenReturn(new ProjectBillingInfo().setBillingEnabled(true));
  }

  private DbUser createUser(String email) {
    DbUser user = new DbUser();
    user.setUsername(email);
    user.setDisabled(false);
    return userDao.save(user);
  }

  private RawlsWorkspaceACL createWorkspaceACL() {
    return createWorkspaceACL(
        new JSONObject()
            .put(
                currentUser.getUsername(),
                new JSONObject()
                    .put("accessLevel", "OWNER")
                    .put("canCompute", true)
                    .put("canShare", true)));
  }

  private RawlsWorkspaceACL createWorkspaceACL(JSONObject acl) {
    return new Gson()
        .fromJson(new JSONObject().put("acl", acl).toString(), RawlsWorkspaceACL.class);
  }

  private void stubFcUpdateWorkspaceACL() {
    when(fireCloudService.updateWorkspaceACL(anyString(), anyString(), anyList()))
        .thenReturn(new RawlsWorkspaceACLUpdateResponseList());
  }

  private void stubFcGetWorkspaceACL() {
    stubFcGetWorkspaceACL(createWorkspaceACL());
  }

  private void stubFcGetWorkspaceACL(RawlsWorkspaceACL acl) {
    when(fireCloudService.getWorkspaceAclAsService(anyString(), anyString())).thenReturn(acl);
  }

  private void stubFcGetGroup() {
    FirecloudManagedGroupWithMembers testGrp = new FirecloudManagedGroupWithMembers();
    testGrp.setGroupEmail("test@firecloud.org");
    when(fireCloudService.getGroup(anyString())).thenReturn(testGrp);
  }

  private void stubGetWorkspace(
      String ns, String firecloudName, String creator, WorkspaceAccessLevel access) {
    stubGetWorkspace(TestMockFactory.createFirecloudWorkspace(ns, firecloudName, creator), access);
  }

  private void stubGetWorkspace(
      RawlsWorkspaceDetails fcWorkspace, WorkspaceAccessLevel access) {
    RawlsWorkspaceResponse fcResponse = new RawlsWorkspaceResponse();
    fcResponse.setWorkspace(fcWorkspace);
    fcResponse.setAccessLevel(access.toString());
    doReturn(fcResponse)
        .when(fireCloudService)
        .getWorkspace(fcWorkspace.getNamespace(), fcWorkspace.getName());
    List<RawlsWorkspaceResponse> workspaceResponses = fireCloudService.getWorkspaces();
    workspaceResponses.add(fcResponse);
    doReturn(workspaceResponses).when(fireCloudService).getWorkspaces();
  }

  /**
   * Mocks out the FireCloud cloneWorkspace call with a FC-model workspace based on the provided
   * details. The mocked workspace object is returned so the caller can make further modifications
   * if needed.
   */
  private RawlsWorkspaceDetails stubCloneWorkspace(
      String toNamespace, String toFirecloudName, String creator) {
    RawlsWorkspaceDetails fcResponse = new RawlsWorkspaceDetails();
    fcResponse.setNamespace(toNamespace);
    fcResponse.setName(toFirecloudName);
    fcResponse.setCreatedBy(creator);
    fcResponse.setGoogleProject(CLONE_GOOGLE_PROJECT_ID);

    when(fireCloudService.cloneWorkspace(
            anyString(), anyString(), eq(toNamespace), eq(toFirecloudName), anyString()))
        .thenReturn(fcResponse);
    when(fireCloudService.createBillingProjectName()).thenReturn(toNamespace);
    return fcResponse;
  }

  private void stubBigQueryCohortCalls() {
    // construct the first TableResult call
    Field count = Field.of("count", LegacySQLTypeName.INTEGER);
    Schema schema = Schema.of(count);
    FieldValue countValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "1");
    List<FieldValueList> tableRows = Arrays.asList(FieldValueList.of(Arrays.asList(countValue)));
    TableResult result =
        new TableResult(schema, tableRows.size(), new PageImpl<>(() -> null, null, tableRows));

    // construct the second TableResult call
    Field personId = Field.of("person_id", LegacySQLTypeName.STRING);
    Field birthDatetime = Field.of("birth_datetime", LegacySQLTypeName.DATETIME);
    Field genderConceptId = Field.of("gender_concept_id", LegacySQLTypeName.INTEGER);
    Field raceConceptId = Field.of("race_concept_id", LegacySQLTypeName.INTEGER);
    Field ethnicityConceptId = Field.of("ethnicity_concept_id", LegacySQLTypeName.INTEGER);
    Field sexAtBirthConceptId = Field.of("sex_at_birth_concept_id", LegacySQLTypeName.INTEGER);
    Field deceased = Field.of("deceased", LegacySQLTypeName.BOOLEAN);
    Schema schema2 =
        Schema.of(
            personId,
            birthDatetime,
            genderConceptId,
            raceConceptId,
            ethnicityConceptId,
            sexAtBirthConceptId,
            deceased);
    FieldValue personIdValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "1");
    FieldValue birthDatetimeValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "1");
    FieldValue genderConceptIdValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "2");
    FieldValue raceConceptIdValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "3");
    FieldValue ethnicityConceptIdValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "4");
    FieldValue sexAtBirthConceptIdValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "5");
    FieldValue deceasedValue = FieldValue.of(FieldValue.Attribute.PRIMITIVE, "false");
    List<FieldValueList> tableRows2 =
        Arrays.asList(
            FieldValueList.of(
                Arrays.asList(
                    personIdValue,
                    birthDatetimeValue,
                    genderConceptIdValue,
                    raceConceptIdValue,
                    ethnicityConceptIdValue,
                    sexAtBirthConceptIdValue,
                    deceasedValue)));
    TableResult result2 =
        new TableResult(schema2, tableRows2.size(), new PageImpl<>(() -> null, null, tableRows2));

    // return the TableResult calls in order of call
    when(bigQueryService.filterBigQueryConfigAndExecuteQuery(null)).thenReturn(result, result2);
  }

  private Workspace createWorkspace() {
    return TestMockFactory.createWorkspace("namespace", "name");
  }

  public Cohort createDefaultCohort(String name) {
    Cohort cohort = new Cohort();
    cohort.setName(name);
    cohort.setCriteria(new Gson().toJson(CohortDefinitions.males()));
    return cohort;
  }

  private List<RawlsWorkspaceACLUpdate> convertUserRolesToUpdateAclRequestList(
      List<UserRole> collaborators) {
    return collaborators.stream()
        .map(c -> FirecloudTransforms.buildAclUpdate(c.getEmail(), c.getRole()))
        .collect(Collectors.toList());
  }

  @Test
  public void getWorkspaces() {
    Workspace workspace = createWorkspace();
    workspace = workspacesController.createWorkspace(workspace).getBody();
    verify(mockWorkspaceAuditor).fireCreateAction(any(Workspace.class), anyLong());

    RawlsWorkspaceResponse fcResponse = new RawlsWorkspaceResponse();
    fcResponse.setWorkspace(
        TestMockFactory.createFirecloudWorkspace(
            workspace.getNamespace(), workspace.getName(), null));
    fcResponse.setAccessLevel(WorkspaceAccessLevel.OWNER.toString());
    doReturn(Collections.singletonList(fcResponse)).when(fireCloudService).getWorkspaces();

    assertThat(workspacesController.getWorkspaces().getBody().getItems().size()).isEqualTo(1);
  }

  @Test
  public void testCreateWorkspace() {
    Workspace workspace = createWorkspace();
    workspace = workspacesController.createWorkspace(workspace).getBody();
    verify(fireCloudService)
        .createWorkspace(
            workspace.getNamespace(), workspace.getName(), registeredTier.getAuthDomainName());
    stubGetWorkspace(
        workspace.getNamespace(),
        workspace.getName(),
        LOGGED_IN_USER_EMAIL,
        WorkspaceAccessLevel.OWNER);
    Workspace retrievedWorkspace =
        workspacesController
            .getWorkspace(workspace.getNamespace(), workspace.getId())
            .getBody()
            .getWorkspace();
    assertThat(retrievedWorkspace.getCreationTime()).isEqualTo(NOW_TIME);
    assertThat(retrievedWorkspace.getLastModifiedTime()).isEqualTo(NOW_TIME);
    assertThat(retrievedWorkspace.getCdrVersionId()).isEqualTo(cdrVersionId);
    assertThat(retrievedWorkspace.getAccessTierShortName())
        .isEqualTo(registeredTier.getShortName());
    assertThat(retrievedWorkspace.getCreator()).isEqualTo(LOGGED_IN_USER_EMAIL);
    assertThat(retrievedWorkspace.getId()).isEqualTo("name");
    assertThat(retrievedWorkspace.getName()).isEqualTo("name");
    assertThat(retrievedWorkspace.getResearchPurpose().getDiseaseFocusedResearch()).isTrue();
    assertThat(retrievedWorkspace.getResearchPurpose().getDiseaseOfFocus()).isEqualTo("cancer");
    assertThat(retrievedWorkspace.getResearchPurpose().getMethodsDevelopment()).isTrue();
    assertThat(retrievedWorkspace.getResearchPurpose().getControlSet()).isTrue();
    assertThat(retrievedWorkspace.getResearchPurpose().getAncestry()).isTrue();
    assertThat(retrievedWorkspace.getResearchPurpose().getCommercialPurpose()).isTrue();
    assertThat(retrievedWorkspace.getResearchPurpose().getSocialBehavioral()).isTrue();
    assertThat(retrievedWorkspace.getResearchPurpose().getPopulationHealth()).isTrue();
    assertThat(retrievedWorkspace.getResearchPurpose().getEducational()).isTrue();
    assertThat(retrievedWorkspace.getResearchPurpose().getDrugDevelopment()).isTrue();
    assertThat(retrievedWorkspace.getResearchPurpose().getAdditionalNotes())
        .isEqualTo("additional notes");
    assertThat(retrievedWorkspace.getResearchPurpose().getReasonForAllOfUs())
        .isEqualTo("reason for aou");
    assertThat(retrievedWorkspace.getResearchPurpose().getIntendedStudy())
        .isEqualTo("intended study");
    assertThat(retrievedWorkspace.getResearchPurpose().getAnticipatedFindings())
        .isEqualTo("anticipated findings");
    assertThat(retrievedWorkspace.getNamespace()).isEqualTo(workspace.getNamespace());
    assertThat(retrievedWorkspace.getResearchPurpose().getReviewRequested()).isTrue();
    assertThat(retrievedWorkspace.getResearchPurpose().getTimeRequested()).isEqualTo(NOW_TIME);
    assertThat(retrievedWorkspace.getGoogleProject()).isEqualTo(DEFAULT_GOOGLE_PROJECT);

    verify(fireCloudService)
        .updateBillingAccount(
            workspace.getNamespace(), TestMockFactory.WORKSPACE_BILLING_ACCOUNT_NAME);
    verify(fireCloudService)
        .createAllOfUsBillingProject(
            workspace.getNamespace(), registeredTier.getServicePerimeter());
    assertThat(retrievedWorkspace.getBillingAccountName())
        .isEqualTo(TestMockFactory.WORKSPACE_BILLING_ACCOUNT_NAME);
    verify(mockIamService, never()).revokeWorkflowRunnerRoleForUsers(anyString(), anyList());
  }

  @Test
  public void testCreateWorkspace_resetBillingAccountOnFailedSave() {
    doThrow(RuntimeException.class).when(workspaceDao).save(any(DbWorkspace.class));
    Workspace workspace = createWorkspace();
    TestMockFactory.stubCreateBillingProject(fireCloudService, workspace.getNamespace());

    try {
      workspacesController.createWorkspace(workspace).getBody();
    } catch (Exception e) {
      verify(fireCloudService)
          .updateBillingAccount(workspace.getNamespace(), workspace.getBillingAccountName());
      verify(fireCloudService)
          .updateBillingAccountAsService(
              workspace.getNamespace(), workbenchConfig.billing.freeTierBillingAccountName());
      return;
    }
    fail();
  }

  @Test
  public void testCreateWorkspace_doNotUpdateBillingForFreeTier() {
    Workspace workspace = createWorkspace();
    workspace.setBillingAccountName(workbenchConfig.billing.freeTierBillingAccountName());

    workspacesController.createWorkspace(workspace);

    verify(fireCloudService, never()).updateBillingAccountAsService(anyString(), anyString());
    verify(fireCloudService, never()).updateBillingAccount(anyString(), anyString());
  }

  @Test
  public void testCreateWorkspace_alreadyApproved() {
    Workspace workspace = createWorkspace();
    workspace.getResearchPurpose().setApproved(true);
    workspace = workspacesController.createWorkspace(workspace).getBody();

    Workspace workspace2 =
        workspacesController
            .getWorkspace(workspace.getNamespace(), workspace.getId())
            .getBody()
            .getWorkspace();
    assertThat(workspace2.getResearchPurpose().getApproved()).isNotEqualTo(true);
  }

  @Test
  public void testCreateWorkspace_createDeleteCycleSameName() {
    Workspace workspace = createWorkspace();

    Set<String> uniqueIds = new HashSet<>();
    for (int i = 0; i < 3; i++) {
      workspace = workspacesController.createWorkspace(workspace).getBody();
      uniqueIds.add(workspace.getId());

      workspacesController.deleteWorkspace(workspace.getNamespace(), workspace.getName());
    }
    assertThat(uniqueIds.size()).isEqualTo(1);
  }

  @Test
  public void testCreateWorkspace_archivedCdrVersionThrows() {
    Workspace workspace = createWorkspace();
    workspace.setCdrVersionId(archivedCdrVersionId);
    assertThrows(
        FailedPreconditionException.class,
        () -> {
          workspacesController.createWorkspace(workspace);
        });
  }

  @Test
  public void testCreateWorkspace_noResearchPurposeThrows() {
    Workspace workspace = createWorkspace();
    workspace.setResearchPurpose(null);
    assertThrows(
        BadRequestException.class,
        () -> {
          workspacesController.createWorkspace(workspace);
        });
  }

  // we do not actually use the accessTierShortName of the Workspace passed to
  // createWorkspace() - instead we derive it from the cdrVersionId

  @Test
  public void testCreateWorkspace_accessTierIgnored() {
    final Workspace requestedWorkspace = createWorkspace();
    assertThat(requestedWorkspace.getAccessTierShortName()).isNull();
    requestedWorkspace.setAccessTierShortName("some nonsense value!");

    final Workspace createdWorkspace =
        workspacesController.createWorkspace(requestedWorkspace).getBody();
    assertThat(createdWorkspace.getAccessTierShortName()).isEqualTo(registeredTier.getShortName());
  }

  @Test
  public void testCreateWorkspaceAsync() {
    Workspace workspace = createWorkspace();
    WorkspaceOperation operation = workspacesController.createWorkspaceAsync(workspace).getBody();
    assertThat(operation.getId()).isNotNull();
    assertThat(operation.getStatus()).isEqualTo(WorkspaceOperationStatus.QUEUED);
    assertThat(operation.getWorkspace()).isNull();
  }

  @Test
  public void testCreateWorkspaceAsync_archivedCdrVersionThrows() {
    Workspace workspace = createWorkspace();
    workspace.setCdrVersionId(archivedCdrVersionId);
    assertThrows(
        FailedPreconditionException.class,
        () -> {
          workspacesController.createWorkspaceAsync(workspace);
        });
  }

  @Test
  public void testCreateWorkspaceAsync_noResearchPurposeThrows() {
    Workspace workspace = createWorkspace();
    workspace.setResearchPurpose(null);
    assertThrows(
        BadRequestException.class,
        () -> {
          workspacesController.createWorkspaceAsync(workspace);
        });
  }

  @Test
  public void testDuplicateWorkspaceAsync() {
    Workspace workspace = createWorkspace();
    CloneWorkspaceRequest request =
        new CloneWorkspaceRequest().workspace(workspace).includeUserRoles(true);

    // mocks Terra returning workspace info
    stubGetWorkspace(
        workspace.getNamespace(),
        workspace.getId(),
        currentUser.getUsername(),
        WorkspaceAccessLevel.READER);

    WorkspaceOperation operation =
        workspacesController
            .duplicateWorkspaceAsync(workspace.getNamespace(), workspace.getId(), request)
            .getBody();
    assertThat(operation.getId()).isNotNull();
    assertThat(operation.getStatus()).isEqualTo(WorkspaceOperationStatus.QUEUED);
    assertThat(operation.getWorkspace()).isNull();
  }

  @Test
  public void testDuplicateWorkspaceAsync_archivedCdrVersionThrows() {
    Workspace workspace = createWorkspace();
    CloneWorkspaceRequest request =
        new CloneWorkspaceRequest().workspace(workspace).includeUserRoles(true);
    workspace.setCdrVersionId(archivedCdrVersionId);
    assertThrows(
        FailedPreconditionException.class,
        () -> {
          workspacesController.duplicateWorkspaceAsync("foo", "bar", request);
        });
  }

  @Test
  public void testDuplicateWorkspaceAsync_noResearchPurposeThrows() {
    Workspace workspace = createWorkspace();
    CloneWorkspaceRequest request =
        new CloneWorkspaceRequest().workspace(workspace).includeUserRoles(true);
    workspace.setResearchPurpose(null);
    assertThrows(
        BadRequestException.class,
        () -> {
          workspacesController.duplicateWorkspaceAsync("foo", "bar", request);
        });
  }

  @Test
  public void testGetWorkspaceOperation() {
    DbWorkspaceOperation dbOperation =
        workspaceOperationDao.save(
            new DbWorkspaceOperation()
                .setCreatorId(currentUser.getUserId())
                .setStatus(DbWorkspaceOperationStatus.SUCCESS));
    assertThat(dbOperation.getId()).isNotNull();
    assertThat(dbOperation.getStatus()).isEqualTo(DbWorkspaceOperationStatus.SUCCESS);
    assertThat(dbOperation.getWorkspaceId()).isNull();

    WorkspaceOperation operation =
        workspacesController.getWorkspaceOperation(dbOperation.getId()).getBody();
    assertThat(operation.getId()).isEqualTo(dbOperation.getId());
    assertThat(operation.getStatus()).isEqualTo(WorkspaceOperationStatus.SUCCESS);
    assertThat(operation.getWorkspace()).isNull();
  }

  @Test
  public void testGetWorkspaceOperation_withWorkspace() {
    Workspace workspace = createWorkspace();
    DbWorkspace dbWorkspace =
        workspaceDao.save(
            new DbWorkspace()
                .setWorkspaceNamespace(workspace.getNamespace())
                .setName(workspace.getName())
                .setFirecloudName(workspace.getId()));
    DbWorkspaceOperation dbOperation =
        workspaceOperationDao.save(
            new DbWorkspaceOperation()
                .setCreatorId(currentUser.getUserId())
                .setStatus(DbWorkspaceOperationStatus.SUCCESS)
                .setWorkspaceId(dbWorkspace.getWorkspaceId()));
    assertThat(dbOperation.getId()).isNotNull();
    assertThat(dbOperation.getStatus()).isEqualTo(DbWorkspaceOperationStatus.SUCCESS);
    assertThat(dbOperation.getWorkspaceId()).isEqualTo(dbWorkspace.getWorkspaceId());

    // mocks Terra returning workspace info
    stubGetWorkspace(
        workspace.getNamespace(),
        workspace.getId(),
        workspace.getCreator(),
        WorkspaceAccessLevel.READER);

    WorkspaceOperation operation =
        workspacesController.getWorkspaceOperation(dbOperation.getId()).getBody();
    assertThat(operation.getId()).isEqualTo(dbOperation.getId());
    assertThat(operation.getStatus()).isEqualTo(WorkspaceOperationStatus.SUCCESS);
    assertThat(operation.getWorkspace()).isNotNull();
    assertThat(operation.getWorkspace().getNamespace()).isEqualTo(workspace.getNamespace());
    assertThat(operation.getWorkspace().getName()).isEqualTo(workspace.getName());
    assertThat(operation.getWorkspace().getId()).isEqualTo(workspace.getId());
  }

  @Test
  public void testGetWorkspaceOperation_notFound() {
    assertThat(workspacesController.getWorkspaceOperation(-1L).getStatusCode())
        .isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  public void testProcessCreateWorkspaceTask_notFound() {
    Workspace workspace = createWorkspace();
    CreateWorkspaceTaskRequest request =
        new CreateWorkspaceTaskRequest().operationId(-1L).workspace(workspace);
    assertThrows(
        NotFoundException.class, () -> workspacesController.processCreateWorkspaceTask(request));
  }

  @Test
  public void testProcessDuplicateWorkspaceTask_notFound() {
    Workspace workspace = createWorkspace();
    DuplicateWorkspaceTaskRequest request =
        new DuplicateWorkspaceTaskRequest()
            .operationId(-1L)
            .fromWorkspaceNamespace("foo")
            .fromWorkspaceFirecloudName("bar")
            .workspace(workspace);
    assertThrows(
        NotFoundException.class, () -> workspacesController.processDuplicateWorkspaceTask(request));
  }

  @Test
  public void testCreateWorkspaceAsync_and_get_operation() {
    Workspace workspace = createWorkspace();
    WorkspaceOperation operation = workspacesController.createWorkspaceAsync(workspace).getBody();
    assertThat(operation.getId()).isNotNull();
    assertThat(operation.getStatus()).isEqualTo(WorkspaceOperationStatus.QUEUED);
    assertThat(operation.getWorkspace()).isNull();

    WorkspaceOperation operation2 =
        workspacesController.getWorkspaceOperation(operation.getId()).getBody();
    assertThat(operation2).isEqualTo(operation);
  }

  @Test
  public void testCreateWorkspaceAsync_and_process() {
    Workspace workspace = createWorkspace().name("a new name for this test");

    WorkspaceOperation operation = workspacesController.createWorkspaceAsync(workspace).getBody();
    WorkspaceOperation operation2 =
        workspacesController.getWorkspaceOperation(operation.getId()).getBody();
    assertThat(operation2).isEqualTo(operation);

    CreateWorkspaceTaskRequest request =
        new CreateWorkspaceTaskRequest().operationId(operation.getId()).workspace(workspace);
    workspacesController.processCreateWorkspaceTask(request);

    WorkspaceOperation operation3 =
        workspacesController.getWorkspaceOperation(operation.getId()).getBody();
    assertThat(operation3.getId()).isEqualTo(operation.getId());
    assertThat(operation3.getStatus()).isEqualTo(WorkspaceOperationStatus.SUCCESS);
    assertThat(operation3.getWorkspace()).isNotNull();
    assertThat(operation3.getWorkspace().getName()).isEqualTo(workspace.getName());
  }

  @Test
  public void testDuplicateWorkspaceAsync_and_get_operation() {
    Workspace workspace = createWorkspace();
    CloneWorkspaceRequest request =
        new CloneWorkspaceRequest().workspace(workspace).includeUserRoles(true);

    // mocks Terra returning workspace info
    stubGetWorkspace(
        workspace.getNamespace(),
        workspace.getId(),
        currentUser.getUsername(),
        WorkspaceAccessLevel.READER);

    WorkspaceOperation operation =
        workspacesController
            .duplicateWorkspaceAsync(workspace.getNamespace(), workspace.getId(), request)
            .getBody();

    WorkspaceOperation operation2 =
        workspacesController.getWorkspaceOperation(operation.getId()).getBody();
    assertThat(operation2).isEqualTo(operation);
  }

  @Test
  public void testDuplicateWorkspaceAsync_and_process() {
    String fromWsNs = "namespace of the source workspace";
    String fromFcName = "firecloud-names-have-no-spaces";

    // the source workspace needs to exist in the DB
    workspaceDao.save(
        new DbWorkspace()
            .setWorkspaceNamespace(fromWsNs)
            .setFirecloudName(fromFcName)
            .setCdrVersion(cdrVersion));

    // mocks Terra returning workspace info
    stubGetWorkspace(fromWsNs, fromFcName, currentUser.getUsername(), WorkspaceAccessLevel.READER);

    Workspace workspace =
        createWorkspace()
            .name("nospacesallowed")
            .id("nospacesallowed")
            .namespace("and finally a unique namespace");
    CloneWorkspaceRequest request =
        new CloneWorkspaceRequest().workspace(workspace).includeUserRoles(true);

    // mocks Terra returning workspace info
    stubGetWorkspace(
        workspace.getNamespace(),
        workspace.getId(),
        currentUser.getUsername(),
        WorkspaceAccessLevel.READER);

    WorkspaceOperation operation =
        workspacesController.duplicateWorkspaceAsync(fromWsNs, fromFcName, request).getBody();
    WorkspaceOperation operation2 =
        workspacesController.getWorkspaceOperation(operation.getId()).getBody();
    assertThat(operation2).isEqualTo(operation);

    // mocks Terra returning workspace duplication info
    stubCloneWorkspace(workspace.getNamespace(), workspace.getId(), LOGGED_IN_USER_EMAIL);

    DuplicateWorkspaceTaskRequest request2 =
        new DuplicateWorkspaceTaskRequest()
            .operationId(operation.getId())
            .fromWorkspaceNamespace(fromWsNs)
            .fromWorkspaceFirecloudName(fromFcName)
            .workspace(workspace);
    workspacesController.processDuplicateWorkspaceTask(request2);

    WorkspaceOperation operation3 =
        workspacesController.getWorkspaceOperation(operation.getId()).getBody();
    assertThat(operation3.getId()).isEqualTo(operation.getId());
    assertThat(operation3.getStatus()).isEqualTo(WorkspaceOperationStatus.SUCCESS);
    assertThat(operation3.getWorkspace()).isNotNull();
    assertThat(operation3.getWorkspace().getName()).isEqualTo(workspace.getName());
  }

  @Test
  public void testDeleteWorkspace() {
    Workspace workspace = createWorkspace();
    workspace = workspacesController.createWorkspace(workspace).getBody();

    workspacesController.deleteWorkspace(workspace.getNamespace(), workspace.getName());
    verify(mockWorkspaceAuditor).fireDeleteAction(any(DbWorkspace.class));
    try {
      workspacesController.getWorkspace(workspace.getNamespace(), workspace.getName());
      fail("NotFoundException expected");
    } catch (NotFoundException e) {
      // expected
    }
  }

  @Test
  public void testUpdateWorkspace() throws Exception {
    Workspace ws = createWorkspace();
    ws = workspacesController.createWorkspace(ws).getBody();
    verify(fireCloudService, times(1))
        .updateBillingAccount(ws.getNamespace(), ws.getBillingAccountName());

    ws.setName("updated-name");
    UpdateWorkspaceRequest request = new UpdateWorkspaceRequest();
    ws.setBillingAccountName("update-billing-account");
    request.setWorkspace(ws);
    Workspace updated =
        workspacesController.updateWorkspace(ws.getNamespace(), ws.getId(), request).getBody();
    ws.setEtag(updated.getEtag());
    assertThat(updated).isEqualTo(ws);

    ArgumentCaptor<String> projectCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<ProjectBillingInfo> billingCaptor =
        ArgumentCaptor.forClass(ProjectBillingInfo.class);
    verify(fireCloudService, times(1))
        .updateBillingAccount(ws.getNamespace(), "update-billing-account");

    ws.setName("updated-name2");
    updated =
        workspacesController.updateWorkspace(ws.getNamespace(), ws.getId(), request).getBody();
    ws.setEtag(updated.getEtag());
    assertThat(updated).isEqualTo(ws);
    Workspace got =
        workspacesController.getWorkspace(ws.getNamespace(), ws.getId()).getBody().getWorkspace();
    assertThat(got).isEqualTo(ws);
  }

  @Test
  public void testUpdateWorkspace_freeTierBilling_noCreditsRemaining() {
    Workspace workspace = createWorkspace();
    workspace = workspacesController.createWorkspace(workspace).getBody();

    doReturn(false)
        .when(mockFreeTierBillingService)
        .userHasRemainingFreeTierCredits(
            argThat(dbUser -> dbUser.getUserId() == currentUser.getUserId()));

    UpdateWorkspaceRequest request = new UpdateWorkspaceRequest();
    workspace.setBillingAccountName(workbenchConfig.billing.freeTierBillingAccountName());
    request.setWorkspace(workspace);
    Workspace response =
        workspacesController
            .updateWorkspace(workspace.getNamespace(), workspace.getId(), request)
            .getBody();

    assertThat(response.getBillingStatus()).isEqualTo(BillingStatus.INACTIVE);
  }

  @Test
  public void testUpdateWorkspace_freeTierBilling_hasCreditsRemaining() {
    Workspace workspace = createWorkspace();
    workspace = workspacesController.createWorkspace(workspace).getBody();

    DbWorkspace dbWorkspace =
        workspaceDao.findByWorkspaceNamespaceAndFirecloudNameAndActiveStatus(
            workspace.getNamespace(),
            workspace.getId(),
            DbStorageEnums.workspaceActiveStatusToStorage(WorkspaceActiveStatus.ACTIVE));
    dbWorkspace.setBillingStatus(BillingStatus.INACTIVE);
    doReturn(true)
        .when(mockFreeTierBillingService)
        .userHasRemainingFreeTierCredits(
            argThat(dbUser -> dbUser.getUserId() == currentUser.getUserId()));

    UpdateWorkspaceRequest request = new UpdateWorkspaceRequest();
    workspace.setBillingAccountName(workbenchConfig.billing.freeTierBillingAccountName());
    workspace.setEtag("\"1\"");
    request.setWorkspace(workspace);
    Workspace response =
        workspacesController
            .updateWorkspace(workspace.getNamespace(), workspace.getId(), request)
            .getBody();

    assertThat(response.getBillingStatus()).isEqualTo(BillingStatus.ACTIVE);
  }

  @Test
  public void testUpdateWorkspaceResearchPurpose() {
    Workspace ws = createWorkspace();
    ws = workspacesController.createWorkspace(ws).getBody();

    ResearchPurpose rp =
        new ResearchPurpose()
            .diseaseFocusedResearch(false)
            .diseaseOfFocus(null)
            .methodsDevelopment(false)
            .controlSet(false)
            .ancestry(false)
            .commercialPurpose(false)
            .populationHealth(false)
            .socialBehavioral(false)
            .drugDevelopment(false)
            .additionalNotes(null)
            .reviewRequested(false);
    ws.setResearchPurpose(rp);
    UpdateWorkspaceRequest request = new UpdateWorkspaceRequest();
    request.setWorkspace(ws);
    ResearchPurpose updatedRp =
        workspacesController
            .updateWorkspace(ws.getNamespace(), ws.getId(), request)
            .getBody()
            .getResearchPurpose();

    assertThat(updatedRp.getDiseaseFocusedResearch()).isFalse();
    assertThat(updatedRp.getDiseaseOfFocus()).isNull();
    assertThat(updatedRp.getMethodsDevelopment()).isFalse();
    assertThat(updatedRp.getControlSet()).isFalse();
    assertThat(updatedRp.getAncestry()).isFalse();
    assertThat(updatedRp.getCommercialPurpose()).isFalse();
    assertThat(updatedRp.getPopulationHealth()).isFalse();
    assertThat(updatedRp.getSocialBehavioral()).isFalse();
    assertThat(updatedRp.getDrugDevelopment()).isFalse();
    assertThat(updatedRp.getAdditionalNotes()).isNull();
    assertThat(updatedRp.getReviewRequested()).isFalse();
  }

  @Test
  public void testReaderUpdateWorkspaceThrows() {
    assertThrows(
        ForbiddenException.class,
        () -> {
          Workspace ws = createWorkspace();
          ws = workspacesController.createWorkspace(ws).getBody();
          ws.setName("updated-name");
          UpdateWorkspaceRequest request = new UpdateWorkspaceRequest();
          request.setWorkspace(ws);
          stubGetWorkspace(
              ws.getNamespace(), ws.getId(), ws.getCreator(), WorkspaceAccessLevel.READER);
          workspacesController.updateWorkspace(ws.getNamespace(), ws.getId(), request);
        });
  }

  @Test
  public void testWriterUpdateWorkspaceThrows() {
    assertThrows(
        ForbiddenException.class,
        () -> {
          Workspace ws = createWorkspace();
          ws = workspacesController.createWorkspace(ws).getBody();
          ws.setName("updated-name");
          UpdateWorkspaceRequest request = new UpdateWorkspaceRequest();
          request.setWorkspace(ws);
          stubGetWorkspace(
              ws.getNamespace(), ws.getId(), ws.getCreator(), WorkspaceAccessLevel.WRITER);
          workspacesController.updateWorkspace(ws.getNamespace(), ws.getId(), request);
        });
  }

  @Test
  public void testUpdateWorkspaceAccessTierThrows() {
    assertThrows(
        BadRequestException.class,
        () -> {
          Workspace ws = createWorkspace();
          ws = workspacesController.createWorkspace(ws).getBody();
          ws.setName("updated-name");
          ws.setAccessTierShortName("new tier");
          UpdateWorkspaceRequest request = new UpdateWorkspaceRequest();
          request.setWorkspace(ws);
          workspacesController.updateWorkspace(ws.getNamespace(), ws.getId(), request);
        });
  }

  @Test
  public void testUpdateWorkspaceStaleThrows() {
    assertThrows(
        ConflictException.class,
        () -> {
          Workspace ws = createWorkspace();
          ws = workspacesController.createWorkspace(ws).getBody();
          UpdateWorkspaceRequest request = new UpdateWorkspaceRequest();
          request.setWorkspace(
              new Workspace()
                  .name("updated-name")
                  .billingAccountName("billing-account")
                  .accessTierShortName(ws.getAccessTierShortName())
                  .etag(ws.getEtag()));
          workspacesController.updateWorkspace(ws.getNamespace(), ws.getId(), request);
          // Still using the initial now-stale etag; this should throw.
          request.setWorkspace(
              new Workspace()
                  .name("updated-name2")
                  .billingAccountName("billing-account")
                  .accessTierShortName(ws.getAccessTierShortName())
                  .etag(ws.getEtag()));
          workspacesController.updateWorkspace(ws.getNamespace(), ws.getId(), request);
        });
  }

  @Test
  public void testUpdateWorkspaceInvalidEtagsThrow() {
    Workspace ws = createWorkspace();
    ws = workspacesController.createWorkspace(ws).getBody();

    // TODO: Refactor to be a @Parameterized test case.
    List<String> cases = ImmutableList.of("", "hello, world", "\"\"", "\"\"1234\"\"", "\"-1\"");
    for (String etag : cases) {
      try {
        UpdateWorkspaceRequest request = new UpdateWorkspaceRequest();
        request.setWorkspace(new Workspace().name("updated-name").etag(etag));
        workspacesController.updateWorkspace(ws.getNamespace(), ws.getId(), request);
        fail(String.format("expected BadRequestException for etag: %s", etag));
      } catch (BadRequestException e) {
        // expected
      }
    }
  }

  @Test
  public void testCloneWorkspace() {
    stubFcGetGroup();
    stubFcGetWorkspaceACL();
    Workspace originalWorkspace = createWorkspace();
    originalWorkspace = workspacesController.createWorkspace(originalWorkspace).getBody();
    final String newBillingAccountName = "cloned-billing-account";

    // The original workspace is shared with one other user.
    final DbUser writerUser = createAndSaveUser("writerfriend@gmail.com", 124L);
    final ShareWorkspaceRequest shareWorkspaceRequest = new ShareWorkspaceRequest();
    shareWorkspaceRequest.setWorkspaceEtag(originalWorkspace.getEtag());

    addUserRoleToShareWorkspaceRequest(
        shareWorkspaceRequest, writerUser.getUsername(), WorkspaceAccessLevel.WRITER);

    stubFcUpdateWorkspaceACL();
    workspacesController.shareWorkspacePatch(
        originalWorkspace.getNamespace(), originalWorkspace.getName(), shareWorkspaceRequest);

    final ResearchPurpose modPurpose = new ResearchPurpose();
    modPurpose.setAncestry(true);
    modPurpose.setPopulationDetails(
        ImmutableList.of(
            SpecificPopulationEnum.DISABILITY_STATUS, SpecificPopulationEnum.GEOGRAPHY));
    modPurpose.setDisseminateResearchFindingList(
        ImmutableList.of(DisseminateResearchEnum.PRESENATATION_SCIENTIFIC_CONFERENCES));
    modPurpose.setResearchOutcomeList(
        ImmutableList.of(ResearchOutcomeEnum.DECREASE_ILLNESS_BURDEN));

    final Workspace modWorkspace = new Workspace();
    modWorkspace.setName("cloned");
    modWorkspace.setNamespace("cloned-ns");
    modWorkspace.setResearchPurpose(modPurpose);
    modWorkspace.setBillingAccountName(newBillingAccountName);

    final CloneWorkspaceRequest req = new CloneWorkspaceRequest();
    req.setWorkspace(modWorkspace);
    final RawlsWorkspaceDetails clonedFirecloudWorkspace =
        stubCloneWorkspace(
            modWorkspace.getNamespace(), modWorkspace.getName(), LOGGED_IN_USER_EMAIL);
    // Assign the same bucket name as the mock-factory's bucket name, so the clone vs. get equality
    // assertion below will pass.
    clonedFirecloudWorkspace.setBucketName(TestMockFactory.WORKSPACE_BUCKET_NAME);
    final Workspace clonedWorkspace =
        workspacesController
            .cloneWorkspace(originalWorkspace.getNamespace(), originalWorkspace.getId(), req)
            .getBody()
            .getWorkspace();
    verify(mockWorkspaceAuditor).fireDuplicateAction(anyLong(), anyLong(), any(Workspace.class));
    verify(fireCloudService)
        .updateBillingAccount(clonedWorkspace.getNamespace(), newBillingAccountName);

    // Stub out the FC service getWorkspace, since that's called by workspacesController.
    stubGetWorkspace(clonedFirecloudWorkspace, WorkspaceAccessLevel.WRITER);
    final Workspace retrievedWorkspace =
        workspacesController
            .getWorkspace(clonedWorkspace.getNamespace(), clonedWorkspace.getId())
            .getBody()
            .getWorkspace();

    // Hack so lists can be compared in isEqualTo regardless of order. Order doesn't matter
    // semantically, but I don't want to go down the rabbit hole of an out-of-class equality
    // method or custom assertion here (which would soon go out of date).
    sortPopulationDetails(clonedWorkspace.getResearchPurpose());
    sortPopulationDetails(retrievedWorkspace.getResearchPurpose());
    sortPopulationDetails(modPurpose);
    assertWithMessage("get and clone responses are inconsistent")
        .that(clonedWorkspace)
        .isEqualTo(retrievedWorkspace);

    assertThat(clonedWorkspace.getName()).isEqualTo(modWorkspace.getName());
    assertThat(clonedWorkspace.getNamespace()).isEqualTo(modWorkspace.getNamespace());
    assertThat(clonedWorkspace.getResearchPurpose()).isEqualTo(modPurpose);
    assertThat(clonedWorkspace.getBillingAccountName()).isEqualTo(newBillingAccountName);

    verify(fireCloudService)
        .createAllOfUsBillingProject(
            clonedWorkspace.getNamespace(), registeredTier.getServicePerimeter());
  }

  @Test
  public void testCloneWorkspace_resetBillingOnFailedSave() throws Exception {
    Workspace originalWorkspace = createWorkspace();
    originalWorkspace = workspacesController.createWorkspace(originalWorkspace).getBody();

    final Workspace modWorkspace = new Workspace();
    modWorkspace.setName("cloned");
    modWorkspace.setNamespace("cloned-ns");
    modWorkspace.setBillingAccountName("cloned-billing-account");
    modWorkspace.setResearchPurpose(new ResearchPurpose());

    final CloneWorkspaceRequest req = new CloneWorkspaceRequest();
    req.setWorkspace(modWorkspace);
    stubCloneWorkspace(modWorkspace.getNamespace(), modWorkspace.getName(), LOGGED_IN_USER_EMAIL);

    doThrow(RuntimeException.class).when(workspaceDao).save(any(DbWorkspace.class));

    try {
      workspacesController
          .cloneWorkspace(originalWorkspace.getNamespace(), originalWorkspace.getId(), req)
          .getBody()
          .getWorkspace();
    } catch (Exception e) {
      verify(fireCloudService)
          .updateBillingAccount(modWorkspace.getNamespace(), modWorkspace.getBillingAccountName());
      verify(fireCloudService)
          .updateBillingAccountAsService(
              modWorkspace.getNamespace(), workbenchConfig.billing.freeTierBillingAccountName());
      return;
    }
    fail();
  }

  @Test
  public void testCloneWorkspace_doNotUpdateBillingForFreeTier() {
    Workspace originalWorkspace = createWorkspace();
    originalWorkspace = workspacesController.createWorkspace(originalWorkspace).getBody();

    final Workspace modWorkspace = new Workspace();
    modWorkspace.setName("cloned");
    modWorkspace.setNamespace("cloned-ns");
    modWorkspace.setBillingAccountName(workbenchConfig.billing.freeTierBillingAccountName());
    modWorkspace.setResearchPurpose(new ResearchPurpose());

    final CloneWorkspaceRequest req = new CloneWorkspaceRequest();
    req.setWorkspace(modWorkspace);
    stubCloneWorkspace(modWorkspace.getNamespace(), modWorkspace.getName(), LOGGED_IN_USER_EMAIL);

    workspacesController.cloneWorkspace(
        originalWorkspace.getNamespace(), originalWorkspace.getId(), req);
    verify(fireCloudService, never())
        .updateBillingAccountAsService(eq(modWorkspace.getNamespace()), anyString());
    verify(fireCloudService, never())
        .updateBillingAccount(eq(modWorkspace.getNamespace()), anyString());
  }

  @Test
  public void testCloneWorkspace_accessTierMismatch() {
    assertThrows(
        BadRequestException.class,
        () -> {
          Workspace originalWorkspace = createWorkspace();
          originalWorkspace = workspacesController.createWorkspace(originalWorkspace).getBody();
          DbAccessTier altAccessTier = TestMockFactory.createControlledTierForTests(accessTierDao);
          altAccessTier = accessTierDao.save(altAccessTier);
          DbCdrVersion altCdrVersion = new DbCdrVersion();
          altCdrVersion.setCdrVersionId(2);
          altCdrVersion.setName("CDR 2");
          // set the db name to be empty since test cases currently
          // run in the workbench schema only.
          altCdrVersion.setCdrDbName("");
          altCdrVersion.setAccessTier(altAccessTier);
          altCdrVersion = cdrVersionDao.save(altCdrVersion);
          final Workspace modWorkspace = new Workspace();
          modWorkspace.setName("cloned");
          modWorkspace.setNamespace("cloned-ns");
          modWorkspace.setBillingAccountName(workbenchConfig.billing.freeTierBillingAccountName());
          modWorkspace.setResearchPurpose(new ResearchPurpose());
          modWorkspace.setCdrVersionId(String.valueOf(altCdrVersion.getCdrVersionId()));
          final CloneWorkspaceRequest req = new CloneWorkspaceRequest();
          req.setWorkspace(modWorkspace);
          stubCloneWorkspace(
              modWorkspace.getNamespace(), modWorkspace.getName(), LOGGED_IN_USER_EMAIL);
          workspacesController.cloneWorkspace(
              originalWorkspace.getNamespace(), originalWorkspace.getId(), req);
        });
  }

  // DbWorkspace stores several fields as Sets, but Workspace sees them as Lists of arbitrary order.
  // Population Details is the only field in this test where we store multiple entries.  Because we
  // want to use the basic equality test, we need to enforce a consistent ordering.
  private void sortPopulationDetails(ResearchPurpose researchPurpose) {
    final List<SpecificPopulationEnum> populationDetailsSorted =
        researchPurpose.getPopulationDetails().stream().sorted().collect(Collectors.toList());
    researchPurpose.setPopulationDetails(populationDetailsSorted);
  }

  private void addUserRoleToShareWorkspaceRequest(
      ShareWorkspaceRequest shareWorkspaceRequest,
      String email,
      WorkspaceAccessLevel workspaceAccessLevel) {
    final UserRole userRole = new UserRole();
    userRole.setEmail(email);
    userRole.setRole(workspaceAccessLevel);
    shareWorkspaceRequest.addItemsItem(userRole);
  }

  private DbUser createAndSaveUser(String email, long userId) {
    DbUser writerUser = new DbUser();
    writerUser.setUsername(email);
    writerUser.setUserId(userId);
    writerUser.setDisabled(false);

    writerUser = userDao.save(writerUser);
    return writerUser;
  }

  @Test
  public void testCloneWorkspaceWithCohortsAndConceptSets() {
    stubFcGetWorkspaceACL();
    Long participantId = 1L;
    CdrVersionContext.setCdrVersionNoCheckAuthDomain(cdrVersion);
    Workspace workspace = createWorkspace();
    workspace = workspacesController.createWorkspace(workspace).getBody();

    Cohort c1 = createDefaultCohort("c1");
    c1 = cohortsController.createCohort(workspace.getNamespace(), workspace.getId(), c1).getBody();
    Cohort c2 = createDefaultCohort("c2");
    c2 = cohortsController.createCohort(workspace.getNamespace(), workspace.getId(), c2).getBody();

    stubBigQueryCohortCalls();
    CreateReviewRequest reviewReq = new CreateReviewRequest().size(1).name("review1");
    CohortReview cr1 =
        cohortReviewController
            .createCohortReview(workspace.getNamespace(), workspace.getId(), c1.getId(), reviewReq)
            .getBody();
    CohortAnnotationDefinition cad1EnumResponse =
        cohortAnnotationDefinitionController
            .createCohortAnnotationDefinition(
                workspace.getNamespace(),
                workspace.getId(),
                c1.getId(),
                new CohortAnnotationDefinition()
                    .cohortId(c1.getId())
                    .annotationType(AnnotationType.ENUM)
                    .columnName("cad")
                    .enumValues(Arrays.asList("value")))
            .getBody();
    ParticipantCohortAnnotation pca1EnumResponse =
        cohortReviewController
            .createParticipantCohortAnnotation(
                workspace.getNamespace(),
                workspace.getId(),
                cr1.getCohortReviewId(),
                participantId,
                new ParticipantCohortAnnotation()
                    .cohortAnnotationDefinitionId(
                        cad1EnumResponse.getCohortAnnotationDefinitionId())
                    .annotationValueEnum("value")
                    .participantId(participantId)
                    .cohortReviewId(cr1.getCohortReviewId()))
            .getBody();
    CohortAnnotationDefinition cad1StringResponse =
        cohortAnnotationDefinitionController
            .createCohortAnnotationDefinition(
                workspace.getNamespace(),
                workspace.getId(),
                c1.getId(),
                new CohortAnnotationDefinition()
                    .cohortId(c1.getId())
                    .annotationType(AnnotationType.STRING)
                    .columnName("cad1"))
            .getBody();
    ParticipantCohortAnnotation pca1StringResponse =
        cohortReviewController
            .createParticipantCohortAnnotation(
                workspace.getNamespace(),
                workspace.getId(),
                cr1.getCohortReviewId(),
                participantId,
                new ParticipantCohortAnnotation()
                    .cohortAnnotationDefinitionId(
                        cad1StringResponse.getCohortAnnotationDefinitionId())
                    .annotationValueString("value1")
                    .participantId(participantId)
                    .cohortReviewId(cr1.getCohortReviewId()))
            .getBody();

    stubBigQueryCohortCalls();
    reviewReq.setSize(2);
    reviewReq.setName("review2");
    CohortReview cr2 =
        cohortReviewController
            .createCohortReview(workspace.getNamespace(), workspace.getId(), c2.getId(), reviewReq)
            .getBody();
    CohortAnnotationDefinition cad2EnumResponse =
        cohortAnnotationDefinitionController
            .createCohortAnnotationDefinition(
                workspace.getNamespace(),
                workspace.getId(),
                c2.getId(),
                new CohortAnnotationDefinition()
                    .cohortId(c2.getId())
                    .annotationType(AnnotationType.ENUM)
                    .columnName("cad")
                    .enumValues(Arrays.asList("value")))
            .getBody();
    ParticipantCohortAnnotation pca2EnumResponse =
        cohortReviewController
            .createParticipantCohortAnnotation(
                workspace.getNamespace(),
                workspace.getId(),
                cr2.getCohortReviewId(),
                participantId,
                new ParticipantCohortAnnotation()
                    .cohortAnnotationDefinitionId(
                        cad2EnumResponse.getCohortAnnotationDefinitionId())
                    .annotationValueEnum("value")
                    .participantId(participantId)
                    .cohortReviewId(cr2.getCohortReviewId()))
            .getBody();
    CohortAnnotationDefinition cad2BooleanResponse =
        cohortAnnotationDefinitionController
            .createCohortAnnotationDefinition(
                workspace.getNamespace(),
                workspace.getId(),
                c2.getId(),
                new CohortAnnotationDefinition()
                    .cohortId(c2.getId())
                    .annotationType(AnnotationType.BOOLEAN)
                    .columnName("cad1"))
            .getBody();
    ParticipantCohortAnnotation pca2BooleanResponse =
        cohortReviewController
            .createParticipantCohortAnnotation(
                workspace.getNamespace(),
                workspace.getId(),
                cr2.getCohortReviewId(),
                participantId,
                new ParticipantCohortAnnotation()
                    .cohortAnnotationDefinitionId(
                        cad2BooleanResponse.getCohortAnnotationDefinitionId())
                    .annotationValueBoolean(Boolean.TRUE)
                    .participantId(participantId)
                    .cohortReviewId(cr2.getCohortReviewId()))
            .getBody();

    DbConceptSetConceptId dbConceptSetConceptId1 =
        DbConceptSetConceptId.builder()
            .addConceptId(CLIENT_CONCEPT_1.getConceptId())
            .addStandard(true)
            .build();
    DbConceptSetConceptId dbConceptSetConceptId2 =
        DbConceptSetConceptId.builder()
            .addConceptId(CLIENT_CONCEPT_2.getConceptId())
            .addStandard(true)
            .build();
    when(conceptBigQueryService.getParticipantCountForConcepts(
            Domain.CONDITION, ImmutableSet.of(dbConceptSetConceptId1, dbConceptSetConceptId2)))
        .thenReturn(123);
    ConceptSetConceptId conceptSetConceptId1 = new ConceptSetConceptId();
    conceptSetConceptId1.setConceptId(CLIENT_CONCEPT_1.getConceptId());
    conceptSetConceptId1.setStandard(true);
    ConceptSet conceptSet1 =
        conceptSetsController
            .createConceptSet(
                workspace.getNamespace(),
                workspace.getId(),
                new CreateConceptSetRequest()
                    .conceptSet(
                        new ConceptSet().name("cs1").description("d1").domain(Domain.CONDITION))
                    .addAddedConceptSetConceptIdsItem(conceptSetConceptId1))
            .getBody();
    ConceptSetConceptId conceptSetConceptId2 = new ConceptSetConceptId();
    conceptSetConceptId2.setConceptId(CLIENT_CONCEPT_3.getConceptId());
    conceptSetConceptId2.setStandard(true);
    ConceptSet conceptSet2 =
        conceptSetsController
            .createConceptSet(
                workspace.getNamespace(),
                workspace.getId(),
                new CreateConceptSetRequest()
                    .conceptSet(
                        new ConceptSet().name("cs2").description("d2").domain(Domain.MEASUREMENT))
                    .addAddedConceptSetConceptIdsItem(conceptSetConceptId2))
            .getBody();
    ConceptSetConceptId conceptSetConceptId3 = new ConceptSetConceptId();
    conceptSetConceptId3.setConceptId(CLIENT_CONCEPT_1.getConceptId());
    conceptSetConceptId3.setStandard(true);
    ConceptSetConceptId conceptSetConceptId4 = new ConceptSetConceptId();
    conceptSetConceptId4.setConceptId(CLIENT_CONCEPT_2.getConceptId());
    conceptSetConceptId4.setStandard(true);
    conceptSet1 =
        conceptSetsController
            .updateConceptSetConcepts(
                workspace.getNamespace(),
                workspace.getId(),
                conceptSet1.getId(),
                new UpdateConceptSetRequest()
                    .etag(conceptSet1.getEtag())
                    .addedConceptSetConceptIds(
                        ImmutableList.of(conceptSetConceptId3, conceptSetConceptId4)))
            .getBody();

    CloneWorkspaceRequest req = new CloneWorkspaceRequest();
    Workspace modWorkspace = new Workspace();
    modWorkspace.setName("cloned");
    modWorkspace.setNamespace("cloned-ns");
    modWorkspace.setBillingAccountName("billing-account");

    final ResearchPurpose modPurpose = new ResearchPurpose();
    modPurpose.setAncestry(true);
    modWorkspace.setResearchPurpose(modPurpose);

    req.setWorkspace(modWorkspace);
    final RawlsWorkspaceDetails clonedWorkspace =
        stubCloneWorkspace(
            modWorkspace.getNamespace(), modWorkspace.getName(), LOGGED_IN_USER_EMAIL);

    stubGetWorkspace(clonedWorkspace, WorkspaceAccessLevel.WRITER);
    Workspace cloned =
        workspacesController
            .cloneWorkspace(workspace.getNamespace(), workspace.getId(), req)
            .getBody()
            .getWorkspace();

    List<Cohort> cohorts =
        cohortsController
            .getCohortsInWorkspace(cloned.getNamespace(), cloned.getId())
            .getBody()
            .getItems();
    List<CohortReview> cohortReviews =
        cohortReviewController
            .getCohortReviewsInWorkspace(cloned.getNamespace(), cloned.getId())
            .getBody()
            .getItems();
    Map<String, Cohort> cohortsByName = Maps.uniqueIndex(cohorts, c -> c.getName());
    Map<String, CohortReview> cohortReviewsByName =
        Maps.uniqueIndex(cohortReviews, c -> c.getCohortName());
    assertThat(cohortsByName.keySet().size()).isEqualTo(2);
    assertThat(cohortsByName.keySet()).containsExactly("c1", "c2");
    assertThat(cohorts.stream().map(c -> c.getId()).collect(Collectors.toList()))
        .containsNoneOf(c1.getId(), c2.getId());

    CohortReview gotCr1 =
        cohortReviewController
            .getParticipantCohortStatuses(
                cloned.getNamespace(),
                cloned.getId(),
                cohortReviewsByName.get("review1").getCohortReviewId(),
                new PageFilterRequest())
            .getBody()
            .getCohortReview();
    assertThat(gotCr1.getReviewSize()).isEqualTo(cr1.getReviewSize());
    assertThat(gotCr1.getParticipantCohortStatuses()).isEqualTo(cr1.getParticipantCohortStatuses());

    CohortAnnotationDefinitionListResponse clonedCad1List =
        cohortAnnotationDefinitionController
            .getCohortAnnotationDefinitions(
                cloned.getNamespace(), cloned.getId(), cohortsByName.get("c1").getId())
            .getBody();
    assertCohortAnnotationDefinitions(
        clonedCad1List,
        Arrays.asList(cad1EnumResponse, cad1StringResponse),
        cohortsByName.get("c1").getId());

    ParticipantCohortAnnotationListResponse clonedPca1List =
        cohortReviewController
            .getParticipantCohortAnnotations(
                cloned.getNamespace(), cloned.getId(), gotCr1.getCohortReviewId(), participantId)
            .getBody();

    assertParticipantCohortAnnotation(
        clonedPca1List,
        clonedCad1List,
        Arrays.asList(pca1EnumResponse, pca1StringResponse),
        gotCr1.getCohortReviewId(),
        participantId);

    CohortReview gotCr2 =
        cohortReviewController
            .getParticipantCohortStatuses(
                cloned.getNamespace(),
                cloned.getId(),
                cohortReviewsByName.get("review2").getCohortReviewId(),
                new PageFilterRequest())
            .getBody()
            .getCohortReview();
    assertThat(gotCr2.getReviewSize()).isEqualTo(cr2.getReviewSize());
    assertThat(gotCr2.getParticipantCohortStatuses()).isEqualTo(cr2.getParticipantCohortStatuses());

    CohortAnnotationDefinitionListResponse clonedCad2List =
        cohortAnnotationDefinitionController
            .getCohortAnnotationDefinitions(
                cloned.getNamespace(), cloned.getId(), cohortsByName.get("c2").getId())
            .getBody();
    assertCohortAnnotationDefinitions(
        clonedCad2List,
        Arrays.asList(cad2EnumResponse, cad2BooleanResponse),
        cohortsByName.get("c2").getId());

    ParticipantCohortAnnotationListResponse clonedPca2List =
        cohortReviewController
            .getParticipantCohortAnnotations(
                cloned.getNamespace(), cloned.getId(), gotCr2.getCohortReviewId(), participantId)
            .getBody();
    assertParticipantCohortAnnotation(
        clonedPca2List,
        clonedCad2List,
        Arrays.asList(pca2EnumResponse, pca2BooleanResponse),
        gotCr2.getCohortReviewId(),
        participantId);

    assertThat(ImmutableSet.of(gotCr1.getCohortReviewId(), gotCr2.getCohortReviewId()))
        .containsNoneOf(cr1.getCohortReviewId(), cr2.getCohortReviewId());

    List<ConceptSet> conceptSets =
        conceptSetsController
            .getConceptSetsInWorkspace(cloned.getNamespace(), cloned.getId())
            .getBody()
            .getItems();
    assertThat(conceptSets.size()).isEqualTo(2);
    assertConceptSetClone(conceptSets.get(0), conceptSet1, cloned, 123);
    assertConceptSetClone(conceptSets.get(1), conceptSet2, cloned, 0);

    workspacesController.deleteWorkspace(workspace.getNamespace(), workspace.getId());
    try {
      workspacesController.getWorkspace(workspace.getNamespace(), workspace.getName());
      fail("NotFoundException expected");
    } catch (NotFoundException e) {
      // expected
    }
  }

  @Test
  public void testCloneWorkspaceWithConceptSetNewCdrVersionNewConceptSetCount() {
    stubFcGetWorkspaceACL();
    CdrVersionContext.setCdrVersionNoCheckAuthDomain(cdrVersion);
    Workspace workspace = createWorkspace();
    workspace = workspacesController.createWorkspace(workspace).getBody();

    DbCdrVersion cdrVersion2 = new DbCdrVersion();
    cdrVersion2.setName("2");
    cdrVersion2.setCdrDbName("");
    cdrVersion2.setAccessTier(registeredTier);
    cdrVersion2 = cdrVersionDao.save(cdrVersion2);

    DbConceptSetConceptId dbConceptSetConceptId1 =
        DbConceptSetConceptId.builder()
            .addConceptId(CLIENT_CONCEPT_1.getConceptId())
            .addStandard(true)
            .build();
    DbConceptSetConceptId dbConceptSetConceptId2 =
        DbConceptSetConceptId.builder()
            .addConceptId(CLIENT_CONCEPT_2.getConceptId())
            .addStandard(true)
            .build();
    when(conceptBigQueryService.getParticipantCountForConcepts(
            Domain.CONDITION, ImmutableSet.of(dbConceptSetConceptId1, dbConceptSetConceptId2)))
        .thenReturn(123);

    ConceptSetConceptId conceptSetConceptId1 = new ConceptSetConceptId();
    conceptSetConceptId1.setConceptId(CLIENT_CONCEPT_1.getConceptId());
    conceptSetConceptId1.setStandard(true);
    ConceptSetConceptId conceptSetConceptId2 = new ConceptSetConceptId();
    conceptSetConceptId2.setConceptId(CLIENT_CONCEPT_2.getConceptId());
    conceptSetConceptId2.setStandard(true);
    ConceptSet conceptSet1 =
        conceptSetsController
            .createConceptSet(
                workspace.getNamespace(),
                workspace.getId(),
                new CreateConceptSetRequest()
                    .conceptSet(
                        new ConceptSet().name("cs1").description("d1").domain(Domain.CONDITION))
                    .addedConceptSetConceptIds(
                        ImmutableList.of(conceptSetConceptId1, conceptSetConceptId2)))
            .getBody();

    CloneWorkspaceRequest req = new CloneWorkspaceRequest();
    Workspace modWorkspace = new Workspace();
    modWorkspace.setName("cloned");
    modWorkspace.setNamespace("cloned-ns");
    modWorkspace.setBillingAccountName("billing-account");
    modWorkspace.setCdrVersionId(String.valueOf(cdrVersion2.getCdrVersionId()));

    ResearchPurpose modPurpose = new ResearchPurpose();
    modPurpose.setAncestry(true);
    modWorkspace.setResearchPurpose(modPurpose);
    req.setWorkspace(modWorkspace);

    RawlsWorkspaceDetails clonedWorkspace =
        stubCloneWorkspace(
            modWorkspace.getNamespace(), modWorkspace.getName(), LOGGED_IN_USER_EMAIL);

    when(conceptBigQueryService.getParticipantCountForConcepts(
            Domain.CONDITION, ImmutableSet.of(dbConceptSetConceptId1, dbConceptSetConceptId2)))
        .thenReturn(456);

    stubGetWorkspace(clonedWorkspace, WorkspaceAccessLevel.WRITER);
    Workspace cloned =
        workspacesController
            .cloneWorkspace(workspace.getNamespace(), workspace.getId(), req)
            .getBody()
            .getWorkspace();
    List<ConceptSet> conceptSets =
        conceptSetsController
            .getConceptSetsInWorkspace(cloned.getNamespace(), cloned.getId())
            .getBody()
            .getItems();
    assertThat(conceptSets.size()).isEqualTo(1);
    assertConceptSetClone(conceptSets.get(0), conceptSet1, cloned, 456);
  }

  @Test
  public void testCloneWorkspace_Dataset() {
    CdrVersionContext.setCdrVersionNoCheckAuthDomain(cdrVersion);
    Workspace workspace = createWorkspace();
    workspace = workspacesController.createWorkspace(workspace).getBody();

    DbWorkspace dbWorkspace =
        workspaceDao.findByWorkspaceNamespaceAndFirecloudNameAndActiveStatus(
            workspace.getNamespace(),
            workspace.getId(),
            DbStorageEnums.workspaceActiveStatusToStorage(WorkspaceActiveStatus.ACTIVE));

    DbCdrVersion cdrVersion2 = new DbCdrVersion();
    cdrVersion2.setName("2");
    cdrVersion2.setCdrDbName("");
    cdrVersion2.setAccessTier(registeredTier);
    cdrVersion2 = cdrVersionDao.save(cdrVersion2);

    final String expectedConceptSetName = "cs1";
    final String expectedConceptSetDescription = "d1";
    DbConceptSet originalConceptSet = new DbConceptSet();
    originalConceptSet.setName(expectedConceptSetName);

    originalConceptSet.setDescription(expectedConceptSetDescription);
    originalConceptSet.setDomainEnum(Domain.CONDITION);
    DbConceptSetConceptId dbConceptSetConceptId =
        DbConceptSetConceptId.builder()
            .addConceptId(CLIENT_CONCEPT_1.getConceptId())
            .addStandard(true)
            .build();
    originalConceptSet.setConceptSetConceptIds(Collections.singleton(dbConceptSetConceptId));
    originalConceptSet.setWorkspaceId(dbWorkspace.getWorkspaceId());
    originalConceptSet = conceptSetDao.save(originalConceptSet);

    final String expectedCohortName = "cohort name";
    final String expectedCohortDescription = "cohort description";
    DbCohort originalCohort = new DbCohort();
    originalCohort.setName(expectedCohortName);
    originalCohort.setDescription(expectedCohortDescription);
    originalCohort.setWorkspaceId(dbWorkspace.getWorkspaceId());
    originalCohort = cohortDao.save(originalCohort);

    final String expectedCohortReviewName = "cohort review";
    final String expectedCohortReviewDefinition = "cohort definition";
    DbCohortReview originalCohortReview = new DbCohortReview();
    originalCohortReview.setCohortName(expectedCohortReviewName);
    originalCohortReview.setCohortDefinition(expectedCohortReviewDefinition);
    originalCohortReview.setCohortId(originalCohort.getCohortId());
    originalCohortReview = cohortReviewDao.save(originalCohortReview);

    originalCohort.setCohortReviews(Collections.singleton(originalCohortReview));
    originalCohort = cohortDao.save(originalCohort);

    final String expectedDatasetName = "data set name";
    DbDataset originalDataSet = new DbDataset();
    originalDataSet.setName(expectedDatasetName);
    originalDataSet.setVersion(1);
    originalDataSet.setConceptSetIds(
        Collections.singletonList(originalConceptSet.getConceptSetId()));
    originalDataSet.setCohortIds(Collections.singletonList(originalCohort.getCohortId()));
    originalDataSet.setWorkspaceId(dbWorkspace.getWorkspaceId());
    originalDataSet.setPrePackagedConceptSetEnum(Arrays.asList(PrePackagedConceptSetEnum.NONE));
    dataSetDao.save(originalDataSet);

    CloneWorkspaceRequest req = new CloneWorkspaceRequest();
    Workspace modWorkspace = new Workspace();
    modWorkspace.setName("cloned");
    modWorkspace.setNamespace("cloned-ns");
    modWorkspace.setBillingAccountName("billing-account");
    modWorkspace.setCdrVersionId(String.valueOf(cdrVersion2.getCdrVersionId()));

    ResearchPurpose modPurpose = new ResearchPurpose();
    modPurpose.setAncestry(true);
    modWorkspace.setResearchPurpose(modPurpose);
    req.setWorkspace(modWorkspace);

    stubGetWorkspace(
        modWorkspace.getNamespace(),
        modWorkspace.getName(),
        LOGGED_IN_USER_EMAIL,
        WorkspaceAccessLevel.OWNER);
    stubFcGetWorkspaceACL();
    RawlsWorkspaceDetails clonedWorkspace =
        stubCloneWorkspace(
            modWorkspace.getNamespace(), modWorkspace.getName(), LOGGED_IN_USER_EMAIL);

    stubGetWorkspace(clonedWorkspace, WorkspaceAccessLevel.READER);
    Workspace cloned =
        workspacesController
            .cloneWorkspace(workspace.getNamespace(), workspace.getId(), req)
            .getBody()
            .getWorkspace();

    DbWorkspace clonedDbWorkspace =
        workspaceDao.findByWorkspaceNamespaceAndFirecloudNameAndActiveStatus(
            cloned.getNamespace(),
            cloned.getId(),
            DbStorageEnums.workspaceActiveStatusToStorage(WorkspaceActiveStatus.ACTIVE));

    List<DbDataset> dataSets = dataSetService.getDataSets(clonedDbWorkspace);
    assertThat(dataSets).hasSize(1);
    assertThat(dataSets.get(0).getName()).isEqualTo(expectedDatasetName);
    assertThat(dataSets.get(0).getDataSetId()).isNotEqualTo(originalDataSet.getDataSetId());

    List<DbConceptSet> conceptSets = dataSetService.getConceptSetsForDataset(dataSets.get(0));
    assertThat(conceptSets).hasSize(1);
    assertThat(conceptSets.get(0).getName()).isEqualTo(expectedConceptSetName);
    assertThat(conceptSets.get(0).getDescription()).isEqualTo(expectedConceptSetDescription);
    assertThat(conceptSets.get(0).getDomainEnum()).isEqualTo(Domain.CONDITION);
    assertThat(conceptSets.get(0).getConceptSetConceptIds())
        .isEqualTo(Collections.singleton(dbConceptSetConceptId));
    assertThat(conceptSets.get(0).getConceptSetId())
        .isNotEqualTo(originalConceptSet.getConceptSetId());

    List<DbCohort> cohorts = dataSetService.getCohortsForDataset(dataSets.get(0));
    assertThat(cohorts).hasSize(1);
    assertThat(cohorts.get(0).getName()).isEqualTo(expectedCohortName);
    assertThat(cohorts.get(0).getDescription()).isEqualTo(expectedCohortDescription);
    assertThat(cohorts.get(0).getCohortId()).isNotEqualTo(originalCohort.getCohortId());

    Set<DbCohortReview> cohortReviews =
        cohortReviewDao.findAllByCohortId(cohorts.get(0).getCohortId());
    assertThat(cohortReviews).hasSize(1);
    assertThat(cohortReviews.iterator().next().getCohortName()).isEqualTo(expectedCohortReviewName);
    assertThat(cohortReviews.iterator().next().getCohortDefinition())
        .isEqualTo(expectedCohortReviewDefinition);
    assertThat(cohortReviews.iterator().next().getCohortReviewId())
        .isNotEqualTo(originalCohortReview.getCohortReviewId());
  }

  private void assertConceptSetClone(
      ConceptSet clonedConceptSet,
      ConceptSet originalConceptSet,
      Workspace clonedWorkspace,
      long participantCount) {
    // Get the full concept set in order to retrieve the concepts.
    clonedConceptSet =
        conceptSetsController
            .getConceptSet(
                clonedWorkspace.getNamespace(), clonedWorkspace.getId(), clonedConceptSet.getId())
            .getBody();
    assertThat(clonedConceptSet.getName()).isEqualTo(originalConceptSet.getName());
    assertThat(clonedConceptSet.getDomain()).isEqualTo(originalConceptSet.getDomain());
    assertThat(clonedConceptSet.getCriteriums()).isEqualTo(originalConceptSet.getCriteriums());
    assertThat(clonedConceptSet.getCreator()).isEqualTo(clonedWorkspace.getCreator());
    assertThat(clonedConceptSet.getCreationTime()).isEqualTo(clonedWorkspace.getCreationTime());
    assertThat(clonedConceptSet.getLastModifiedTime())
        .isEqualTo(clonedWorkspace.getLastModifiedTime());
    assertThat(clonedConceptSet.getEtag()).isEqualTo(Etags.fromVersion(1));
    assertThat(clonedConceptSet.getParticipantCount()).isEqualTo(participantCount);
  }

  private void assertCohortAnnotationDefinitions(
      CohortAnnotationDefinitionListResponse responseList,
      List<CohortAnnotationDefinition> expectedCads,
      Long cohortId) {
    assertThat(responseList.getItems().size()).isEqualTo(expectedCads.size());
    int i = 0;
    for (CohortAnnotationDefinition clonedDefinition : responseList.getItems()) {
      CohortAnnotationDefinition expectedCad = expectedCads.get(i++);
      assertThat(clonedDefinition.getCohortAnnotationDefinitionId())
          .isNotEqualTo(expectedCad.getCohortAnnotationDefinitionId());
      assertThat(clonedDefinition.getCohortId()).isEqualTo(cohortId);
      assertThat(clonedDefinition.getColumnName()).isEqualTo(expectedCad.getColumnName());
      assertThat(clonedDefinition.getAnnotationType()).isEqualTo(expectedCad.getAnnotationType());
      assertThat(clonedDefinition.getEnumValues()).isEqualTo(expectedCad.getEnumValues());
    }
  }

  private void assertParticipantCohortAnnotation(
      ParticipantCohortAnnotationListResponse pcaResponseList,
      CohortAnnotationDefinitionListResponse cadResponseList,
      List<ParticipantCohortAnnotation> expectedPcas,
      Long cohortReviewId,
      Long participantId) {
    assertThat(pcaResponseList.getItems().size()).isEqualTo(expectedPcas.size());
    int i = 0;
    for (ParticipantCohortAnnotation clonedAnnotation : pcaResponseList.getItems()) {
      ParticipantCohortAnnotation expectedPca = expectedPcas.get(i);
      assertThat(clonedAnnotation.getAnnotationId()).isNotEqualTo(expectedPca.getAnnotationId());
      assertThat(clonedAnnotation.getAnnotationValueEnum())
          .isEqualTo(expectedPca.getAnnotationValueEnum());
      assertThat(clonedAnnotation.getCohortAnnotationDefinitionId())
          .isEqualTo(cadResponseList.getItems().get(i++).getCohortAnnotationDefinitionId());
      assertThat(clonedAnnotation.getCohortReviewId()).isEqualTo(cohortReviewId);
      assertThat(clonedAnnotation.getParticipantId()).isEqualTo(participantId);
    }
  }

  @Test
  public void testCloneWorkspaceDifferentOwner() {
    Workspace workspace = createWorkspace();
    workspace = workspacesController.createWorkspace(workspace).getBody();

    DbUser cloner = new DbUser();
    cloner.setUsername("cloner@gmail.com");
    cloner.setUserId(456L);
    cloner.setDisabled(false);
    currentUser = userDao.save(cloner);

    CloneWorkspaceRequest req = new CloneWorkspaceRequest();
    Workspace modWorkspace = new Workspace();
    modWorkspace.setName("cloned");
    modWorkspace.setNamespace("cloned-ns");
    modWorkspace.setBillingAccountName("billing-account");
    ResearchPurpose modPurpose = new ResearchPurpose();
    modPurpose.setAncestry(true);
    modWorkspace.setResearchPurpose(modPurpose);
    req.setWorkspace(modWorkspace);
    stubCloneWorkspace(modWorkspace.getNamespace(), modWorkspace.getName(), "cloner@gmail.com");

    Workspace workspace2 =
        workspacesController
            .cloneWorkspace(workspace.getNamespace(), workspace.getId(), req)
            .getBody()
            .getWorkspace();

    assertThat(workspace2.getCreator()).isEqualTo(cloner.getUsername());
  }

  @Test
  public void testCloneWorkspaceCdrVersion() {
    DbCdrVersion cdrVersion2 = new DbCdrVersion();
    cdrVersion2.setName("2");
    cdrVersion2.setCdrDbName("");
    cdrVersion2.setAccessTier(registeredTier);
    cdrVersion2 = cdrVersionDao.save(cdrVersion2);
    String cdrVersionId2 = Long.toString(cdrVersion2.getCdrVersionId());

    Workspace workspace = workspacesController.createWorkspace(createWorkspace()).getBody();

    Workspace modWorkspace =
        new Workspace()
            .name("cloned")
            .namespace("cloned-ns")
            .billingAccountName("billing-account")
            .researchPurpose(workspace.getResearchPurpose())
            .cdrVersionId(cdrVersionId2);
    stubCloneWorkspace(modWorkspace.getNamespace(), modWorkspace.getName(), "cloner@gmail.com");

    CloneWorkspaceRequest req = new CloneWorkspaceRequest().workspace(modWorkspace);
    Workspace workspace2 =
        workspacesController
            .cloneWorkspace(workspace.getNamespace(), workspace.getId(), req)
            .getBody()
            .getWorkspace();

    assertThat(workspace2.getCdrVersionId()).isEqualTo(cdrVersionId2);
  }

  @Test
  public void testCloneWorkspaceBadCdrVersion() {
    assertThrows(
        NumberFormatException.class,
        () -> {
          Workspace workspace = workspacesController.createWorkspace(createWorkspace()).getBody();
          Workspace modWorkspace =
              new Workspace()
                  .name("cloned")
                  .namespace("cloned-ns")
                  .researchPurpose(workspace.getResearchPurpose())
                  .cdrVersionId("bad-cdr-version-id");
          stubCloneWorkspace(
              modWorkspace.getNamespace(), modWorkspace.getName(), "cloner@gmail.com");

          workspacesController.cloneWorkspace(
              workspace.getNamespace(),
              workspace.getId(),
              new CloneWorkspaceRequest().workspace(modWorkspace));
        });
  }

  @Test
  public void testCloneWorkspaceMissingCdrVersion() {
    assertThrows(
        BadRequestException.class,
        () -> {
          Workspace workspace = workspacesController.createWorkspace(createWorkspace()).getBody();
          Workspace modWorkspace =
              new Workspace()
                  .name("cloned")
                  .namespace("cloned-ns")
                  .researchPurpose(workspace.getResearchPurpose())
                  .cdrVersionId("100");
          stubCloneWorkspace(
              modWorkspace.getNamespace(), modWorkspace.getName(), "cloner@gmail.com");

          workspacesController.cloneWorkspace(
              workspace.getNamespace(),
              workspace.getId(),
              new CloneWorkspaceRequest().workspace(modWorkspace));
        });
  }

  @Test
  public void testCloneWorkspaceArchivedCdrVersionThrows() {
    assertThrows(
        FailedPreconditionException.class,
        () -> {
          Workspace workspace = workspacesController.createWorkspace(createWorkspace()).getBody();
          Workspace modWorkspace =
              new Workspace()
                  .name("cloned")
                  .namespace("cloned-ns")
                  .researchPurpose(workspace.getResearchPurpose())
                  .cdrVersionId(archivedCdrVersionId);
          stubCloneWorkspace(
              modWorkspace.getNamespace(), modWorkspace.getName(), "cloner@gmail.com");
          workspacesController.cloneWorkspace(
              workspace.getNamespace(),
              workspace.getId(),
              new CloneWorkspaceRequest().workspace(modWorkspace));
        });
  }

  @Test
  public void testCloneWorkspaceIncludeUserRoles() {
    stubFcGetGroup();
    DbUser cloner = createUser("cloner@gmail.com");
    DbUser reader = createUser("reader@gmail.com");
    DbUser writer = createUser("writer@gmail.com");
    DbCdrVersion controlledTierCdr =
        TestMockFactory.createControlledTierCdrVersion(cdrVersionDao, accessTierDao, 2);

    Workspace workspace =
        workspacesController
            .createWorkspace(
                createWorkspace().cdrVersionId(String.valueOf(controlledTierCdr.getCdrVersionId())))
            .getBody();
    List<UserRole> collaborators =
        new ArrayList<>(
            Arrays.asList(
                new UserRole().email(cloner.getUsername()).role(WorkspaceAccessLevel.OWNER),
                new UserRole().email(LOGGED_IN_USER_EMAIL).role(WorkspaceAccessLevel.OWNER),
                new UserRole().email(reader.getUsername()).role(WorkspaceAccessLevel.READER),
                new UserRole().email(writer.getUsername()).role(WorkspaceAccessLevel.WRITER)));

    stubFcUpdateWorkspaceACL();
    RawlsWorkspaceACL workspaceAclsFromCloned =
        createWorkspaceACL(
            new JSONObject()
                .put(
                    "cloner@gmail.com",
                    new JSONObject()
                        .put("accessLevel", "OWNER")
                        .put("canCompute", true)
                        .put("canShare", true)));

    RawlsWorkspaceACL workspaceAclsFromOriginal =
        createWorkspaceACL(
            new JSONObject()
                .put(
                    "cloner@gmail.com",
                    new JSONObject()
                        .put("accessLevel", "READER")
                        .put("canCompute", true)
                        .put("canShare", true))
                .put(
                    "reader@gmail.com",
                    new JSONObject()
                        .put("accessLevel", "READER")
                        .put("canCompute", false)
                        .put("canShare", false))
                .put(
                    "writer@gmail.com",
                    new JSONObject()
                        .put("accessLevel", "WRITER")
                        .put("canCompute", true)
                        .put("canShare", false))
                .put(
                    LOGGED_IN_USER_EMAIL,
                    new JSONObject()
                        .put("accessLevel", "OWNER")
                        .put("canCompute", true)
                        .put("canShare", true)));

    when(fireCloudService.getWorkspaceAclAsService("cloned-ns", "cloned"))
        .thenReturn(workspaceAclsFromCloned);
    when(fireCloudService.getWorkspaceAclAsService(workspace.getNamespace(), workspace.getName()))
        .thenReturn(workspaceAclsFromOriginal);

    currentUser = cloner;

    Workspace modWorkspace =
        new Workspace()
            .namespace("cloned-ns")
            .name("cloned")
            .researchPurpose(workspace.getResearchPurpose())
            .billingAccountName("billing-account")
            .cdrVersionId(String.valueOf(controlledTierCdr.getCdrVersionId()));

    stubCloneWorkspace("cloned-ns", "cloned", cloner.getUsername());

    Workspace workspace2 =
        workspacesController
            .cloneWorkspace(
                workspace.getNamespace(),
                workspace.getId(),
                new CloneWorkspaceRequest().includeUserRoles(true).workspace(modWorkspace))
            .getBody()
            .getWorkspace();

    assertThat(workspace2.getCreator()).isEqualTo(cloner.getUsername());
    List<RawlsWorkspaceACLUpdate> updateACLRequestList =
        convertUserRolesToUpdateAclRequestList(collaborators);

    verify(fireCloudService)
        .updateWorkspaceACL(
            eq("cloned-ns"),
            eq("cloned"),
            // Accept the ACL update list in any order.
            argThat(arg -> new HashSet<>(updateACLRequestList).equals(new HashSet<>(arg))));
  }

  @Test
  public void testCloneWorkspaceBadRequest() {
    assertThrows(
        BadRequestException.class,
        () -> {
          Workspace workspace = createWorkspace();
          workspace = workspacesController.createWorkspace(workspace).getBody();
          CloneWorkspaceRequest req = new CloneWorkspaceRequest();
          Workspace modWorkspace = new Workspace();
          modWorkspace.setName("cloned");
          modWorkspace.setNamespace("cloned-ns");
          req.setWorkspace(modWorkspace);
          // Missing research purpose.
          workspacesController.cloneWorkspace(workspace.getNamespace(), workspace.getId(), req);
        });
  }

  @Test
  public void testClonePermissionDenied() {
    assertThrows(
        NotFoundException.class,
        () -> {
          Workspace workspace = createWorkspace();
          workspace = workspacesController.createWorkspace(workspace).getBody();
          // Clone with a different user.
          DbUser cloner = new DbUser();
          cloner.setUsername("cloner@gmail.com");
          cloner.setUserId(456L);
          cloner.setDisabled(false);
          currentUser = userDao.save(cloner);
          // Permission denied manifests as a 404 in Firecloud.
          when(fireCloudService.getWorkspace(workspace.getNamespace(), workspace.getName()))
              .thenThrow(new NotFoundException());
          CloneWorkspaceRequest req = new CloneWorkspaceRequest();
          Workspace modWorkspace = new Workspace();
          modWorkspace.setName("cloned");
          modWorkspace.setNamespace("cloned-ns");
          req.setWorkspace(modWorkspace);
          ResearchPurpose modPurpose = new ResearchPurpose();
          modPurpose.setAncestry(true);
          modWorkspace.setResearchPurpose(modPurpose);
          workspacesController.cloneWorkspace(workspace.getNamespace(), workspace.getId(), req);
        });
  }

  @Test
  public void testShareWorkspacePatch() {
    stubFcGetGroup();
    DbUser writerUser = createAndSaveUser("writerfriend@gmail.com", 124L);
    DbUser readerUser = createAndSaveUser("readerfriend@gmail.com", 125L);

    stubFcGetWorkspaceACL();
    Workspace workspace = createWorkspace();
    workspace = workspacesController.createWorkspace(workspace).getBody();
    ShareWorkspaceRequest shareWorkspaceRequest = new ShareWorkspaceRequest();
    shareWorkspaceRequest.setWorkspaceEtag(workspace.getEtag());

    addUserRoleToShareWorkspaceRequest(
        shareWorkspaceRequest, "readerfriend@gmail.com", WorkspaceAccessLevel.READER);
    addUserRoleToShareWorkspaceRequest(
        shareWorkspaceRequest, "writerfriend@gmail.com", WorkspaceAccessLevel.WRITER);

    // Simulate time between API calls to trigger last-modified/@Version changes.
    fakeClock.increment(1000);
    stubFcUpdateWorkspaceACL();
    WorkspaceUserRolesResponse shareResp =
        workspacesController
            .shareWorkspacePatch(
                workspace.getNamespace(), workspace.getName(), shareWorkspaceRequest)
            .getBody();

    verify(mockWorkspaceAuditor).fireCollaborateAction(anyLong(), anyMap());

    Workspace workspace2 =
        workspacesController
            .getWorkspace(workspace.getNamespace(), workspace.getName())
            .getBody()
            .getWorkspace();
    assertThat(shareResp.getWorkspaceEtag()).isEqualTo(workspace2.getEtag());

    List<RawlsWorkspaceACLUpdate> updateACLRequestList =
        convertUserRolesToUpdateAclRequestList(shareWorkspaceRequest.getItems());
    verify(fireCloudService).updateWorkspaceACL(any(), any(), eq(updateACLRequestList));
    verify(mockIamService, never()).revokeWorkflowRunnerRoleForUsers(anyString(), anyList());
  }

  @Test
  public void testShareWorkspacePatch_needsOwner() {
    stubFcGetGroup();
    stubFcUpdateWorkspaceACL();

    String namespace = "namespace";
    String name = "name";
    stubGetWorkspace(namespace, name, currentUser.getUsername(), WorkspaceAccessLevel.WRITER);

    ShareWorkspaceRequest shareWorkspaceRequest = new ShareWorkspaceRequest();
    shareWorkspaceRequest.setWorkspaceEtag("etag");

    assertThrows(
        ForbiddenException.class,
        () -> workspacesController.shareWorkspacePatch(namespace, name, shareWorkspaceRequest));
  }

  @Test
  public void testShareWorkspacePatch_AddBillingProjectUser() {
    stubFcGetGroup();
    DbUser writerUser = createAndSaveUser("writerfriend@gmail.com", 124L);
    DbUser ownerUser = createAndSaveUser("ownerfriend@gmail.com", 125L);

    stubFcGetWorkspaceACL();
    Workspace workspace = createWorkspace();
    workspace = workspacesController.createWorkspace(workspace).getBody();
    ShareWorkspaceRequest shareWorkspaceRequest =
        new ShareWorkspaceRequest()
            .workspaceEtag(workspace.getEtag())
            .addItemsItem(
                new UserRole().email(writerUser.getUsername()).role(WorkspaceAccessLevel.WRITER))
            .addItemsItem(
                new UserRole().email(ownerUser.getUsername()).role(WorkspaceAccessLevel.OWNER));

    stubFcUpdateWorkspaceACL();
    workspacesController.shareWorkspacePatch(
        workspace.getNamespace(), workspace.getName(), shareWorkspaceRequest);

    verify(fireCloudService, times(1))
        .addOwnerToBillingProject(ownerUser.getUsername(), workspace.getNamespace());
    verify(fireCloudService, never()).addOwnerToBillingProject(eq(writerUser.getUsername()), any());
    verify(fireCloudService, never())
        .removeOwnerFromBillingProject(any(), any(), eq(Optional.empty()));
  }

  @Test
  public void testShareWorkspacePatch_removeBillingProjectUser() {
    stubFcGetGroup();
    DbUser writerUser = createAndSaveUser("writerfriend@gmail.com", 124L);
    DbUser ownerUser = createAndSaveUser("ownerfriend@gmail.com", 125L);

    when(fireCloudService.getWorkspaceAclAsService(anyString(), anyString()))
        .thenReturn(
            createWorkspaceACL(
                new JSONObject()
                    .put(
                        currentUser.getUsername(),
                        new JSONObject()
                            .put("accessLevel", "OWNER")
                            .put("canCompute", true)
                            .put("canShare", true))
                    .put(
                        writerUser.getUsername(),
                        new JSONObject()
                            .put("accessLevel", "WRITER")
                            .put("canCompute", true)
                            .put("canShare", true))
                    .put(
                        ownerUser.getUsername(),
                        new JSONObject()
                            .put("accessLevel", "OWNER")
                            .put("canCompute", true)
                            .put("canShare", true))));

    Workspace workspace = createWorkspace();
    workspace = workspacesController.createWorkspace(workspace).getBody();
    // added as part of the createWorkspace() process
    verify(fireCloudService, times(1)).addOwnerToBillingProject(any(), any());

    ShareWorkspaceRequest shareWorkspaceRequest =
        new ShareWorkspaceRequest()
            .workspaceEtag(workspace.getEtag())
            // Removed WRITER, demoted OWNER to READER.
            .addItemsItem(
                new UserRole().email(writerUser.getUsername()).role(WorkspaceAccessLevel.NO_ACCESS))
            .addItemsItem(
                new UserRole().email(ownerUser.getUsername()).role(WorkspaceAccessLevel.READER));

    stubFcUpdateWorkspaceACL();
    workspacesController.shareWorkspacePatch(
        workspace.getNamespace(), workspace.getName(), shareWorkspaceRequest);

    verify(fireCloudService, times(1))
        .removeOwnerFromBillingProject(
            ownerUser.getUsername(), workspace.getNamespace(), Optional.empty());
    verify(fireCloudService, never())
        .removeOwnerFromBillingProject(eq(writerUser.getUsername()), any(), eq(Optional.empty()));
    verify(fireCloudService, never())
        .removeOwnerFromBillingProject(eq(currentUser.getUsername()), any(), eq(Optional.empty()));
  }

  @Test
  public void testShareWorkspacePatch_publishedWorkspace() {
    DbCdrVersion ctCdrVersion =
        TestMockFactory.createControlledTierCdrVersion(cdrVersionDao, accessTierDao, 5L);

    DbUser writerUser = createAndSaveUser("writerfriend@gmail.com", 124L);

    when(fireCloudService.getWorkspaceAclAsService(anyString(), anyString()))
        .thenReturn(
            createWorkspaceACL(
                new JSONObject()
                    .put(
                        // Specifically, the REGISTERED tier is used for publishing.
                        registeredTier.getAuthDomainGroupEmail(),
                        new JSONObject()
                            .put("accessLevel", "READER")
                            .put("canCompute", false)
                            .put("canShare", false))
                    .put(
                        currentUser.getUsername(),
                        new JSONObject()
                            .put("accessLevel", "OWNER")
                            .put("canCompute", true)
                            .put("canShare", true))
                    .put(
                        writerUser.getUsername(),
                        new JSONObject()
                            .put("accessLevel", "WRITER")
                            .put("canCompute", true)
                            .put("canShare", true))));

    Workspace workspace =
        createWorkspace().cdrVersionId(Long.toString(ctCdrVersion.getCdrVersionId()));
    workspace = workspacesController.createWorkspace(workspace).getBody();

    ShareWorkspaceRequest shareWorkspaceRequest =
        new ShareWorkspaceRequest()
            .workspaceEtag(workspace.getEtag())
            // Removing writer.
            .addItemsItem(
                new UserRole()
                    .email(writerUser.getUsername())
                    .role(WorkspaceAccessLevel.NO_ACCESS));

    stubFcUpdateWorkspaceACL();
    workspacesController.shareWorkspacePatch(
        workspace.getNamespace(), workspace.getName(), shareWorkspaceRequest);

    verify(fireCloudService)
        .updateWorkspaceACL(
            any(),
            any(),
            eq(
                ImmutableList.of(
                    // Specifically, the Registered Tier group should not be removed by this update.
                    FirecloudTransforms.buildAclUpdate(
                        writerUser.getUsername(), WorkspaceAccessLevel.NO_ACCESS))));
  }

  @Test
  public void testSharePatch_workspaceNoRoleFailure() {
    DbUser writerUser = createAndSaveUser("writerfriend@gmail.com", 124L);
    stubFcGetWorkspaceACL();
    final Workspace workspace = workspacesController.createWorkspace(createWorkspace()).getBody();
    ShareWorkspaceRequest shareWorkspaceRequest = new ShareWorkspaceRequest();
    shareWorkspaceRequest.setWorkspaceEtag(workspace.getEtag());
    addUserRoleToShareWorkspaceRequest(
        shareWorkspaceRequest, LOGGED_IN_USER_EMAIL, WorkspaceAccessLevel.OWNER);
    UserRole writer = new UserRole();
    writer.setEmail("writerfriend@gmail.com");
    shareWorkspaceRequest.addItemsItem(writer);

    // Simulate time between API calls to trigger last-modified/@Version changes.
    fakeClock.increment(1000);
    stubFcUpdateWorkspaceACL();
    assertThrows(
        BadRequestException.class,
        () ->
            workspacesController.shareWorkspacePatch(
                workspace.getNamespace(), workspace.getName(), shareWorkspaceRequest));
  }

  @Test
  public void testUnshareWorkspace() {
    stubFcGetGroup();
    DbUser writerUser = createAndSaveUser("writerfriend@gmail.com", 124L);
    DbUser readerUser = createAndSaveUser("readerfriend@gmail.com", 125L);

    Workspace workspace = createWorkspace();
    workspace = workspacesController.createWorkspace(workspace).getBody();

    // Mock firecloud ACLs
    RawlsWorkspaceACL workspaceACLs =
        createWorkspaceACL(
            new JSONObject()
                .put(
                    LOGGED_IN_USER_EMAIL,
                    new JSONObject()
                        .put("accessLevel", "OWNER")
                        .put("canCompute", true)
                        .put("canShare", true))
                .put(
                    writerUser.getUsername(),
                    new JSONObject()
                        .put("accessLevel", "WRITER")
                        .put("canCompute", true)
                        .put("canShare", false))
                .put(
                    readerUser.getUsername(),
                    new JSONObject()
                        .put("accessLevel", "READER")
                        .put("canCompute", false)
                        .put("canShare", false)));
    when(fireCloudService.getWorkspaceAclAsService(any(), any())).thenReturn(workspaceACLs);

    fakeClock.increment(1000);
    stubFcUpdateWorkspaceACL();

    final ShareWorkspaceRequest shareWorkspaceRequest = new ShareWorkspaceRequest();
    shareWorkspaceRequest.setWorkspaceEtag(workspace.getEtag());
    UserRole reader = new UserRole();
    reader.setEmail(readerUser.getUsername());
    reader.setRole(WorkspaceAccessLevel.NO_ACCESS);
    shareWorkspaceRequest.addItemsItem(reader);

    WorkspaceUserRolesResponse shareResp =
        workspacesController
            .shareWorkspacePatch(
                workspace.getNamespace(), workspace.getName(), shareWorkspaceRequest)
            .getBody();
    Workspace workspace2 =
        workspacesController
            .getWorkspace(workspace.getNamespace(), workspace.getId())
            .getBody()
            .getWorkspace();
    assertThat(shareResp.getWorkspaceEtag()).isEqualTo(workspace2.getEtag());

    List<RawlsWorkspaceACLUpdate> updateACLRequestList =
        convertUserRolesToUpdateAclRequestList(shareWorkspaceRequest.getItems());
    verify(fireCloudService)
        .updateWorkspaceACL(
            any(),
            any(),
            eq(
                updateACLRequestList.stream()
                    .sorted(Comparator.comparing(RawlsWorkspaceACLUpdate::getEmail))
                    .collect(Collectors.toList())));
  }

  @Test
  public void testShareWorkspacePatch_staleEtag() {
    stubFcGetGroup();
    final Workspace workspace = workspacesController.createWorkspace(createWorkspace()).getBody();
    ShareWorkspaceRequest shareWorkspaceRequest1 = new ShareWorkspaceRequest();
    shareWorkspaceRequest1.setWorkspaceEtag(workspace.getEtag());

    // Simulate time between API calls to trigger last-modified/@Version changes.
    fakeClock.increment(1000);
    stubFcUpdateWorkspaceACL();
    stubFcGetWorkspaceACL();
    workspacesController.shareWorkspacePatch(
        workspace.getNamespace(), workspace.getName(), shareWorkspaceRequest1);

    // Simulate time between API calls to trigger last-modified/@Version changes.
    fakeClock.increment(1000);
    ShareWorkspaceRequest shareWorkspaceRequest2 = new ShareWorkspaceRequest();
    // Use the initial etag, not the updated value from shareWorkspace.
    shareWorkspaceRequest2.setWorkspaceEtag(workspace.getEtag());
    assertThrows(
        ConflictException.class,
        () ->
            workspacesController.shareWorkspacePatch(
                workspace.getNamespace(), workspace.getName(), shareWorkspaceRequest2));
  }

  @Test
  public void testUnableToShareWithNonExistentUser() {
    Workspace workspace = createWorkspace();
    workspacesController.createWorkspace(workspace);
    ShareWorkspaceRequest shareWorkspaceRequest = new ShareWorkspaceRequest();
    addUserRoleToShareWorkspaceRequest(
        shareWorkspaceRequest, "does-not-exist@gmail.com", WorkspaceAccessLevel.WRITER);

    assertThrows(
        BadRequestException.class,
        () -> {
          workspacesController.shareWorkspacePatch(
              workspace.getNamespace(), workspace.getName(), shareWorkspaceRequest);
        });
  }

  @Test
  public void testEmptyFireCloudWorkspaces() {
    when(fireCloudService.getWorkspaces()).thenReturn(new ArrayList<>());
    try {
      ResponseEntity<WorkspaceResponseListResponse> response = workspacesController.getWorkspaces();
      assertThat(response.getBody().getItems()).isEmpty();
    } catch (Exception ex) {
      fail();
    }
  }

  @Test
  public void testGetFirecloudWorkspaceUserRoles() {
    stubFcGetGroup();
    stubFcGetWorkspaceACL();

    Workspace workspace = createWorkspace();
    workspace = workspacesController.createWorkspace(workspace).getBody();
    WorkspaceUserRolesResponse resp =
        workspacesController
            .getFirecloudWorkspaceUserRoles(workspace.getNamespace(), workspace.getId())
            .getBody();

    assertThat(resp.getItems())
        .containsExactly(
            new UserRole().email(currentUser.getUsername()).role(WorkspaceAccessLevel.OWNER));
  }

  @Test
  public void testGetFirecloudWorkspaceUserRoles_noAccess() {
    Workspace workspace = createWorkspace();
    when(fireCloudService.getWorkspace(workspace.getNamespace(), workspace.getId()))
        .thenThrow(new ForbiddenException());

    assertThrows(
        ForbiddenException.class,
        () ->
            workspacesController
                .getFirecloudWorkspaceUserRoles(workspace.getNamespace(), workspace.getId())
                .getBody());
  }

  @Test
  public void testGetPublishedWorkspaces() {
    stubFcGetGroup();
    stubFcUpdateWorkspaceACL();

    Workspace workspace = createWorkspace();
    workspace = workspacesController.createWorkspace(workspace).getBody();
    workspaceAdminService.setPublished(workspace.getNamespace(), workspace.getId(), true);

    RawlsWorkspaceResponse fcResponse = new RawlsWorkspaceResponse();
    fcResponse.setWorkspace(
        TestMockFactory.createFirecloudWorkspace(
            workspace.getNamespace(), workspace.getName(), null));
    fcResponse.setAccessLevel(WorkspaceAccessLevel.OWNER.toString());
    doReturn(Collections.singletonList(fcResponse)).when(fireCloudService).getWorkspaces();

    assertThat(workspacesController.getPublishedWorkspaces().getBody().getItems().size())
        .isEqualTo(1);
  }

  @Test
  public void testGetWorkspacesGetsPublishedIfOwner() {
    stubFcGetGroup();
    stubFcUpdateWorkspaceACL();

    Workspace workspace = createWorkspace();
    workspace = workspacesController.createWorkspace(workspace).getBody();
    workspaceAdminService.setPublished(workspace.getNamespace(), workspace.getId(), true);

    RawlsWorkspaceResponse fcResponse = new RawlsWorkspaceResponse();
    fcResponse.setWorkspace(
        TestMockFactory.createFirecloudWorkspace(
            workspace.getNamespace(), workspace.getName(), null));
    fcResponse.setAccessLevel(WorkspaceAccessLevel.OWNER.toString());
    doReturn(Collections.singletonList(fcResponse)).when(fireCloudService).getWorkspaces();

    assertThat(workspacesController.getWorkspaces().getBody().getItems().size()).isEqualTo(1);
  }

  @Test
  public void testGetWorkspacesGetsPublishedIfWriter() {
    stubFcGetGroup();
    stubFcUpdateWorkspaceACL();

    Workspace workspace = createWorkspace();
    workspace = workspacesController.createWorkspace(workspace).getBody();
    workspaceAdminService.setPublished(workspace.getNamespace(), workspace.getId(), true);

    RawlsWorkspaceResponse fcResponse = new RawlsWorkspaceResponse();
    fcResponse.setWorkspace(
        TestMockFactory.createFirecloudWorkspace(
            workspace.getNamespace(), workspace.getName(), null));
    fcResponse.setAccessLevel(WorkspaceAccessLevel.WRITER.toString());
    doReturn(Collections.singletonList(fcResponse)).when(fireCloudService).getWorkspaces();

    assertThat(workspacesController.getWorkspaces().getBody().getItems().size()).isEqualTo(1);
  }

  @Test
  public void testGetWorkspacesDoesNotGetsPublishedIfReader() {
    stubFcGetGroup();
    stubFcUpdateWorkspaceACL();

    Workspace workspace = createWorkspace();
    workspace = workspacesController.createWorkspace(workspace).getBody();
    workspaceAdminService.setPublished(workspace.getNamespace(), workspace.getId(), true);

    RawlsWorkspaceResponse fcResponse = new RawlsWorkspaceResponse();
    fcResponse.setWorkspace(
        TestMockFactory.createFirecloudWorkspace(
            workspace.getNamespace(), workspace.getName(), null));
    fcResponse.setAccessLevel(WorkspaceAccessLevel.READER.toString());
    doReturn(Collections.singletonList(fcResponse)).when(fireCloudService).getWorkspaces();

    assertThat(workspacesController.getWorkspaces().getBody().getItems().size()).isEqualTo(0);
  }

  @Test
  public void testGetBillingUsage() {
    Double cost = 150.50;
    Workspace ws = createWorkspace();
    ws = workspacesController.createWorkspace(ws).getBody();
    stubGetWorkspace(ws.getNamespace(), ws.getId(), ws.getCreator(), WorkspaceAccessLevel.OWNER);
    when(mockFreeTierBillingService.getWorkspaceFreeTierBillingUsage(any())).thenReturn(cost);

    WorkspaceBillingUsageResponse workspaceBillingUsageResponse =
        workspacesController.getBillingUsage(ws.getNamespace(), ws.getId()).getBody();
    assertThat(workspaceBillingUsageResponse.getCost()).isEqualTo(cost);
  }

  @Test
  public void testGetBillingUsageWithoutAccess() {
    assertThrows(
        ForbiddenException.class,
        () -> {
          Workspace ws = createWorkspace();
          ws = workspacesController.createWorkspace(ws).getBody();
          stubGetWorkspace(
              ws.getNamespace(), ws.getId(), ws.getCreator(), WorkspaceAccessLevel.READER);
          workspacesController.getBillingUsage(ws.getNamespace(), ws.getId());
        });
  }

  @Test
  public void testGetBillingUsageWithNoSpend() {
    Workspace ws = createWorkspace();
    ws = workspacesController.createWorkspace(ws).getBody();
    stubGetWorkspace(ws.getNamespace(), ws.getId(), ws.getCreator(), WorkspaceAccessLevel.OWNER);
    WorkspaceBillingUsageResponse workspaceBillingUsageResponse =
        workspacesController.getBillingUsage(ws.getNamespace(), ws.getId()).getBody();
    assertThat(workspaceBillingUsageResponse.getCost()).isEqualTo(0.0d);
  }

  @Test
  public void getUserRecentWorkspaces() {
    Workspace workspace = createWorkspace();
    workspace = workspacesController.createWorkspace(workspace).getBody();
    stubGetWorkspace(
        workspace.getNamespace(),
        workspace.getName(),
        LOGGED_IN_USER_EMAIL,
        WorkspaceAccessLevel.OWNER);
    DbWorkspace dbWorkspace = workspaceDao.get(workspace.getNamespace(), workspace.getId());
    workspaceService.updateRecentWorkspaces(dbWorkspace);
    ResponseEntity<RecentWorkspaceResponse> recentWorkspaceResponseEntity =
        workspacesController.getUserRecentWorkspaces();
    RecentWorkspace recentWorkspace = recentWorkspaceResponseEntity.getBody().get(0);
    assertThat(recentWorkspace.getWorkspace().getNamespace())
        .isEqualTo(dbWorkspace.getWorkspaceNamespace());
    assertThat(recentWorkspace.getWorkspace().getName()).isEqualTo(dbWorkspace.getName());
  }

  @Test
  public void updateRecentWorkspaces_nullWorkspace() {
    assertThrows(
        NotFoundException.class, () -> workspacesController.updateRecentWorkspaces("foo", "bar"));
  }

  // Does not compare: etag, lastModifiedTime, page, pageSize, participantCohortStatuses,
  // queryResultSize, reviewedCount, reviewSize, reviewStatus, sortColumn, sortOrder
  private void compareCohortReviewFields(
      CohortReview observedCohortReview, CohortReview expectedCohortReview) {
    assertThat(observedCohortReview.getCdrVersionId())
        .isEqualTo(expectedCohortReview.getCdrVersionId());
    assertThat(observedCohortReview.getCohortDefinition())
        .isEqualTo(expectedCohortReview.getCohortDefinition());
    assertThat(observedCohortReview.getCohortId()).isEqualTo(expectedCohortReview.getCohortId());
    assertThat(observedCohortReview.getCohortName())
        .isEqualTo(expectedCohortReview.getCohortName());
    assertThat(observedCohortReview.getCohortReviewId())
        .isEqualTo(expectedCohortReview.getCohortReviewId());
    assertThat(observedCohortReview.getCreationTime())
        .isEqualTo(expectedCohortReview.getCreationTime());
    assertThat(observedCohortReview.getDescription())
        .isEqualTo(expectedCohortReview.getDescription());
    assertThat(observedCohortReview.getMatchedParticipantCount())
        .isEqualTo(expectedCohortReview.getMatchedParticipantCount());
  }

  private void compareDatasetMetadata(DataSet observedDataSet, DataSet expectedDataSet) {
    assertThat(observedDataSet.getDescription()).isEqualTo(expectedDataSet.getDescription());
    assertThat(observedDataSet.getEtag()).isEqualTo(expectedDataSet.getEtag());
    assertThat(observedDataSet.getId()).isEqualTo(expectedDataSet.getId());
    assertThat(observedDataSet.getIncludesAllParticipants())
        .isEqualTo(expectedDataSet.getIncludesAllParticipants());
    assertThat(observedDataSet.getLastModifiedTime())
        .isEqualTo(expectedDataSet.getLastModifiedTime());
    assertThat(observedDataSet.getName()).isEqualTo(expectedDataSet.getName());
    assertThat(observedDataSet.getPrePackagedConceptSet())
        .isEqualTo(expectedDataSet.getPrePackagedConceptSet());
  }

  @Test
  public void getWorkspaceResources() {
    CdrVersionContext.setCdrVersionNoCheckAuthDomain(cdrVersion);
    Workspace workspace = workspacesController.createWorkspace(createWorkspace()).getBody();

    Cohort cohort =
        cohortsController
            .createCohort(
                workspace.getNamespace(), workspace.getId(), createDefaultCohort("cohort"))
            .getBody();
    stubBigQueryCohortCalls();
    CohortReview cohortReview =
        cohortReviewController
            .createCohortReview(
                workspace.getNamespace(),
                workspace.getId(),
                cohort.getId(),
                new CreateReviewRequest().size(1).name("review1"))
            .getBody();

    ConceptSetConceptId conceptSetConceptId1 = new ConceptSetConceptId();
    conceptSetConceptId1.setConceptId(CLIENT_CONCEPT_1.getConceptId());
    conceptSetConceptId1.setStandard(true);
    ConceptSet conceptSet =
        conceptSetsController
            .createConceptSet(
                workspace.getNamespace(),
                workspace.getId(),
                new CreateConceptSetRequest()
                    .conceptSet(
                        new ConceptSet().name("cs1").description("d1").domain(Domain.CONDITION))
                    .addAddedConceptSetConceptIdsItem(conceptSetConceptId1))
            .getBody();
    DataSet dataSet =
        dataSetController
            .createDataSet(
                workspace.getNamespace(),
                workspace.getId(),
                new DataSetRequest()
                    .prePackagedConceptSet(ImmutableList.of(PrePackagedConceptSetEnum.NONE))
                    .addConceptSetIdsItem(conceptSet.getId())
                    .addCohortIdsItem(cohort.getId())
                    .name("dataset")
                    .domainValuePairs(
                        ImmutableList.of(
                            new DomainValuePair().value("VALUE").domain(Domain.CONDITION))))
            .getBody();

    List<String> typesToFetch =
        ImmutableList.of(
            ResourceType.COHORT.toString(),
            ResourceType.COHORT_REVIEW.toString(),
            ResourceType.CONCEPT_SET.toString(),
            ResourceType.DATASET.toString());

    WorkspaceResourceResponse workspaceResourceResponse =
        workspacesController
            .getWorkspaceResourcesV2(workspace.getNamespace(), workspace.getId(), typesToFetch)
            .getBody();
    assertThat(workspaceResourceResponse).hasSize(4);

    List<Cohort> cohorts =
        workspaceResourceResponse.stream()
            .map(WorkspaceResource::getCohort)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    List<CohortReview> cohortReviews =
        workspaceResourceResponse.stream()
            .map(WorkspaceResource::getCohortReview)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    List<ConceptSet> conceptSets =
        workspaceResourceResponse.stream()
            .map(WorkspaceResource::getConceptSet)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    List<DataSet> dataSets =
        workspaceResourceResponse.stream()
            .map(WorkspaceResource::getDataSet)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    assertThat(cohorts).hasSize(1);
    assertThat(cohorts.get(0)).isEqualTo(cohort);
    assertThat(cohortReviews).hasSize(1);
    compareCohortReviewFields(cohortReviews.get(0), cohortReview);
    assertThat(conceptSets).hasSize(1);
    // Ignore arrays in subtables.
    assertThat(conceptSets.get(0)).isEqualTo(conceptSet);
    assertThat(dataSets).hasSize(1);
    compareDatasetMetadata(dataSets.get(0), dataSet);
  }
}
