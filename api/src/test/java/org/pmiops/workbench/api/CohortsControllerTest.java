package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.pmiops.workbench.utils.TestMockFactory.createDefaultCdrVersion;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.access.AccessModuleService;
import org.pmiops.workbench.access.AccessTierServiceImpl;
import org.pmiops.workbench.actionaudit.auditors.BillingProjectAuditor;
import org.pmiops.workbench.actionaudit.auditors.UserServiceAuditor;
import org.pmiops.workbench.actionaudit.auditors.WorkspaceAuditor;
import org.pmiops.workbench.actionaudit.bucket.BucketAuditQueryServiceImpl;
import org.pmiops.workbench.billing.FreeTierBillingService;
import org.pmiops.workbench.cdr.CdrVersionService;
import org.pmiops.workbench.cdr.ConceptBigQueryService;
import org.pmiops.workbench.cloudtasks.TaskQueueService;
import org.pmiops.workbench.cohortbuilder.CohortBuilderService;
import org.pmiops.workbench.cohortbuilder.CohortQueryBuilder;
import org.pmiops.workbench.cohortbuilder.mapper.CohortBuilderMapper;
import org.pmiops.workbench.cohortreview.CohortReviewServiceImpl;
import org.pmiops.workbench.cohortreview.ReviewQueryBuilder;
import org.pmiops.workbench.cohortreview.mapper.CohortReviewMapperImpl;
import org.pmiops.workbench.cohortreview.mapper.ParticipantCohortAnnotationMapper;
import org.pmiops.workbench.cohortreview.mapper.ParticipantCohortStatusMapper;
import org.pmiops.workbench.cohorts.CohortCloningService;
import org.pmiops.workbench.cohorts.CohortFactoryImpl;
import org.pmiops.workbench.cohorts.CohortMapperImpl;
import org.pmiops.workbench.cohorts.CohortService;
import org.pmiops.workbench.conceptset.ConceptSetService;
import org.pmiops.workbench.conceptset.mapper.ConceptSetMapperImpl;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.dataset.DataSetService;
import org.pmiops.workbench.dataset.mapper.DataSetMapperImpl;
import org.pmiops.workbench.db.dao.AccessTierDao;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.CohortReviewDao;
import org.pmiops.workbench.db.dao.ConceptSetDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.exfiltration.ObjectNameLengthServiceImpl;
import org.pmiops.workbench.exfiltration.impl.EgressObjectLengthsRemediationService;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.google.CloudBillingClient;
import org.pmiops.workbench.google.CloudStorageClient;
import org.pmiops.workbench.google.DirectoryService;
import org.pmiops.workbench.iam.IamService;
import org.pmiops.workbench.leonardo.LeonardoApiClient;
import org.pmiops.workbench.mail.MailService;
import org.pmiops.workbench.model.Cohort;
import org.pmiops.workbench.model.CohortDefinition;
import org.pmiops.workbench.model.DuplicateCohortRequest;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.ResearchPurpose;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.monitoring.LogsBasedMetricServiceFakeImpl;
import org.pmiops.workbench.monitoring.MonitoringService;
import org.pmiops.workbench.moodle.MoodleService;
import org.pmiops.workbench.notebooks.NotebooksServiceImpl;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceACL;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceAccessEntry;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceDetails;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceResponse;
import org.pmiops.workbench.test.CohortDefinitions;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.test.FakeLongRandom;
import org.pmiops.workbench.testconfig.UserServiceTestConfiguration;
import org.pmiops.workbench.utils.TestMockFactory;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.utils.mappers.FirecloudMapper;
import org.pmiops.workbench.utils.mappers.FirecloudMapperImpl;
import org.pmiops.workbench.utils.mappers.UserMapperImpl;
import org.pmiops.workbench.utils.mappers.WorkspaceMapperImpl;
import org.pmiops.workbench.workspaces.WorkspaceAuthService;
import org.pmiops.workbench.workspaces.WorkspaceOperationMapper;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.pmiops.workbench.workspaces.WorkspaceServiceImpl;
import org.pmiops.workbench.workspaces.resources.UserRecentResourceService;
import org.pmiops.workbench.workspaces.resources.WorkspaceResourceMapperImpl;
import org.pmiops.workbench.workspaces.resources.WorkspaceResourcesServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
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
public class CohortsControllerTest {

