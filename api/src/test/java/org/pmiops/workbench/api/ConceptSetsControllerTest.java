package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.pmiops.workbench.utils.TestMockFactory.createDefaultCdrVersion;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.access.AccessModuleService;
import org.pmiops.workbench.access.AccessTierServiceImpl;
import org.pmiops.workbench.actionaudit.auditors.BillingProjectAuditor;
import org.pmiops.workbench.actionaudit.auditors.UserServiceAuditor;
import org.pmiops.workbench.actionaudit.auditors.WorkspaceAuditor;
import org.pmiops.workbench.actionaudit.bucket.BucketAuditQueryServiceImpl;
import org.pmiops.workbench.billing.FreeTierBillingService;
import org.pmiops.workbench.cdr.ConceptBigQueryService;
import org.pmiops.workbench.cdr.dao.CBCriteriaDao;
import org.pmiops.workbench.cdr.model.DbCriteria;
import org.pmiops.workbench.cloudtasks.TaskQueueService;
import org.pmiops.workbench.cohortbuilder.CohortBuilderServiceImpl;
import org.pmiops.workbench.cohortbuilder.CohortQueryBuilder;
import org.pmiops.workbench.cohortbuilder.mapper.CohortBuilderMapperImpl;
import org.pmiops.workbench.cohortreview.CohortReviewServiceImpl;
import org.pmiops.workbench.cohortreview.mapper.CohortReviewMapperImpl;
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
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.exfiltration.ObjectNameLengthServiceImpl;
import org.pmiops.workbench.exfiltration.impl.EgressObjectLengthsRemediationService;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.google.CloudBillingClient;
import org.pmiops.workbench.google.CloudStorageClient;
import org.pmiops.workbench.google.DirectoryService;
import org.pmiops.workbench.iam.IamService;
import org.pmiops.workbench.mail.MailService;
import org.pmiops.workbench.model.ConceptSet;
import org.pmiops.workbench.model.ConceptSetConceptId;
import org.pmiops.workbench.model.CopyRequest;
import org.pmiops.workbench.model.CreateConceptSetRequest;
import org.pmiops.workbench.model.Criteria;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.ResearchPurpose;
import org.pmiops.workbench.model.UpdateConceptSetRequest;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.monitoring.LogsBasedMetricServiceFakeImpl;
import org.pmiops.workbench.moodle.MoodleService;
import org.pmiops.workbench.notebooks.NotebooksService;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceACL;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceAccessEntry;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceAccessLevel;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceDetails;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceResponse;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.test.FakeLongRandom;
import org.pmiops.workbench.testconfig.UserServiceTestConfiguration;
import org.pmiops.workbench.utils.TestMockFactory;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.utils.mappers.FirecloudMapperImpl;
import org.pmiops.workbench.utils.mappers.UserMapperImpl;
import org.pmiops.workbench.utils.mappers.WorkspaceMapperImpl;
import org.pmiops.workbench.workspaces.WorkspaceAuthService;
import org.pmiops.workbench.workspaces.WorkspaceOperationMapper;
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class ConceptSetsControllerTest {

  public static final String UPDATED_DESC = "Updated Desc";
  private static Criteria CRITERIA_CONDITION_1 =
      new Criteria()
          .conceptId(123L)
          .name("a concept")
          .standard(true)
          .code("conceptA")
          .type("V1")
          .domainId(Domain.CONDITION.toString())
          .childCount(123L)
          .parentCount(123L)
          .count(0L);

  private static final Criteria CRITERIA_MEASUREMENT_1 =
      new Criteria()
          .conceptId(456L)
          .standard(true)
          .name("b concept")
          .code("conceptB")
          .type("V2")
          .domainId(Domain.MEASUREMENT.toString())
          .childCount(456L)
          .parentCount(456L)
          .count(0L);

  private static final Criteria CRITERIA_CONDITION_2 =
      new Criteria()
          .conceptId(789L)
          .standard(true)
          .name("multi word concept")
          .code("conceptC")
          .type("V3")
          .domainId(Domain.CONDITION.toString())
          .childCount(789L)
          .parentCount(789L)
          .count(0L);

  private static final Criteria CRITERIA_CONDITION_3 =
      new Criteria()
          .conceptId(7890L)
          .standard(true)
          .name("conceptD test concept")
          .code("conceptE")
          .type("V5")
          .domainId(Domain.CONDITION.toString())
          .childCount(7890L)
          .parentCount(7890L)
          .count(0L);

  private static final Criteria CRITERIA_SURVEY_1 =
      new Criteria()
          .conceptId(987L)
          .name("a concept")
          .standard(true)
          .code("conceptA")
          .type("V1")
          .domainId(Domain.OBSERVATION.toString())
          .childCount(123L)
          .parentCount(123L)
          .count(0L);

  private static final String USER_EMAIL = "bob@gmail.com";
  private static final String WORKSPACE_NAMESPACE = "ns";
  private static final String WORKSPACE_NAME = "name";
  private static final String WORKSPACE_NAME_2 = "name2";
  private static final Instant NOW = FakeClockConfiguration.NOW.toInstant();

  public static final String CONCEPT_SET_NAME_1 = "concept set 1";
  public static final String CONCEPT_SET_DESC_1 = "desc 1";
  public static final String UPDATED_NAME = "Updated Name";
  private static DbUser currentUser;
  private Workspace workspace;
  private Workspace workspace2;
  private ConceptSet defaultConceptSet;

  @Autowired AccessTierDao accessTierDao;

  @Autowired CdrVersionDao cdrVersionDao;

  @Autowired CBCriteriaDao cbCriteriaDao;

  @Autowired UserDao userDao;

  @Autowired CloudBillingClient cloudBillingClient;

  @Autowired FireCloudService fireCloudService;

  @Autowired ConceptSetsController conceptSetsController;

  @Autowired ConceptBigQueryService conceptBigQueryService;

  @Autowired WorkspacesController workspacesController;

  @Autowired private FakeClock fakeClock;

  @TestConfiguration
  @Import({
    FakeClockConfiguration.class,
    CommonMappers.class,
    CohortBuilderMapperImpl.class,
    CohortBuilderServiceImpl.class,
    ConceptSetMapperImpl.class,
    ConceptSetService.class,
    ConceptSetsController.class,
    LogsBasedMetricServiceFakeImpl.class,
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
    CloudBillingClient.class,
    CloudStorageClient.class,
    CohortCloningService.class,
    CohortFactoryImpl.class,
    CohortMapperImpl.class,
    CohortQueryBuilder.class,
    CohortReviewMapperImpl.class,
    CohortReviewServiceImpl.class,
    CohortService.class,
    MoodleService.class,
    ConceptBigQueryService.class,
    DataSetMapperImpl.class,
    DataSetService.class,
    DirectoryService.class,
    FireCloudService.class,
    FirecloudMapperImpl.class,
    FreeTierBillingService.class,
    IamService.class,
    MailService.class,
    NotebooksService.class,
    TaskQueueService.class,
    UserRecentResourceService.class,
    UserServiceAuditor.class,
    WorkspaceAuditor.class,
    WorkspaceOperationMapper.class,
    EgressObjectLengthsRemediationService.class,
  })
  static class Configuration {
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
  public void setUp() throws Exception {
    // reset after every test
    ConceptSetService.MAX_CONCEPTS_PER_SET = 1000; // original value

    TestMockFactory.stubCreateBillingProject(fireCloudService);
    TestMockFactory.stubCreateFcWorkspace(fireCloudService);
    TestMockFactory.stubPollCloudBillingLinked(cloudBillingClient, "billing-account");

    DbUser user = new DbUser();
    user.setUsername(USER_EMAIL);
    user.setUserId(123L);
    user.setDisabled(false);
    user = userDao.save(user);
    currentUser = user;

    DbCdrVersion cdrVersion = createDefaultCdrVersion();
    accessTierDao.save(cdrVersion.getAccessTier());
    cdrVersion = cdrVersionDao.save(cdrVersion);
    workspace =
        createTestWorkspace(
            WORKSPACE_NAMESPACE,
            WORKSPACE_NAME,
            cdrVersion.getCdrVersionId(),
            RawlsWorkspaceAccessLevel.OWNER);
    workspace2 =
        createTestWorkspace(
            WORKSPACE_NAMESPACE,
            WORKSPACE_NAME_2,
            cdrVersion.getCdrVersionId(),
            RawlsWorkspaceAccessLevel.OWNER);
    // save different criteria (there is no workspace associated with criteria
    saveAllTestCriteria();
    // create concept set as owner or writer in workspace
    defaultConceptSet =
        createTestConceptSet(
            workspace,
            CONCEPT_SET_NAME_1,
            CONCEPT_SET_DESC_1,
            Domain.CONDITION,
            CRITERIA_CONDITION_1);
  }

  //////////// createConceptSet tests ////////////
  @Test
  public void createConceptSetAddMany() {
    ConceptSet conceptSet =
        new ConceptSet()
            .domain(Domain.CONDITION)
            .description(CONCEPT_SET_DESC_1)
            .name(CONCEPT_SET_NAME_1);

    CreateConceptSetRequest createConceptSetRequest =
        buildCreateConceptSetRequest(
            conceptSet,
            CRITERIA_CONDITION_1.getConceptId(),
            CRITERIA_CONDITION_2.getConceptId(),
            CRITERIA_CONDITION_3.getConceptId());

    ConceptSet savedConceptSet =
        conceptSetsController
            .createConceptSet(workspace.getNamespace(), WORKSPACE_NAME, createConceptSetRequest)
            .getBody();

    List<Criteria> expectedCriteriums =
        createExpectedCriteria(
            savedConceptSet.getCriteriums(),
            ImmutableList.of(CRITERIA_CONDITION_1, CRITERIA_CONDITION_2, CRITERIA_CONDITION_3));

    assertConceptSetAndCriteria(savedConceptSet, expectedCriteriums);
  }

  @Test
  public void createConceptSetAddTooMany() {
    // define too many
    ConceptSetService.MAX_CONCEPTS_PER_SET = 2;
    ConceptSet conceptSet =
        new ConceptSet()
            .domain(Domain.CONDITION)
            .description(CONCEPT_SET_DESC_1)
            .name(CONCEPT_SET_NAME_1);
    // add 3 concepts
    CreateConceptSetRequest createConceptSetRequest =
        buildCreateConceptSetRequest(
            conceptSet,
            CRITERIA_CONDITION_1.getConceptId(),
            CRITERIA_CONDITION_2.getConceptId(),
            CRITERIA_CONDITION_3.getConceptId());

    Throwable exception =
        assertThrows(
            ConflictException.class,
            () ->
                conceptSetsController.createConceptSet(
                    workspace.getNamespace(), WORKSPACE_NAME, createConceptSetRequest));

    assertThat(exception)
        .hasMessageThat()
        .containsMatch(
            String.format("Exceeded %d concept set limit", ConceptSetService.MAX_CONCEPTS_PER_SET));
  }

  @Test
  public void createConceptSetOwner() {
    // change access, create and check
    stubWorkspaceAccessLevel(workspace, RawlsWorkspaceAccessLevel.OWNER);
    ConceptSet conceptSet =
        new ConceptSet()
            .domain(Domain.CONDITION)
            .description(CONCEPT_SET_DESC_1)
            .name(CONCEPT_SET_NAME_1);

    CreateConceptSetRequest createConceptSetRequest =
        buildCreateConceptSetRequest(conceptSet, CRITERIA_CONDITION_1.getConceptId());

    ConceptSet savedConceptSet =
        conceptSetsController
            .createConceptSet(workspace.getNamespace(), WORKSPACE_NAME, createConceptSetRequest)
            .getBody();

    List<Criteria> expectedCriteriums =
        createExpectedCriteria(
            savedConceptSet.getCriteriums(), ImmutableList.of(CRITERIA_CONDITION_1));
    assertConceptSetAndCriteria(savedConceptSet, expectedCriteriums);
  }

  @Test
  public void createConceptSetWriter() {
    // change access, create and check
    stubWorkspaceAccessLevel(workspace, RawlsWorkspaceAccessLevel.WRITER);
    ConceptSet conceptSet =
        new ConceptSet()
            .domain(Domain.CONDITION)
            .description(CONCEPT_SET_DESC_1)
            .name(CONCEPT_SET_NAME_1);

    CreateConceptSetRequest createConceptSetRequest =
        buildCreateConceptSetRequest(conceptSet, CRITERIA_CONDITION_1.getConceptId());

    ConceptSet savedConceptSet =
        conceptSetsController
            .createConceptSet(workspace.getNamespace(), WORKSPACE_NAME, createConceptSetRequest)
            .getBody();

    List<Criteria> expectedCriteriums =
        createExpectedCriteria(
            savedConceptSet.getCriteriums(), ImmutableList.of(CRITERIA_CONDITION_1));
    assertConceptSetAndCriteria(savedConceptSet, expectedCriteriums);
  }

  @Test
  public void createConceptSetReader() {
    // change access, create and check
    stubWorkspaceAccessLevel(workspace, RawlsWorkspaceAccessLevel.READER);
    ConceptSet conceptSet =
        new ConceptSet()
            .domain(Domain.CONDITION)
            .description(CONCEPT_SET_DESC_1)
            .name(CONCEPT_SET_NAME_1);

    CreateConceptSetRequest createConceptSetRequest =
        buildCreateConceptSetRequest(conceptSet, CRITERIA_CONDITION_1.getConceptId());

    Throwable exception =
        assertThrows(
            ForbiddenException.class,
            () ->
                conceptSetsController.createConceptSet(
                    workspace.getNamespace(), WORKSPACE_NAME, createConceptSetRequest));

    assertForbiddenException(exception);
  }

  @Test
  public void createConceptSetNoAccess() {
    // change access, create and check
    stubWorkspaceAccessLevel(workspace, RawlsWorkspaceAccessLevel.NO_ACCESS);
    ConceptSet conceptSet =
        new ConceptSet()
            .domain(Domain.CONDITION)
            .description(CONCEPT_SET_DESC_1)
            .name(CONCEPT_SET_NAME_1);

    CreateConceptSetRequest createConceptSetRequest =
        buildCreateConceptSetRequest(conceptSet, CRITERIA_CONDITION_1.getConceptId());

    Throwable exception =
        assertThrows(
            ForbiddenException.class,
            () ->
                conceptSetsController.createConceptSet(
                    workspace.getNamespace(), WORKSPACE_NAME, createConceptSetRequest));

    assertForbiddenException(exception);
  }

  //////////// deleteConceptSet tests ////////////
  @Test
  public void deleteConceptSetOwner() {
    // use defaultConceptSet

    // change access, create and check
    stubWorkspaceAccessLevel(workspace, RawlsWorkspaceAccessLevel.OWNER);

    ResponseEntity<EmptyResponse> response =
        conceptSetsController.deleteConceptSet(
            workspace.getNamespace(), WORKSPACE_NAME, defaultConceptSet.getId());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    Throwable exception =
        assertThrows(
            NotFoundException.class,
            () ->
                conceptSetsController.getConceptSet(
                    workspace.getNamespace(), workspace.getId(), defaultConceptSet.getId()));

    assertNotFoundException(exception, defaultConceptSet.getId());
  }

  @Test
  public void deleteConceptSetWriter() {
    // use defaultConceptSet
    // change access, create and check
    stubWorkspaceAccessLevel(workspace, RawlsWorkspaceAccessLevel.WRITER);

    ResponseEntity<EmptyResponse> response =
        conceptSetsController.deleteConceptSet(
            workspace.getNamespace(), WORKSPACE_NAME, defaultConceptSet.getId());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    Throwable exception =
        assertThrows(
            NotFoundException.class,
            () ->
                conceptSetsController.getConceptSet(
                    workspace.getNamespace(), workspace.getId(), defaultConceptSet.getId()));

    assertNotFoundException(exception, defaultConceptSet.getId());
  }

  @Test
  public void deleteConceptSetReader() {
    // use defaultConceptSet
    // change access, create and check
    stubWorkspaceAccessLevel(workspace, RawlsWorkspaceAccessLevel.READER);

    Throwable exception =
        assertThrows(
            ForbiddenException.class,
            () ->
                conceptSetsController.deleteConceptSet(
                    workspace.getNamespace(), workspace.getId(), defaultConceptSet.getId()));

    assertForbiddenException(exception);
  }

  @Test
  public void deleteConceptSetNoAccess() {
    // use defaultConceptSet
    // change access, create and check
    stubWorkspaceAccessLevel(workspace, RawlsWorkspaceAccessLevel.NO_ACCESS);

    Throwable exception =
        assertThrows(
            ForbiddenException.class,
            () ->
                conceptSetsController.deleteConceptSet(
                    workspace.getNamespace(), workspace.getId(), defaultConceptSet.getId()));

    assertForbiddenException(exception);
  }

  //////////// getConceptSet tests ////////////

  @Test
  public void getConceptSetNotExists() {
    Throwable exception =
        assertThrows(
            NotFoundException.class,
            () ->
                conceptSetsController.getConceptSet(
                    workspace2.getNamespace(), workspace2.getId(), 1L));

    assertNotFoundException(exception, 1L);
  }

  @Test
  public void getConceptSetWrongWorkspace() {
    // use defaultConceptSet
    // check access from different workspace
    Throwable exception =
        assertThrows(
            NotFoundException.class,
            () ->
                conceptSetsController.getConceptSet(
                    workspace2.getNamespace(), workspace2.getId(), defaultConceptSet.getId()));

    assertNotFoundException(exception, 1L);
  }

  @Test
  public void getConceptSetWrongConceptId() {
    // use defaultConceptSet
    // check for incorrect conceptId
    Long wrongConceptSetId = defaultConceptSet.getId() + 100L;

    Throwable exception =
        assertThrows(
            NotFoundException.class,
            () ->
                conceptSetsController.getConceptSet(
                    workspace.getNamespace(), workspace.getId(), wrongConceptSetId));

    assertNotFoundException(exception, wrongConceptSetId);
  }

  @Test
  public void getConceptSetOwner() {
    // use defaultConceptSet
    // change access, get and check
    stubWorkspaceAccessLevel(workspace, RawlsWorkspaceAccessLevel.OWNER);
    ConceptSet retrieved =
        conceptSetsController
            .getConceptSet(workspace.getNamespace(), workspace.getId(), defaultConceptSet.getId())
            .getBody();

    assertThat(retrieved).isEqualTo(defaultConceptSet.participantCount(0));
  }

  @Test
  public void getConceptSetWriter() {
    // use defaultConceptSet
    // change access, get and check
    stubWorkspaceAccessLevel(workspace, RawlsWorkspaceAccessLevel.WRITER);

    ConceptSet retrieved =
        conceptSetsController
            .getConceptSet(workspace.getNamespace(), workspace.getId(), defaultConceptSet.getId())
            .getBody();

    assertThat(retrieved).isEqualTo(defaultConceptSet.participantCount(0));
  }

  @Test
  public void getConceptSetReader() {
    // create concept set as owner or writer
    // use defaultConceptSet
    // change access, get and check
    stubWorkspaceAccessLevel(workspace, RawlsWorkspaceAccessLevel.READER);

    ConceptSet retrieved =
        conceptSetsController
            .getConceptSet(workspace.getNamespace(), workspace.getId(), defaultConceptSet.getId())
            .getBody();

    assertThat(retrieved).isEqualTo(defaultConceptSet.participantCount(0));
  }

  @Test
  public void getConceptSetNoAccess() {
    // use defaultConceptSet
    // change access, get and chaeck
    stubWorkspaceAccessLevel(workspace, RawlsWorkspaceAccessLevel.NO_ACCESS);

    Throwable exception =
        assertThrows(
            ForbiddenException.class,
            () ->
                conceptSetsController.getConceptSet(
                    workspace.getNamespace(), workspace.getId(), defaultConceptSet.getId()));

    assertForbiddenException(exception);
  }

  //////////// getConceptSetsInWorkspace tests ////////////

  @Test
  public void getConceptSetsInWorkspaceEmpty() {
    assertThat(
            conceptSetsController
                .getConceptSetsInWorkspace(workspace2.getNamespace(), workspace2.getId())
                .getBody()
                .getItems())
        .isEmpty();
  }

  @Test
  public void getConceptSetsInWorkspaceOne() {
    // use defaultConceptSet
    List<ConceptSet> response =
        conceptSetsController
            .getConceptSetsInWorkspace(workspace.getNamespace(), workspace.getId())
            .getBody()
            .getItems();

    assertThat(response.size()).isEqualTo(1);
    assertConceptSet(response.get(0), defaultConceptSet);
  }

  @Test
  public void countConceptsInConceptSet() {
    // use defaultConceptSet
    assertThat(
            conceptSetsController
                .countConceptsInConceptSet(
                    workspace.getNamespace(), workspace.getId(), defaultConceptSet.getId())
                .getBody())
        .isEqualTo(1);
  }

  @Test
  public void getConceptSetsInWorkspaceMany() {
    // use defaultConceptSet
    ConceptSet conceptSet2 =
        createTestConceptSet(
            workspace,
            CONCEPT_SET_NAME_1 + "2",
            CONCEPT_SET_DESC_1 + "2",
            Domain.CONDITION,
            CRITERIA_CONDITION_1,
            CRITERIA_CONDITION_2,
            CRITERIA_CONDITION_3);
    ConceptSet conceptSet3 =
        createTestConceptSet(
            workspace,
            CONCEPT_SET_NAME_1 + "3",
            CONCEPT_SET_DESC_1 + "3",
            Domain.MEASUREMENT,
            CRITERIA_MEASUREMENT_1);
    ConceptSet conceptSet4 =
        createTestConceptSet(
            workspace,
            CONCEPT_SET_NAME_1 + "4",
            CONCEPT_SET_DESC_1 + "4",
            Domain.OBSERVATION,
            CRITERIA_SURVEY_1);

    // check for 4 conceptSets
    List<ConceptSet> response =
        conceptSetsController
            .getConceptSetsInWorkspace(workspace.getNamespace(), workspace.getId())
            .getBody()
            .getItems();

    assertThat(response.size()).isEqualTo(4);
    // defaultConceptSet
    assertConceptSet(response.get(0), defaultConceptSet);
    // conceptSet2
    assertConceptSet(response.get(1), conceptSet2);
    // conceptSet3
    assertConceptSet(response.get(2), conceptSet3);
    // conceptSet4
    assertConceptSet(response.get(3), conceptSet4);
  }

  @Test
  public void getConceptSetsInWorkspaceWrongWorkspace() {
    // use defaultConceptSet
    // access from workspace2 check is empty
    assertThat(
            conceptSetsController
                .getConceptSetsInWorkspace(workspace2.getNamespace(), workspace2.getId())
                .getBody()
                .getItems())
        .isEmpty();
  }

  @Test
  public void getConceptSetsInWorkspaceOwner() {
    // use defaultConceptSet
    // change access, call and check
    stubWorkspaceAccessLevel(workspace, RawlsWorkspaceAccessLevel.OWNER);

    List<ConceptSet> response =
        conceptSetsController
            .getConceptSetsInWorkspace(workspace.getNamespace(), workspace.getId())
            .getBody()
            .getItems();

    assertThat(response.size()).isEqualTo(1);
    assertConceptSet(response.get(0), defaultConceptSet);
  }

  @Test
  public void getConceptSetsInWorkspaceWriter() {
    // use defaultConceptSet
    // change access, call and check
    stubWorkspaceAccessLevel(workspace, RawlsWorkspaceAccessLevel.WRITER);

    List<ConceptSet> response =
        conceptSetsController
            .getConceptSetsInWorkspace(workspace.getNamespace(), workspace.getId())
            .getBody()
            .getItems();

    assertThat(response.size()).isEqualTo(1);
    assertConceptSet(response.get(0), defaultConceptSet);
  }

  @Test
  public void getConceptSetsInWorkspaceReader() {
    // use defaultConceptSet
    // change access, call and check
    stubWorkspaceAccessLevel(workspace, RawlsWorkspaceAccessLevel.READER);

    List<ConceptSet> response =
        conceptSetsController
            .getConceptSetsInWorkspace(workspace.getNamespace(), workspace.getId())
            .getBody()
            .getItems();

    assertThat(response.size()).isEqualTo(1);
    assertConceptSet(response.get(0), defaultConceptSet);
  }

  @Test
  public void getConceptSetsInWorkspaceNoAccess() {
    // use defaultConceptSet
    // change access, call and check
    stubWorkspaceAccessLevel(workspace, RawlsWorkspaceAccessLevel.NO_ACCESS);

    Throwable exception =
        assertThrows(
            ForbiddenException.class,
            () ->
                conceptSetsController.getConceptSetsInWorkspace(
                    workspace.getNamespace(), workspace.getId()));

    assertForbiddenException(exception);
  }

  //////////// updateConceptSet tests ////////////

  @Test
  public void updateConceptSetNotExists() {
    ConceptSet conceptSet =
        new ConceptSet()
            .description(CONCEPT_SET_DESC_1)
            .name(CONCEPT_SET_NAME_1)
            .domain(Domain.CONDITION)
            .id(1L)
            .etag(Etags.fromVersion(1));

    Throwable exception =
        assertThrows(
            NotFoundException.class,
            () ->
                conceptSetsController.updateConceptSet(
                    workspace2.getNamespace(), workspace2.getId(), conceptSet.getId(), conceptSet));

    assertNotFoundException(exception, conceptSet.getId());
  }

  @Test
  public void updateConceptSetWrongWorkspace() {
    // use defaultConceptSet
    // check update in workspace2
    Throwable exception =
        assertThrows(
            NotFoundException.class,
            () ->
                conceptSetsController.updateConceptSet(
                    workspace2.getNamespace(),
                    workspace2.getId(),
                    defaultConceptSet.getId(),
                    defaultConceptSet));

    assertNotFoundException(exception, defaultConceptSet.getId());
  }

  @Test
  public void testUpdateConceptSetDomainChange() {
    // use defaultConceptSet
    // change domain when updating
    defaultConceptSet.setDescription(UPDATED_DESC);
    defaultConceptSet.setName(UPDATED_NAME);
    defaultConceptSet.domain(Domain.PROCEDURE);

    Throwable exception =
        assertThrows(
            ConflictException.class,
            () ->
                conceptSetsController.updateConceptSet(
                    workspace.getNamespace(),
                    workspace.getId(),
                    defaultConceptSet.getId(),
                    defaultConceptSet));

    assertThat(exception).hasMessageThat().containsMatch("Concept Set is not the same domain");
  }

  @Test
  public void testUpdateConceptSetWrongEtag() {
    // use defaultConceptSet
    defaultConceptSet.setDescription(UPDATED_DESC);
    defaultConceptSet.setName(UPDATED_NAME);
    defaultConceptSet.setEtag(Etags.fromVersion(2));

    Throwable exception =
        assertThrows(
            ConflictException.class,
            () ->
                conceptSetsController.updateConceptSet(
                    workspace.getNamespace(),
                    workspace.getId(),
                    defaultConceptSet.getId(),
                    defaultConceptSet));

    assertThat(exception)
        .hasMessageThat()
        .containsMatch("Attempted to modify outdated concept set version");
  }

  @Test
  public void updateConceptSetOwner() {
    // use defaultConceptSet
    // change access, update and check
    stubWorkspaceAccessLevel(workspace, RawlsWorkspaceAccessLevel.OWNER);
    fakeClock.increment(1000L);

    ConceptSet updatedConceptSet =
        conceptSetsController
            .updateConceptSet(
                workspace.getNamespace(),
                workspace.getId(),
                defaultConceptSet.getId(),
                defaultConceptSet.description(UPDATED_DESC).name(UPDATED_NAME))
            .getBody();

    assertConceptSets(updatedConceptSet, defaultConceptSet);
  }

  @Test
  public void updateConceptSetWriter() {
    // use defaultConceptSet
    // change access, update and check
    stubWorkspaceAccessLevel(workspace, RawlsWorkspaceAccessLevel.WRITER);
    fakeClock.increment(1000L);

    ConceptSet updatedConceptSet =
        conceptSetsController
            .updateConceptSet(
                workspace.getNamespace(),
                workspace.getId(),
                defaultConceptSet.getId(),
                defaultConceptSet.description(UPDATED_DESC).name(UPDATED_NAME))
            .getBody();

    assertConceptSets(updatedConceptSet, defaultConceptSet);
  }

  @Test
  public void updateConceptSetReader() {
    // use defaultConceptSet
    // change access, update and check
    stubWorkspaceAccessLevel(workspace, RawlsWorkspaceAccessLevel.READER);
    fakeClock.increment(1000L);

    Throwable exception =
        assertThrows(
            ForbiddenException.class,
            () ->
                conceptSetsController.updateConceptSet(
                    workspace.getNamespace(),
                    workspace.getId(),
                    defaultConceptSet.getId(),
                    defaultConceptSet.description(UPDATED_DESC).name(UPDATED_NAME)));

    assertForbiddenException(exception);
  }

  @Test
  public void updateConceptSetNoAccess() {
    // use defaultConceptSet
    // change access, update and check
    stubWorkspaceAccessLevel(workspace, RawlsWorkspaceAccessLevel.NO_ACCESS);
    fakeClock.increment(1000L);

    Throwable exception =
        assertThrows(
            ForbiddenException.class,
            () ->
                conceptSetsController.updateConceptSet(
                    workspace.getNamespace(),
                    workspace.getId(),
                    defaultConceptSet.getId(),
                    defaultConceptSet.description(UPDATED_DESC).name(UPDATED_NAME)));

    assertForbiddenException(exception);
  }

  //////////// updateConceptSetConcepts tests ////////////

  @Test
  public void updateConceptSetConceptsWrongEtag() {
    // use defaultConceptSet
    // build request with incorrect eTag version
    Throwable exception =
        assertThrows(
            ConflictException.class,
            () ->
                conceptSetsController.updateConceptSetConcepts(
                    workspace.getNamespace(),
                    WORKSPACE_NAME,
                    defaultConceptSet.getId(),
                    buildUpdateConceptsRequest(
                        Etags.fromVersion(2), CRITERIA_CONDITION_2.getConceptId())));

    assertThat(exception)
        .hasMessageThat()
        .containsMatch("Attempted to modify outdated concept set version");
  }

  @Test
  public void updateConceptSetConceptsWrongCriteriaDomain() {
    // use defaultConceptSet
    // check update concept for incorrect criteria domain
    UpdateConceptSetRequest updateConceptSetRequest =
        buildUpdateConceptsRequest(defaultConceptSet.getEtag(), CRITERIA_SURVEY_1.getConceptId());

    ConceptSet updated =
        conceptSetsController
            .updateConceptSetConcepts(
                workspace.getNamespace(),
                workspace.getId(),
                defaultConceptSet.getId(),
                updateConceptSetRequest)
            .getBody();

    // incorrect criteria domain is-not-added
    assertThat(updated.getEtag()).isNotEqualTo(defaultConceptSet.getEtag());
    updated.setEtag(defaultConceptSet.getEtag());
    assertThat(updated).isEqualTo(defaultConceptSet);
  }

  @Test
  public void updateConceptSetConceptsAddMany() {
    // use defaultConceptSet
    // check update concept set - add criteriums same domain
    UpdateConceptSetRequest updateConceptSetRequest =
        buildUpdateConceptsRequest(
            defaultConceptSet.getEtag(),
            CRITERIA_CONDITION_2.getConceptId(),
            CRITERIA_CONDITION_3.getConceptId());
    fakeClock.increment(1000L);

    ConceptSet updated =
        conceptSetsController
            .updateConceptSetConcepts(
                workspace.getNamespace(),
                workspace.getId(),
                defaultConceptSet.getId(),
                updateConceptSetRequest)
            .getBody();

    List<Criteria> expectedCriteriums =
        createExpectedCriteria(
            updated.getCriteriums(),
            ImmutableList.of(CRITERIA_CONDITION_1, CRITERIA_CONDITION_2, CRITERIA_CONDITION_3));

    assertUpdatedConceptSetConcepts(defaultConceptSet, updated, expectedCriteriums);
  }

  @Test
  public void updateConceptSetConceptsAddAndRemove() {
    // create concept set as owner or writer in workspace
    ConceptSet conceptSet =
        createTestConceptSet(
            workspace,
            CONCEPT_SET_NAME_1,
            CONCEPT_SET_DESC_1,
            Domain.CONDITION,
            CRITERIA_CONDITION_1,
            CRITERIA_CONDITION_2);
    // check update concept set - add new concept and remove an existing concept
    UpdateConceptSetRequest updateConceptSetRequest =
        new UpdateConceptSetRequest().etag(conceptSet.getEtag());
    // add
    ConceptSetConceptId addedConcept3 =
        new ConceptSetConceptId()
            .conceptId(CRITERIA_CONDITION_3.getConceptId())
            .standard(CRITERIA_CONDITION_3.isStandard());
    updateConceptSetRequest.addAddedConceptSetConceptIdsItem(addedConcept3);
    // remove
    ConceptSetConceptId removedConcept1 =
        new ConceptSetConceptId()
            .conceptId(CRITERIA_CONDITION_1.getConceptId())
            .standard(CRITERIA_CONDITION_1.isStandard());
    updateConceptSetRequest.addRemovedConceptSetConceptIdsItem(removedConcept1);
    fakeClock.increment(1000L);

    ConceptSet updated =
        conceptSetsController
            .updateConceptSetConcepts(
                workspace.getNamespace(),
                workspace.getId(),
                conceptSet.getId(),
                updateConceptSetRequest)
            .getBody();

    List<Criteria> expectedCriteriums =
        createExpectedCriteria(
            updated.getCriteriums(), ImmutableList.of(CRITERIA_CONDITION_2, CRITERIA_CONDITION_3));

    // updated should contain only cond_2 (cond_1 removed) and cond_3 (added)
    assertUpdatedConceptSetConcepts(conceptSet, updated, expectedCriteriums);
  }

  @Test
  public void updateConceptSetConceptsAddTooMany() {
    // use defaultConceptSet
    // set max concepts ot add
    ConceptSetService.MAX_CONCEPTS_PER_SET = 2;
    // check update concept set - add new concept and remove an existing concept
    UpdateConceptSetRequest updateConceptSetRequest =
        buildUpdateConceptsRequest(
            defaultConceptSet.getEtag(),
            CRITERIA_CONDITION_2.getConceptId(),
            CRITERIA_CONDITION_3.getConceptId());
    fakeClock.increment(1000L);

    Throwable exception =
        assertThrows(
            ConflictException.class,
            () ->
                conceptSetsController.updateConceptSetConcepts(
                    workspace.getNamespace(),
                    workspace.getId(),
                    defaultConceptSet.getId(),
                    updateConceptSetRequest));

    assertThat(exception)
        .hasMessageThat()
        .containsMatch(
            String.format("Exceeded %d concept set limit", ConceptSetService.MAX_CONCEPTS_PER_SET));
  }

  @Test
  public void updateConceptSetConceptsOwner() {
    // use defaultConceptSet
    // change access, update and check
    stubWorkspaceAccessLevel(workspace, RawlsWorkspaceAccessLevel.OWNER);
    UpdateConceptSetRequest updateConceptSetRequest =
        buildUpdateConceptsRequest(
            defaultConceptSet.getEtag(), CRITERIA_CONDITION_2.getConceptId());
    fakeClock.increment(1000L);

    ConceptSet updated =
        conceptSetsController
            .updateConceptSetConcepts(
                workspace.getNamespace(),
                workspace.getId(),
                defaultConceptSet.getId(),
                updateConceptSetRequest)
            .getBody();

    List<Criteria> expectedCriteriums =
        createExpectedCriteria(
            updated.getCriteriums(), ImmutableList.of(CRITERIA_CONDITION_1, CRITERIA_CONDITION_2));

    assertUpdatedConceptSetConcepts(defaultConceptSet, updated, expectedCriteriums);
  }

  @Test
  public void updateConceptSetConceptsWriter() {
    // use defaultConceptSet
    // change access, update and check
    stubWorkspaceAccessLevel(workspace, RawlsWorkspaceAccessLevel.WRITER);
    UpdateConceptSetRequest updateConceptSetRequest =
        buildUpdateConceptsRequest(
            defaultConceptSet.getEtag(), CRITERIA_CONDITION_2.getConceptId());
    fakeClock.increment(1000L);

    ConceptSet updated =
        conceptSetsController
            .updateConceptSetConcepts(
                workspace.getNamespace(),
                workspace.getId(),
                defaultConceptSet.getId(),
                updateConceptSetRequest)
            .getBody();

    List<Criteria> expectedCriteriums =
        createExpectedCriteria(
            updated.getCriteriums(), ImmutableList.of(CRITERIA_CONDITION_1, CRITERIA_CONDITION_2));

    assertUpdatedConceptSetConcepts(defaultConceptSet, updated, expectedCriteriums);
  }

  @Test
  public void updateConceptSetConceptsReader() {
    // use defaultConceptSet
    // change access, update and check
    stubWorkspaceAccessLevel(workspace, RawlsWorkspaceAccessLevel.READER);
    UpdateConceptSetRequest updateConceptSetRequest =
        buildUpdateConceptsRequest(
            defaultConceptSet.getEtag(), CRITERIA_CONDITION_2.getConceptId());
    fakeClock.increment(1000L);

    Throwable exception =
        assertThrows(
            ForbiddenException.class,
            () ->
                conceptSetsController.updateConceptSetConcepts(
                    workspace.getNamespace(),
                    workspace.getId(),
                    defaultConceptSet.getId(),
                    updateConceptSetRequest));

    assertForbiddenException(exception);
  }

  @Test
  public void updateConceptSetConceptsNoAccess() {
    // use defaultConceptSet
    // change access, update and check
    stubWorkspaceAccessLevel(workspace, RawlsWorkspaceAccessLevel.NO_ACCESS);
    UpdateConceptSetRequest updateConceptSetRequest =
        buildUpdateConceptsRequest(
            defaultConceptSet.getEtag(), CRITERIA_CONDITION_2.getConceptId());
    fakeClock.increment(1000L);

    Throwable exception =
        assertThrows(
            ForbiddenException.class,
            () ->
                conceptSetsController.updateConceptSetConcepts(
                    workspace.getNamespace(),
                    workspace.getId(),
                    defaultConceptSet.getId(),
                    updateConceptSetRequest));

    assertForbiddenException(exception);
  }

  //////////// copyConceptSet tests ////////////

  @Test
  public void copyConceptSetOwner() {
    // from: workspace
    ConceptSet conceptSet =
        createTestConceptSet(
            workspace,
            CONCEPT_SET_NAME_1 + "+2",
            CONCEPT_SET_DESC_1 + "+2",
            Domain.CONDITION,
            CRITERIA_CONDITION_1,
            CRITERIA_CONDITION_2,
            CRITERIA_CONDITION_3);
    // copy to: workspace2 owner
    stubWorkspaceAccessLevel(workspace2, RawlsWorkspaceAccessLevel.OWNER);
    CopyRequest copyRequest =
        new CopyRequest()
            .newName(conceptSet.getName() + "_copy")
            .toWorkspaceName(workspace2.getName())
            .toWorkspaceNamespace(workspace2.getNamespace());

    ConceptSet conceptSetCopy =
        conceptSetsController
            .copyConceptSet(
                workspace.getNamespace(),
                workspace.getId(),
                String.valueOf(conceptSet.getId()),
                copyRequest)
            .getBody();

    assertThat(conceptSetCopy.getName()).contains(conceptSet.getName());
    assertThat(conceptSetCopy.getName()).contains("_copy");
    assertThat(conceptSetCopy.getDescription()).isEqualTo(conceptSet.getDescription());
    assertThat(conceptSet.getCriteriums())
        .containsAtLeastElementsIn(conceptSetCopy.getCriteriums())
        .inOrder();
  }

  @Test
  public void copyConceptSetWriter() {
    // from: workspace
    ConceptSet conceptSet =
        createTestConceptSet(
            workspace,
            CONCEPT_SET_NAME_1 + "+2",
            CONCEPT_SET_DESC_1 + "+2",
            Domain.CONDITION,
            CRITERIA_CONDITION_1,
            CRITERIA_CONDITION_2,
            CRITERIA_CONDITION_3);
    // copy to: workspace2 writer
    stubWorkspaceAccessLevel(workspace2, RawlsWorkspaceAccessLevel.WRITER);
    CopyRequest copyRequest =
        new CopyRequest()
            .newName(conceptSet.getName() + "_copy")
            .toWorkspaceName(workspace2.getName())
            .toWorkspaceNamespace(workspace2.getNamespace());

    ConceptSet conceptSetCopy =
        conceptSetsController
            .copyConceptSet(
                workspace.getNamespace(),
                workspace.getId(),
                String.valueOf(conceptSet.getId()),
                copyRequest)
            .getBody();

    assertThat(conceptSetCopy.getName()).contains(conceptSet.getName());
    assertThat(conceptSetCopy.getName()).contains("_copy");
    assertThat(conceptSetCopy.getDescription()).isEqualTo(conceptSet.getDescription());
    assertThat(conceptSet.getCriteriums())
        .containsAtLeastElementsIn(conceptSetCopy.getCriteriums())
        .inOrder();
  }

  @Test
  public void copyConceptSetReader() {
    // from: workspace
    // use defaultConceptSet
    // copy to: workspace2 reader
    stubWorkspaceAccessLevel(workspace2, RawlsWorkspaceAccessLevel.READER);
    CopyRequest copyRequest =
        new CopyRequest()
            .newName(defaultConceptSet.getName() + "_copy")
            .toWorkspaceName(workspace2.getName())
            .toWorkspaceNamespace(workspace2.getNamespace());

    Throwable exception =
        assertThrows(
            ForbiddenException.class,
            () ->
                conceptSetsController.copyConceptSet(
                    workspace.getNamespace(),
                    workspace.getId(),
                    String.valueOf(defaultConceptSet.getId()),
                    copyRequest));

    assertForbiddenException(exception);
  }

  @Test
  public void copyConceptSetNoAccess() {
    // from: workspace
    // use defaultConceptSet
    // copy to: workspace2 reader
    stubWorkspaceAccessLevel(workspace2, RawlsWorkspaceAccessLevel.NO_ACCESS);
    CopyRequest copyRequest =
        new CopyRequest()
            .newName(defaultConceptSet.getName() + "_copy")
            .toWorkspaceName(workspace2.getName())
            .toWorkspaceNamespace(workspace2.getNamespace());

    Throwable exception =
        assertThrows(
            ForbiddenException.class,
            () ->
                conceptSetsController.copyConceptSet(
                    workspace.getNamespace(),
                    workspace.getId(),
                    String.valueOf(defaultConceptSet.getId()),
                    copyRequest));

    assertForbiddenException(exception);
  }

  //////////// validate* tests ////////////
  @Test
  public void validateCreateConceptSetRequestNoConcepts() {
    // just defaultConceptSet not enough also need non null set of defaultConceptSet ids
    final CreateConceptSetRequest createConceptSetRequest =
        new CreateConceptSetRequest().conceptSet(new ConceptSet().domain(Domain.CONDITION));

    Throwable exception =
        assertThrows(
            BadRequestException.class,
            () -> conceptSetsController.validateCreateConceptSetRequest(createConceptSetRequest),
            "Expected BadRequestException not thrown");

    assertThat(exception).hasMessageThat().contains("Cannot create a concept set with no concepts");
  }

  @Test
  public void validateCreateConceptSetRequestEmptyConcepts() {
    // also need non-emptyList of conceptSetIds
    final CreateConceptSetRequest createConceptSetRequest =
        new CreateConceptSetRequest()
            .conceptSet(new ConceptSet().domain(Domain.CONDITION))
            .addedConceptSetConceptIds(new ArrayList<>());

    Throwable exception =
        assertThrows(
            BadRequestException.class,
            () -> conceptSetsController.validateCreateConceptSetRequest(createConceptSetRequest),
            "Expected BadRequestException not thrown");

    assertThat(exception).hasMessageThat().contains("Cannot create a concept set with no concepts");
  }

  @Test
  public void validateCreateConceptSetWithOneConceptId() {
    // also need non-emptyList of conceptSetIds
    final CreateConceptSetRequest createConceptSetRequest =
        new CreateConceptSetRequest()
            .conceptSet(new ConceptSet().domain(Domain.CONDITION))
            .addAddedConceptSetConceptIdsItem(new ConceptSetConceptId().conceptId(1L));

    assertDoesNotThrow(
        () -> conceptSetsController.validateCreateConceptSetRequest(createConceptSetRequest),
        "BadRequestException is not expected to be thrown");
  }

  @Test
  public void validateCreateConceptSetWithManyConceptIds() {
    // also need non-emptyList of conceptSetIds
    final CreateConceptSetRequest createConceptSetRequest =
        new CreateConceptSetRequest()
            .conceptSet(new ConceptSet().domain(Domain.CONDITION))
            .addedConceptSetConceptIds(
                ImmutableList.of(
                    new ConceptSetConceptId().conceptId(1L),
                    new ConceptSetConceptId().conceptId(2L)));

    assertDoesNotThrow(
        () -> conceptSetsController.validateCreateConceptSetRequest(createConceptSetRequest),
        "BadRequestException is not expected to be thrown");
  }

  @Test
  public void validateUpdateConceptSetMissingEtagAndDomain() {
    // invalid empty object need etag and domain
    Throwable exception =
        assertThrows(
            BadRequestException.class,
            () -> conceptSetsController.validateUpdateConceptSet(new ConceptSet()),
            "Expected BadRequestException not thrown");

    assertThat(exception).hasMessageThat().contains("missing required update field 'etag'");
  }

  @Test
  public void validateUpdateConceptSetMissingDomain() {
    // invalid empty object need etag and domain
    final ConceptSet conceptSet = new ConceptSet().etag("testEtag");

    Throwable exception =
        assertThrows(
            BadRequestException.class,
            () -> conceptSetsController.validateUpdateConceptSet(conceptSet),
            "Expected BadRequestException not thrown");

    assertThat(exception).hasMessageThat().contains("Domain cannot be null");
  }

  @Test
  public void validateUpdateConceptSetWithEtagAndDomain() {
    // add etag and domain
    final ConceptSet conceptSet = new ConceptSet().etag("testEtag").domain(Domain.CONDITION);

    assertDoesNotThrow(
        () -> conceptSetsController.validateUpdateConceptSet(conceptSet),
        "BadRequestException is not expected to be thrown");
  }

  @Test
  public void validateUpdateConceptSetConceptsEmptyRequest() {
    // invalid empty object
    Throwable exception =
        assertThrows(
            BadRequestException.class,
            () ->
                conceptSetsController.validateUpdateConceptSetConcepts(
                    new UpdateConceptSetRequest()),
            "Expected BadRequestException not thrown");

    assertThat(exception).hasMessageThat().contains("missing required update field 'etag'");
  }

  @Test
  public void validateUpdateConceptSetConceptsWithEtag() {
    // add etag which is required
    assertDoesNotThrow(
        () ->
            conceptSetsController.validateUpdateConceptSetConcepts(
                new UpdateConceptSetRequest().etag("testEtag")),
        "BadRequestException is not expected to be thrown");
  }

  //////////// assertion helpers ////////////

  private List<Criteria> createExpectedCriteria(
      List<Criteria> actualCriteriums, List<Criteria> expectedCriteriums) {
    expectedCriteriums.forEach(
        expected -> {
          actualCriteriums.forEach(
              actual -> {
                if (actual.getConceptId().equals(expected.getConceptId())) {
                  expected.id(actual.getId()).parentId(actual.getParentId());
                }
              });
        });
    return expectedCriteriums;
  }

  private void assertConceptSets(ConceptSet actual, ConceptSet expected) {
    assertThat(actual.getCriteriums()).containsAtLeastElementsIn(expected.getCriteriums());
    assertThat(actual.getDescription()).isEqualTo(UPDATED_DESC);
    assertThat(actual.getName()).isEqualTo(UPDATED_NAME);
    assertThat(actual.getDomain()).isEqualTo(expected.getDomain());
    assertThat(actual.getCreationTime()).isEqualTo(expected.getCreationTime());
    assertThat(actual.getLastModifiedTime()).isGreaterThan(expected.getLastModifiedTime());
    assertThat(actual.getEtag()).isEqualTo(Etags.fromVersion(2));
  }

  private void assertForbiddenException(Throwable exception) {
    assertThat(exception)
        .hasMessageThat()
        .containsMatch("You do not have sufficient permissions to access");
  }

  private void assertNotFoundException(Throwable exception, Long conceptId) {
    assertThat(exception)
        .hasMessageThat()
        .containsMatch(
            String.format("ConceptSet not found for workspaceId.*and conceptSetId: %d", conceptId));
  }

  private void assertUpdatedConceptSetConcepts(
      ConceptSet initial, ConceptSet updated, List<Criteria> expectedCriteria) {
    assertThat(updated.getCreationTime()).isEqualTo(initial.getCreationTime());
    assertThat(updated.getLastModifiedTime()).isGreaterThan(initial.getLastModifiedTime());
    assertThat(updated.getEtag()).isNotEqualTo(initial.getEtag());
    assertThat(updated.getCriteriums().size()).isEqualTo(expectedCriteria.size());
    assertThat(updated.getCriteriums()).containsExactlyElementsIn(expectedCriteria);
  }

  private void assertConceptSetAndCriteria(ConceptSet conceptSet, List<Criteria> expectedCriteria) {
    assertThat(conceptSet.getCreationTime()).isEqualTo(NOW.toEpochMilli());
    assertThat(conceptSet.getDescription()).isEqualTo(CONCEPT_SET_DESC_1);
    assertThat(conceptSet.getDomain()).isEqualTo(Domain.CONDITION);
    assertThat(conceptSet.getEtag()).isEqualTo(Etags.fromVersion(1));
    assertThat(conceptSet.getLastModifiedTime()).isEqualTo(NOW.toEpochMilli());
    assertThat(conceptSet.getName()).isEqualTo(CONCEPT_SET_NAME_1);
    assertThat(conceptSet.getCriteriums().size()).isEqualTo(expectedCriteria.size());
    assertThat(conceptSet.getCriteriums()).containsExactlyElementsIn(expectedCriteria);
  }

  //////////// other helpers for setup and intermediate objects ////////////

  private ConceptSet createTestConceptSet(
      Workspace workspace,
      String name,
      String desc,
      Domain domain,
      Criteria... criteriumsForDomain) {
    // change access to owner and create
    stubWorkspaceAccessLevel(workspace, RawlsWorkspaceAccessLevel.OWNER);
    ConceptSet conceptSet = new ConceptSet().domain(domain).description(desc).name(name);
    // use only criteria that matches domain
    Long[] domainConceptIds =
        Arrays.stream(criteriumsForDomain)
            .filter(o -> o.getDomainId().equals(domain.toString()))
            .map(Criteria::getConceptId)
            .toArray(Long[]::new);

    CreateConceptSetRequest createConceptSetRequest =
        buildCreateConceptSetRequest(conceptSet, domainConceptIds);

    return conceptSetsController
        .createConceptSet(workspace.getNamespace(), workspace.getId(), createConceptSetRequest)
        .getBody();
  }

  private CreateConceptSetRequest buildCreateConceptSetRequest(
      ConceptSet conceptSet, Long... conceptIds) {
    return new CreateConceptSetRequest()
        .conceptSet(conceptSet)
        .addedConceptSetConceptIds(buildConceptSetConceptIdList(conceptIds));
  }

  private UpdateConceptSetRequest buildUpdateConceptsRequest(String etag, Long... conceptIds) {
    return new UpdateConceptSetRequest()
        .etag(etag)
        .addedConceptSetConceptIds(buildConceptSetConceptIdList(conceptIds));
  }

  private List<ConceptSetConceptId> buildConceptSetConceptIdList(Long... conceptIds) {
    return Arrays.stream(conceptIds)
        .map(c -> new ConceptSetConceptId().conceptId(c).standard(true))
        .collect(Collectors.toList());
  }

  private void stubWorkspaceAccessLevel(
      Workspace workspace, RawlsWorkspaceAccessLevel workspaceAccessLevel) {
    stubGetWorkspace(workspace.getNamespace(), workspace.getName(), workspaceAccessLevel);
    stubGetWorkspaceAcl(workspace.getNamespace(), workspace.getName(), workspaceAccessLevel);
  }

  private void stubGetWorkspace(
      String ns, String name, RawlsWorkspaceAccessLevel workspaceAccessLevel) {
    RawlsWorkspaceDetails fcWorkspace = new RawlsWorkspaceDetails();
    fcWorkspace.setNamespace(ns);
    fcWorkspace.setName(name);
    fcWorkspace.setCreatedBy(USER_EMAIL);
    RawlsWorkspaceResponse fcResponse = new RawlsWorkspaceResponse();
    fcResponse.setWorkspace(fcWorkspace);
    fcResponse.setAccessLevel(workspaceAccessLevel);
    when(fireCloudService.getWorkspace(ns, name)).thenReturn(fcResponse);
  }

  private void stubGetWorkspaceAcl(
      String ns, String name, RawlsWorkspaceAccessLevel workspaceAccessLevel) {
    RawlsWorkspaceACL workspaceAccessLevelResponse = new RawlsWorkspaceACL();
    RawlsWorkspaceAccessEntry accessLevelEntry =
        new RawlsWorkspaceAccessEntry().accessLevel(workspaceAccessLevel.toString());
    Map<String, RawlsWorkspaceAccessEntry> userEmailToAccessEntry =
        ImmutableMap.of(USER_EMAIL, accessLevelEntry);
    workspaceAccessLevelResponse.setAcl(userEmailToAccessEntry);
    when(fireCloudService.getWorkspaceAclAsService(ns, name))
        .thenReturn(workspaceAccessLevelResponse);
  }

  private Workspace createTestWorkspace(
      String workspaceNamespace,
      String workspaceName,
      long cdrVersionId,
      RawlsWorkspaceAccessLevel workspaceAccessLevel) {
    Workspace tmpWorkspace = new Workspace();
    tmpWorkspace.setName(workspaceName);
    tmpWorkspace.setNamespace(workspaceNamespace);
    tmpWorkspace.setResearchPurpose(new ResearchPurpose());
    tmpWorkspace.setCdrVersionId(String.valueOf(cdrVersionId));
    tmpWorkspace.setBillingAccountName("billing-account");

    TestMockFactory.stubCreateFcWorkspace(fireCloudService);

    tmpWorkspace = workspacesController.createWorkspace(tmpWorkspace).getBody();
    stubWorkspaceAccessLevel(tmpWorkspace, workspaceAccessLevel);

    return tmpWorkspace;
  }

  private void saveAllTestCriteria() {
    cbCriteriaDao.save(makeDbCriteria(CRITERIA_CONDITION_1));
    cbCriteriaDao.save(makeDbCriteria(CRITERIA_CONDITION_2));
    cbCriteriaDao.save(makeDbCriteria(CRITERIA_CONDITION_3));
    cbCriteriaDao.save(makeDbCriteria(CRITERIA_MEASUREMENT_1));
    cbCriteriaDao.save(makeDbCriteria(CRITERIA_SURVEY_1));
  }

  private static DbCriteria makeDbCriteria(Criteria criteria) {
    return DbCriteria.builder()
        .addConceptId(criteria.getConceptId().toString())
        .addParentCount(criteria.getParentCount())
        .addChildCount(criteria.getChildCount())
        .addType(criteria.getType())
        .addDomainId(criteria.getDomainId())
        .addCode(criteria.getCode())
        .addStandard(criteria.isStandard())
        .addName(criteria.getName())
        .addFullText("+[" + criteria.getDomainId() + "_rank1]")
        .build();
  }

  private void assertConceptSet(ConceptSet actualConceptSet, ConceptSet expectedConceptSet) {
    assertThat(actualConceptSet.getCriteriums()).isNotNull();
    assertThat(actualConceptSet.getCreationTime()).isEqualTo(expectedConceptSet.getCreationTime());
    assertThat(actualConceptSet.getDescription()).isEqualTo(expectedConceptSet.getDescription());
    assertThat(actualConceptSet.getDomain()).isEqualTo(expectedConceptSet.getDomain());
    assertThat(actualConceptSet.getEtag()).isEqualTo(expectedConceptSet.getEtag());
    assertThat(actualConceptSet.getLastModifiedTime())
        .isEqualTo(expectedConceptSet.getLastModifiedTime());
    assertThat(actualConceptSet.getName()).isEqualTo(expectedConceptSet.getName());
  }
}
