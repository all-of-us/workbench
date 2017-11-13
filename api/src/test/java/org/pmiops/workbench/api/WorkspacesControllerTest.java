package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Provider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.dao.WorkspaceService;
import org.pmiops.workbench.db.dao.WorkspaceServiceImpl;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.model.DataAccessLevel;
import org.pmiops.workbench.model.ResearchPurpose;
import org.pmiops.workbench.model.ResearchPurposeReviewRequest;
import org.pmiops.workbench.model.UserRole;
import org.pmiops.workbench.model.UserRoleList;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration.class)
@AutoConfigureTestDatabase(replace= AutoConfigureTestDatabase.Replace.NONE)
public class WorkspacesControllerTest {

  WorkspaceService workspaceService;
  @Autowired
  WorkspaceDao workspaceDao;
  @Autowired
  CdrVersionDao cdrVersionDao;
  @Autowired
  UserDao userDao;
  @Mock
  Provider<User> userProvider;
  @Mock
  FireCloudService fireCloudService;

  private WorkspacesController workspacesController;

  private static final Instant NOW = Instant.now();
  private static final long NOW_TIME = Timestamp.from(NOW).getTime();

  @Before
  public void setUp() {
    User user = new User();
    user.setEmail("bob@gmail.com");
    user.setUserId(123L);
    user = userDao.save(user);
    when(userProvider.get()).thenReturn(user);

    // Injecting WorkspaceService fails in the test environment. Work around it by injecting the
    // DAO and creating the service directly.
    workspaceService = new WorkspaceServiceImpl();
    workspaceService.setDao(workspaceDao);

    this.workspacesController = new WorkspacesController(workspaceService, cdrVersionDao,
        userDao, userProvider, fireCloudService,
        Clock.fixed(NOW, ZoneId.systemDefault()));
  }

  public Workspace createDefaultWorkspace() {
    ResearchPurpose researchPurpose = new ResearchPurpose();
    researchPurpose.setDiseaseFocusedResearch(true);
    researchPurpose.setDiseaseOfFocus("cancer");
    researchPurpose.setMethodsDevelopment(true);
    researchPurpose.setControlSet(true);
    researchPurpose.setAggregateAnalysis(true);
    researchPurpose.setAncestry(true);
    researchPurpose.setCommercialPurpose(true);
    researchPurpose.setPopulation(true);
    researchPurpose.setPopulationOfFocus("population");
    researchPurpose.setAdditionalNotes("additional notes");
    researchPurpose.setTimeRequested(new Long(1000));
    researchPurpose.setTimeReviewed(new Long(1500));
    researchPurpose.setReviewRequested(true);
    researchPurpose.setApproved(false);
    Workspace workspace = new Workspace();
    workspace.setName("name");
    workspace.setNamespace("namespace");
    workspace.setDescription("description");
    workspace.setDataAccessLevel(DataAccessLevel.PROTECTED);
    workspace.setResearchPurpose(researchPurpose);
    workspace.setUserRoles(new ArrayList<UserRole>());
    return workspace;
  }

  @Test
  public void testCreateWorkspace() throws Exception {
    Workspace workspace = createDefaultWorkspace();
    workspacesController.createWorkspace(workspace);
    verify(fireCloudService).createWorkspace("namespace", "name");

    Workspace workspace2 =
        workspacesController.getWorkspace("namespace", "name")
            .getBody();
    assertThat(workspace2.getCreationTime()).isEqualTo(NOW_TIME);
    assertThat(workspace2.getLastModifiedTime()).isEqualTo(NOW_TIME);
    assertThat(workspace2.getCdrVersionId()).isNull();
    assertThat(workspace2.getCreator()).isEqualTo("bob@gmail.com");
    assertThat(workspace2.getDataAccessLevel()).isEqualTo(DataAccessLevel.PROTECTED);
    assertThat(workspace2.getDescription()).isEqualTo("description");
    assertThat(workspace2.getId()).isEqualTo("name");
    assertThat(workspace2.getName()).isEqualTo("name");
    assertThat(workspace2.getResearchPurpose().getDiseaseFocusedResearch()).isTrue();
    assertThat(workspace2.getResearchPurpose().getDiseaseOfFocus()).isEqualTo("cancer");
    assertThat(workspace2.getResearchPurpose().getMethodsDevelopment()).isTrue();
    assertThat(workspace2.getResearchPurpose().getControlSet()).isTrue();
    assertThat(workspace2.getResearchPurpose().getAggregateAnalysis()).isTrue();
    assertThat(workspace2.getResearchPurpose().getAncestry()).isTrue();
    assertThat(workspace2.getResearchPurpose().getCommercialPurpose()).isTrue();
    assertThat(workspace2.getResearchPurpose().getPopulation()).isTrue();
    assertThat(workspace2.getResearchPurpose().getPopulationOfFocus()).isEqualTo("population");
    assertThat(workspace2.getResearchPurpose().getAdditionalNotes()).isEqualTo("additional notes");
    assertThat(workspace2.getNamespace()).isEqualTo("namespace");
    assertThat(workspace2.getResearchPurpose().getReviewRequested()).isTrue();
    assertThat(workspace2.getResearchPurpose().getApproved()).isFalse();
    assertThat(workspace2.getResearchPurpose().getTimeReviewed()).isEqualTo(new Long(1500));
    assertThat(workspace2.getResearchPurpose().getTimeRequested()).isEqualTo(new Long(1000));

    //Test that the correct owner is added.
    assertThat(workspace2.getUserRoles().size()).isEqualTo(1);
    assertThat(workspace2.getUserRoles().get(0).getRole()).isEqualTo(WorkspaceAccessLevel.OWNER);
  }