  private static final Instant NOW = Instant.now();
  private static final FakeClock CLOCK = new FakeClock(NOW, ZoneId.systemDefault());
  private static final String CDR_VERSION_NAME = "cdrVersion";
  private static final String WORKSPACE_NAME = "workspace";
  private static final String WORKSPACE_NAME_2 = "workspace2";
  private static final String WORKSPACE_NAMESPACE = "ns";
  private static final String COHORT_NAME = "cohort";
  private static final String CREATOR_EMAIL = "bob@gmail.com";
  public static final String UPDATED_COHORT_NAME = "updatedCohortName";
  public static final String DUPLICATED_COHORT_NAME = "Duplicated Cohort Name";

  private static DbUser currentUser;

  @Autowired WorkspacesController workspacesController;
  @Autowired CohortsController cohortsController;
  @Autowired ConceptSetsController conceptSetsController;

  Workspace workspace;
  Workspace workspace2;
  DbCdrVersion cdrVersion;
  CohortDefinition cohortDefinition;
  String cohortCriteria;
  String badCohortCriteria;

  @Autowired CdrVersionService cdrVersionService;
  @Autowired CloudStorageClient cloudStorageClient;
  @Autowired CloudBillingClient cloudBillingClient;
  @Autowired MoodleService moodleService;
  @Autowired FireCloudService fireCloudService;
  @Autowired UserRecentResourceService userRecentResourceService;
  @Autowired UserService userService;
  @Autowired WorkspaceService workspaceService;
  @Autowired WorkspaceAuthService workspaceAuthService;

  @Autowired AccessTierDao accessTierDao;
  @Autowired CdrVersionDao cdrVersionDao;
  @Autowired CohortDao cohortDao;
  @Autowired CohortReviewDao cohortReviewDao;
  @Autowired ConceptSetDao conceptSetDao;
  @Autowired DataSetService dataSetService;
  @Autowired UserDao userDao;

  @Autowired FirecloudMapper firecloudMapper;

  @TestConfiguration
  @Import({
    FakeClockConfiguration.class,
    CohortCloningService.class,
    CohortFactoryImpl.class,
    CohortMapperImpl.class,
    CohortReviewMapperImpl.class,
    CohortReviewServiceImpl.class,
    CohortsController.class,
    CommonMappers.class,
    ConceptSetMapperImpl.class,
    ConceptSetService.class,
    ConceptSetsController.class,
    DataSetMapperImpl.class,
    FirecloudMapperImpl.class,
    LogsBasedMetricServiceFakeImpl.class,
    NotebooksServiceImpl.class,
    UserMapperImpl.class,
    UserServiceTestConfiguration.class,
    WorkspaceMapperImpl.class,
    WorkspaceResourceMapperImpl.class,
    WorkspaceResourcesServiceImpl.class,
    WorkspaceServiceImpl.class,
    WorkspaceAuthService.class,
    WorkspacesController.class,
    AccessTierServiceImpl.class,
    ObjectNameLengthServiceImpl.class,
    BucketAuditQueryServiceImpl.class,
    EgressObjectLengthsRemediationService.class,
  })
  @MockBean({
    AccessModuleService.class,
    BigQueryService.class,
    BillingProjectAuditor.class,
    CdrVersionService.class,
    CloudBillingClient.class,
    CloudStorageClient.class,
    CohortBuilderMapper.class,
    CohortBuilderService.class,
    CohortService.class,
    CohortQueryBuilder.class,
    MoodleService.class,
    ConceptBigQueryService.class,
    DataSetService.class,
    DirectoryService.class,
    FireCloudService.class,
    FreeTierBillingService.class,
    LeonardoApiClient.class,
    IamService.class,
    MailService.class,
    MonitoringService.class,
    ParticipantCohortAnnotationMapper.class,
    ParticipantCohortStatusMapper.class,
    ReviewQueryBuilder.class,
    TaskQueueService.class,
    UserRecentResourceService.class,
    UserServiceAuditor.class,
    WorkspaceAuditor.class,
    WorkspaceOperationMapper.class,
    EgressObjectLengthsRemediationService.class,
  })
  static class Configuration {
    @Bean
    Clock clock() {
      return CLOCK;
    }

