package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.google.appengine.repackaged.com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
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
import org.pmiops.workbench.billing.FreeTierBillingService;
import org.pmiops.workbench.cdr.ConceptBigQueryService;
import org.pmiops.workbench.cdr.dao.CBCriteriaDao;
import org.pmiops.workbench.cdr.model.DbCriteria;
import org.pmiops.workbench.cdrselector.WorkspaceResourcesServiceImpl;
import org.pmiops.workbench.cohortbuilder.CohortBuilderServiceImpl;
import org.pmiops.workbench.cohortbuilder.CohortQueryBuilder;
import org.pmiops.workbench.cohortbuilder.mapper.CohortBuilderMapperImpl;
import org.pmiops.workbench.cohortreview.CohortReviewServiceImpl;
import org.pmiops.workbench.cohortreview.mapper.CohortReviewMapperImpl;
import org.pmiops.workbench.cohorts.CohortCloningService;
import org.pmiops.workbench.cohorts.CohortFactoryImpl;
import org.pmiops.workbench.cohorts.CohortMapperImpl;
import org.pmiops.workbench.cohorts.CohortService;
import org.pmiops.workbench.compliance.ComplianceService;
import org.pmiops.workbench.conceptset.ConceptSetService;
import org.pmiops.workbench.conceptset.mapper.ConceptSetMapperImpl;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.dataset.DataSetService;
import org.pmiops.workbench.dataset.mapper.DataSetMapperImpl;
import org.pmiops.workbench.db.dao.AccessTierDao;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserRecentResourceService;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbConceptSetConceptId;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceACL;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceAccessEntry;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceDetails;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceResponse;
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
import org.pmiops.workbench.model.Surveys;
import org.pmiops.workbench.model.UpdateConceptSetRequest;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.monitoring.LogsBasedMetricServiceFakeImpl;
import org.pmiops.workbench.notebooks.NotebooksService;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.test.FakeLongRandom;
import org.pmiops.workbench.testconfig.UserServiceTestConfiguration;
import org.pmiops.workbench.utils.TestMockFactory;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.utils.mappers.FirecloudMapperImpl;
import org.pmiops.workbench.utils.mappers.UserMapperImpl;
import org.pmiops.workbench.utils.mappers.WorkspaceMapperImpl;
import org.pmiops.workbench.workspaces.WorkspaceAuthService;
import org.pmiops.workbench.workspaces.WorkspaceServiceImpl;
import org.pmiops.workbench.workspaces.resources.WorkspaceResourceMapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
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

  private static final Criteria CRITERIA_CONDITION_1 =
      new Criteria()
          .conceptId(123L)
          .name("a concept")
          .isStandard(true)
          .code("conceptA")
          .type("V1")
          .domainId(Domain.CONDITION.toString())
          .childCount(123L)
          .parentCount(123L)
          .count(0L);

  private static final Criteria CRITERIA_MEASUREMENT_1 =
      new Criteria()
          .conceptId(456L)
          .isStandard(true)
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
          .isStandard(true)
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
          .isStandard(true)
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
          .isStandard(true)
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
  private static DbUser currentUser;
  private Workspace workspace;
  private Workspace workspace2;

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
    CohortReviewMapperImpl.class,
    CohortReviewServiceImpl.class,
    CohortService.class,
    CohortQueryBuilder.class,
    ComplianceService.class,
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
    UserRecentResourceService.class,
    UserServiceAuditor.class,
    WorkspaceAuditor.class,
  })
  static class Configuration {
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
      workbenchConfig.billing.accountId = "free-tier";
      return workbenchConfig;
    }
  }

  @BeforeEach
  public void setUp() throws Exception {
    TestMockFactory.stubCreateBillingProject(fireCloudService);
    TestMockFactory.stubCreateFcWorkspace(fireCloudService);
    TestMockFactory.stubPollCloudBillingLinked(cloudBillingClient, "billing-account");

    DbUser user = new DbUser();
    user.setUsername(USER_EMAIL);
    user.setUserId(123L);
    user.setDisabled(false);
    user = userDao.save(user);
    currentUser = user;

    DbCdrVersion cdrVersion = TestMockFactory.createDefaultCdrVersion(cdrVersionDao, accessTierDao);
    workspace =
        createTestWorkspace(
            WORKSPACE_NAMESPACE,
            WORKSPACE_NAME,
            cdrVersion.getCdrVersionId(),
            WorkspaceAccessLevel.OWNER);
    workspace2 =
        createTestWorkspace(
            WORKSPACE_NAMESPACE,
            WORKSPACE_NAME_2,
            cdrVersion.getCdrVersionId(),
            WorkspaceAccessLevel.OWNER);
    // save different criteria (there is no workspace associated with criteria
    saveAllTestCriteria();
  }

  //////////// savedConceptSet tests ////////////
  @Test
  public void createConceptSetOwnerAddMultipleCriteria() {
    // change access, create and check
    stubWorkspaceAccessLevel(workspace, WorkspaceAccessLevel.OWNER);
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

    assertConceptSetAndCriteria(
        savedConceptSet, CRITERIA_CONDITION_1, CRITERIA_CONDITION_2, CRITERIA_CONDITION_3);
  }

  @Test
  public void createConceptSetOwner() {
    // change access, create and check
    stubWorkspaceAccessLevel(workspace, WorkspaceAccessLevel.OWNER);
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

    assertConceptSetAndCriteria(savedConceptSet, CRITERIA_CONDITION_1);
  }

  @Test
  public void createConceptSetWriter() {
    // change access, create and check
    stubWorkspaceAccessLevel(workspace, WorkspaceAccessLevel.WRITER);
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

    assertConceptSetAndCriteria(savedConceptSet, CRITERIA_CONDITION_1);
  }

  @Test
  public void createConceptSetReader() {
    // change access, create and check
    stubWorkspaceAccessLevel(workspace, WorkspaceAccessLevel.READER);
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
    stubWorkspaceAccessLevel(workspace, WorkspaceAccessLevel.NO_ACCESS);
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
    // create concept set as owner or writer
    ConceptSet conceptSet =
        createTestConceptSet(
            workspace,
            CONCEPT_SET_NAME_1,
            CONCEPT_SET_DESC_1,
            Domain.CONDITION,
            CRITERIA_CONDITION_1);
    // change access, create and check
    stubWorkspaceAccessLevel(workspace, WorkspaceAccessLevel.OWNER);

    ResponseEntity<EmptyResponse> response =
        conceptSetsController.deleteConceptSet(
            workspace.getNamespace(), WORKSPACE_NAME, conceptSet.getId());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    Throwable exception =
        assertThrows(
            NotFoundException.class,
            () ->
                conceptSetsController.getConceptSet(
                    workspace.getNamespace(), workspace.getId(), conceptSet.getId()));

    assertNotFoundException(exception, conceptSet.getId());
  }

  @Test
  public void deleteConceptSetWriter() {
    // create concept set as owner or writer
    ConceptSet conceptSet =
        createTestConceptSet(
            workspace,
            CONCEPT_SET_NAME_1,
            CONCEPT_SET_DESC_1,
            Domain.CONDITION,
            CRITERIA_CONDITION_1);
    // change access, create and check
    stubWorkspaceAccessLevel(workspace, WorkspaceAccessLevel.WRITER);

    ResponseEntity<EmptyResponse> response =
        conceptSetsController.deleteConceptSet(
            workspace.getNamespace(), WORKSPACE_NAME, conceptSet.getId());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    Throwable exception =
        assertThrows(
            NotFoundException.class,
            () ->
                conceptSetsController.getConceptSet(
                    workspace.getNamespace(), workspace.getId(), conceptSet.getId()));
    assertNotFoundException(exception, conceptSet.getId());
  }

  @Test
  public void deleteConceptSetReader() {
    // create concept set as owner or writer
    ConceptSet conceptSet =
        createTestConceptSet(
            workspace,
            CONCEPT_SET_NAME_1,
            CONCEPT_SET_DESC_1,
            Domain.CONDITION,
            CRITERIA_CONDITION_1);
    // change access, create and check
    stubWorkspaceAccessLevel(workspace, WorkspaceAccessLevel.READER);

    Throwable exception =
        assertThrows(
            ForbiddenException.class,
            () ->
                conceptSetsController.deleteConceptSet(
                    workspace.getNamespace(), WORKSPACE_NAME, conceptSet.getId()));
    assertForbiddenException(exception);
  }

  @Test
  public void deleteConceptSetNoAccess() {
    // create concept set as owner or writer
    ConceptSet conceptSet =
        createTestConceptSet(
            workspace,
            CONCEPT_SET_NAME_1,
            CONCEPT_SET_DESC_1,
            Domain.CONDITION,
            CRITERIA_CONDITION_1);
    // change access, create and check
    stubWorkspaceAccessLevel(workspace, WorkspaceAccessLevel.NO_ACCESS);

    Throwable exception =
        assertThrows(
            ForbiddenException.class,
            () ->
                conceptSetsController.deleteConceptSet(
                    workspace.getNamespace(), WORKSPACE_NAME, conceptSet.getId()));
    assertForbiddenException(exception);
  }

  //////////// getConceptSet tests ////////////

  @Test
  public void getConceptSetNotExists() {
    Throwable exception =
        assertThrows(
            NotFoundException.class,
            () ->
                conceptSetsController.getConceptSet(workspace.getNamespace(), WORKSPACE_NAME, 1L));
    assertNotFoundException(exception, 1L);
  }

  @Test
  public void getConceptSetWrongWorkspace() {
    // create concept set as owner or writer
    ConceptSet conceptSet =
        createTestConceptSet(
            workspace,
            CONCEPT_SET_NAME_1,
            CONCEPT_SET_DESC_1,
            Domain.CONDITION,
            CRITERIA_CONDITION_1);
    // check access from different workspace
    Throwable exception =
        assertThrows(
            NotFoundException.class,
            () ->
                conceptSetsController.getConceptSet(
                    workspace2.getNamespace(), workspace2.getId(), conceptSet.getId()));
    assertNotFoundException(exception, 1L);
  }

  @Test
  public void getConceptSetWrongConceptId() {
    // create concept set as owner or writer
    ConceptSet conceptSet =
        createTestConceptSet(
            workspace,
            CONCEPT_SET_NAME_1,
            CONCEPT_SET_DESC_1,
            Domain.OBSERVATION,
            CRITERIA_SURVEY_1);
    // check for incorrect conceptId
    Long wrongConceptSetId = conceptSet.getId() + 100L;
    Throwable exception = assertThrows(
        NotFoundException.class,
        () ->
            conceptSetsController.getConceptSet(workspace.getNamespace(), workspace.getId(), wrongConceptSetId));
    assertNotFoundException(exception, wrongConceptSetId);
  }

  @Test
  public void getConceptSetOwner() {
    // create concept set as owner or writer
    ConceptSet conceptSet =
        createTestConceptSet(
            workspace,
            CONCEPT_SET_NAME_1,
            CONCEPT_SET_DESC_1,
            Domain.CONDITION,
            CRITERIA_CONDITION_1);
    // change access, get and chaeck
    stubWorkspaceAccessLevel(workspace, WorkspaceAccessLevel.OWNER);
    ConceptSet retrieved =
        conceptSetsController
            .getConceptSet(workspace.getNamespace(), workspace.getId(), conceptSet.getId())
            .getBody();

    assertThat(retrieved.participantCount(null)).isEqualTo(conceptSet);
  }

  @Test
  public void getConceptSetWriter() {
    // create concept set as owner or writer
    ConceptSet conceptSet =
        createTestConceptSet(
            workspace,
            CONCEPT_SET_NAME_1,
            CONCEPT_SET_DESC_1,
            Domain.CONDITION,
            CRITERIA_CONDITION_1);
    // change access, get and chaeck
    stubWorkspaceAccessLevel(workspace, WorkspaceAccessLevel.WRITER);
    ConceptSet retrieved =
        conceptSetsController
            .getConceptSet(workspace.getNamespace(), workspace.getId(), conceptSet.getId())
            .getBody();

    assertThat(retrieved.participantCount(null)).isEqualTo(conceptSet);
  }

  @Test
  public void getConceptSetReader() {
    // create concept set as owner or writer
    ConceptSet conceptSet =
        createTestConceptSet(
            workspace,
            CONCEPT_SET_NAME_1,
            CONCEPT_SET_DESC_1,
            Domain.CONDITION,
            CRITERIA_CONDITION_1);
    // change access, get and chaeck
    stubWorkspaceAccessLevel(workspace, WorkspaceAccessLevel.READER);
    ConceptSet retrieved =
        conceptSetsController
            .getConceptSet(workspace.getNamespace(), workspace.getId(), conceptSet.getId())
            .getBody();

    assertThat(retrieved.participantCount(null)).isEqualTo(conceptSet);
  }

  @Test
  public void getConceptSetNoAccess() {
    // create concept set as owner or writer
    ConceptSet conceptSet =
        createTestConceptSet(
            workspace,
            CONCEPT_SET_NAME_1,
            CONCEPT_SET_DESC_1,
            Domain.CONDITION,
            CRITERIA_CONDITION_1);
    // change access, get and chaeck
    stubWorkspaceAccessLevel(workspace, WorkspaceAccessLevel.NO_ACCESS);
    Throwable exception =
        assertThrows(
            ForbiddenException.class,
            () ->
                conceptSetsController.getConceptSet(
                    workspace.getNamespace(), workspace.getId(), conceptSet.getId()));

    assertForbiddenException(exception);
  }

  //////////// getConceptSetsInWorkspace tests ////////////

  @Test
  public void getConceptSetsInWorkspaceEmpty() {
    assertThat(
            conceptSetsController
                .getConceptSetsInWorkspace(workspace.getNamespace(), WORKSPACE_NAME)
                .getBody()
                .getItems())
        .isEmpty();
  }

  @Test
  public void getConceptSetsInWorkspaceOne() {
    // create concept set as owner or writer
    ConceptSet conceptSet =
        createTestConceptSet(
            workspace,
            CONCEPT_SET_NAME_1,
            CONCEPT_SET_DESC_1,
            Domain.CONDITION,
            CRITERIA_CONDITION_1);
    List<ConceptSet> response =
        conceptSetsController
            .getConceptSetsInWorkspace(workspace.getNamespace(), WORKSPACE_NAME)
            .getBody()
            .getItems();
    assertThat(response.size()).isEqualTo(1);
    assertThat(response).containsAllIn(ImmutableList.of(conceptSet));
  }

  @Test
  public void getConceptSetsInWorkspaceMany() {
    // create concept set as owner or writer
    ConceptSet conceptSet =
        createTestConceptSet(
            workspace,
            CONCEPT_SET_NAME_1,
            CONCEPT_SET_DESC_1,
            Domain.CONDITION,
            CRITERIA_CONDITION_1);
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
            .getConceptSetsInWorkspace(workspace.getNamespace(), WORKSPACE_NAME)
            .getBody()
            .getItems();
    assertThat(response.size()).isEqualTo(4);
    assertThat(response)
        .containsAllIn(ImmutableList.of(conceptSet, conceptSet2, conceptSet3, conceptSet4));
  }

  @Test
  public void getConceptSetsInWorkspaceWrongWorkspace() {
    // create concept set as owner or writer in workspace
    ConceptSet conceptSet =
        createTestConceptSet(
            workspace,
            CONCEPT_SET_NAME_1,
            CONCEPT_SET_DESC_1,
            Domain.CONDITION,
            CRITERIA_CONDITION_1);
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
    // create concept set as owner or writer
    ConceptSet conceptSet =
        createTestConceptSet(
            workspace,
            CONCEPT_SET_NAME_1,
            CONCEPT_SET_DESC_1,
            Domain.CONDITION,
            CRITERIA_CONDITION_1);
    // change access, call and check
    stubWorkspaceAccessLevel(workspace, WorkspaceAccessLevel.OWNER);

    List<ConceptSet> response =
        conceptSetsController
            .getConceptSetsInWorkspace(workspace.getNamespace(), WORKSPACE_NAME)
            .getBody()
            .getItems();
    assertThat(response.size()).isEqualTo(1);
    assertThat(response).containsAllIn(ImmutableList.of(conceptSet));
  }

  @Test
  public void getConceptSetsInWorkspaceWriter() {
    // create concept set as owner or writer
    ConceptSet conceptSet =
        createTestConceptSet(
            workspace,
            CONCEPT_SET_NAME_1,
            CONCEPT_SET_DESC_1,
            Domain.CONDITION,
            CRITERIA_CONDITION_1);
    // change access, call and check
    stubWorkspaceAccessLevel(workspace, WorkspaceAccessLevel.WRITER);

    List<ConceptSet> response =
        conceptSetsController
            .getConceptSetsInWorkspace(workspace.getNamespace(), WORKSPACE_NAME)
            .getBody()
            .getItems();
    assertThat(response.size()).isEqualTo(1);
    assertThat(response).containsAllIn(ImmutableList.of(conceptSet));
  }

  @Test
  public void getConceptSetsInWorkspaceReader() {
    // create concept set as owner or writer
    ConceptSet conceptSet =
        createTestConceptSet(
            workspace,
            CONCEPT_SET_NAME_1,
            CONCEPT_SET_DESC_1,
            Domain.CONDITION,
            CRITERIA_CONDITION_1);
    // change access, call and check
    stubWorkspaceAccessLevel(workspace, WorkspaceAccessLevel.READER);

    List<ConceptSet> response =
        conceptSetsController
            .getConceptSetsInWorkspace(workspace.getNamespace(), WORKSPACE_NAME)
            .getBody()
            .getItems();
    assertThat(response.size()).isEqualTo(1);
    assertThat(response).containsAllIn(ImmutableList.of(conceptSet));
  }

  @Test
  public void getConceptSetsInWorkspaceNoAccess() {
    // create concept set as owner or writer
    ConceptSet conceptSet =
        createTestConceptSet(
            workspace,
            CONCEPT_SET_NAME_1,
            CONCEPT_SET_DESC_1,
            Domain.CONDITION,
            CRITERIA_CONDITION_1);
    // change access, call and check
    stubWorkspaceAccessLevel(workspace, WorkspaceAccessLevel.NO_ACCESS);

    Throwable exception =
        assertThrows(
            ForbiddenException.class,
            () ->
                conceptSetsController.getConceptSetsInWorkspace(
                    workspace.getNamespace(), WORKSPACE_NAME));
    assertForbiddenException(exception);
  }

  //////////// updateConceptSet tests ////////////

  @Test
  public void updateConceptSetNotExists() {
    ConceptSet conceptSet = new ConceptSet();
    conceptSet.setDescription(CONCEPT_SET_DESC_1);
    conceptSet.setName(CONCEPT_SET_NAME_1);
    conceptSet.setDomain(Domain.CONDITION);
    conceptSet.setId(1L);
    conceptSet.setEtag(Etags.fromVersion(1));

    Throwable exception = assertThrows(
        NotFoundException.class,
        () ->
            conceptSetsController.updateConceptSet(
                workspace.getNamespace(), WORKSPACE_NAME, conceptSet.getId(), conceptSet));
    assertNotFoundException(exception, conceptSet.getId());
  }

  @Test
  public void updateConceptSetWrongWorkspace() {
    // create concept set as owner or writer in workspace
    ConceptSet conceptSet =
        createTestConceptSet(
            workspace,
            CONCEPT_SET_NAME_1,
            CONCEPT_SET_DESC_1,
            Domain.CONDITION,
            CRITERIA_CONDITION_1);
    // check update in workspace2
    Throwable exception = assertThrows(
        NotFoundException.class,
        () ->
            conceptSetsController.updateConceptSet(
                workspace2.getNamespace(), workspace2.getId(), conceptSet.getId(), conceptSet));
    assertNotFoundException(exception,conceptSet.getId());
  }

  @Test
  public void updateConceptSet() {
    // create conceptSet
    ConceptSet conceptSet = makeConceptSet1();

    conceptSet.setDescription("new description");
    conceptSet.setName("new name");
    Instant newInstant = NOW.plusMillis(1);
    fakeClock.setInstant(newInstant);
    ConceptSet updatedConceptSet =
        conceptSetsController
            .updateConceptSet(
                workspace.getNamespace(), WORKSPACE_NAME, conceptSet.getId(), conceptSet)
            .getBody();
    assertThat(updatedConceptSet.getCreator()).isEqualTo(USER_EMAIL);
    assertThat(updatedConceptSet.getCriteriums()).isNotNull();
    assertThat(updatedConceptSet.getCreationTime()).isEqualTo(NOW.toEpochMilli());
    assertThat(updatedConceptSet.getDescription()).isEqualTo("new description");
    assertThat(updatedConceptSet.getDomain()).isEqualTo(Domain.CONDITION);
    assertThat(updatedConceptSet.getEtag()).isEqualTo(Etags.fromVersion(2));
    assertThat(updatedConceptSet.getLastModifiedTime()).isEqualTo(newInstant.toEpochMilli());
    assertThat(conceptSet.getName()).isEqualTo("new name");
  }

  @Test
  public void testUpdateConceptSetDomainChange() {
    ConceptSet conceptSet = makeConceptSet1();
    conceptSet.setDescription("new description");
    conceptSet.setName("new name");
    conceptSet.setDomain(Domain.DEATH);
    assertThrows(
        ConflictException.class,
        () ->
            conceptSetsController.updateConceptSet(
                workspace.getNamespace(), WORKSPACE_NAME, conceptSet.getId(), conceptSet));
  }

  @Test
  public void testUpdateConceptSetWrongEtag() {
    ConceptSet conceptSet = makeConceptSet1();
    conceptSet.setDescription("new description");
    conceptSet.setName("new name");
    conceptSet.setEtag(Etags.fromVersion(2));

    assertThrows(
        ConflictException.class,
        () ->
            conceptSetsController.updateConceptSet(
                workspace.getNamespace(), WORKSPACE_NAME, conceptSet.getId(), conceptSet));
  }

  //////////// updateConceptSetConcepts tests ////////////

  @Test
  public void updateConceptSetConceptsWrongDomain() {
    // create concept set as owner or writer in workspace
    ConceptSet conceptSet =
        createTestConceptSet(
            workspace,
            CONCEPT_SET_NAME_1,
            CONCEPT_SET_DESC_1,
            Domain.CONDITION,
            CRITERIA_CONDITION_1);
    // check update concept from incorrect domain for conceptSet
    UpdateConceptSetRequest updateConceptSetRequestWithSurvey = buildUpdateConceptsRequest(conceptSet.getEtag(), CRITERIA_SURVEY_1.getConceptId());

    ConceptSet updated = conceptSetsController.updateConceptSetConcepts(
        workspace.getNamespace(),
        workspace.getId(),
        conceptSet.getId(),
        updateConceptSetRequestWithSurvey).getBody();
    assertThat(updated.getEtag()).isNotEqualTo(conceptSet.getEtag());
    updated.setEtag(conceptSet.getEtag());
    assertThat(updated).isEqualTo(conceptSet);
  }

  @Test
  public void testUpdateConceptSetConceptsAddAndRemove() {
    saveAllTestCriteria();
    DbConceptSetConceptId dbConceptSetConceptId1 =
        DbConceptSetConceptId.builder()
            .addConceptId(CRITERIA_CONDITION_1.getConceptId())
            .addStandard(true)
            .build();
    DbConceptSetConceptId dbConceptSetConceptId2 =
        DbConceptSetConceptId.builder()
            .addConceptId(CRITERIA_CONDITION_2.getConceptId())
            .addStandard(true)
            .build();
    when(conceptBigQueryService.getParticipantCountForConcepts(
            Domain.CONDITION, ImmutableSet.of(dbConceptSetConceptId1)))
        .thenReturn(123);
    when(conceptBigQueryService.getParticipantCountForConcepts(
            Domain.CONDITION, ImmutableSet.of(dbConceptSetConceptId1, dbConceptSetConceptId2)))
        .thenReturn(246);
    ConceptSet conceptSet = makeConceptSet1();
    ConceptSet updated =
        conceptSetsController
            .updateConceptSetConcepts(
                workspace.getNamespace(),
                WORKSPACE_NAME,
                conceptSet.getId(),
                buildUpdateConceptsRequest(
                    conceptSet.getEtag(), CRITERIA_CONDITION_2.getConceptId()))
            .getBody();
    assertThat(updated.getCriteriums()).hasSize(2);
    assertThat(updated.getCriteriums().get(0).getConceptId())
        .isEqualTo(CRITERIA_CONDITION_1.getConceptId());
    assertThat(updated.getEtag()).isNotEqualTo(conceptSet.getEtag());

    ConceptSet removed =
        conceptSetsController
            .updateConceptSetConcepts(
                workspace.getNamespace(),
                WORKSPACE_NAME,
                conceptSet.getId(),
                buildRemoveConceptsRequest(updated.getEtag(), CRITERIA_CONDITION_2.getConceptId()))
            .getBody();
    assertThat(removed.getCriteriums()).hasSize(1);
    assertThat(removed.getEtag()).isNotEqualTo(conceptSet.getEtag());
    assertThat(removed.getEtag()).isNotEqualTo(updated.getEtag());
  }

  @Test
  public void testUpdateConceptSetConceptsAddMany() {
    saveAllTestCriteria();
    DbConceptSetConceptId dbConceptSetConceptId1 =
        DbConceptSetConceptId.builder()
            .addConceptId(CRITERIA_CONDITION_1.getConceptId())
            .addStandard(true)
            .build();
    DbConceptSetConceptId dbConceptSetConceptId2 =
        DbConceptSetConceptId.builder()
            .addConceptId(CRITERIA_CONDITION_2.getConceptId())
            .addStandard(true)
            .build();
    DbConceptSetConceptId dbConceptSetConceptId3 =
        DbConceptSetConceptId.builder()
            .addConceptId(CRITERIA_CONDITION_3.getConceptId())
            .addStandard(true)
            .build();
    ConceptSetService.MAX_CONCEPTS_PER_SET = 1000;
    when(conceptBigQueryService.getParticipantCountForConcepts(
            Domain.CONDITION,
            ImmutableSet.of(
                dbConceptSetConceptId1, dbConceptSetConceptId2, dbConceptSetConceptId3)))
        .thenReturn(456);
    ConceptSet conceptSet = makeConceptSet1();
    ConceptSet updated =
        conceptSetsController
            .updateConceptSetConcepts(
                workspace.getNamespace(),
                WORKSPACE_NAME,
                conceptSet.getId(),
                buildUpdateConceptsRequest(
                    conceptSet.getEtag(),
                    CRITERIA_CONDITION_1.getConceptId(),
                    CRITERIA_CONDITION_2.getConceptId(),
                    CRITERIA_CONDITION_3.getConceptId()))
            .getBody();
    assertThat(updated.getCriteriums()).hasSize(3);
    assertThat(updated.getCriteriums().get(0).getConceptId())
        .isEqualTo(CRITERIA_CONDITION_1.getConceptId());
    assertThat(updated.getCriteriums().get(1).getConceptId())
        .isEqualTo(CRITERIA_CONDITION_3.getConceptId());
    assertThat(updated.getCriteriums().get(2).getConceptId())
        .isEqualTo(CRITERIA_CONDITION_2.getConceptId());
    assertThat(updated.getEtag()).isNotEqualTo(conceptSet.getEtag());

    when(conceptBigQueryService.getParticipantCountForConcepts(
            Domain.CONDITION, ImmutableSet.of(dbConceptSetConceptId1)))
        .thenReturn(123);
    ConceptSet removed =
        conceptSetsController
            .updateConceptSetConcepts(
                workspace.getNamespace(),
                WORKSPACE_NAME,
                conceptSet.getId(),
                buildRemoveConceptsRequest(
                    updated.getEtag(),
                    CRITERIA_CONDITION_2.getConceptId(),
                    CRITERIA_CONDITION_3.getConceptId()))
            .getBody();
    assertThat(removed.getCriteriums()).hasSize(1);
    assertThat(removed.getCriteriums().get(0).getConceptId())
        .isEqualTo(CRITERIA_CONDITION_1.getConceptId());
    assertThat(removed.getEtag()).isNotEqualTo(conceptSet.getEtag());
    assertThat(removed.getEtag()).isNotEqualTo(updated.getEtag());
  }

  @Test
  public void testUpdateConceptSetConceptsAddManyOnCreate() {
    saveAllTestCriteria();
    DbConceptSetConceptId dbConceptSetConceptId1 =
        DbConceptSetConceptId.builder()
            .addConceptId(CRITERIA_CONDITION_1.getConceptId())
            .addStandard(true)
            .build();
    DbConceptSetConceptId dbConceptSetConceptId2 =
        DbConceptSetConceptId.builder()
            .addConceptId(CRITERIA_CONDITION_2.getConceptId())
            .addStandard(true)
            .build();
    DbConceptSetConceptId dbConceptSetConceptId3 =
        DbConceptSetConceptId.builder()
            .addConceptId(CRITERIA_CONDITION_3.getConceptId())
            .addStandard(true)
            .build();
    when(conceptBigQueryService.getParticipantCountForConcepts(
            Domain.CONDITION,
            ImmutableSet.of(
                dbConceptSetConceptId1, dbConceptSetConceptId2, dbConceptSetConceptId3)))
        .thenReturn(456);
    ConceptSet conceptSet =
        makeConceptSet1(
            CRITERIA_CONDITION_1.getConceptId(),
            CRITERIA_CONDITION_2.getConceptId(),
            CRITERIA_CONDITION_3.getConceptId());
    assertThat(conceptSet.getCriteriums()).hasSize(3);
    assertThat(conceptSet.getCriteriums().get(0).getConceptId())
        .isEqualTo(CRITERIA_CONDITION_1.getConceptId());
    assertThat(conceptSet.getCriteriums().get(1).getConceptId())
        .isEqualTo(CRITERIA_CONDITION_3.getConceptId());
    assertThat(conceptSet.getCriteriums().get(2).getConceptId())
        .isEqualTo(CRITERIA_CONDITION_2.getConceptId());
  }

  @Test
  public void testUpdateConceptSetConceptsAddTooMany() {
    saveAllTestCriteria();
    ConceptSet conceptSet = makeConceptSet1();
    ConceptSetService.MAX_CONCEPTS_PER_SET = 2;

    assertThrows(
        ConflictException.class,
        () ->
            conceptSetsController
                .updateConceptSetConcepts(
                    workspace.getNamespace(),
                    WORKSPACE_NAME,
                    conceptSet.getId(),
                    buildUpdateConceptsRequest(
                        conceptSet.getEtag(),
                        CRITERIA_CONDITION_1.getConceptId(),
                        CRITERIA_CONDITION_2.getConceptId(),
                        CRITERIA_CONDITION_3.getConceptId()))
                .getBody());
  }

  @Test
  public void testUpdateConceptSetConceptsWrongEtag() {
    saveAllTestCriteria();
    ConceptSet conceptSet = makeConceptSet1();

    assertThrows(
        ConflictException.class,
        () ->
            conceptSetsController.updateConceptSetConcepts(
                workspace.getNamespace(),
                WORKSPACE_NAME,
                conceptSet.getId(),
                buildUpdateConceptsRequest(
                    Etags.fromVersion(2), CRITERIA_CONDITION_1.getConceptId())));
  }

  //////////// copyConceptSet tests ////////////

  @Test
  public void testCopyConceptSetOwnerToOwner() {
    // owner: to workspace has no permission to write
    testCopyConceptSetForAccessLevels(workspace, workspace2);
  }

  @Test
  public void testCopyConceptSetWriterToWriter() {
    // writer: minimal access level to create and copy conceptSet
    stubWorkspaceAccessLevel(workspace, WorkspaceAccessLevel.WRITER);
    stubWorkspaceAccessLevel(workspace2, WorkspaceAccessLevel.WRITER);

    testCopyConceptSetForAccessLevels(workspace, workspace2);
  }

  @Test
  public void testCopyConceptSetOwnerToWriter() {
    // writer: to workspace has permission to write
    stubGetWorkspace(workspace2.getNamespace(), workspace2.getName(), WorkspaceAccessLevel.WRITER);
    stubGetWorkspaceAcl(
        workspace2.getNamespace(), workspace2.getName(), WorkspaceAccessLevel.WRITER);
    testCopyConceptSetForAccessLevels(workspace, workspace2);
  }

  @Test
  public void testCopyConceptSetOwnerToReaderFail() {
    // reader: to workspace has no permission to write
    stubGetWorkspace(workspace2.getNamespace(), workspace2.getName(), WorkspaceAccessLevel.READER);
    stubGetWorkspaceAcl(
        workspace2.getNamespace(), workspace2.getName(), WorkspaceAccessLevel.READER);
    Throwable exception =
        assertThrows(
            ForbiddenException.class,
            () -> testCopyConceptSetForAccessLevels(workspace, workspace2));
    assertThat(exception)
        .hasMessageThat()
        .contains(
            String.format(
                "You do not have sufficient permissions to access workspace %s/%s",
                workspace2.getNamespace(), workspace2.getId()));
  }

  @Test
  public void testCopyConceptSetOwnerToNoAccessFail() {
    // no access: to workspace has no permission to write
    stubGetWorkspace(
        workspace2.getNamespace(), workspace2.getName(), WorkspaceAccessLevel.NO_ACCESS);
    stubGetWorkspaceAcl(
        workspace2.getNamespace(), workspace2.getName(), WorkspaceAccessLevel.NO_ACCESS);
    Throwable exception =
        assertThrows(
            ForbiddenException.class,
            () -> testCopyConceptSetForAccessLevels(workspace, workspace2));
    assertForbiddenException(exception);
  }

  @Test
  public void validateCreateConceptSetRequest() {
    // just conceptSet not enough also need non null set of conceptSet ids
    final CreateConceptSetRequest createConceptSetRequest =
        new CreateConceptSetRequest().conceptSet(new ConceptSet().domain(Domain.CONDITION));
    Throwable exception =
        assertThrows(
            BadRequestException.class,
            () -> conceptSetsController.validateCreateConceptSetRequest(createConceptSetRequest),
            "Expected BadRequestException not thrown");
    assertThat(exception).hasMessageThat().contains("Cannot create a concept set with no concepts");
    // also need non-emptyList of conceptSetIds
    final CreateConceptSetRequest createConceptSetRequest2 =
        createConceptSetRequest.addedConceptSetConceptIds(new ArrayList<>());
    exception =
        assertThrows(
            BadRequestException.class,
            () -> conceptSetsController.validateCreateConceptSetRequest(createConceptSetRequest2),
            "Expected BadRequestException not thrown");
    assertThat(exception).hasMessageThat().contains("Cannot create a concept set with no concepts");
    // add a single conceptSetConceptId to List
    final CreateConceptSetRequest createConceptSetRequest3 =
        createConceptSetRequest.addAddedConceptSetConceptIdsItem(
            new ConceptSetConceptId().conceptId(1000L));
    assertDoesNotThrow(
        () -> conceptSetsController.validateCreateConceptSetRequest(createConceptSetRequest3),
        "BadRequestException is not expected to be thrown");
    // add a multiple conceptSetConceptIds to list
    final CreateConceptSetRequest createConceptSetRequest4 =
        createConceptSetRequest.addedConceptSetConceptIds(
            Arrays.asList(
                new ConceptSetConceptId().conceptId(1000L),
                new ConceptSetConceptId().conceptId(2000L)));
    assertDoesNotThrow(
        () -> conceptSetsController.validateCreateConceptSetRequest(createConceptSetRequest4),
        "BadRequestException is not expected to be thrown");
  }

  @Test
  public void validateUpdateConceptSet() {
    // invalid empty object need etag and domain
    Throwable exception =
        assertThrows(
            BadRequestException.class,
            () -> conceptSetsController.validateUpdateConceptSet(new ConceptSet()),
            "Expected BadRequestException not thrown");
    assertThat(exception).hasMessageThat().contains("missing required update field 'etag'");
    // just add etag
    final ConceptSet conceptSet = new ConceptSet().etag("testEtag");
    exception =
        assertThrows(
            BadRequestException.class,
            () -> conceptSetsController.validateUpdateConceptSet(conceptSet),
            "Expected BadRequestException not thrown");
    assertThat(exception).hasMessageThat().contains("Domain cannot be null");
    // add etag and domain
    final ConceptSet conceptSet2 = new ConceptSet().etag("testEtag").domain(Domain.CONDITION);
    assertDoesNotThrow(
        () -> conceptSetsController.validateUpdateConceptSet(conceptSet2),
        "BadRequestException is not expected to be thrown");
  }

  @Test
  public void validateUpdateConceptSetConcepts() {
    // invalid empty object
    Throwable exception =
        assertThrows(
            BadRequestException.class,
            () ->
                conceptSetsController.validateUpdateConceptSetConcepts(
                    new UpdateConceptSetRequest()),
            "Expected BadRequestException not thrown");
    assertThat(exception).hasMessageThat().contains("missing required update field 'etag'");
    // just add etag which is required
    assertDoesNotThrow(
        () ->
            conceptSetsController.validateUpdateConceptSetConcepts(
                buildUpdateConceptsRequest("eTagTest", 1000L, 1001L)),
        "BadRequestException is not expected to be thrown");
  }

  private ConceptSet createTestConceptSet(
      Workspace workspace,
      String name,
      String desc,
      Domain domain,
      Criteria... criteriumsForDomain) {
    // change access to owner and create
    stubWorkspaceAccessLevel(workspace, WorkspaceAccessLevel.OWNER);
    ConceptSet conceptSet = new ConceptSet().domain(domain).description(desc).name(name);
    // use only criteria that matches domain
    List<Long> domainConceptIds =
        Arrays.stream(criteriumsForDomain)
            .filter(o -> o.getDomainId().equals(domain.toString()))
            .map(Criteria::getConceptId)
            .collect(Collectors.toList());

    CreateConceptSetRequest createConceptSetRequest =
        buildCreateConceptSetRequest(
            conceptSet, domainConceptIds.toArray(new Long[domainConceptIds.size()]));

    return conceptSetsController
        .createConceptSet(workspace.getNamespace(), WORKSPACE_NAME, createConceptSetRequest)
        .getBody();
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

  private void assertConceptSetAndCriteria(ConceptSet conceptSet, Criteria... expectedCriteria) {
    assertThat(conceptSet.getCreationTime()).isEqualTo(NOW.toEpochMilli());
    assertThat(conceptSet.getDescription()).isEqualTo(CONCEPT_SET_DESC_1);
    assertThat(conceptSet.getDomain()).isEqualTo(Domain.CONDITION);
    assertThat(conceptSet.getEtag()).isEqualTo(Etags.fromVersion(1));
    assertThat(conceptSet.getLastModifiedTime()).isEqualTo(NOW.toEpochMilli());
    assertThat(conceptSet.getName()).isEqualTo(CONCEPT_SET_NAME_1);
    List<Criteria> actualCriteriums = conceptSet.getCriteriums();
    assertThat(actualCriteriums.size()).isEqualTo(expectedCriteria.length);
    for (Criteria criteria : actualCriteriums) {
      criteria.setId(null);
      criteria.setParentId(null);
    }
    assertThat(actualCriteriums).containsAllIn(expectedCriteria);
  }

  private void testCopyConceptSetForAccessLevels(Workspace fromWs, Workspace toWs) {
    ConceptSet conceptSet =
        createDbConceptSetForWorkspace(
            fromWs.getNamespace(),
            fromWs.getId(),
            "From_3-Cnditions_Concept_set",
            "From_Cond_CS",
            CRITERIA_CONDITION_1,
            CRITERIA_CONDITION_2,
            CRITERIA_CONDITION_3);

    assertThat(conceptSet.getCriteriums().size()).isEqualTo(3);
    CopyRequest copyRequest =
        new CopyRequest()
            .newName("from_concept_set_copy")
            .toWorkspaceName(toWs.getId())
            .toWorkspaceNamespace(toWs.getNamespace());
    ConceptSet conceptSetCopy =
        conceptSetsController
            .copyConceptSet(
                fromWs.getNamespace(),
                fromWs.getId(),
                String.valueOf(conceptSet.getId()),
                copyRequest)
            .getBody();
    assertThat(conceptSet.getCriteriums()).containsAllIn(conceptSetCopy.getCriteriums()).inOrder();
  }

  private ConceptSet createDbConceptSetForWorkspace(
      String workspaceName, String workspaceId, String desc, String name, Criteria... criteria) {
    // only domain of the 1st in the list is used
    List<ConceptSetConceptId> conceptSetConceptIdList = new ArrayList<>();
    Domain domain = Domain.valueOf(criteria[0].getDomainId());
    for (Criteria criterium : criteria) {
      if (domain.toString().equals(criterium.getDomainId())) {
        cbCriteriaDao.save(makeDbCriteria(criterium));
        conceptSetConceptIdList.add(
            new ConceptSetConceptId()
                .conceptId(criterium.getConceptId())
                .standard(criterium.getIsStandard()));
      }
    }
    ConceptSet conceptSet = new ConceptSet();
    conceptSet.setDescription(desc);
    conceptSet.setName(name);
    conceptSet.setDomain(domain);

    return conceptSetsController
        .createConceptSet(
            workspaceName,
            workspaceId,
            new CreateConceptSetRequest()
                .conceptSet(conceptSet)
                .addedConceptSetConceptIds(conceptSetConceptIdList))
        .getBody();
  }

  private CreateConceptSetRequest buildCreateConceptSetRequest(
      ConceptSet conceptSet, Long... conceptIds) {
    List<ConceptSetConceptId> conceptSetConceptIdList = buildConceptSetConceptIdList(conceptIds);
    return new CreateConceptSetRequest()
        .conceptSet(conceptSet)
        .addedConceptSetConceptIds(conceptSetConceptIdList);
  }

  private UpdateConceptSetRequest buildUpdateConceptsRequest(String etag, Long... conceptIds) {
    List<ConceptSetConceptId> conceptSetConceptIdList = buildConceptSetConceptIdList(conceptIds);
    UpdateConceptSetRequest request = new UpdateConceptSetRequest();
    request.setEtag(etag);
    request.setAddedConceptSetConceptIds(conceptSetConceptIdList);
    return request;
  }

  private List<ConceptSetConceptId> buildConceptSetConceptIdList(Long... conceptIds) {
    return Arrays.stream(conceptIds)
        .map(
            c -> {
              ConceptSetConceptId conceptSetConceptId = new ConceptSetConceptId();
              conceptSetConceptId.setConceptId(c);
              conceptSetConceptId.setStandard(true);
              return conceptSetConceptId;
            })
        .collect(Collectors.toList());
  }

  private UpdateConceptSetRequest buildRemoveConceptsRequest(String etag, Long... conceptIds) {
    List<ConceptSetConceptId> conceptSetConceptIdList = buildConceptSetConceptIdList(conceptIds);
    UpdateConceptSetRequest request = new UpdateConceptSetRequest();
    request.setEtag(etag);
    request.setRemovedConceptSetConceptIds(conceptSetConceptIdList);
    return request;
  }

  private ConceptSet makeSurveyConceptSet1(Long... addedIds) {
    List<ConceptSetConceptId> conceptSetConceptIdList = buildConceptSetConceptIdList(addedIds);
    ConceptSetConceptId conceptSetConceptId = new ConceptSetConceptId();
    conceptSetConceptId.setConceptId(CRITERIA_SURVEY_1.getConceptId());
    conceptSetConceptId.setStandard(true);
    ConceptSet conceptSet = new ConceptSet();
    conceptSet.setDescription("description 1");
    conceptSet.setName("Survey Concept set 1");
    conceptSet.setDomain(Domain.OBSERVATION);
    conceptSet.setSurvey(Surveys.THE_BASICS);
    CreateConceptSetRequest request =
        new CreateConceptSetRequest()
            .conceptSet(conceptSet)
            .addAddedConceptSetConceptIdsItem(conceptSetConceptId);
    if (addedIds.length > 0) {
      request = request.addedConceptSetConceptIds(conceptSetConceptIdList);
    }
    return conceptSetsController
        .createConceptSet(workspace.getNamespace(), WORKSPACE_NAME, request)
        .getBody();
  }

  private ConceptSet makeConceptSet1(Long... addedIds) {
    List<ConceptSetConceptId> conceptSetConceptIdList =
        Arrays.stream(addedIds)
            .map(c -> new ConceptSetConceptId().conceptId(c).standard(true))
            .collect(Collectors.toList());
    ConceptSet conceptSet = new ConceptSet();
    conceptSet.setDescription(CONCEPT_SET_DESC_1);
    conceptSet.setName(CONCEPT_SET_NAME_1);
    conceptSet.setDomain(Domain.CONDITION);
    CreateConceptSetRequest request =
        new CreateConceptSetRequest()
            .conceptSet(conceptSet)
            .addAddedConceptSetConceptIdsItem(
                new ConceptSetConceptId()
                    .conceptId(CRITERIA_CONDITION_1.getConceptId())
                    .standard(true));
    if (addedIds.length > 0) {
      request = request.addedConceptSetConceptIds(conceptSetConceptIdList);
    }
    return conceptSetsController
        .createConceptSet(workspace.getNamespace(), WORKSPACE_NAME, request)
        .getBody();
  }

  private ConceptSet makeConceptSet(
      Criteria criteria, Domain domain, String nameSpace, String workspaceName) {
    ConceptSetConceptId conceptSetConceptId = new ConceptSetConceptId();
    conceptSetConceptId.setConceptId(criteria.getConceptId());
    conceptSetConceptId.setStandard(true);
    ConceptSet conceptSet = new ConceptSet();
    conceptSet.setDescription("desc 2");
    conceptSet.setName("concept set 2");
    conceptSet.setDomain(domain);
    return conceptSetsController
        .createConceptSet(
            nameSpace,
            workspaceName,
            new CreateConceptSetRequest()
                .conceptSet(conceptSet)
                .addAddedConceptSetConceptIdsItem(conceptSetConceptId))
        .getBody();
  }

  private void stubWorkspaceAccessLevel(
      Workspace workspace, WorkspaceAccessLevel workspaceAccessLevel) {
    stubGetWorkspace(workspace.getNamespace(), workspace.getName(), workspaceAccessLevel);
    stubGetWorkspaceAcl(workspace.getNamespace(), workspace.getName(), workspaceAccessLevel);
  }

  private void stubGetWorkspace(String ns, String name, WorkspaceAccessLevel workspaceAccessLevel) {
    FirecloudWorkspaceDetails fcWorkspace = new FirecloudWorkspaceDetails();
    fcWorkspace.setNamespace(ns);
    fcWorkspace.setName(name);
    fcWorkspace.setCreatedBy(USER_EMAIL);
    FirecloudWorkspaceResponse fcResponse = new FirecloudWorkspaceResponse();
    fcResponse.setWorkspace(fcWorkspace);
    fcResponse.setAccessLevel(workspaceAccessLevel.toString());
    when(fireCloudService.getWorkspace(ns, name)).thenReturn(fcResponse);
  }

  private void stubGetWorkspaceAcl(
      String ns, String name, WorkspaceAccessLevel workspaceAccessLevel) {
    FirecloudWorkspaceACL workspaceAccessLevelResponse = new FirecloudWorkspaceACL();
    FirecloudWorkspaceAccessEntry accessLevelEntry =
        new FirecloudWorkspaceAccessEntry().accessLevel(workspaceAccessLevel.toString());
    Map<String, FirecloudWorkspaceAccessEntry> userEmailToAccessEntry =
        ImmutableMap.of(USER_EMAIL, accessLevelEntry);
    workspaceAccessLevelResponse.setAcl(userEmailToAccessEntry);
    when(fireCloudService.getWorkspaceAclAsService(ns, name))
        .thenReturn(workspaceAccessLevelResponse);
  }

  private Workspace createTestWorkspace(
      String workspaceNamespace,
      String workspaceName,
      long cdrVersionId,
      WorkspaceAccessLevel workspaceAccessLevel) {
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
        .addStandard(criteria.getIsStandard())
        .addName(criteria.getName())
        .addFullText("+[" + criteria.getDomainId() + "_rank1]")
        .build();
  }
}