  @Test
  public void testApproveWorkspace() throws Exception {
    Workspace ws = createDefaultWorkspace();
    ResearchPurpose researchPurpose = ws.getResearchPurpose();
    researchPurpose.setApproved(null);
    researchPurpose.setTimeReviewed(null);
    workspacesController.createWorkspace(ws);

    // TODO(RW-216) Inject Clock and verify timestamps.
    ResearchPurposeReviewRequest request = new ResearchPurposeReviewRequest();
    request.setApproved(true);
    workspacesController.reviewWorkspace(ws.getNamespace(), ws.getName(), request);

    ws = workspacesController.getWorkspace(ws.getNamespace(), ws.getName()).getBody();
    researchPurpose = ws.getResearchPurpose();

    assertThat(researchPurpose.getApproved()).isTrue();
    assertThat(researchPurpose.getTimeReviewed()).isNotNull();
  }

  @Test(expected = BadRequestException.class)
  public void testRejectAfterApproveThrows() throws Exception {
    Workspace ws = createDefaultWorkspace();
    ResearchPurpose researchPurpose = ws.getResearchPurpose();
    researchPurpose.setApproved(true);
    workspacesController.createWorkspace(ws);

    ResearchPurposeReviewRequest request = new ResearchPurposeReviewRequest();
    request.setApproved(false);

    workspacesController.reviewWorkspace(ws.getNamespace(), ws.getName(), request);
  }

  @Test
  public void testListForApproval() throws Exception {
    List<Workspace> forApproval =
        workspacesController.getWorkspacesForReview().getBody().getItems();
    assertThat(forApproval).isEmpty();

    Workspace ws;
    ResearchPurpose researchPurpose;
    String nameForRequested = "requestedButNotApprovedYet";
    // requested approval, but not approved
    ws = createDefaultWorkspace();
    ws.setName(nameForRequested);
    researchPurpose = ws.getResearchPurpose();
    researchPurpose.setApproved(null);
    researchPurpose.setTimeReviewed(null);
    workspacesController.createWorkspace(ws);
    // already approved
    ws = createDefaultWorkspace();
    ws.setName("alreadyApproved");
    researchPurpose = ws.getResearchPurpose();
    researchPurpose.setApproved(true);
    workspacesController.createWorkspace(ws);
    // no approval requested
    ws = createDefaultWorkspace();
    ws.setName("noApprovalRequested");
    researchPurpose = ws.getResearchPurpose();
    researchPurpose.setReviewRequested(false);
    researchPurpose.setTimeRequested(null);
    researchPurpose.setApproved(null);
    researchPurpose.setTimeReviewed(null);
    workspacesController.createWorkspace(ws);

    forApproval = workspacesController.getWorkspacesForReview().getBody().getItems();
    assertThat(forApproval.size()).isEqualTo(1);
    ws = forApproval.get(0);
    assertThat(ws.getName()).isEqualTo(nameForRequested);
  }