    @Bean
    Random random() {
      return new FakeLongRandom(123);
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    DbUser user() {
      return currentUser;
    }

    @Bean
    WorkbenchConfig workbenchConfig() {
      WorkbenchConfig workbenchConfig = WorkbenchConfig.createEmptyConfig();
      workbenchConfig.billing.accountId = "free-tier";
      return workbenchConfig;
    }
  }

  @BeforeEach
  public void setUp() {
    TestMockFactory.stubCreateBillingProject(fireCloudService);
    TestMockFactory.stubCreateFcWorkspace(fireCloudService);
    TestMockFactory.stubPollCloudBillingLinked(cloudBillingClient, "billing-account");
    DbUser user = new DbUser();
    user.setUsername(CREATOR_EMAIL);
    user.setUserId(123L);
    user.setDisabled(false);
    user = userDao.save(user);
    currentUser = user;

    cdrVersion = createDefaultCdrVersion();
    accessTierDao.save(cdrVersion.getAccessTier());
    cdrVersion.setName(CDR_VERSION_NAME);
    cdrVersion = cdrVersionDao.save(cdrVersion);

    cohortDefinition = CohortDefinitions.males();
    cohortCriteria = new Gson().toJson(cohortDefinition);
    badCohortCriteria =
        "{\n"
            + "  \"includes\": [\n"
            + "    {\n"
            + "      \"id\": \"includes_7dhyhvbch\",\n"
            + "      \"items\": [\n"
            + "        {\n"
            + "          \"id\": \"items_38kgpbn2e\",\n"
            + "          \"type\": \"DRUG\",\n"
            + "          \"searchParameters\": [\n"
            + "            {\n"
            + "              \"parameterId\": \"param11332011819\",\n"
            + "              \"name\": \"Buprenorphine\",\n"
            + "              \"domain\": \"DRUG\",\n"
            + "              \"type\": \"RXNORM\",\n"
            + "              \"group\": false,\n"
            + "              \"attributes\": [],\n"
            + "              \"ancestorData\": true,\n"
            + "              \"standard\": true,\n"
            + "              \"conceptId\": 1133201,\n"
            + "              \"value\": \"1819\"\n"
            + "            }\n"
            + "          ],\n"
            + "          \"modifiers\": [\n"
            + "            {\n"
            + "              \"name\": \"AGE_AT_EVENT\",\n"
            + "              \"operator\": {\n"
            + "                \"label\": \"Any\"\n"
            + "              },\n"
            + "              \"operands\": [\n"
            + "                \"1\"\n"
            + "              ]\n"
            + "            }\n"
            + "          ]\n"
            + "        }\n"
            + "      ],\n"
            + "      \"temporal\": false\n"
            + "    }\n"
            + "  ],\n"
            + "  \"excludes\": [],\n"
            + "  \"dataFilters\": []\n"
            + "}";

    workspace = new Workspace();
    workspace.setName(WORKSPACE_NAME);
    workspace.setNamespace(WORKSPACE_NAMESPACE);
    workspace.setResearchPurpose(new ResearchPurpose());
    workspace.setCdrVersionId(String.valueOf(cdrVersion.getCdrVersionId()));
    workspace.setBillingAccountName("billing-account");

    workspace2 = new Workspace();
    workspace2.setName(WORKSPACE_NAME_2);
    workspace2.setNamespace(WORKSPACE_NAMESPACE);
    workspace2.setResearchPurpose(new ResearchPurpose());
    workspace2.setCdrVersionId(String.valueOf(cdrVersion.getCdrVersionId()));
    workspace2.setBillingAccountName("billing-account");

    CLOCK.setInstant(NOW);

    Cohort cohort = new Cohort();
    cohort.setName("demo");
    cohort.setDescription("demo");
    cohort.setType("demo");
    cohort.setCriteria(createDemoCriteria().toString());

    workspace = workspacesController.createWorkspace(workspace).getBody();
    workspace2 = workspacesController.createWorkspace(workspace2).getBody();

    stubWorkspaceAccessLevelWithCreatorEmail(workspace, WorkspaceAccessLevel.OWNER);
    stubWorkspaceAccessLevelWithCreatorEmail(workspace2, WorkspaceAccessLevel.OWNER);
  }

