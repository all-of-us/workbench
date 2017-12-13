package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static junit.framework.TestCase.fail;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import javax.inject.Provider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.pmiops.workbench.cohorts.CohortMaterializationService;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceService;
import org.pmiops.workbench.db.dao.WorkspaceServiceImpl;
import org.pmiops.workbench.db.model.CdrVersion;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.model.Cohort;
import org.pmiops.workbench.model.CohortStatus;
import org.pmiops.workbench.model.DataAccessLevel;
import org.pmiops.workbench.model.MaterializeCohortRequest;
import org.pmiops.workbench.model.MaterializeCohortResponse;
import org.pmiops.workbench.model.ResearchPurpose;
import org.pmiops.workbench.model.SearchGroup;
import org.pmiops.workbench.model.SearchGroupItem;
import org.pmiops.workbench.model.SearchRequest;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.test.SearchRequests;
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
@AutoConfigureTestDatabase(replace= AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class CohortsControllerTest {
  private static final Instant NOW = Instant.now();
  private static final FakeClock CLOCK = new FakeClock(NOW, ZoneId.systemDefault());
  private static final String CDR_VERSION_NAME = "cdrVersion";
  private static final String WORKSPACE_NAME = "workspace";
  private static final String WORKSPACE_NAMESPACE = "ns";
  private static final String COHORT_NAME = "cohort";


  @TestConfiguration
  @Import(WorkspaceServiceImpl.class)
  @MockBean(FireCloudService.class)
  static class Configuration {
    @Bean
    Clock clock() {
      return CLOCK;
    }
  }

  Workspace workspace;
  CdrVersion cdrVersion;
  SearchRequest searchRequest;
  String cohortCriteria;
  @Autowired
  WorkspaceService workspaceService;
  @Autowired
  CdrVersionDao cdrVersionDao;
  @Autowired
  CohortDao cohortDao;
  @Autowired
  UserDao userDao;
  @Mock
  CohortMaterializationService cohortMaterializationService;
  @Mock
  Provider<User> userProvider;
  @Autowired
  FireCloudService fireCloudService;

  private CohortsController cohortsController;

  @Before
  public void setUp() {
    User user = new User();
    user.setEmail("bob@gmail.com");
    user.setUserId(123L);
    user = userDao.save(user);
    when(userProvider.get()).thenReturn(user);

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

    CLOCK.setInstant(NOW);
    WorkspacesController workspacesController = new WorkspacesController(workspaceService,
        cdrVersionDao, userDao, userProvider, fireCloudService, CLOCK);
    workspace = workspacesController.createWorkspace(workspace).getBody();
    this.cohortsController = new CohortsController(
        workspaceService, cohortDao, cdrVersionDao, cohortMaterializationService,
        userProvider, CLOCK);
  }

  public Cohort createDefaultCohort() {
    Cohort cohort = new Cohort();
    cohort.setName(COHORT_NAME);
    cohort.setCriteria(cohortCriteria);
    return cohort;
  }

  @Test
  public void testUpdateCohort() throws Exception {
    Cohort cohort = createDefaultCohort();
    cohort = cohortsController.createCohort(workspace.getNamespace(), workspace.getId(), cohort).getBody();

    cohort.setName("updated-name");
    Cohort updated = cohortsController.updateCohort(workspace.getNamespace(), workspace.getId(), cohort.getId(), cohort).getBody();
    cohort.setEtag(updated.getEtag());
    assertThat(updated).isEqualTo(cohort);

    cohort.setName("updated-name2");
    updated = cohortsController.updateCohort(workspace.getNamespace(), workspace.getId(), cohort.getId(), cohort).getBody();
    cohort.setEtag(updated.getEtag());
    assertThat(updated).isEqualTo(cohort);

    Cohort got = cohortsController.getCohort(workspace.getNamespace(), workspace.getId(), cohort.getId()).getBody();
    assertThat(got).isEqualTo(cohort);
  }

  @Test(expected = ConflictException.class)
  public void testUpdateCohortStaleThrows() throws Exception {
    Cohort cohort = createDefaultCohort();
    cohort = cohortsController.createCohort(workspace.getNamespace(), workspace.getId(), cohort).getBody();

    cohortsController.updateCohort(workspace.getNamespace(), workspace.getId(), cohort.getId(),
        new Cohort().name("updated-name").etag(cohort.getEtag())).getBody();

    // Still using the initial etag.
    cohortsController.updateCohort(workspace.getNamespace(), workspace.getId(), cohort.getId(),
        new Cohort().name("updated-name2").etag(cohort.getEtag())).getBody();
  }

  @Test
  public void testUpdateCohortInvalidEtagsThrow() throws Exception {
    Cohort cohort = createDefaultCohort();
    cohort = cohortsController.createCohort(workspace.getNamespace(), workspace.getId(), cohort).getBody();

    // TODO: Refactor to be a @Parameterized test case.
    List<String> cases = ImmutableList.of("", "hello, world", "\"\"", "\"\"1234\"\"", "\"-1\"");
    for (String etag : cases) {
      try {
        cohortsController.updateCohort(workspace.getNamespace(), workspace.getId(), cohort.getId(),
            new Cohort().name("updated-name").etag(etag));
        fail(String.format("expected BadRequestException for etag: %s", etag));
      } catch(BadRequestException e) {
        // expected
      }
    }
  }

  @Test(expected = NotFoundException.class)
  public void testMaterializeCohortWorkspaceNotFound() throws Exception {
    Cohort cohort = createDefaultCohort();
    cohort = cohortsController.createCohort(workspace.getNamespace(), workspace.getId(), cohort).getBody();

    MaterializeCohortRequest request = new MaterializeCohortRequest();
    request.setCohortName(cohort.getName());
    cohortsController.materializeCohort(WORKSPACE_NAMESPACE, "badWorkspace", request);
  }

  @Test(expected = NotFoundException.class)
  public void testMaterializeCohortCdrVersionNotFound() throws Exception {
    Cohort cohort = createDefaultCohort();
    cohort = cohortsController.createCohort(workspace.getNamespace(), workspace.getId(), cohort).getBody();

    MaterializeCohortRequest request = new MaterializeCohortRequest();
    request.setCohortName(cohort.getName());
    request.setCdrVersionName("badCdrVersion");
    cohortsController.materializeCohort(WORKSPACE_NAMESPACE, WORKSPACE_NAME, request);
  }

  @Test(expected = NotFoundException.class)
  public void testMaterializeCohortCohortNotFound() throws Exception {
    Cohort cohort = createDefaultCohort();
    cohort = cohortsController.createCohort(workspace.getNamespace(), workspace.getId(), cohort).getBody();

    MaterializeCohortRequest request = new MaterializeCohortRequest();
    request.setCohortName("badCohort");
    cohortsController.materializeCohort(WORKSPACE_NAMESPACE, WORKSPACE_NAME, request);
  }

  @Test(expected = BadRequestException.class)
  public void testMaterializeCohortBadCohortSpec() throws Exception {
    Cohort cohort = createDefaultCohort();
    cohort = cohortsController.createCohort(workspace.getNamespace(), workspace.getId(), cohort).getBody();

    MaterializeCohortRequest request = new MaterializeCohortRequest();
    request.setCohortSpec("badSpec");
    cohortsController.materializeCohort(WORKSPACE_NAMESPACE, WORKSPACE_NAME, request);
  }

  @Test(expected = BadRequestException.class)
  public void testMaterializeCohortNoSpecOrCohortName() throws Exception {
    Cohort cohort = createDefaultCohort();
    cohort = cohortsController.createCohort(workspace.getNamespace(), workspace.getId(), cohort).getBody();

    MaterializeCohortRequest request = new MaterializeCohortRequest();
    cohortsController.materializeCohort(WORKSPACE_NAMESPACE, WORKSPACE_NAME, request);
  }

  @Test(expected = BadRequestException.class)
  public void testMaterializeCohortPageSizeTooSmall() throws Exception {
    Cohort cohort = createDefaultCohort();
    cohort = cohortsController.createCohort(workspace.getNamespace(), workspace.getId(), cohort).getBody();

    MaterializeCohortRequest request = new MaterializeCohortRequest();
    request.setCohortName(cohort.getName());
    request.setPageSize(-1);
    cohortsController.materializeCohort(WORKSPACE_NAMESPACE, WORKSPACE_NAME, request);
  }

  public void testMaterializeCohortPageSizeZero() throws Exception {
    Cohort cohort = createDefaultCohort();
    cohort = cohortsController.createCohort(workspace.getNamespace(), workspace.getId(), cohort).getBody();

    MaterializeCohortRequest request = new MaterializeCohortRequest();
    request.setCohortName(cohort.getName());
    request.setPageSize(0);
    MaterializeCohortResponse response = new MaterializeCohortResponse();
    when(cohortMaterializationService.materializeCohort(cdrVersion, searchRequest, null,
        CohortsController.DEFAULT_PAGE_SIZE, null)).thenReturn(response);
    assertThat(cohortsController.materializeCohort(WORKSPACE_NAMESPACE, WORKSPACE_NAME,
        request).getBody()).isEqualTo(response);
  }

  public void testMaterializeCohortPageSizeTooLarge() throws Exception {
    Cohort cohort = createDefaultCohort();
    cohort = cohortsController.createCohort(workspace.getNamespace(), workspace.getId(), cohort).getBody();

    MaterializeCohortRequest request = new MaterializeCohortRequest();
    request.setCohortName(cohort.getName());
    request.setPageSize(CohortsController.MAX_PAGE_SIZE + 1);
    MaterializeCohortResponse response = new MaterializeCohortResponse();
    when(cohortMaterializationService.materializeCohort(cdrVersion, searchRequest, null,
        CohortsController.MAX_PAGE_SIZE, null)).thenReturn(response);
    assertThat(cohortsController.materializeCohort(WORKSPACE_NAMESPACE, WORKSPACE_NAME,
        request).getBody()).isEqualTo(response);
  }

  @Test
  public void testMaterializeCohortNamedCohort() throws Exception {
    Cohort cohort = createDefaultCohort();
    cohort = cohortsController.createCohort(workspace.getNamespace(), workspace.getId(), cohort).getBody();

    MaterializeCohortRequest request = new MaterializeCohortRequest();
    request.setCohortName(cohort.getName());
    MaterializeCohortResponse response = new MaterializeCohortResponse();
    when(cohortMaterializationService.materializeCohort(cdrVersion, searchRequest, null,
        CohortsController.DEFAULT_PAGE_SIZE, null)).thenReturn(response);
    assertThat(cohortsController.materializeCohort(WORKSPACE_NAMESPACE, WORKSPACE_NAME,
        request).getBody()).isEqualTo(response);
  }

  @Test
  public void testMaterializeCohortWithSpec() throws Exception {
    Cohort cohort = createDefaultCohort();
    cohort = cohortsController.createCohort(workspace.getNamespace(), workspace.getId(), cohort).getBody();

    MaterializeCohortRequest request = new MaterializeCohortRequest();
    request.setCohortSpec(cohort.getCriteria());
    MaterializeCohortResponse response = new MaterializeCohortResponse();
    when(cohortMaterializationService.materializeCohort(cdrVersion, searchRequest, null,
        CohortsController.DEFAULT_PAGE_SIZE, null)).thenReturn(response);
    assertThat(cohortsController.materializeCohort(WORKSPACE_NAMESPACE, WORKSPACE_NAME,
        request).getBody()).isEqualTo(response);
  }

  @Test
  public void testMaterializeCohortWithEverything() throws Exception {
    Cohort cohort = createDefaultCohort();
    cohort = cohortsController.createCohort(workspace.getNamespace(), workspace.getId(), cohort).getBody();

    MaterializeCohortRequest request = new MaterializeCohortRequest();
    request.setCohortName(cohort.getName());
    request.setPageSize(123);
    request.setPageToken("token");
    request.setCdrVersionName(CDR_VERSION_NAME);
    List<CohortStatus> statuses = ImmutableList.of(CohortStatus.INCLUDED, CohortStatus.NOT_REVIEWED);
    request.setStatusFilter(statuses);
    MaterializeCohortResponse response = new MaterializeCohortResponse();
    when(cohortMaterializationService.materializeCohort(cdrVersion, searchRequest, statuses,
        123, "token")).thenReturn(response);
    assertThat(cohortsController.materializeCohort(WORKSPACE_NAMESPACE, WORKSPACE_NAME,
        request).getBody()).isEqualTo(response);
  }
}