  @Test
  public void testShareWorkspace() {
    User writerUser = new User();
    writerUser.setEmail("writerfriend@gmail.com");
    writerUser.setUserId(124L);
    writerUser = userDao.save(writerUser);
    User readerUser = new User();
    readerUser.setEmail("readerfriend@gmail.com");
    readerUser.setUserId(125L);
    readerUser = userDao.save(readerUser);
    Workspace workspace = createDefaultWorkspace();
    workspacesController.createWorkspace(workspace);
    UserRoleList updatedUserRoles = new UserRoleList();
    UserRole creator = new UserRole();
    creator.setEmail("bob@gmail.com");
    creator.setRole(WorkspaceAccessLevel.OWNER);
    updatedUserRoles.addItemsItem(creator);
    UserRole writer = new UserRole();
    writer.setEmail("writerfriend@gmail.com");
    writer.setRole(WorkspaceAccessLevel.WRITER);
    updatedUserRoles.addItemsItem(writer);
    UserRole reader = new UserRole();
    reader.setEmail("readerfriend@gmail.com");
    reader.setRole(WorkspaceAccessLevel.READER);
    updatedUserRoles.addItemsItem(reader);

    workspacesController.shareWorkspace("namespace", "name", updatedUserRoles);
    Workspace workspace2 =
        workspacesController.getWorkspace("namespace", "name")
            .getBody();

    assertThat(workspace2.getUserRoles().size()).isEqualTo(3);
    int numOwners = 0;
    int numWriters = 0;
    int numReaders = 0;
    for (UserRole userRole : workspace2.getUserRoles()) {
      if (userRole.getRole().equals(WorkspaceAccessLevel.OWNER)) {
        assertThat(userRole.getEmail()).isEqualTo("bob@gmail.com");
        numOwners++;
      } else if (userRole.getRole().equals(WorkspaceAccessLevel.WRITER)) {
        assertThat(userRole.getEmail()).isEqualTo("writerfriend@gmail.com");
        numWriters++;
      } else {
        assertThat(userRole.getEmail()).isEqualTo("readerfriend@gmail.com");
        numReaders++;
      }
    }
    assertThat(numOwners).isEqualTo(1);
    assertThat(numWriters).isEqualTo(1);
    assertThat(numReaders).isEqualTo(1);
  }

  @Test
  public void testUnshareWorkspace() {
    User writerUser = new User();
    writerUser.setEmail("writerfriend@gmail.com");
    writerUser.setUserId(124L);
    writerUser = userDao.save(writerUser);
    User readerUser = new User();
    readerUser.setEmail("readerfriend@gmail.com");
    readerUser.setUserId(125L);
    readerUser = userDao.save(readerUser);
    Workspace workspace = createDefaultWorkspace();
    workspacesController.createWorkspace(workspace);
    UserRoleList updatedUserRoles = new UserRoleList();
    UserRole creator = new UserRole();
    creator.setEmail("bob@gmail.com");
    creator.setRole(WorkspaceAccessLevel.OWNER);
    updatedUserRoles.addItemsItem(creator);
    UserRole writer = new UserRole();
    writer.setEmail("writerfriend@gmail.com");
    writer.setRole(WorkspaceAccessLevel.WRITER);
    updatedUserRoles.addItemsItem(writer);
    UserRole reader = new UserRole();
    reader.setEmail("readerfriend@gmail.com");
    reader.setRole(WorkspaceAccessLevel.READER);
    updatedUserRoles.addItemsItem(reader);

    workspacesController.shareWorkspace("namespace", "name", updatedUserRoles);
    updatedUserRoles = new UserRoleList();
    updatedUserRoles.addItemsItem(creator);
    updatedUserRoles.addItemsItem(writer);
    workspacesController.shareWorkspace("namespace", "name", updatedUserRoles);
    Workspace workspace2 =
        workspacesController.getWorkspace("namespace", "name")
            .getBody();

    assertThat(workspace2.getUserRoles().size()).isEqualTo(2);
    int numOwners = 0;
    int numWriters = 0;
    int numReaders = 0;
    for (UserRole userRole : workspace2.getUserRoles()) {
      if (userRole.getRole().equals(WorkspaceAccessLevel.OWNER)) {
        assertThat(userRole.getEmail()).isEqualTo("bob@gmail.com");
        numOwners++;
      } else if (userRole.getRole().equals(WorkspaceAccessLevel.WRITER)) {
        assertThat(userRole.getEmail()).isEqualTo("writerfriend@gmail.com");
        numWriters++;
      } else {
        assertThat(userRole.getEmail()).isEqualTo("readerfriend@gmail.com");
        numReaders++;
      }
    }
    assertThat(numOwners).isEqualTo(1);
    assertThat(numWriters).isEqualTo(1);
    assertThat(numReaders).isEqualTo(0);
  }

  @Test(expected = BadRequestException.class)
  public void testUnableToShareWithNonExistantUser() throws Exception {
    Workspace workspace = createDefaultWorkspace();
    workspacesController.createWorkspace(workspace);
    UserRoleList updatedUserRoles = new UserRoleList();
    UserRole creator = new UserRole();
    creator.setEmail("bob@gmail.com");
    creator.setRole(WorkspaceAccessLevel.OWNER);
    updatedUserRoles.addItemsItem(creator);
    UserRole writer = new UserRole();
    writer.setEmail("writerfriend@gmail.com");
    writer.setRole(WorkspaceAccessLevel.WRITER);
    updatedUserRoles.addItemsItem(writer);
    workspacesController.shareWorkspace("namespace", "name", updatedUserRoles);
  }
}