  private JSONObject createDemoCriteria() {
    JSONObject criteria = new JSONObject();
    criteria.append("includes", new JSONArray());
    criteria.append("excludes", new JSONArray());
    return criteria;
  }

  private void stubGetWorkspace(
      String ns, String name, String creator, WorkspaceAccessLevel access) {
    RawlsWorkspaceDetails fcWorkspace = new RawlsWorkspaceDetails();
    fcWorkspace.setNamespace(ns);
    fcWorkspace.setName(name);
    fcWorkspace.setCreatedBy(creator);
    RawlsWorkspaceResponse fcResponse = new RawlsWorkspaceResponse();
    fcResponse.setWorkspace(fcWorkspace);
    fcResponse.setAccessLevel(firecloudMapper.apiToFcWorkspaceAccessLevel(access));
    when(fireCloudService.getWorkspace(ns, name)).thenReturn(fcResponse);
    stubGetWorkspaceAcl(ns, name, creator, access);
  }

  private void stubGetWorkspaceAcl(
      String ns, String name, String creator, WorkspaceAccessLevel access) {
    RawlsWorkspaceACL workspaceAccessLevelResponse = new RawlsWorkspaceACL();
    RawlsWorkspaceAccessEntry accessLevelEntry =
        new RawlsWorkspaceAccessEntry().accessLevel(access.toString());
    Map<String, RawlsWorkspaceAccessEntry> userEmailToAccessEntry =
        ImmutableMap.of(creator, accessLevelEntry);
    workspaceAccessLevelResponse.setAcl(userEmailToAccessEntry);
    when(fireCloudService.getWorkspaceAclAsService(ns, name))
        .thenReturn(workspaceAccessLevelResponse);
  }

  private void stubWorkspaceAccessLevelWithCreatorEmail(
      Workspace workspace, WorkspaceAccessLevel workspaceAccessLevel) {
    stubGetWorkspace(
        workspace.getNamespace(), workspace.getName(), CREATOR_EMAIL, workspaceAccessLevel);
    stubGetWorkspaceAcl(
        workspace.getNamespace(), workspace.getName(), CREATOR_EMAIL, workspaceAccessLevel);
  }

  private Cohort createDefaultCohort() {
    Cohort cohort = new Cohort();
    cohort.setName(COHORT_NAME);
    cohort.setCriteria(cohortCriteria);
    return cohort;
  }

  @Test
  public void testGetCohortsInWorkspace() throws Exception {
    Cohort c1 = createDefaultCohort();
    c1.setName("c1");
    c1 = cohortsController.createCohort(workspace.getNamespace(), workspace.getId(), c1).getBody();
    Cohort c2 = createDefaultCohort();
    c2.setName("c2");
    c2 = cohortsController.createCohort(workspace.getNamespace(), workspace.getId(), c2).getBody();

    List<Cohort> cohorts =
        cohortsController
            .getCohortsInWorkspace(workspace.getNamespace(), workspace.getId())
            .getBody()
            .getItems();
    assertThat(cohorts.size()).isEqualTo(2);
    assertThat(cohorts).containsExactly(c1, c2);
  }

