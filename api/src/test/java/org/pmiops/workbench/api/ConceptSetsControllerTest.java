package org.pmiops.workbench.api;

import static org.mockito.Mockito.when;
import static com.google.common.truth.Truth.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import javax.inject.Provider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.pmiops.workbench.cdr.dao.ConceptDao;
import org.pmiops.workbench.cdr.dao.ConceptService;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.CohortService;
import org.pmiops.workbench.db.dao.ConceptSetDao;
import org.pmiops.workbench.db.dao.ConceptSetService;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserRecentResourceService;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.dao.WorkspaceService;
import org.pmiops.workbench.db.dao.WorkspaceServiceImpl;
import org.pmiops.workbench.db.model.CdrVersion;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.google.CloudStorageService;
import org.pmiops.workbench.model.ConceptSet;
import org.pmiops.workbench.model.DataAccessLevel;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.EmailVerificationStatus;
import org.pmiops.workbench.model.ResearchPurpose;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.notebooks.NotebooksService;
import org.pmiops.workbench.test.FakeClock;
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

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration.class)
@AutoConfigureTestDatabase(replace= AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class ConceptSetsControllerTest {

  private static final String USER_EMAIL = "bob@gmail.com";
  private static final String WORKSPACE_NAMESPACE = "ns";
  private static final String WORKSPACE_NAME = "name";
  private static final String WORKSPACE_NAME_2 = "name2";
  private static final Instant NOW = Instant.now();
  private static final FakeClock CLOCK = new FakeClock(NOW, ZoneId.systemDefault());

  @Autowired
  WorkspaceService workspaceService;

  @Autowired
  ConceptSetDao conceptSetDao;

  @Autowired
  CdrVersionDao cdrVersionDao;

  @Autowired
  ConceptDao conceptDao;

  @Autowired
  CohortDao cohortDao;

  @Autowired
  WorkspaceDao workspaceDao;

  @Autowired
  UserDao userDao;

  @Autowired
  CloudStorageService cloudStorageService;

  @Autowired
  UserService userService;

  @Autowired
  FireCloudService fireCloudService;

  @Mock
  Provider<User> userProvider;

  private ConceptSetsController conceptSetsController;
  private User user;

  @TestConfiguration
  @Import({WorkspaceServiceImpl.class, CohortService.class, ConceptSetService.class,
      UserService.class, ConceptSetsController.class})
  @MockBean({FireCloudService.class, CloudStorageService.class})
  static class Configuration {
    @Bean
    Clock clock() {
      return CLOCK;
    }
  }

  @Before
  public void setUp() throws Exception {
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

    Workspace workspace = new org.pmiops.workbench.model.Workspace();
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

    WorkspacesController workspacesController = new WorkspacesController(workspaceService,
        cdrVersionDao, cohortDao, userDao, userProvider, fireCloudService, cloudStorageService, CLOCK,
        userService);
    stubGetWorkspace(WORKSPACE_NAMESPACE, WORKSPACE_NAME, USER_EMAIL, WorkspaceAccessLevel.OWNER);
    stubGetWorkspace(WORKSPACE_NAMESPACE, WORKSPACE_NAME_2, USER_EMAIL, WorkspaceAccessLevel.OWNER);
    workspacesController.createWorkspace(workspace);
    workspacesController.createWorkspace(workspace2);

    org.pmiops.workbench.firecloud.model.WorkspaceResponse fcResponse =
        new org.pmiops.workbench.firecloud.model.WorkspaceResponse();
    fcResponse.setAccessLevel(WorkspaceAccessLevel.OWNER.name());
    when(fireCloudService.getWorkspace(WORKSPACE_NAMESPACE, WORKSPACE_NAME))
        .thenReturn(fcResponse);
    when(fireCloudService.getWorkspace(WORKSPACE_NAMESPACE, WORKSPACE_NAME_2))
        .thenReturn(fcResponse);

    conceptSetsController = new ConceptSetsController(workspaceService, conceptSetDao, conceptDao,
        userProvider, CLOCK);
  }

  @Test
  public void testGetConceptSetsInWorkspaceEmpty() {
    assertThat(conceptSetsController.getConceptSetsInWorkspace(WORKSPACE_NAMESPACE, WORKSPACE_NAME)
        .getBody().getItems()).isEmpty();
  }

  @Test
  public void testCreateAndGetConceptSet() {
    ConceptSet conceptSet = makeConceptSet1();
    assertThat(conceptSet.getCreator()).isEqualTo(USER_EMAIL);
    assertThat(conceptSet.getConcepts()).isNull();
    assertThat(conceptSet.getCreationTime()).isEqualTo(NOW.toEpochMilli());
    assertThat(conceptSet.getDescription()).isEqualTo("desc 1");
    assertThat(conceptSet.getDomain()).isEqualTo(Domain.CONDITION);
    assertThat(conceptSet.getEtag()).isEqualTo(Etags.fromVersion(1));
    assertThat(conceptSet.getLastModifiedTime()).isEqualTo(NOW.toEpochMilli());
    assertThat(conceptSet.getName()).isEqualTo("concept set 1");

    assertThat(conceptSetsController.getConceptSet(WORKSPACE_NAMESPACE, WORKSPACE_NAME,
        conceptSet.getId()).getBody()).isEqualTo(conceptSet);
    assertThat(conceptSetsController.getConceptSetsInWorkspace(WORKSPACE_NAMESPACE, WORKSPACE_NAME)
        .getBody().getItems()).containsExactly(conceptSet);
    assertThat(conceptSetsController.getConceptSetsInWorkspace(WORKSPACE_NAMESPACE, WORKSPACE_NAME_2)
        .getBody().getItems()).isEmpty();
  }

  private ConceptSet makeConceptSet1() {
    ConceptSet conceptSet = new ConceptSet();
    conceptSet.setDescription("desc 1");
    conceptSet.setName("concept set 1");
    conceptSet.setDomain(Domain.CONDITION);
    return conceptSetsController.createConceptSet(WORKSPACE_NAMESPACE, WORKSPACE_NAME, conceptSet)
        .getBody();

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

}
