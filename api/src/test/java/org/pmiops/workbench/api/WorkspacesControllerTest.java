package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.pmiops.workbench.FakeClockConfiguration.NOW_TIME;
import static org.pmiops.workbench.exfiltration.ExfiltrationUtils.EGRESS_OBJECT_LENGTHS_SERVICE_QUALIFIER;
import static org.pmiops.workbench.utils.TestMockFactory.DEFAULT_GOOGLE_PROJECT;
import static org.pmiops.workbench.utils.TestMockFactory.createControlledTier;
import static org.pmiops.workbench.utils.TestMockFactory.createControlledTierCdrVersion;
import static org.pmiops.workbench.utils.TestMockFactory.createDefaultCdrVersion;
import static org.pmiops.workbench.utils.TestMockFactory.createRegisteredTier;

import com.google.api.services.cloudbilling.model.ProjectBillingInfo;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.LegacySQLTypeName;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.TableResult;
import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.access.AccessTierService;
import org.pmiops.workbench.actionaudit.ActionAuditQueryService;
import org.pmiops.workbench.actionaudit.auditors.AdminAuditor;
import org.pmiops.workbench.actionaudit.auditors.BillingProjectAuditor;
import org.pmiops.workbench.actionaudit.auditors.LeonardoRuntimeAuditor;
import org.pmiops.workbench.actionaudit.auditors.WorkspaceAuditor;
import org.pmiops.workbench.actionaudit.bucket.BucketAuditQueryService;
import org.pmiops.workbench.actionaudit.bucket.BucketAuditQueryServiceImpl;
import org.pmiops.workbench.cdr.CdrVersionService;
import org.pmiops.workbench.cdr.ConceptBigQueryService;
import org.pmiops.workbench.cloudtasks.TaskQueueService;
import org.pmiops.workbench.config.CdrBigQuerySchemaConfigService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.AccessTierDao;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.CohortReviewDao;
import org.pmiops.workbench.db.dao.ConceptSetDao;
import org.pmiops.workbench.db.dao.DataSetDao;
import org.pmiops.workbench.db.dao.FeaturedWorkspaceDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserRecentWorkspaceDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.dao.WorkspaceFreeTierUsageDao;
import org.pmiops.workbench.db.dao.WorkspaceOperationDao;
import org.pmiops.workbench.db.jdbc.ReportingQueryService;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbCdrVersion;
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
import org.pmiops.workbench.google.CloudBillingClient;
import org.pmiops.workbench.google.CloudMonitoringService;
import org.pmiops.workbench.google.CloudStorageClient;
import org.pmiops.workbench.iam.IamService;
import org.pmiops.workbench.initialcredits.InitialCreditsService;
import org.pmiops.workbench.lab.notebooks.NotebooksService;
import org.pmiops.workbench.mail.MailService;
import org.pmiops.workbench.model.*;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceACL;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceACLUpdate;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceACLUpdateResponseList;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceAccessLevel;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceDetails;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceListResponse;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceResponse;
import org.pmiops.workbench.tanagra.api.TanagraApi;
import org.pmiops.workbench.test.CohortDefinitions;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.user.VwbUserService;
import org.pmiops.workbench.utils.BigQueryUtils;
import org.pmiops.workbench.utils.TestMockFactory;
import org.pmiops.workbench.utils.mappers.AnalysisLanguageMapperImpl;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.utils.mappers.FeaturedWorkspaceMapper;
import org.pmiops.workbench.utils.mappers.FirecloudMapper;
import org.pmiops.workbench.utils.mappers.FirecloudMapperImpl;
import org.pmiops.workbench.utils.mappers.LeonardoMapperImpl;
import org.pmiops.workbench.utils.mappers.UserMapperImpl;
import org.pmiops.workbench.utils.mappers.WorkspaceMapperImpl;
import org.pmiops.workbench.vwb.admin.VwbAdminQueryService;
import org.pmiops.workbench.vwb.wsm.WsmClient;
import org.pmiops.workbench.workspaceadmin.WorkspaceAdminService;
import org.pmiops.workbench.workspaceadmin.WorkspaceAdminServiceImpl;
import org.pmiops.workbench.workspaces.*;
import org.pmiops.workbench.workspaces.migration.WorkspaceMigrationService;
import org.pmiops.workbench.workspaces.resources.UserRecentResourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class WorkspacesControllerTest {

  @MockitoBean private ActionAuditQueryService actionAuditQueryService;
  @MockitoBean private AdminAuditor adminAuditor;
  @MockitoBean private AnalysisLanguageMapperImpl analysisLanguageMapperImpl;
  @MockitoBean private BillingProjectAuditor billingProjectAuditor;
  @MockitoBean private CdrBigQuerySchemaConfigService cdrBigQuerySchemaConfigService;
  @MockitoBean private CdrVersionService cdrVersionService;
  @MockitoBean private CloudMonitoringService cloudMonitoringService;

  @MockitoBean private LeonardoRuntimeAuditor leonardoRuntimeAuditor;
  @MockitoBean private MailService mailService;
  @MockitoBean private NotebooksService notebooksService;
  @MockitoBean private ReportingQueryService reportingQueryService;
  @MockitoBean private TanagraApi tanagraApi;
  @MockitoBean private TaskQueueService taskQueueService;
  @MockitoBean private UserService userService;
  @MockitoBean private WorkspaceAuditor workspaceAuditor;
  @MockitoBean private WorkspaceMigrationService workspaceMigrationService;
  @MockitoBean private WsmClient wsmClient;
  @MockitoBean private VwbUserService vwbUserService;
  @MockitoBean private VwbAdminQueryService vwbAdminQueryService;
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

  @MockitoBean private BigQueryService bigQueryService;
  @MockitoBean private CloudStorageClient cloudStorageClient;
  @MockitoBean private ConceptBigQueryService conceptBigQueryService;
  @MockitoBean private UserRecentResourceService userRecentResourceService;
  @MockitoBean private WorkspaceServiceFactory workspaceServiceFactory;
  @MockitoBean AccessTierService accessTierService;
  @MockitoBean BucketAuditQueryService bucketAuditQueryService;
  @MockitoBean CloudBillingClient mockCloudBillingClient;
  @MockitoBean FeaturedWorkspaceMapper featuredWorkspaceMapper;
  @MockitoBean FireCloudService fireCloudService;
  @MockitoBean InitialCreditsService mockInitialCreditsService;
  @MockitoBean IamService mockIamService;

  @MockitoBean
  @Qualifier(EGRESS_OBJECT_LENGTHS_SERVICE_QUALIFIER)
  EgressRemediationService egressRemediationService;

  @MockitoSpyBean @Autowired WorkspaceDao workspaceDao;
  @MockitoSpyBean @Autowired WorkspaceService workspaceService;

  @Autowired AccessTierDao accessTierDao;
  @Autowired CdrVersionDao cdrVersionDao;
  @Autowired CohortDao cohortDao;
  @Autowired CohortReviewDao cohortReviewDao;
  @Autowired ConceptSetDao conceptSetDao;
  @Autowired DataSetDao dataSetDao;
  @Autowired FakeClock fakeClock;
  @Autowired FirecloudMapper firecloudMapper;
  @Autowired ObjectNameLengthService objectNameLengthService;
  @Autowired UserDao userDao;
  @Autowired UserRecentWorkspaceDao userRecentWorkspaceDao;
  @Autowired WorkspaceAdminService workspaceAdminService;
  @Autowired WorkspaceAuditor mockWorkspaceAuditor;
  @Autowired WorkspaceFreeTierUsageDao workspaceFreeTierUsageDao;
  @Autowired WorkspaceOperationDao workspaceOperationDao;
  @Autowired WorkspacesController workspacesController;
  @Autowired FeaturedWorkspaceDao featuredWorkspaceDao;

  private static DbUser currentUser;
  private static WorkbenchConfig workbenchConfig;

  private DbAccessTier registeredTier;
  private DbCdrVersion cdrVersion;
  private String cdrVersionId;
  private String archivedCdrVersionId;

  @TestConfiguration
  @Import({
    BucketAuditQueryServiceImpl.class,
    CdrVersionService.class,
    CommonMappers.class,
    FakeClockConfiguration.class,
    FirecloudMapperImpl.class,
    LeonardoMapperImpl.class,
    ObjectNameLengthServiceImpl.class,
    UserMapperImpl.class,
    WorkspaceAdminServiceImpl.class,
    WorkspaceAuthService.class,
    WorkspaceMapperImpl.class,
    WorkspaceOperationMapperImpl.class,
    WorkspaceServiceImpl.class,
    WorkspacesController.class,
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
    workbenchConfig.billing.accountId = "initial-credits";
    workbenchConfig.billing.projectNamePrefix = "aou-local";

    currentUser = createUser(LOGGED_IN_USER_EMAIL);
    registeredTier = accessTierDao.save(createRegisteredTier());

    when(accessTierService.getAccessTierShortNamesForUser(currentUser))
        .thenReturn(List.of(AccessTierService.REGISTERED_TIER_SHORT_NAME));
    when(accessTierService.getRegisteredTierOrThrow()).thenReturn(registeredTier);

    cdrVersion = createDefaultCdrVersion(1);
    accessTierDao.save(cdrVersion.getAccessTier());
    cdrVersion.setName("1");
    // set the db name to be empty since test cases currently
    // run in the workbench schema only.
    cdrVersion.setCdrDbName("");
    cdrVersion.setAccessTier(registeredTier);
    cdrVersion = cdrVersionDao.save(cdrVersion);
    cdrVersionId = Long.toString(cdrVersion.getCdrVersionId());

    DbCdrVersion archivedCdrVersion = createDefaultCdrVersion(2);
    accessTierDao.save(archivedCdrVersion.getAccessTier());
    archivedCdrVersion.setName("archived");
    archivedCdrVersion.setCdrDbName("");
    archivedCdrVersion.setArchivalStatusEnum(ArchivalStatus.ARCHIVED);
    archivedCdrVersion = cdrVersionDao.save(archivedCdrVersion);
    archivedCdrVersionId = Long.toString(archivedCdrVersion.getCdrVersionId());

    TestMockFactory.stubCreateBillingProject(fireCloudService);
    TestMockFactory.stubCreateFcWorkspace(fireCloudService);

    ProjectBillingInfo billingInfo = new ProjectBillingInfo().setBillingEnabled(true);
    when(mockCloudBillingClient.pollUntilBillingAccountLinked(any(), any()))
        .thenReturn(billingInfo);
    when(mockCloudBillingClient.pollUntilBillingAccountLinked(any(), any(), anyBoolean()))
        .thenReturn(billingInfo);

    when(workspaceServiceFactory.getWorkspaceService(anyBoolean())).thenReturn(workspaceService);
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
      String workspaceNamespace,
      String workspaceTerraName,
      String creator,
      WorkspaceAccessLevel access) {
    stubGetWorkspace(
        TestMockFactory.createTerraWorkspace(workspaceNamespace, workspaceTerraName, creator),
        access);
  }

  private void stubGetWorkspace(RawlsWorkspaceDetails fcWorkspace, WorkspaceAccessLevel access) {
    RawlsWorkspaceListResponse fcResponse = new RawlsWorkspaceListResponse();
    fcResponse.setWorkspace(fcWorkspace);
    fcResponse.setAccessLevel(firecloudMapper.apiToFcWorkspaceAccessLevel(access));

    RawlsWorkspaceResponse fcGetResponse = new RawlsWorkspaceResponse();
    fcGetResponse.setWorkspace(fcWorkspace);
    fcGetResponse.setAccessLevel(firecloudMapper.apiToFcWorkspaceAccessLevel(access));

    doReturn(fcGetResponse)
        .when(fireCloudService)
        .getWorkspace(fcWorkspace.getNamespace(), fcWorkspace.getName());
    List<RawlsWorkspaceListResponse> workspaceResponses = fireCloudService.listWorkspaces();
    workspaceResponses.add(fcResponse);
    doReturn(workspaceResponses).when(fireCloudService).listWorkspaces();
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
    List<FieldValueList> tableRows = List.of(FieldValueList.of(List.of(countValue)));
    TableResult result = BigQueryUtils.newTableResult(schema, tableRows);

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
        List.of(
            FieldValueList.of(
                Arrays.asList(
                    personIdValue,
                    birthDatetimeValue,
                    genderConceptIdValue,
                    raceConceptIdValue,
                    ethnicityConceptIdValue,
                    sexAtBirthConceptIdValue,
                    deceasedValue)));
    TableResult result2 = BigQueryUtils.newTableResult(schema2, tableRows2);

    // return the TableResult calls in order of call
    when(bigQueryService.filterBigQueryConfigAndExecuteQuery(null)).thenReturn(result, result2);
  }

  private static final String testWorkspaceNamespace = "namespace";
  private static final String testWorkspaceDisplayName = "Workspace Name";
  private static final String testWorkspaceTerraName = "workspacename";

  private Workspace createWorkspace() {
    return TestMockFactory.createWorkspace(
        testWorkspaceNamespace, testWorkspaceDisplayName, testWorkspaceTerraName);
  }

  public Cohort createDefaultCohort(String name) {
    Cohort cohort = new Cohort();
    cohort.setName(name);
    cohort.setCriteria(new Gson().toJson(CohortDefinitions.males()));
    return cohort;
  }

  private List<RawlsWorkspaceACLUpdate> convertUserRolesToUpdateAclRequestList(
      Collection<UserRole> collaborators) {
    return collaborators.stream()
        .map(c -> FirecloudTransforms.buildAclUpdate(c.getEmail(), c.getRole()))
        .collect(Collectors.toList());
  }

  private Workspace createWorkspaceAndGrantAccess(WorkspaceAccessLevel accessLevel) {
    Workspace ws = createWorkspace();
    ws = workspacesController.createWorkspace(ws).getBody();
    stubGetWorkspace(
        ws.getNamespace(), ws.getTerraName(), ws.getCreatorUser().getUserName(), accessLevel);
    return ws;
  }

  @Test
  public void getWorkspaces() {
    Workspace workspace = createWorkspace();
    workspace = workspacesController.createWorkspace(workspace).getBody();
    verify(mockWorkspaceAuditor).fireCreateAction(any(Workspace.class), anyLong());

    RawlsWorkspaceListResponse fcResponse = new RawlsWorkspaceListResponse();
    fcResponse.setWorkspace(
        TestMockFactory.createTerraWorkspace(
            workspace.getNamespace(), workspace.getTerraName(), null));
    fcResponse.setAccessLevel(RawlsWorkspaceAccessLevel.OWNER);
    doReturn(Collections.singletonList(fcResponse)).when(fireCloudService).listWorkspaces();

    assertThat(workspacesController.getWorkspaces().getBody().getItems().size()).isEqualTo(1);
    assertThat(
            workspacesController
                .getWorkspaces()
                .getBody()
                .getItems()
                .get(0)
                .getWorkspace()
                .isUsesTanagra())
        .isEqualTo(false);
  }

  @Test
  public void testCreateWorkspace() {
    Workspace workspace = createWorkspace();
    workspace = workspacesController.createWorkspace(workspace).getBody();
    verify(fireCloudService)
        .createWorkspace(
            workspace.getNamespace(), workspace.getTerraName(), registeredTier.getAuthDomainName());
    stubGetWorkspace(
        workspace.getNamespace(),
        workspace.getTerraName(),
        LOGGED_IN_USER_EMAIL,
        WorkspaceAccessLevel.OWNER);
    Workspace retrievedWorkspace =
        workspacesController
            .getWorkspace(workspace.getNamespace(), workspace.getTerraName())
            .getBody()
            .getWorkspace();
    assertThat(retrievedWorkspace.getCreationTime()).isEqualTo(NOW_TIME);
    assertThat(retrievedWorkspace.getLastModifiedTime()).isEqualTo(NOW_TIME);
    assertThat(retrievedWorkspace.getCdrVersionId()).isEqualTo(cdrVersionId);
    assertThat(retrievedWorkspace.getAccessTierShortName())
        .isEqualTo(registeredTier.getShortName());
    assertThat(retrievedWorkspace.getCreatorUser().getUserName()).isEqualTo(LOGGED_IN_USER_EMAIL);
    assertThat(retrievedWorkspace.getName()).isEqualTo(testWorkspaceDisplayName);
    assertThat(retrievedWorkspace.getDisplayName()).isEqualTo(testWorkspaceDisplayName);
    assertThat(retrievedWorkspace.getTerraName()).isEqualTo(testWorkspaceTerraName);
    assertThat(retrievedWorkspace.getResearchPurpose().isDiseaseFocusedResearch()).isTrue();
    assertThat(retrievedWorkspace.getResearchPurpose().getDiseaseOfFocus()).isEqualTo("cancer");
    assertThat(retrievedWorkspace.getResearchPurpose().isMethodsDevelopment()).isTrue();
    assertThat(retrievedWorkspace.getResearchPurpose().isControlSet()).isTrue();
    assertThat(retrievedWorkspace.getResearchPurpose().isAncestry()).isTrue();
    assertThat(retrievedWorkspace.getResearchPurpose().isCommercialPurpose()).isTrue();
    assertThat(retrievedWorkspace.getResearchPurpose().isSocialBehavioral()).isTrue();
    assertThat(retrievedWorkspace.getResearchPurpose().isPopulationHealth()).isTrue();
    assertThat(retrievedWorkspace.getResearchPurpose().isEducational()).isTrue();
    assertThat(retrievedWorkspace.getResearchPurpose().isDrugDevelopment()).isTrue();
    assertThat(retrievedWorkspace.getResearchPurpose().getAdditionalNotes())
        .isEqualTo("additional notes");
    assertThat(retrievedWorkspace.getResearchPurpose().getReasonForAllOfUs())
        .isEqualTo("reason for aou");
    assertThat(retrievedWorkspace.getResearchPurpose().getIntendedStudy())
        .isEqualTo("intended study");
    assertThat(retrievedWorkspace.getResearchPurpose().getAnticipatedFindings())
        .isEqualTo("anticipated findings");
    assertThat(retrievedWorkspace.getNamespace()).isEqualTo(workspace.getNamespace());
    assertThat(retrievedWorkspace.getResearchPurpose().isReviewRequested()).isTrue();
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
  }

  @Test
  public void testCreateWorkspace_resetBillingAccountOnFailedSave() {
    doThrow(new RuntimeException()).when(workspaceDao).save(any(DbWorkspace.class));
    Workspace workspace = createWorkspace();
    TestMockFactory.stubCreateBillingProject(fireCloudService, workspace.getNamespace());

    try {
      workspacesController.createWorkspace(workspace).getBody();
    } catch (Exception e) {
      verify(fireCloudService)
          .updateBillingAccount(workspace.getNamespace(), workspace.getBillingAccountName());
      verify(fireCloudService)
          .updateBillingAccountAsService(
              workspace.getNamespace(), workbenchConfig.billing.initialCreditsBillingAccountName());
      return;
    }
    fail();
  }

  @Test
  public void testCreateWorkspace_doNotUpdateBillingForFreeTier() {
    Workspace workspace = createWorkspace();
    workspace.setBillingAccountName(workbenchConfig.billing.initialCreditsBillingAccountName());

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
            .getWorkspace(workspace.getNamespace(), workspace.getTerraName())
            .getBody()
            .getWorkspace();
    assertThat(workspace2.getResearchPurpose().isApproved()).isNotEqualTo(true);
  }

  @Test
  public void testCreateWorkspace_createDeleteCycleSameName() {
    Workspace workspace = createWorkspace();

    Set<String> uniqueIds = new HashSet<>();
    for (int i = 0; i < 3; i++) {
      workspace = workspacesController.createWorkspace(workspace).getBody();
      uniqueIds.add(workspace.getTerraName());

      workspacesController.deleteWorkspace(workspace.getNamespace(), workspace.getTerraName());
    }
    assertThat(uniqueIds.size()).isEqualTo(1);
  }

  @Test
  public void testCreateWorkspace_archivedCdrVersionThrows() {
    Workspace workspace = createWorkspace();
    workspace.setCdrVersionId(archivedCdrVersionId);
    assertThrows(
        FailedPreconditionException.class, () -> workspacesController.createWorkspace(workspace));
  }

  @Test
  public void testCreateWorkspace_noResearchPurposeThrows() {
    Workspace workspace = createWorkspace();
    workspace.setResearchPurpose(null);
    assertThrows(BadRequestException.class, () -> workspacesController.createWorkspace(workspace));
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
        () -> workspacesController.createWorkspaceAsync(workspace));
  }

  @Test
  public void testCreateWorkspaceAsync_noResearchPurposeThrows() {
    Workspace workspace = createWorkspace();
    workspace.setResearchPurpose(null);
    assertThrows(
        BadRequestException.class, () -> workspacesController.createWorkspaceAsync(workspace));
  }

  @Test
  public void testDuplicateWorkspaceAsync() {
    Workspace workspace = createWorkspace();
    CloneWorkspaceRequest request =
        new CloneWorkspaceRequest().workspace(workspace).includeUserRoles(true);

    // mocks Terra returning workspace info
    stubGetWorkspace(
        workspace.getNamespace(),
        workspace.getTerraName(),
        currentUser.getUsername(),
        WorkspaceAccessLevel.READER);

    WorkspaceOperation operation =
        workspacesController
            .duplicateWorkspaceAsync(workspace.getNamespace(), workspace.getTerraName(), request)
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
        () -> workspacesController.duplicateWorkspaceAsync("foo", "bar", request));
  }

  @Test
  public void testDuplicateWorkspaceAsync_noResearchPurposeThrows() {
    Workspace workspace = createWorkspace();
    CloneWorkspaceRequest request =
        new CloneWorkspaceRequest().workspace(workspace).includeUserRoles(true);
    workspace.setResearchPurpose(null);
    assertThrows(
        BadRequestException.class,
        () -> workspacesController.duplicateWorkspaceAsync("foo", "bar", request));
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
                .setName(workspace.getDisplayName())
                .setFirecloudName(workspace.getTerraName()));
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
        workspace.getTerraName(),
        workspace.getCreatorUser().getUserName(),
        WorkspaceAccessLevel.READER);

    WorkspaceOperation operation =
        workspacesController.getWorkspaceOperation(dbOperation.getId()).getBody();
    assertThat(operation.getId()).isEqualTo(dbOperation.getId());
    assertThat(operation.getStatus()).isEqualTo(WorkspaceOperationStatus.SUCCESS);
    assertThat(operation.getWorkspace()).isNotNull();
    assertThat(operation.getWorkspace().getNamespace()).isEqualTo(workspace.getNamespace());
    assertThat(operation.getWorkspace().getDisplayName()).isEqualTo(workspace.getDisplayName());
    assertThat(operation.getWorkspace().getTerraName()).isEqualTo(workspace.getTerraName());
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
        workspace.getTerraName(),
        currentUser.getUsername(),
        WorkspaceAccessLevel.READER);

    WorkspaceOperation operation =
        workspacesController
            .duplicateWorkspaceAsync(workspace.getNamespace(), workspace.getTerraName(), request)
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
            .terraName("nospacesallowed")
            .namespace("and finally a unique namespace");
    CloneWorkspaceRequest request =
        new CloneWorkspaceRequest().workspace(workspace).includeUserRoles(true);

    // mocks Terra returning workspace info
    stubGetWorkspace(
        workspace.getNamespace(),
        workspace.getTerraName(),
        currentUser.getUsername(),
        WorkspaceAccessLevel.READER);

    WorkspaceOperation operation =
        workspacesController.duplicateWorkspaceAsync(fromWsNs, fromFcName, request).getBody();
    WorkspaceOperation operation2 =
        workspacesController.getWorkspaceOperation(operation.getId()).getBody();
    assertThat(operation2).isEqualTo(operation);

    // mocks Terra returning workspace duplication info
    stubCloneWorkspace(workspace.getNamespace(), workspace.getTerraName(), LOGGED_IN_USER_EMAIL);

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
  public void testGetWorkspaceAccessNotFound() {
    assertThat(workspacesController.getWorkspaceAccess("none").getBody())
        .startsWith("Workspace not found");
  }

  @ParameterizedTest(name = "testGetWorkspaceAccess({0} user access, expected access {1})")
  @MethodSource("workspaceAccessLevels")
  public void testGetWorkspaceAccess(RawlsWorkspaceAccessLevel accessLevel, String expected) {
    Workspace workspace = createWorkspace();
    workspace = workspacesController.createWorkspace(workspace).getBody();
    stubFcGetWorkspace(workspace.getNamespace(), workspace.getTerraName(), accessLevel);
    assertThat(workspacesController.getWorkspaceAccess(workspace.getNamespace()).getBody())
        .startsWith(expected);
  }

  private void stubFcGetWorkspace(
      String namespace, String fcName, RawlsWorkspaceAccessLevel accessLevel) {
    final RawlsWorkspaceResponse toReturn =
        new RawlsWorkspaceResponse()
            .workspace(new RawlsWorkspaceDetails().namespace(namespace).name(fcName))
            .accessLevel(accessLevel);
    when(fireCloudService.getWorkspace(namespace, fcName)).thenReturn(toReturn);
  }

  private static Stream<Arguments> workspaceAccessLevels() {
    return Stream.of(
        Arguments.of(RawlsWorkspaceAccessLevel.OWNER, "OWNER"),
        Arguments.of(RawlsWorkspaceAccessLevel.WRITER, "WRITER"),
        Arguments.of(RawlsWorkspaceAccessLevel.READER, "READER"),
        Arguments.of(
            RawlsWorkspaceAccessLevel.NO_ACCESS,
            "You do not have sufficient permissions to access workspace"));
  }

  @Test
  public void testDeleteWorkspace() {
    Workspace workspace = createWorkspace();
    workspace = workspacesController.createWorkspace(workspace).getBody();

    workspacesController.deleteWorkspace(workspace.getNamespace(), workspace.getTerraName());
    verify(mockWorkspaceAuditor).fireDeleteAction(any(DbWorkspace.class));
    try {
      workspacesController.getWorkspace(workspace.getNamespace(), workspace.getTerraName());
      fail("NotFoundException expected");
    } catch (NotFoundException e) {
      // expected
    }
  }

  @Test
  public void testDeleteWorkspace_creatorCanDeleteWithoutOwnerAccess() {
    Workspace workspace = createWorkspace();
    workspace = workspacesController.createWorkspace(workspace).getBody();

    // Simulate creator no longer having OWNER access in Rawls.
    stubGetWorkspace(
        workspace.getNamespace(),
        workspace.getTerraName(),
        workspace.getCreatorUser().getUserName(),
        WorkspaceAccessLevel.READER);

    workspacesController.deleteWorkspace(workspace.getNamespace(), workspace.getTerraName());
    verify(mockWorkspaceAuditor).fireDeleteAction(any(DbWorkspace.class));
  }

  @Test
  public void testDeleteWorkspace_nonCreatorRequiresOwnerAccess() {
    Workspace workspace = createWorkspace();
    workspace = workspacesController.createWorkspace(workspace).getBody();
    final String namespace = workspace.getNamespace();
    final String terraName = workspace.getTerraName();

    currentUser = createUser("notcreator@gmail.com");
    stubGetWorkspace(
        namespace,
        terraName,
        workspace.getCreatorUser().getUserName(),
        WorkspaceAccessLevel.READER);

    assertThrows(
        ForbiddenException.class, () -> workspacesController.deleteWorkspace(namespace, terraName));
  }

  @Test
  public void testUpdateWorkspace() throws Exception {
    Workspace ws = createWorkspace();
    ws = workspacesController.createWorkspace(ws).getBody();
    verify(fireCloudService, times(1))
        .updateBillingAccount(ws.getNamespace(), ws.getBillingAccountName());

    ws.setName("updated-name");
    ws.setDisplayName("updated-name");
    UpdateWorkspaceRequest request = new UpdateWorkspaceRequest();
    ws.setBillingAccountName("update-billing-account");
    request.setWorkspace(ws);
    Workspace updated =
        workspacesController
            .updateWorkspace(ws.getNamespace(), ws.getTerraName(), request)
            .getBody();
    ws.setEtag(updated.getEtag());
    assertThat(updated).isEqualTo(ws);

    verify(fireCloudService, times(1))
        .updateBillingAccount(ws.getNamespace(), "update-billing-account");

    ws.setName("updated-name2");
    ws.setDisplayName("updated-name2");
    updated =
        workspacesController
            .updateWorkspace(ws.getNamespace(), ws.getTerraName(), request)
            .getBody();
    ws.setEtag(updated.getEtag());
    assertThat(updated).isEqualTo(ws);
    Workspace got =
        workspacesController
            .getWorkspace(ws.getNamespace(), ws.getTerraName())
            .getBody()
            .getWorkspace();
    assertThat(got).isEqualTo(ws);
  }

  @Test
  public void testUpdateWorkspace_freeTierBilling_noCreditsRemaining() {
    Workspace workspace = createWorkspace();
    workspace = workspacesController.createWorkspace(workspace).getBody();

    doReturn(false)
        .when(mockInitialCreditsService)
        .userHasRemainingInitialCredits(
            argThat(dbUser -> dbUser.getUserId() == currentUser.getUserId()));

    UpdateWorkspaceRequest request = new UpdateWorkspaceRequest();
    workspace.setBillingAccountName(workbenchConfig.billing.initialCreditsBillingAccountName());
    request.setWorkspace(workspace);
    Workspace response =
        workspacesController
            .updateWorkspace(workspace.getNamespace(), workspace.getTerraName(), request)
            .getBody();

    assert response != null;
    assertThat(response.getInitialCredits().isExhausted()).isEqualTo(true);
  }

  @Test
  public void testUpdateWorkspace_freeTierBilling_hasCreditsRemaining() {
    Workspace workspace = createWorkspace();
    workspace = workspacesController.createWorkspace(workspace).getBody();

    DbWorkspace dbWorkspace =
        workspaceDao.findByWorkspaceNamespaceAndFirecloudNameAndActiveStatus(
            workspace.getNamespace(),
            workspace.getTerraName(),
            DbStorageEnums.workspaceActiveStatusToStorage(WorkspaceActiveStatus.ACTIVE));
    doReturn(true)
        .when(mockInitialCreditsService)
        .userHasRemainingInitialCredits(
            argThat(dbUser -> dbUser.getUserId() == currentUser.getUserId()));

    UpdateWorkspaceRequest request = new UpdateWorkspaceRequest();
    workspace.setBillingAccountName(workbenchConfig.billing.initialCreditsBillingAccountName());
    workspace.setEtag("\"1\"");
    request.setWorkspace(workspace);
    Workspace response =
        workspacesController
            .updateWorkspace(workspace.getNamespace(), workspace.getTerraName(), request)
            .getBody();

    assert response != null;
    assertThat(response.getInitialCredits().isExhausted()).isEqualTo(false);
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
            .updateWorkspace(ws.getNamespace(), ws.getTerraName(), request)
            .getBody()
            .getResearchPurpose();

    assertThat(updatedRp.isDiseaseFocusedResearch()).isFalse();
    assertThat(updatedRp.getDiseaseOfFocus()).isNull();
    assertThat(updatedRp.isMethodsDevelopment()).isFalse();
    assertThat(updatedRp.isControlSet()).isFalse();
    assertThat(updatedRp.isAncestry()).isFalse();
    assertThat(updatedRp.isCommercialPurpose()).isFalse();
    assertThat(updatedRp.isPopulationHealth()).isFalse();
    assertThat(updatedRp.isSocialBehavioral()).isFalse();
    assertThat(updatedRp.isDrugDevelopment()).isFalse();
    assertThat(updatedRp.getAdditionalNotes()).isNull();
    assertThat(updatedRp.isReviewRequested()).isFalse();
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
              ws.getNamespace(),
              ws.getTerraName(),
              ws.getCreatorUser().getUserName(),
              WorkspaceAccessLevel.READER);
          workspacesController.updateWorkspace(ws.getNamespace(), ws.getTerraName(), request);
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
              ws.getNamespace(),
              ws.getTerraName(),
              ws.getCreatorUser().getUserName(),
              WorkspaceAccessLevel.WRITER);
          workspacesController.updateWorkspace(ws.getNamespace(), ws.getTerraName(), request);
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
          workspacesController.updateWorkspace(ws.getNamespace(), ws.getTerraName(), request);
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
          workspacesController.updateWorkspace(ws.getNamespace(), ws.getTerraName(), request);
          // Still using the initial now-stale etag; this should throw.
          request.setWorkspace(
              new Workspace()
                  .name("updated-name2")
                  .billingAccountName("billing-account")
                  .accessTierShortName(ws.getAccessTierShortName())
                  .etag(ws.getEtag()));
          workspacesController.updateWorkspace(ws.getNamespace(), ws.getTerraName(), request);
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
        workspacesController.updateWorkspace(ws.getNamespace(), ws.getTerraName(), request);
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
        originalWorkspace.getNamespace(), originalWorkspace.getTerraName(), shareWorkspaceRequest);

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
    modWorkspace.setName("Cloned");
    modWorkspace.setDisplayName("Cloned");
    modWorkspace.setTerraName("cloned");
    modWorkspace.setNamespace("cloned-ns");
    modWorkspace.setResearchPurpose(modPurpose);
    modWorkspace.setBillingAccountName(newBillingAccountName);

    final CloneWorkspaceRequest req = new CloneWorkspaceRequest();
    req.setWorkspace(modWorkspace);
    final RawlsWorkspaceDetails clonedFirecloudWorkspace =
        stubCloneWorkspace(
            modWorkspace.getNamespace(), modWorkspace.getTerraName(), LOGGED_IN_USER_EMAIL);
    // Assign the same bucket name as the mock-factory's bucket name, so the clone vs. get equality
    // assertion below will pass.
    clonedFirecloudWorkspace.setBucketName(TestMockFactory.WORKSPACE_BUCKET_NAME);
    final Workspace clonedWorkspace =
        workspacesController
            .cloneWorkspace(originalWorkspace.getNamespace(), originalWorkspace.getTerraName(), req)
            .getBody()
            .getWorkspace();
    verify(mockWorkspaceAuditor).fireDuplicateAction(anyLong(), anyLong(), any(Workspace.class));
    verify(fireCloudService)
        .updateBillingAccount(clonedWorkspace.getNamespace(), newBillingAccountName);

    // Stub out the FC service getWorkspace, since that's called by workspacesController.
    stubGetWorkspace(clonedFirecloudWorkspace, WorkspaceAccessLevel.WRITER);
    final Workspace retrievedWorkspace =
        workspacesController
            .getWorkspace(clonedWorkspace.getNamespace(), clonedWorkspace.getTerraName())
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
    modWorkspace.setName("Cloned");
    modWorkspace.setDisplayName("Cloned");
    modWorkspace.setTerraName("cloned");
    modWorkspace.setNamespace("cloned-ns");
    modWorkspace.setBillingAccountName("cloned-billing-account");
    modWorkspace.setResearchPurpose(new ResearchPurpose());

    final CloneWorkspaceRequest req = new CloneWorkspaceRequest();
    req.setWorkspace(modWorkspace);
    stubCloneWorkspace(
        modWorkspace.getNamespace(), modWorkspace.getTerraName(), LOGGED_IN_USER_EMAIL);

    doThrow(new RuntimeException()).when(workspaceDao).save(any(DbWorkspace.class));

    try {
      workspacesController
          .cloneWorkspace(originalWorkspace.getNamespace(), originalWorkspace.getTerraName(), req)
          .getBody()
          .getWorkspace();
    } catch (Exception e) {
      verify(fireCloudService)
          .updateBillingAccount(modWorkspace.getNamespace(), modWorkspace.getBillingAccountName());
      return;
    }
    fail();
  }

  @Test
  public void testCloneWorkspace_doNotUpdateBillingForFreeTier() {
    Workspace originalWorkspace = createWorkspace();
    originalWorkspace = workspacesController.createWorkspace(originalWorkspace).getBody();

    final Workspace modWorkspace = new Workspace();
    modWorkspace.setName("Cloned");
    modWorkspace.setDisplayName("Cloned");
    modWorkspace.setTerraName("cloned");
    modWorkspace.setNamespace("cloned-ns");
    modWorkspace.setBillingAccountName(workbenchConfig.billing.initialCreditsBillingAccountName());
    modWorkspace.setResearchPurpose(new ResearchPurpose());

    final CloneWorkspaceRequest req = new CloneWorkspaceRequest();
    req.setWorkspace(modWorkspace);
    stubCloneWorkspace(
        modWorkspace.getNamespace(), modWorkspace.getTerraName(), LOGGED_IN_USER_EMAIL);

    workspacesController.cloneWorkspace(
        originalWorkspace.getNamespace(), originalWorkspace.getTerraName(), req);
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
          DbAccessTier altAccessTier = accessTierDao.save(createControlledTier());
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
          modWorkspace.setName("Cloned");
          modWorkspace.setDisplayName("Cloned");
          modWorkspace.setTerraName("cloned");
          modWorkspace.setNamespace("cloned-ns");
          modWorkspace.setBillingAccountName(
              workbenchConfig.billing.initialCreditsBillingAccountName());
          modWorkspace.setResearchPurpose(new ResearchPurpose());
          modWorkspace.setCdrVersionId(String.valueOf(altCdrVersion.getCdrVersionId()));
          final CloneWorkspaceRequest req = new CloneWorkspaceRequest();
          req.setWorkspace(modWorkspace);
          stubCloneWorkspace(
              modWorkspace.getNamespace(), modWorkspace.getTerraName(), LOGGED_IN_USER_EMAIL);
          workspacesController.cloneWorkspace(
              originalWorkspace.getNamespace(), originalWorkspace.getTerraName(), req);
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
    modWorkspace.setName("Cloned");
    modWorkspace.setDisplayName("Cloned");
    modWorkspace.setTerraName("cloned");
    modWorkspace.setNamespace("cloned-ns");
    modWorkspace.setBillingAccountName("billing-account");

    ResearchPurpose modPurpose = new ResearchPurpose();
    modPurpose.setAncestry(true);
    modWorkspace.setResearchPurpose(modPurpose);

    req.setWorkspace(modWorkspace);
    stubCloneWorkspace(
        modWorkspace.getNamespace(), modWorkspace.getTerraName(), "cloner@gmail.com");

    Workspace workspace2 =
        workspacesController
            .cloneWorkspace(workspace.getNamespace(), workspace.getTerraName(), req)
            .getBody()
            .getWorkspace();

    assertThat(workspace2.getCreatorUser().getUserName()).isEqualTo(cloner.getUsername());
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
            .name("Cloned")
            .displayName("Cloned")
            .terraName("cloned")
            .namespace("cloned-ns")
            .billingAccountName("billing-account")
            .researchPurpose(workspace.getResearchPurpose())
            .cdrVersionId(cdrVersionId2);
    stubCloneWorkspace(
        modWorkspace.getNamespace(), modWorkspace.getTerraName(), "cloner@gmail.com");

    CloneWorkspaceRequest req = new CloneWorkspaceRequest().workspace(modWorkspace);
    Workspace workspace2 =
        workspacesController
            .cloneWorkspace(workspace.getNamespace(), workspace.getTerraName(), req)
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
              modWorkspace.getNamespace(), modWorkspace.getTerraName(), "cloner@gmail.com");

          workspacesController.cloneWorkspace(
              workspace.getNamespace(),
              workspace.getTerraName(),
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
              modWorkspace.getNamespace(), modWorkspace.getTerraName(), "cloner@gmail.com");

          workspacesController.cloneWorkspace(
              workspace.getNamespace(),
              workspace.getTerraName(),
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
              modWorkspace.getNamespace(), modWorkspace.getTerraName(), "cloner@gmail.com");
          workspacesController.cloneWorkspace(
              workspace.getNamespace(),
              workspace.getTerraName(),
              new CloneWorkspaceRequest().workspace(modWorkspace));
        });
  }

  @Test
  public void testCloneWorkspaceIncludeUserRoles() {
    stubFcGetGroup();
    DbUser cloner = createUser("cloner@gmail.com");
    DbUser reader = createUser("reader@gmail.com");
    DbUser writer = createUser("writer@gmail.com");
    DbCdrVersion controlledTierCdr = createControlledTierCdrVersion(2);
    accessTierDao.save(controlledTierCdr.getAccessTier());
    cdrVersionDao.save(controlledTierCdr);

    Workspace originalWorkspace =
        workspacesController
            .createWorkspace(
                createWorkspace().cdrVersionId(String.valueOf(controlledTierCdr.getCdrVersionId())))
            .getBody();

    stubFcUpdateWorkspaceACL();

    // setting the "include user roles" flag will update the list of workspace collaborators after
    // cloning/duplication.

    // The original Workspace has "cloner" as READER, LOGGED_IN_USER_EMAIL as OWNER, and an
    // additional READER and WRITER.

    // We show that these are retained in the new Workspace, with the exception of:
    // a. the "cloner" user who called this method - they get upgraded to OWNER.
    // b. the "published" group - it is removed

    RawlsWorkspaceACL originalAcl =
        createWorkspaceACL(
            new JSONObject()
                .put(
                    "cloner@gmail.com",
                    new JSONObject()
                        .put("accessLevel", "READER")
                        .put("canCompute", false)
                        .put("canShare", false))
                .put(
                    "reader@gmail.com",
                    new JSONObject()
                        .put("accessLevel", "READER")
                        .put("canCompute", false)
                        .put("canShare", false))
                // this is how we indicate that a workspace has been published
                .put(
                    workspaceService.getPublishedWorkspacesGroupEmail(),
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

    // cloning/duplication is not atomic. When the workspace is first created, it will only have
    // creator=OWNER access, like a newly-created workspace.  We add other permissions later.

    RawlsWorkspaceACL clonedAclBeforeUpdate =
        createWorkspaceACL(
            new JSONObject()
                .put(
                    "cloner@gmail.com",
                    new JSONObject()
                        .put("accessLevel", "OWNER")
                        .put("canCompute", true)
                        .put("canShare", true)));

    when(fireCloudService.getWorkspaceAclAsService(
            originalWorkspace.getNamespace(), originalWorkspace.getTerraName()))
        .thenReturn(originalAcl);
    when(fireCloudService.getWorkspaceAclAsService("cloned-ns", "cloned"))
        .thenReturn(clonedAclBeforeUpdate);

    // cloner is now OWNER, and the "published" group has NO_ACCESS
    List<RawlsWorkspaceACLUpdate> expectedCollaboratorsAfterUpdate =
        convertUserRolesToUpdateAclRequestList(
            Set.of(
                new UserRole().email(cloner.getUsername()).role(WorkspaceAccessLevel.OWNER),
                new UserRole().email(LOGGED_IN_USER_EMAIL).role(WorkspaceAccessLevel.OWNER),
                new UserRole().email(reader.getUsername()).role(WorkspaceAccessLevel.READER),
                new UserRole().email(writer.getUsername()).role(WorkspaceAccessLevel.WRITER),
                new UserRole()
                    .email(workspaceService.getPublishedWorkspacesGroupEmail())
                    .role(WorkspaceAccessLevel.NO_ACCESS)));

    currentUser = cloner;

    stubCloneWorkspace("cloned-ns", "cloned", cloner.getUsername());

    CloneWorkspaceRequest cloneRequest =
        new CloneWorkspaceRequest()
            .includeUserRoles(true)
            .workspace(
                new Workspace()
                    .namespace("cloned-ns")
                    .name("cloned")
                    .researchPurpose(originalWorkspace.getResearchPurpose())
                    .billingAccountName("billing-account")
                    .cdrVersionId(String.valueOf(controlledTierCdr.getCdrVersionId())));

    Workspace clonedWorkspace =
        workspacesController
            .cloneWorkspace(
                originalWorkspace.getNamespace(), originalWorkspace.getTerraName(), cloneRequest)
            .getBody()
            .getWorkspace();

    assertThat(clonedWorkspace.getCreatorUser().getUserName()).isEqualTo(cloner.getUsername());

    verify(fireCloudService)
        .updateWorkspaceACL(
            eq("cloned-ns"),
            eq("cloned"),
            // Accept the ACL update list in any order.
            assertArg(
                arg ->
                    assertThat(arg).containsExactlyElementsIn(expectedCollaboratorsAfterUpdate)));
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
          modWorkspace.setName("Cloned");
          modWorkspace.setDisplayName("Cloned");
          modWorkspace.setTerraName("cloned");
          modWorkspace.setNamespace("cloned-ns");
          req.setWorkspace(modWorkspace);
          // Missing research purpose.
          workspacesController.cloneWorkspace(
              workspace.getNamespace(), workspace.getTerraName(), req);
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
          when(fireCloudService.getWorkspace(workspace.getNamespace(), workspace.getTerraName()))
              .thenThrow(new NotFoundException());
          CloneWorkspaceRequest req = new CloneWorkspaceRequest();
          Workspace modWorkspace = new Workspace();
          modWorkspace.setName("Cloned");
          modWorkspace.setDisplayName("Cloned");
          modWorkspace.setTerraName("cloned");
          modWorkspace.setNamespace("cloned-ns");
          req.setWorkspace(modWorkspace);
          ResearchPurpose modPurpose = new ResearchPurpose();
          modPurpose.setAncestry(true);
          modWorkspace.setResearchPurpose(modPurpose);
          workspacesController.cloneWorkspace(
              workspace.getNamespace(), workspace.getTerraName(), req);
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
                workspace.getNamespace(), workspace.getTerraName(), shareWorkspaceRequest)
            .getBody();

    verify(mockWorkspaceAuditor).fireCollaborateAction(anyLong(), anyMap());

    Workspace workspace2 =
        workspacesController
            .getWorkspace(workspace.getNamespace(), workspace.getTerraName())
            .getBody()
            .getWorkspace();
    assertThat(shareResp.getWorkspaceEtag()).isEqualTo(workspace2.getEtag());

    List<RawlsWorkspaceACLUpdate> updateACLRequestList =
        convertUserRolesToUpdateAclRequestList(shareWorkspaceRequest.getItems());
    verify(fireCloudService).updateWorkspaceACL(any(), any(), eq(updateACLRequestList));
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
        workspace.getNamespace(), workspace.getTerraName(), shareWorkspaceRequest);

    verify(fireCloudService, times(1))
        .addOwnerToBillingProject(ownerUser.getUsername(), workspace.getNamespace());
    verify(fireCloudService, never()).addOwnerToBillingProject(eq(writerUser.getUsername()), any());
    verify(fireCloudService, never()).removeOwnerFromBillingProjectAsService(any(), any());
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
        workspace.getNamespace(), workspace.getTerraName(), shareWorkspaceRequest);

    verify(fireCloudService, times(1))
        .removeOwnerFromBillingProjectAsService(ownerUser.getUsername(), workspace.getNamespace());
    verify(fireCloudService, never())
        .removeOwnerFromBillingProjectAsService(eq(writerUser.getUsername()), any());
    verify(fireCloudService, never())
        .removeOwnerFromBillingProjectAsService(eq(currentUser.getUsername()), any());
  }

  @Test
  public void testShareWorkspacePatch_publishedWorkspace() {
    DbCdrVersion ctCdrVersion = createControlledTierCdrVersion(5L);
    accessTierDao.save(ctCdrVersion.getAccessTier());
    cdrVersionDao.save(ctCdrVersion);

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
        workspace.getNamespace(), workspace.getTerraName(), shareWorkspaceRequest);

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
                workspace.getNamespace(), workspace.getTerraName(), shareWorkspaceRequest));
  }

  @Test
  public void testUnshareWorkspace() {
    stubFcGetGroup();
    DbUser writerUser = createAndSaveUser("writerfriend@gmail.com", 124L);
    DbUser readerUser = createAndSaveUser("readerfriend@gmail.com", 125L);

    Workspace workspace = createWorkspace();
    workspace = workspacesController.createWorkspace(workspace).getBody();

    // Mock firecloud ACL
    RawlsWorkspaceACL workspaceACL =
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
    when(fireCloudService.getWorkspaceAclAsService(any(), any())).thenReturn(workspaceACL);

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
                workspace.getNamespace(), workspace.getTerraName(), shareWorkspaceRequest)
            .getBody();
    Workspace workspace2 =
        workspacesController
            .getWorkspace(workspace.getNamespace(), workspace.getTerraName())
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
        workspace.getNamespace(), workspace.getTerraName(), shareWorkspaceRequest1);

    // Simulate time between API calls to trigger last-modified/@Version changes.
    fakeClock.increment(1000);
    ShareWorkspaceRequest shareWorkspaceRequest2 = new ShareWorkspaceRequest();
    // Use the initial etag, not the updated value from shareWorkspace.
    shareWorkspaceRequest2.setWorkspaceEtag(workspace.getEtag());
    assertThrows(
        ConflictException.class,
        () ->
            workspacesController.shareWorkspacePatch(
                workspace.getNamespace(), workspace.getTerraName(), shareWorkspaceRequest2));
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
        () ->
            workspacesController.shareWorkspacePatch(
                workspace.getNamespace(), workspace.getTerraName(), shareWorkspaceRequest));
  }

  @Test
  public void testEmptyFireCloudWorkspaces() {
    when(fireCloudService.listWorkspaces()).thenReturn(new ArrayList<>());
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
            .getFirecloudWorkspaceUserRoles(workspace.getNamespace(), workspace.getTerraName())
            .getBody();

    assertThat(resp.getItems())
        .containsExactly(
            new UserRole().email(currentUser.getUsername()).role(WorkspaceAccessLevel.OWNER));
  }

  @Test
  public void testGetFirecloudWorkspaceUserRoles_noAccess() {
    Workspace workspace = createWorkspace();
    when(fireCloudService.getWorkspace(workspace.getNamespace(), workspace.getTerraName()))
        .thenThrow(new ForbiddenException());

    assertThrows(
        ForbiddenException.class,
        () ->
            workspacesController
                .getFirecloudWorkspaceUserRoles(workspace.getNamespace(), workspace.getTerraName())
                .getBody());
  }

  @Test
  public void testGetBillingUsage() {
    Double cost = 150.50;
    Workspace ws = createWorkspace();
    ws = workspacesController.createWorkspace(ws).getBody();
    stubGetWorkspace(
        ws.getNamespace(),
        ws.getTerraName(),
        ws.getCreatorUser().getUserName(),
        WorkspaceAccessLevel.OWNER);
    when(mockInitialCreditsService.getWorkspaceInitialCreditsUsage(any())).thenReturn(cost);

    WorkspaceBillingUsageResponse workspaceBillingUsageResponse =
        workspacesController.getBillingUsage(ws.getNamespace(), ws.getTerraName()).getBody();
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
              ws.getNamespace(),
              ws.getTerraName(),
              ws.getCreatorUser().getUserName(),
              WorkspaceAccessLevel.READER);
          workspacesController.getBillingUsage(ws.getNamespace(), ws.getTerraName());
        });
  }

  @Test
  public void testGetBillingUsageWithNoSpend() {
    Workspace ws = createWorkspace();
    ws = workspacesController.createWorkspace(ws).getBody();
    stubGetWorkspace(
        ws.getNamespace(),
        ws.getTerraName(),
        ws.getCreatorUser().getUserName(),
        WorkspaceAccessLevel.OWNER);
    WorkspaceBillingUsageResponse workspaceBillingUsageResponse =
        workspacesController.getBillingUsage(ws.getNamespace(), ws.getTerraName()).getBody();
    assertThat(workspaceBillingUsageResponse.getCost()).isEqualTo(0.0d);
  }

  @Test
  public void testPublishCommunityWorkspace_ByUserWithWriterAccess() {
    Workspace ws = createWorkspaceAndGrantAccess(WorkspaceAccessLevel.WRITER);
    String wsNamespace = ws.getNamespace();
    assertThrows(
        ForbiddenException.class,
        () -> {
          workspacesController.publishCommunityWorkspace(wsNamespace);
        });
  }

  @Test
  public void testPublishCommunityWorkspace_ByUserWithReaderAccess() {
    Workspace ws = createWorkspaceAndGrantAccess(WorkspaceAccessLevel.READER);
    String wsNamespace = ws.getNamespace();
    assertThrows(
        ForbiddenException.class,
        () -> {
          workspacesController.publishCommunityWorkspace(wsNamespace);
        });
  }

  @Test
  public void testPublishCommunityWorkspace() {
    Workspace ws = createWorkspaceAndGrantAccess(WorkspaceAccessLevel.OWNER);
    String wsNamespace = ws.getNamespace();
    doNothing().when(workspaceService).publishCommunityWorkspace(any(DbWorkspace.class));
    workspacesController.publishCommunityWorkspace(wsNamespace);
    verify(workspaceService).publishCommunityWorkspace(any(DbWorkspace.class));
    verify(mockWorkspaceAuditor).firePublishAction(anyLong());
  }

  @Test
  public void testGetMigrationBucketContents() {
    Workspace workspace = createWorkspace();
    workspace = workspacesController.createWorkspace(workspace).getBody();

    String namespace = workspace.getNamespace();
    String terraName = workspace.getTerraName();

    MigrationBucketContentsResponse mockResponse =
        new MigrationBucketContentsResponse()
            .bucketName("test-bucket")
            .folders(List.of("notebooks/", "data/", "results/"));

    when(workspaceMigrationService.getBucketContents(namespace, terraName))
        .thenReturn(mockResponse);

    MigrationBucketContentsResponse response =
        workspacesController.getMigrationBucketContents(namespace, terraName).getBody();

    assertThat(response.getBucketName()).isEqualTo("test-bucket");
    assertThat(response.getFolders()).containsExactly("notebooks/", "data/", "results/");

    verify(workspaceMigrationService).getBucketContents(namespace, terraName);
  }
}