  @Test
  public void testCreateCohortOwner() {
    stubWorkspaceAccessLevelWithCreatorEmail(workspace, WorkspaceAccessLevel.OWNER);
    Cohort saved =
        cohortsController
            .createCohort(workspace.getNamespace(), workspace.getId(), createDefaultCohort())
            .getBody();
    assertThat(saved.getCriteria()).isEqualTo(createDefaultCohort().getCriteria());
    assertThat(saved.getCreator()).isEqualTo(CREATOR_EMAIL);
    assertThat(saved.getCreationTime()).isNotNull();
    assertThat(saved.getCreationTime()).isEqualTo(saved.getLastModifiedTime());
  }

  @Test
  public void testCreateCohortWriter() {
    stubWorkspaceAccessLevelWithCreatorEmail(workspace, WorkspaceAccessLevel.WRITER);
    Cohort saved =
        cohortsController
            .createCohort(workspace.getNamespace(), workspace.getId(), createDefaultCohort())
            .getBody();
    assertThat(saved.getCriteria()).isEqualTo(createDefaultCohort().getCriteria());
    assertThat(saved.getCreator()).isEqualTo(CREATOR_EMAIL);
    assertThat(saved.getCreationTime()).isNotNull();
    assertThat(saved.getCreationTime()).isEqualTo(saved.getLastModifiedTime());
  }

  @Test
  public void testCreateCohortReader() {
    stubWorkspaceAccessLevelWithCreatorEmail(workspace, WorkspaceAccessLevel.READER);
    Throwable exception =
        assertThrows(
            ForbiddenException.class,
            () ->
                cohortsController.createCohort(
                    workspace.getNamespace(), workspace.getId(), createDefaultCohort()));
    assertForbiddenException(exception);
  }

  @Test
  public void testCreateCohortNoAccess() {
    stubWorkspaceAccessLevelWithCreatorEmail(workspace, WorkspaceAccessLevel.NO_ACCESS);
    Throwable exception =
        assertThrows(
            ForbiddenException.class,
            () ->
                cohortsController.createCohort(
                    workspace.getNamespace(), workspace.getId(), createDefaultCohort()));
    assertForbiddenException(exception);
  }

  @Test
  public void testCreateCohortBadCriteria() {
    assertThrows(
        ServerErrorException.class,
        () -> {
          Cohort cohort = createDefaultCohort();
          cohort.setCriteria(badCohortCriteria);
          cohortsController
              .createCohort(workspace.getNamespace(), workspace.getId(), cohort)
              .getBody();
        });
  }

  @Test
  public void testUpdateCohortOwner() {
    // minimal access to create
    stubWorkspaceAccessLevelWithCreatorEmail(workspace, WorkspaceAccessLevel.WRITER);
    Cohort saved =
        cohortsController
            .createCohort(workspace.getNamespace(), workspace.getId(), createDefaultCohort())
            .getBody();
    // change access, update and check
    CLOCK.increment(1000); // lets say time ticked 1 sec past
    stubWorkspaceAccessLevelWithCreatorEmail(workspace, WorkspaceAccessLevel.OWNER);
    Cohort updated =
        cohortsController
            .updateCohort(
                workspace.getNamespace(),
                workspace.getId(),
                saved.getId(),
                saved.name(UPDATED_COHORT_NAME))
            .getBody();
    // assert
    assertThat(updated.getName()).isEqualTo(UPDATED_COHORT_NAME);
    assertThat(updated.getCriteria()).isEqualTo(saved.getCriteria());
    assertThat(updated.getCreationTime()).isEqualTo(saved.getCreationTime());
    assertThat(updated.getLastModifiedTime()).isGreaterThan(saved.getLastModifiedTime());
  }

