package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static junit.framework.TestCase.fail;
import static org.mockito.Mockito.when;
import static org.pmiops.workbench.api.ConceptsControllerTest.makeConcept;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.inject.Provider;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.pmiops.workbench.billing.BillingProjectBufferService;
import org.pmiops.workbench.cdr.CdrVersionService;
import org.pmiops.workbench.cdr.ConceptBigQueryService;
import org.pmiops.workbench.cdr.dao.ConceptDao;
import org.pmiops.workbench.cdr.dao.ConceptService;
import org.pmiops.workbench.cohorts.CohortCloningService;
import org.pmiops.workbench.cohorts.CohortFactoryImpl;
import org.pmiops.workbench.cohorts.CohortMaterializationService;
import org.pmiops.workbench.compliance.ComplianceService;
import org.pmiops.workbench.conceptset.ConceptSetService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.CohortReviewDao;
import org.pmiops.workbench.db.dao.ConceptSetDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserRecentResourceService;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.CdrVersion;
import org.pmiops.workbench.db.model.CohortReview;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.google.CloudStorageService;
import org.pmiops.workbench.google.DirectoryService;
import org.pmiops.workbench.model.Cohort;
import org.pmiops.workbench.model.CohortStatus;
import org.pmiops.workbench.model.Concept;
import org.pmiops.workbench.model.ConceptSet;
import org.pmiops.workbench.model.CreateConceptSetRequest;
import org.pmiops.workbench.model.DataAccessLevel;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.DuplicateCohortRequest;
import org.pmiops.workbench.model.EmailVerificationStatus;
import org.pmiops.workbench.model.FieldSet;
import org.pmiops.workbench.model.MaterializeCohortRequest;
import org.pmiops.workbench.model.MaterializeCohortResponse;
import org.pmiops.workbench.model.ResearchPurpose;
import org.pmiops.workbench.model.SearchRequest;
import org.pmiops.workbench.model.TableQuery;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.notebooks.LeonardoNotebooksClient;
import org.pmiops.workbench.notebooks.NotebooksServiceImpl;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.test.FakeLongRandom;
import org.pmiops.workbench.test.SearchRequests;
import org.pmiops.workbench.workspaces.WorkspaceMapper;
import org.pmiops.workbench.workspaces.WorkspaceMapperImpl;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.pmiops.workbench.workspaces.WorkspaceServiceImpl;
import org.pmiops.workbench.workspaces.WorkspacesController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
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
  private static final String CONCEPT_SET_NAME = "concept_set";

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
          .conceptSynonyms(new ArrayList<String>());

  private static final Concept CLIENT_CONCEPT_2 =
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
          .conceptSynonyms(new ArrayList<String>());

  private static final org.pmiops.workbench.cdr.model.Concept CONCEPT_1 =
      makeConcept(CLIENT_CONCEPT_1);
  private static final org.pmiops.workbench.cdr.model.Concept CONCEPT_2 =
      makeConcept(CLIENT_CONCEPT_2);

  @Autowired WorkspacesController workspacesController;
  @Autowired CohortsController cohortsController;
  @Autowired ConceptSetsController conceptSetsController;

  Workspace workspace;
  CdrVersion cdrVersion;
  SearchRequest searchRequest;
  String cohortCriteria;
  @Autowired WorkspaceService workspaceService;
  @Autowired CdrVersionDao cdrVersionDao;
  @Autowired CohortDao cohortDao;
  @Autowired ConceptSetDao conceptSetDao;
  @Autowired ConceptDao conceptDao;
  @Autowired CohortReviewDao cohortReviewDao;
  @Autowired UserRecentResourceService userRecentResourceService;
  @Autowired UserDao userDao;
  @Autowired CohortMaterializationService cohortMaterializationService;
  @Mock Provider<User> userProvider;
  @Autowired FireCloudService fireCloudService;
  @Autowired UserService userService;
  @Autowired CloudStorageService cloudStorageService;
  @Autowired CdrVersionService cdrVersionService;
  @Autowired ComplianceService complianceService;

  @TestConfiguration
  @Import({
    WorkspaceServiceImpl.class,
    CohortCloningService.class,
    CohortFactoryImpl.class,
    NotebooksServiceImpl.class,
    UserService.class,
    WorkspacesController.class,
    CohortsController.class,
    ConceptSetsController.class,
    WorkspaceMapperImpl.class
  })
  @MockBean({
    BillingProjectBufferService.class,
    ConceptBigQueryService.class,
    FireCloudService.class,
    LeonardoNotebooksClient.class,
    CloudStorageService.class,
    ConceptSetService.class,
    UserRecentResourceService.class,
    CohortMaterializationService.class,
    CdrVersionService.class,
    ConceptService.class,
    ComplianceService.class,
    DirectoryService.class
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
    WorkbenchConfig workbenchConfig() {
      WorkbenchConfig workbenchConfig = new WorkbenchConfig();
      workbenchConfig.featureFlags = new WorkbenchConfig.FeatureFlagsConfig();
      workbenchConfig.featureFlags.useBillingProjectBuffer = false;
      return workbenchConfig;
    }
  }

  @Before
  public void setUp() throws Exception {
    User user = new User();
    user.setEmail("bob@gmail.com");
    user.setUserId(123L);
    user.setDisabled(false);
    user.setEmailVerificationStatusEnum(EmailVerificationStatus.SUBSCRIBED);
    user = userDao.save(user);
    when(userProvider.get()).thenReturn(user);
    workspacesController.setUserProvider(userProvider);
    cohortsController.setUserProvider(userProvider);
    conceptSetsController.setUserProvider(userProvider);

    cdrVersion = new CdrVersion();
    cdrVersion.setName(CDR_VERSION_NAME);
    cdrVersionDao.save(cdrVersion);

    searchRequest = SearchRequests.males();
    cohortCriteria = new Gson().toJson(searchRequest);

    workspace = new Workspace();
    workspace.setName(WORKSPACE_NAME);
    workspace.setNamespace(WORKSPACE_NAMESPACE);
    workspace.setDataAccessLevel(DataAccessLevel.PROTECTED);
    workspace.setResearchPurpose(new ResearchPurpose());
    workspace.setCdrVersionId(String.valueOf(cdrVersion.getCdrVersionId()));

    Workspace workspace2 = new Workspace();
    workspace2.setName(WORKSPACE_NAME_2);
    workspace2.setNamespace(WORKSPACE_NAMESPACE);
    workspace2.setDataAccessLevel(DataAccessLevel.PROTECTED);
    workspace2.setResearchPurpose(new ResearchPurpose());
    workspace2.setCdrVersionId(String.valueOf(cdrVersion.getCdrVersionId()));

    CLOCK.setInstant(NOW);
    stubGetWorkspace(
        WORKSPACE_NAMESPACE, WORKSPACE_NAME, "bob@gmail.com", WorkspaceAccessLevel.OWNER);
    stubGetWorkspace(
        WORKSPACE_NAMESPACE, WORKSPACE_NAME_2, "bob@gmail.com", WorkspaceAccessLevel.OWNER);

    Cohort cohort = new Cohort();
    cohort.setName("demo");
    cohort.setDescription("demo");
    cohort.setType("demo");
    cohort.setCriteria(createDemoCriteria().toString());

    workspace = workspacesController.createWorkspace(workspace).getBody();
    workspacesController.createWorkspace(workspace2);
  }

  private JSONObject createDemoCriteria() {
    JSONObject criteria = new JSONObject();
    criteria.append("includes", new JSONArray());
    criteria.append("excludes", new JSONArray());
    return criteria;
  }

  private void stubGetWorkspace(String ns, String name, String creator, WorkspaceAccessLevel access)
      throws Exception {
    org.pmiops.workbench.firecloud.model.Workspace fcWorkspace =
        new org.pmiops.workbench.firecloud.model.Workspace();
    fcWorkspace.setNamespace(ns);
    fcWorkspace.setName(name);
    fcWorkspace.setCreatedBy(creator);
    org.pmiops.workbench.firecloud.model.WorkspaceResponse fcResponse =
        new org.pmiops.workbench.firecloud.model.WorkspaceResponse();
    fcResponse.setWorkspace(fcWorkspace);
    fcResponse.setAccessLevel(access.toString());
    when(fireCloudService.getWorkspace(ns, name)).thenReturn(fcResponse);
  }

  public Cohort createDefaultCohort() {
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
    assertThat(cohorts).containsAllOf(c1, c2);
    assertThat(cohorts.size()).isEqualTo(2);
  }

  @Test
  public void testUpdateCohort() throws Exception {
    Cohort cohort = createDefaultCohort();
    cohort =
        cohortsController
            .createCohort(workspace.getNamespace(), workspace.getId(), cohort)
            .getBody();

    cohort.setName("updated-name");
    Cohort updated =
        cohortsController
            .updateCohort(workspace.getNamespace(), workspace.getId(), cohort.getId(), cohort)
            .getBody();
    cohort.setEtag(updated.getEtag());
    assertThat(updated).isEqualTo(cohort);

    cohort.setName("updated-name2");
    updated =
        cohortsController
            .updateCohort(workspace.getNamespace(), workspace.getId(), cohort.getId(), cohort)
            .getBody();
    cohort.setEtag(updated.getEtag());
    assertThat(updated).isEqualTo(cohort);

    Cohort got =
        cohortsController
            .getCohort(workspace.getNamespace(), workspace.getId(), cohort.getId())
            .getBody();
    assertThat(got).isEqualTo(cohort);
  }

  @Test
  public void testDuplicateCohort() {
    Cohort originalCohort = createDefaultCohort();
    originalCohort =
        cohortsController
            .createCohort(workspace.getNamespace(), workspace.getId(), originalCohort)
            .getBody();

    DuplicateCohortRequest params = new DuplicateCohortRequest();
    params.setNewName("New Cohort Name");
    params.setOriginalCohortId(originalCohort.getId());

    Cohort newCohort =
        cohortsController
            .duplicateCohort(workspace.getNamespace(), workspace.getId(), params)
            .getBody();
    newCohort =
        cohortsController
            .getCohort(workspace.getNamespace(), workspace.getId(), newCohort.getId())
            .getBody();

    assertThat(newCohort.getName()).isEqualTo(params.getNewName());
    assertThat(newCohort.getCriteria()).isEqualTo(originalCohort.getCriteria());
    assertThat(newCohort.getType()).isEqualTo(originalCohort.getType());
    assertThat(newCohort.getDescription()).isEqualTo(originalCohort.getDescription());
  }

  @Test(expected = NotFoundException.class)
  public void testGetCohortWrongWorkspace() throws Exception {
    Cohort cohort = createDefaultCohort();
    cohort =
        cohortsController
            .createCohort(workspace.getNamespace(), workspace.getId(), cohort)
            .getBody();
    cohortsController.getCohort(workspace.getNamespace(), WORKSPACE_NAME_2, cohort.getId());
  }

  @Test(expected = ConflictException.class)
  public void testUpdateCohortStaleThrows() throws Exception {
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
  }

  @Test
  public void testUpdateCohortInvalidEtagsThrow() throws Exception {
    Cohort cohort = createDefaultCohort();
    cohort =
        cohortsController
            .createCohort(workspace.getNamespace(), workspace.getId(), cohort)
            .getBody();

    // TODO: Refactor to be a @Parameterized test case.
    List<String> cases = ImmutableList.of("", "hello, world", "\"\"", "\"\"1234\"\"", "\"-1\"");
    for (String etag : cases) {
      try {
        cohortsController.updateCohort(
            workspace.getNamespace(),
            workspace.getId(),
            cohort.getId(),
            new Cohort().name("updated-name").etag(etag));
        fail(String.format("expected BadRequestException for etag: %s", etag));
      } catch (BadRequestException e) {
        // expected
      }
    }
  }

  @Test(expected = NotFoundException.class)
  public void testMaterializeCohortWorkspaceNotFound() throws Exception {
    Cohort cohort = createDefaultCohort();
    cohort =
        cohortsController
            .createCohort(workspace.getNamespace(), workspace.getId(), cohort)
            .getBody();
    WorkspaceAccessLevel owner = WorkspaceAccessLevel.OWNER;
    String workspaceName = "badWorkspace";
    org.pmiops.workbench.firecloud.model.WorkspaceResponse fcResponse =
        new org.pmiops.workbench.firecloud.model.WorkspaceResponse();
    fcResponse.setAccessLevel(owner.toString());
    when(fireCloudService.getWorkspace(WORKSPACE_NAMESPACE, workspaceName)).thenReturn(fcResponse);
    when(workspaceService.getWorkspaceAccessLevel(WORKSPACE_NAMESPACE, workspaceName))
        .thenThrow(new NotFoundException());
    MaterializeCohortRequest request = new MaterializeCohortRequest();
    request.setCohortName(cohort.getName());
    cohortsController.materializeCohort(WORKSPACE_NAMESPACE, workspaceName, request);
  }

  @Test(expected = NotFoundException.class)
  public void testMaterializeCohortCdrVersionNotFound() throws Exception {
    Cohort cohort = createDefaultCohort();
    cohort =
        cohortsController
            .createCohort(workspace.getNamespace(), workspace.getId(), cohort)
            .getBody();

    MaterializeCohortRequest request = new MaterializeCohortRequest();
    request.setCohortName(cohort.getName());
    request.setCdrVersionName("badCdrVersion");
    cohortsController.materializeCohort(WORKSPACE_NAMESPACE, WORKSPACE_NAME, request);
  }

  @Test(expected = NotFoundException.class)
  public void testMaterializeCohortCohortNotFound() throws Exception {
    Cohort cohort = createDefaultCohort();
    cohort =
        cohortsController
            .createCohort(workspace.getNamespace(), workspace.getId(), cohort)
            .getBody();

    MaterializeCohortRequest request = new MaterializeCohortRequest();
    request.setCohortName("badCohort");
    cohortsController.materializeCohort(WORKSPACE_NAMESPACE, WORKSPACE_NAME, request);
  }

  @Test(expected = BadRequestException.class)
  public void testMaterializeCohortNoSpecOrCohortName() throws Exception {
    Cohort cohort = createDefaultCohort();
    cohort =
        cohortsController
            .createCohort(workspace.getNamespace(), workspace.getId(), cohort)
            .getBody();

    MaterializeCohortRequest request = new MaterializeCohortRequest();
    cohortsController.materializeCohort(WORKSPACE_NAMESPACE, WORKSPACE_NAME, request);
  }

  @Test(expected = BadRequestException.class)
  public void testMaterializeCohortPageSizeTooSmall() throws Exception {
    Cohort cohort = createDefaultCohort();
    cohort =
        cohortsController
            .createCohort(workspace.getNamespace(), workspace.getId(), cohort)
            .getBody();

    MaterializeCohortRequest request = new MaterializeCohortRequest();
    request.setCohortName(cohort.getName());
    request.setPageSize(-1);
    cohortsController.materializeCohort(WORKSPACE_NAMESPACE, WORKSPACE_NAME, request);
  }

  @Test
  public void testMaterializeCohortPageSizeZero() throws Exception {
    Cohort cohort = createDefaultCohort();
    cohort =
        cohortsController
            .createCohort(workspace.getNamespace(), workspace.getId(), cohort)
            .getBody();

    MaterializeCohortRequest request = new MaterializeCohortRequest();
    request.setCohortName(cohort.getName());
    request.setPageSize(0);
    MaterializeCohortRequest adjustedRequest = new MaterializeCohortRequest();
    adjustedRequest.setCohortName(cohort.getName());
    adjustedRequest.setPageSize(CohortsController.DEFAULT_PAGE_SIZE);
    MaterializeCohortResponse response = new MaterializeCohortResponse();
    when(cohortMaterializationService.materializeCohort(
            null, cohortCriteria, null, adjustedRequest))
        .thenReturn(response);
    assertThat(
            cohortsController
                .materializeCohort(WORKSPACE_NAMESPACE, WORKSPACE_NAME, request)
                .getBody())
        .isEqualTo(response);
  }

  @Test
  public void testMaterializeCohortPageSizeTooLarge() throws Exception {
    Cohort cohort = createDefaultCohort();
    cohort =
        cohortsController
            .createCohort(workspace.getNamespace(), workspace.getId(), cohort)
            .getBody();

    MaterializeCohortRequest request = new MaterializeCohortRequest();
    request.setCohortName(cohort.getName());
    request.setPageSize(CohortsController.MAX_PAGE_SIZE + 1);
    MaterializeCohortRequest adjustedRequest = new MaterializeCohortRequest();
    adjustedRequest.setCohortName(cohort.getName());
    adjustedRequest.setPageSize(CohortsController.MAX_PAGE_SIZE);
    MaterializeCohortResponse response = new MaterializeCohortResponse();
    when(cohortMaterializationService.materializeCohort(
            null, cohortCriteria, null, adjustedRequest))
        .thenReturn(response);
    assertThat(
            cohortsController
                .materializeCohort(WORKSPACE_NAMESPACE, WORKSPACE_NAME, request)
                .getBody())
        .isEqualTo(response);
  }

  @Test
  public void testMaterializeCohortNamedCohort() throws Exception {
    Cohort cohort = createDefaultCohort();
    cohort =
        cohortsController
            .createCohort(workspace.getNamespace(), workspace.getId(), cohort)
            .getBody();
    MaterializeCohortRequest request = new MaterializeCohortRequest();
    request.setCohortName(cohort.getName());
    MaterializeCohortResponse response = new MaterializeCohortResponse();
    when(cohortMaterializationService.materializeCohort(null, cohortCriteria, null, request))
        .thenReturn(response);
    assertThat(
            cohortsController
                .materializeCohort(WORKSPACE_NAMESPACE, WORKSPACE_NAME, request)
                .getBody())
        .isEqualTo(response);
  }

  @Test
  public void testMaterializeCohortNamedCohortWithConceptSet() throws Exception {
    conceptDao.save(CONCEPT_1);
    conceptDao.save(CONCEPT_2);
    Cohort cohort = createDefaultCohort();
    cohort =
        cohortsController
            .createCohort(workspace.getNamespace(), workspace.getId(), cohort)
            .getBody();
    ConceptSet conceptSet = new ConceptSet().domain(Domain.CONDITION).name(CONCEPT_SET_NAME);
    conceptSet =
        conceptSetsController
            .createConceptSet(
                workspace.getNamespace(),
                workspace.getId(),
                new CreateConceptSetRequest()
                    .conceptSet(conceptSet)
                    .addedIds(
                        ImmutableList.of(
                            CLIENT_CONCEPT_1.getConceptId(), CLIENT_CONCEPT_2.getConceptId())))
            .getBody();

    MaterializeCohortRequest request = new MaterializeCohortRequest();
    request.setCohortName(cohort.getName());
    TableQuery tableQuery =
        new TableQuery().tableName("condition_occurrence").conceptSetName(CONCEPT_SET_NAME);
    request.setFieldSet(new FieldSet().tableQuery(tableQuery));
    MaterializeCohortResponse response = new MaterializeCohortResponse();
    when(cohortMaterializationService.materializeCohort(
            null,
            cohortCriteria,
            ImmutableSet.of(CLIENT_CONCEPT_1.getConceptId(), CLIENT_CONCEPT_2.getConceptId()),
            request))
        .thenReturn(response);
    assertThat(
            cohortsController
                .materializeCohort(WORKSPACE_NAMESPACE, WORKSPACE_NAME, request)
                .getBody())
        .isEqualTo(response);
  }

  @Test(expected = BadRequestException.class)
  public void testMaterializeCohortNamedCohortWithConceptSetWrongTable() throws Exception {
    conceptDao.save(CONCEPT_1);
    conceptDao.save(CONCEPT_2);
    Cohort cohort = createDefaultCohort();
    cohort =
        cohortsController
            .createCohort(workspace.getNamespace(), workspace.getId(), cohort)
            .getBody();
    ConceptSet conceptSet = new ConceptSet().domain(Domain.CONDITION).name(CONCEPT_SET_NAME);
    conceptSet =
        conceptSetsController
            .createConceptSet(
                workspace.getNamespace(),
                workspace.getId(),
                new CreateConceptSetRequest()
                    .conceptSet(conceptSet)
                    .addedIds(
                        ImmutableList.of(
                            CLIENT_CONCEPT_1.getConceptId(), CLIENT_CONCEPT_2.getConceptId())))
            .getBody();

    MaterializeCohortRequest request = new MaterializeCohortRequest();
    request.setCohortName(cohort.getName());
    TableQuery tableQuery =
        new TableQuery().tableName("observation").conceptSetName(CONCEPT_SET_NAME);
    request.setFieldSet(new FieldSet().tableQuery(tableQuery));
    cohortsController.materializeCohort(WORKSPACE_NAMESPACE, WORKSPACE_NAME, request);
  }

  @Test(expected = NotFoundException.class)
  public void testMaterializeCohortNamedCohortWithConceptSetNotFound() throws Exception {
    Cohort cohort = createDefaultCohort();
    cohort =
        cohortsController
            .createCohort(workspace.getNamespace(), workspace.getId(), cohort)
            .getBody();
    MaterializeCohortRequest request = new MaterializeCohortRequest();
    request.setCohortName(cohort.getName());
    TableQuery tableQuery =
        new TableQuery().tableName("condition_occurrence").conceptSetName(CONCEPT_SET_NAME);
    request.setFieldSet(new FieldSet().tableQuery(tableQuery));
    cohortsController.materializeCohort(WORKSPACE_NAMESPACE, WORKSPACE_NAME, request);
  }

  @Test
  public void testMaterializeCohortNamedCohortWithReview() throws Exception {
    Cohort cohort = createDefaultCohort();
    cohort =
        cohortsController
            .createCohort(workspace.getNamespace(), workspace.getId(), cohort)
            .getBody();
    CohortReview cohortReview = new CohortReview();
    cohortReview.setCohortId(cohort.getId());
    cohortReview.setCdrVersionId(cdrVersion.getCdrVersionId());
    cohortReview.setReviewSize(2);
    cohortReview.setReviewedCount(2);
    cohortReviewDao.save(cohortReview);

    MaterializeCohortRequest request = new MaterializeCohortRequest();
    request.setCohortName(cohort.getName());
    MaterializeCohortResponse response = new MaterializeCohortResponse();
    when(cohortMaterializationService.materializeCohort(
            cohortReview, cohortCriteria, null, request))
        .thenReturn(response);
    assertThat(
            cohortsController
                .materializeCohort(WORKSPACE_NAMESPACE, WORKSPACE_NAME, request)
                .getBody())
        .isEqualTo(response);
  }

  @Test
  public void testMaterializeCohortWithSpec() throws Exception {
    Cohort cohort = createDefaultCohort();
    cohort =
        cohortsController
            .createCohort(workspace.getNamespace(), workspace.getId(), cohort)
            .getBody();

    MaterializeCohortRequest request = new MaterializeCohortRequest();
    request.setCohortSpec(cohort.getCriteria());
    MaterializeCohortResponse response = new MaterializeCohortResponse();
    when(cohortMaterializationService.materializeCohort(null, cohortCriteria, null, request))
        .thenReturn(response);
    assertThat(
            cohortsController
                .materializeCohort(WORKSPACE_NAMESPACE, WORKSPACE_NAME, request)
                .getBody())
        .isEqualTo(response);
  }

  @Test
  public void testMaterializeCohortWithEverything() throws Exception {
    Cohort cohort = createDefaultCohort();
    cohort =
        cohortsController
            .createCohort(workspace.getNamespace(), workspace.getId(), cohort)
            .getBody();

    MaterializeCohortRequest request = new MaterializeCohortRequest();
    request.setCohortName(cohort.getName());
    request.setPageSize(123);
    request.setPageToken("token");
    request.setCdrVersionName(CDR_VERSION_NAME);
    List<CohortStatus> statuses =
        ImmutableList.of(CohortStatus.INCLUDED, CohortStatus.NOT_REVIEWED);
    request.setStatusFilter(statuses);
    MaterializeCohortResponse response = new MaterializeCohortResponse();
    when(cohortMaterializationService.materializeCohort(null, cohortCriteria, null, request))
        .thenReturn(response);
    assertThat(
            cohortsController
                .materializeCohort(WORKSPACE_NAMESPACE, WORKSPACE_NAME, request)
                .getBody())
        .isEqualTo(response);
  }
}