  @Test
  public void testUpdateCohortWriter() {
    // minimal access to create
    stubWorkspaceAccessLevelWithCreatorEmail(workspace, WorkspaceAccessLevel.WRITER);
    Cohort saved =
        cohortsController
            .createCohort(workspace.getNamespace(), workspace.getId(), createDefaultCohort())
            .getBody();
    // change access, update and check
    CLOCK.increment(1000); // lets say time ticked 1 sec past
    stubWorkspaceAccessLevelWithCreatorEmail(workspace, WorkspaceAccessLevel.WRITER);
    Cohort updated =
        cohortsController
            .updateCohort(
                workspace.getNamespace(),
                workspace.getId(),
                saved.getId(),
                saved.name(UPDATED_COHORT_NAME))
            .getBody();
    // assert
    assertThat(updated.getName()).isEqualTo(UPDATED_COHORT_NAME);
    assertThat(updated.getCriteria()).isEqualTo(saved.getCriteria());
    assertThat(updated.getCreationTime()).isEqualTo(saved.getCreationTime());
    assertThat(updated.getLastModifiedTime()).isGreaterThan(saved.getLastModifiedTime());
  }

  @Test
  public void testUpdateCohortReader() {
    // minimal access to create
    stubWorkspaceAccessLevelWithCreatorEmail(workspace, WorkspaceAccessLevel.WRITER);
    Cohort saved =
        cohortsController
            .createCohort(workspace.getNamespace(), workspace.getId(), createDefaultCohort())
            .getBody();
    // change access, update and check
    stubWorkspaceAccessLevelWithCreatorEmail(workspace, WorkspaceAccessLevel.READER);
    Throwable exception =
        assertThrows(
            ForbiddenException.class,
            () ->
                cohortsController.updateCohort(
                    workspace.getNamespace(),
                    workspace.getId(),
                    saved.getId(),
                    saved.name(UPDATED_COHORT_NAME)));
    assertForbiddenException(exception);
  }

  @Test
  public void testUpdateCohortNoAccess() {
    // minimal access to create
    stubWorkspaceAccessLevelWithCreatorEmail(workspace, WorkspaceAccessLevel.WRITER);
    Cohort saved =
        cohortsController
            .createCohort(workspace.getNamespace(), workspace.getId(), createDefaultCohort())
            .getBody();
    // change access, update and check
    stubWorkspaceAccessLevelWithCreatorEmail(workspace, WorkspaceAccessLevel.NO_ACCESS);
    Throwable exception =
        assertThrows(
            ForbiddenException.class,
            () ->
                cohortsController.updateCohort(
                    workspace.getNamespace(),
                    workspace.getId(),
                    saved.getId(),
                    saved.name(UPDATED_COHORT_NAME)));
    assertForbiddenException(exception);
  }

  @Test
  public void testDeleteCohortOwner() {
    // minimal access to create
    stubWorkspaceAccessLevelWithCreatorEmail(workspace, WorkspaceAccessLevel.WRITER);
    Cohort saved =
        cohortsController
            .createCohort(workspace.getNamespace(), workspace.getId(), createDefaultCohort())
            .getBody();
    // change access, delete and check
    stubWorkspaceAccessLevelWithCreatorEmail(workspace, WorkspaceAccessLevel.OWNER);
    ResponseEntity<EmptyResponse> response =
        cohortsController.deleteCohort(workspace.getNamespace(), workspace.getId(), saved.getId());
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  public void testDeleteCohortWriter() {
    // minimal access to create
    stubWorkspaceAccessLevelWithCreatorEmail(workspace, WorkspaceAccessLevel.WRITER);
    Cohort saved =
        cohortsController
            .createCohort(workspace.getNamespace(), workspace.getId(), createDefaultCohort())
            .getBody();
    // change access, delete and check
    stubWorkspaceAccessLevelWithCreatorEmail(workspace, WorkspaceAccessLevel.WRITER);
    ResponseEntity<EmptyResponse> response =
        cohortsController.deleteCohort(workspace.getNamespace(), workspace.getId(), saved.getId());
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  public void testDeleteCohortReader() {
    // minimal access to create
    stubWorkspaceAccessLevelWithCreatorEmail(workspace, WorkspaceAccessLevel.WRITER);
    Cohort saved =
        cohortsController
            .createCohort(workspace.getNamespace(), workspace.getId(), createDefaultCohort())
            .getBody();
    // change access, delete and check
    stubWorkspaceAccessLevelWithCreatorEmail(workspace, WorkspaceAccessLevel.READER);
    Throwable exception =
        assertThrows(
            ForbiddenException.class,
            () ->
                cohortsController.deleteCohort(
                    workspace.getNamespace(), workspace.getId(), saved.getId()));
    assertForbiddenException(exception);
  }

  @Test
  public void testDeleteCohortNoAccess() {
    // minimal access to create
    stubWorkspaceAccessLevelWithCreatorEmail(workspace, WorkspaceAccessLevel.WRITER);
    Cohort saved =
        cohortsController
            .createCohort(workspace.getNamespace(), workspace.getId(), createDefaultCohort())
            .getBody();
    // change access, delete and check
    stubWorkspaceAccessLevelWithCreatorEmail(workspace, WorkspaceAccessLevel.READER);
    Throwable exception =
        assertThrows(
            ForbiddenException.class,
            () ->
                cohortsController.deleteCohort(
                    workspace.getNamespace(), workspace.getId(), saved.getId()));
    assertForbiddenException(exception);
  }

  @Test
  public void testDuplicateCohortOwner() {
    // minimal access to create
    stubWorkspaceAccessLevelWithCreatorEmail(workspace, WorkspaceAccessLevel.WRITER);
    Cohort original =
        cohortsController
            .createCohort(workspace.getNamespace(), workspace.getId(), createDefaultCohort())
            .getBody();
    // change access, duplicate and check
    CLOCK.increment(1000L); // increment clock before duplicating
    stubWorkspaceAccessLevelWithCreatorEmail(workspace, WorkspaceAccessLevel.OWNER);
    DuplicateCohortRequest duplicateCohortRequest =
        new DuplicateCohortRequest()
            .newName(DUPLICATED_COHORT_NAME)
            .originalCohortId(original.getId());
    Cohort duplicated =
        cohortsController
            .duplicateCohort(workspace.getNamespace(), workspace.getId(), duplicateCohortRequest)
            .getBody();
    assertDuplicatedCohort(original, duplicated);
  }

  @Test
  public void testDuplicateCohortWriter() {
    // minimal access to create
    stubWorkspaceAccessLevelWithCreatorEmail(workspace, WorkspaceAccessLevel.WRITER);
    Cohort original =
        cohortsController
            .createCohort(workspace.getNamespace(), workspace.getId(), createDefaultCohort())
            .getBody();
    // change access, duplicate and check
    CLOCK.increment(1000L); // increment clock before duplicating
    stubWorkspaceAccessLevelWithCreatorEmail(workspace, WorkspaceAccessLevel.WRITER);
    DuplicateCohortRequest duplicateCohortRequest =
        new DuplicateCohortRequest()
            .newName(DUPLICATED_COHORT_NAME)
            .originalCohortId(original.getId());
    Cohort duplicated =
        cohortsController
            .duplicateCohort(workspace.getNamespace(), workspace.getId(), duplicateCohortRequest)
            .getBody();
    assertDuplicatedCohort(original, duplicated);
  }

  @Test
  public void testDuplicateCohortReader() {
    // minimal access to create
    stubWorkspaceAccessLevelWithCreatorEmail(workspace, WorkspaceAccessLevel.WRITER);
    Cohort original =
        cohortsController
            .createCohort(workspace.getNamespace(), workspace.getId(), createDefaultCohort())
            .getBody();
    // change access, duplicate and check
    stubWorkspaceAccessLevelWithCreatorEmail(workspace, WorkspaceAccessLevel.READER);
    DuplicateCohortRequest duplicateCohortRequest =
        new DuplicateCohortRequest()
            .newName(DUPLICATED_COHORT_NAME)
            .originalCohortId(original.getId());
    Throwable exception =
        assertThrows(
            ForbiddenException.class,
            () ->
                cohortsController.duplicateCohort(
                    workspace.getNamespace(), workspace.getId(), duplicateCohortRequest));
    assertForbiddenException(exception);
  }

  @Test
  public void testDuplicateCohortNoAccess() {
    // minimal access to create
    stubWorkspaceAccessLevelWithCreatorEmail(workspace, WorkspaceAccessLevel.WRITER);
    Cohort original =
        cohortsController
            .createCohort(workspace.getNamespace(), workspace.getId(), createDefaultCohort())
            .getBody();
    // change access, duplicate and check
    stubWorkspaceAccessLevelWithCreatorEmail(workspace, WorkspaceAccessLevel.NO_ACCESS);
    DuplicateCohortRequest duplicateCohortRequest =
        new DuplicateCohortRequest()
            .newName(DUPLICATED_COHORT_NAME)
            .originalCohortId(original.getId());
    Throwable exception =
        assertThrows(
            ForbiddenException.class,
            () ->
                cohortsController.duplicateCohort(
                    workspace.getNamespace(), workspace.getId(), duplicateCohortRequest));
    assertForbiddenException(exception);
  }

  @Test
  public void testGetCohortWrongWorkspace() throws Exception {
    assertThrows(
        NotFoundException.class,
        () -> {
          Cohort cohort = createDefaultCohort();
          cohort =
              cohortsController
                  .createCohort(workspace.getNamespace(), workspace.getId(), cohort)
                  .getBody();
          cohortsController.getCohort(workspace2.getNamespace(), WORKSPACE_NAME_2, cohort.getId());
        });
  }

  @Test
  public void testUpdateCohortStaleThrows() throws Exception {
    assertThrows(
        ConflictException.class,
        () -> {
          Cohort cohort = createDefaultCohort();
          cohort =
              cohortsController
                  .createCohort(workspace.getNamespace(), workspace.getId(), cohort)
                  .getBody();
          cohortsController
              .updateCohort(
                  workspace.getNamespace(),
                  workspace.getId(),
                  cohort.getId(),
                  new Cohort().name("updated-name").etag(cohort.getEtag()))
              .getBody();
          // Still using the initial etag.
          cohortsController
              .updateCohort(
                  workspace.getNamespace(),
                  workspace.getId(),
                  cohort.getId(),
                  new Cohort().name("updated-name2").etag(cohort.getEtag()))
              .getBody();
        });
  }

  @ParameterizedTest(name = "testUpdateCohortInvalidEtagsThrow eTag=[{0}]")
  @NullAndEmptySource
  @ValueSource(strings = {"  ", "hello, world", "\"\"", "\"\"1234\"\"", "\"-1\""})
  public void testUpdateCohortInvalidEtagsThrow(String eTag) throws Exception {
    final Cohort cohort =
        cohortsController
            .createCohort(workspace.getNamespace(), workspace.getId(), createDefaultCohort())
            .getBody();
    Throwable exception =
        assertThrows(
            BadRequestException.class,
            () ->
                cohortsController.updateCohort(
                    workspace.getNamespace(),
                    workspace.getId(),
                    cohort.getId(),
                    new Cohort().name("updated-name").etag(eTag)));
    String errMsg = "missing required update field 'etag'";
    if (eTag != null && eTag.length() > 0) {
      errMsg = String.format("Invalid etag provided: %s", eTag);
    }
    assertThat(exception).hasMessageThat().isEqualTo(errMsg);
  }

  private void assertForbiddenException(Throwable exception) {
    assertThat(exception)
        .hasMessageThat()
        .isEqualTo(
            String.format(
                "You do not have sufficient permissions to access workspace %s/workspace",
                workspace.getNamespace()));
  }

  private void assertDuplicatedCohort(Cohort original, Cohort duplicated) {
    assertThat(duplicated.getName()).isEqualTo(DUPLICATED_COHORT_NAME);
    assertThat(duplicated.getCreationTime()).isEqualTo(duplicated.getLastModifiedTime());
    assertThat(duplicated.getCreationTime()).isGreaterThan(original.getCreationTime());
    assertThat(duplicated.getCriteria()).isEqualTo(original.getCriteria());
    assertThat(duplicated.getType()).isEqualTo(original.getType());
    assertThat(duplicated.getDescription()).isEqualTo(original.getDescription());
  }
}
