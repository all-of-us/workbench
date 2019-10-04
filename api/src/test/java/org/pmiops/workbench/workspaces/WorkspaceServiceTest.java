package org.pmiops.workbench.workspaces;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.pmiops.workbench.cohorts.CohortCloningService;
import org.pmiops.workbench.conceptset.ConceptSetService;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserRecentWorkspaceDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.db.model.UserRecentWorkspace;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.Workspace;
import org.pmiops.workbench.firecloud.model.WorkspaceACL;
import org.pmiops.workbench.firecloud.model.WorkspaceAccessEntry;
import org.pmiops.workbench.firecloud.model.WorkspaceResponse;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.model.WorkspaceActiveStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class WorkspaceServiceTest {
  @TestConfiguration
  @Import({WorkspaceMapper.class})
  static class Configuration {}

  @Autowired private WorkspaceDao workspaceDao;
  @Autowired private UserDao userDao;
  @Autowired private UserRecentWorkspaceDao userRecentWorkspaceDao;
  @Autowired private WorkspaceMapper workspaceMapper;

  @Mock private CohortCloningService mockCohortCloningService;
  @Mock private ConceptSetService mockConceptSetService;
  @Mock private Provider<User> mockUserProvider;
  @Mock private FireCloudService mockFireCloudService;
  @Mock private Clock mockClock;

  private WorkspaceService workspaceService;

  private List<WorkspaceResponse> mockWorkspaceResponses = new ArrayList<>();
  private List<org.pmiops.workbench.db.model.Workspace> mockWorkspaces = new ArrayList<>();
  private AtomicLong workspaceIdIncrementer = new AtomicLong(1);
  private Instant NOW = Instant.now();
  private long USER_ID = 1L;
  private String DEFAULT_USER_EMAIL = "mock@mock.com";
  private String DEFAULT_WORKSPACE_NAMESPACE = "namespace";

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    workspaceService =
        new WorkspaceServiceImpl(
            mockClock,
            mockCohortCloningService,
            mockConceptSetService,
            mockFireCloudService,
            userDao,
            mockUserProvider,
            userRecentWorkspaceDao,
            workspaceDao,
            workspaceMapper);

    mockWorkspaceResponses.clear();
    mockWorkspaces.clear();
    addMockedWorkspace(
        workspaceIdIncrementer.getAndIncrement(),
        "reader",
        DEFAULT_WORKSPACE_NAMESPACE,
        WorkspaceAccessLevel.READER,
        WorkspaceActiveStatus.ACTIVE);
    addMockedWorkspace(
        workspaceIdIncrementer.getAndIncrement(),
        "writer",
        DEFAULT_WORKSPACE_NAMESPACE,
        WorkspaceAccessLevel.WRITER,
        WorkspaceActiveStatus.ACTIVE);
    addMockedWorkspace(
        workspaceIdIncrementer.getAndIncrement(),
        "owner",
        DEFAULT_WORKSPACE_NAMESPACE,
        WorkspaceAccessLevel.OWNER,
        WorkspaceActiveStatus.ACTIVE);
    addMockedWorkspace(
        workspaceIdIncrementer.getAndIncrement(),
        "extra",
        DEFAULT_WORKSPACE_NAMESPACE,
        WorkspaceAccessLevel.OWNER,
        WorkspaceActiveStatus.ACTIVE);
    addMockedWorkspace(
        workspaceIdIncrementer.getAndIncrement(),
        "another_extra",
        DEFAULT_WORKSPACE_NAMESPACE,
        WorkspaceAccessLevel.OWNER,
        WorkspaceActiveStatus.ACTIVE);

    doReturn(mockWorkspaceResponses).when(mockFireCloudService).getWorkspaces();
    User mockUser = mock(User.class);
    doReturn(mockUser).when(mockUserProvider).get();
    doReturn(DEFAULT_USER_EMAIL).when(mockUser).getEmail();
    doReturn(USER_ID).when(mockUser).getUserId();
  }

  private WorkspaceResponse mockFirecloudWorkspaceResponse(
      String workspaceId,
      String workspaceName,
      String workspaceNamespace,
      WorkspaceAccessLevel accessLevel) {
    Workspace mockWorkspace = mock(Workspace.class);
    doReturn(workspaceNamespace).when(mockWorkspace).getNamespace();
    doReturn(workspaceName).when(mockWorkspace).getName();
    doReturn(workspaceId).when(mockWorkspace).getWorkspaceId();
    WorkspaceResponse mockWorkspaceResponse = mock(WorkspaceResponse.class);
    doReturn(mockWorkspace).when(mockWorkspaceResponse).getWorkspace();
    doReturn(accessLevel.toString()).when(mockWorkspaceResponse).getAccessLevel();
    return mockWorkspaceResponse;
  }

  private org.pmiops.workbench.db.model.Workspace buildDbWorkspace(
      long dbId, String name, String namespace, WorkspaceActiveStatus activeStatus) {
    org.pmiops.workbench.db.model.Workspace workspace =
        new org.pmiops.workbench.db.model.Workspace();
    Timestamp nowTimestamp = Timestamp.from(NOW);
    workspace.setLastModifiedTime(nowTimestamp);
    workspace.setCreationTime(nowTimestamp);
    workspace.setName(name);
    workspace.setWorkspaceId(dbId);
    workspace.setWorkspaceNamespace(namespace);
    workspace.setWorkspaceActiveStatusEnum(activeStatus);
    workspace.setFirecloudName(name);
    workspace.setFirecloudUuid(Long.toString(dbId));
    return workspace;
  }

  private org.pmiops.workbench.db.model.Workspace addMockedWorkspace(
      long workspaceId,
      String workspaceName,
      String workspaceNamespace,
      WorkspaceAccessLevel accessLevel,
      WorkspaceActiveStatus activeStatus) {

    WorkspaceACL workspaceAccessLevelResponse = spy(WorkspaceACL.class);
    HashMap<String, WorkspaceAccessEntry> acl = new HashMap<>();
    WorkspaceAccessEntry accessLevelEntry =
        new WorkspaceAccessEntry().accessLevel(accessLevel.toString());
    acl.put(DEFAULT_USER_EMAIL, accessLevelEntry);
    doReturn(acl).when(workspaceAccessLevelResponse).getAcl();
    workspaceAccessLevelResponse.setAcl(acl);
    WorkspaceResponse mockWorkspaceResponse =
        mockFirecloudWorkspaceResponse(
            Long.toString(workspaceId), workspaceName, workspaceNamespace, accessLevel);
    doReturn(workspaceAccessLevelResponse)
        .when(mockFireCloudService)
        .getWorkspaceAcl(workspaceNamespace, workspaceName);
    mockWorkspaceResponses.add(mockWorkspaceResponse);

    org.pmiops.workbench.db.model.Workspace dbWorkspace =
        workspaceDao.save(
            buildDbWorkspace(
                workspaceId,
                mockWorkspaceResponse.getWorkspace().getName(),
                workspaceNamespace,
                activeStatus));

    mockWorkspaces.add(dbWorkspace);
    return dbWorkspace;
  }

  @Test
  public void getWorkspaces() {
    assertThat(workspaceService.getWorkspaces()).hasSize(5);
  }

  @Test
  public void getWorkspaces_skipPending() {
    int currentWorkspacesSize = workspaceService.getWorkspaces().size();

    addMockedWorkspace(
        workspaceIdIncrementer.getAndIncrement(),
        "inactive",
        DEFAULT_WORKSPACE_NAMESPACE,
        WorkspaceAccessLevel.OWNER,
        WorkspaceActiveStatus.PENDING_DELETION_POST_1PPW_MIGRATION);
    assertThat(workspaceService.getWorkspaces().size()).isEqualTo(currentWorkspacesSize);
  }

  @Test
  public void getWorkspaces_skipDeleted() {
    int currentWorkspacesSize = workspaceService.getWorkspaces().size();

    addMockedWorkspace(
        workspaceIdIncrementer.getAndIncrement(),
        "deleted",
        DEFAULT_WORKSPACE_NAMESPACE,
        WorkspaceAccessLevel.OWNER,
        WorkspaceActiveStatus.DELETED);
    assertThat(workspaceService.getWorkspaces().size()).isEqualTo(currentWorkspacesSize);
  }

  @Test
  public void activeStatus() {
    EnumSet.allOf(WorkspaceActiveStatus.class)
        .forEach(
            status ->
                assertThat(
                        buildDbWorkspace(
                                workspaceIdIncrementer.getAndIncrement(),
                                "1",
                                DEFAULT_WORKSPACE_NAMESPACE,
                                status)
                            .getWorkspaceActiveStatusEnum())
                    .isEqualTo(status));
  }

  @Test
  public void updateRecentWorkspaces() {
    mockWorkspaces.forEach(
        workspace -> {
          // Need a new 'now' each time or else we won't have lastAccessDates that are different
          // from each other
          workspaceService.updateRecentWorkspaces(
              workspace,
              USER_ID,
              Timestamp.from(NOW.minusSeconds(mockWorkspaces.size() - workspace.getWorkspaceId())));
        });
    List<UserRecentWorkspace> recentWorkspaces = workspaceService.getRecentWorkspaces();
    assertThat(recentWorkspaces.size()).isEqualTo(WorkspaceServiceImpl.RECENT_WORKSPACE_COUNT);

    List<Long> actualIds =
        recentWorkspaces.stream()
            .map(UserRecentWorkspace::getWorkspaceId)
            .collect(Collectors.toList());
    List<Long> expectedIds =
        mockWorkspaces
            .subList(
                mockWorkspaces.size() - WorkspaceServiceImpl.RECENT_WORKSPACE_COUNT,
                mockWorkspaces.size())
            .stream()
            .map(org.pmiops.workbench.db.model.Workspace::getWorkspaceId)
            .collect(Collectors.toList());
    assertThat(actualIds).containsAll(expectedIds);
  }

  @Test
  public void updateRecentWorkspaces_flipFlop() {
    workspaceService.updateRecentWorkspaces(
        mockWorkspaces.get(0), USER_ID, Timestamp.from(NOW.minusSeconds(4)));
    workspaceService.updateRecentWorkspaces(
        mockWorkspaces.get(1), USER_ID, Timestamp.from(NOW.minusSeconds(3)));
    workspaceService.updateRecentWorkspaces(
        mockWorkspaces.get(0), USER_ID, Timestamp.from(NOW.minusSeconds(2)));
    workspaceService.updateRecentWorkspaces(
        mockWorkspaces.get(1), USER_ID, Timestamp.from(NOW.minusSeconds(1)));
    workspaceService.updateRecentWorkspaces(mockWorkspaces.get(0), USER_ID, Timestamp.from(NOW));

    List<UserRecentWorkspace> recentWorkspaces = workspaceService.getRecentWorkspaces();
    assertThat(recentWorkspaces.size()).isEqualTo(2);
    List<Long> actualIds =
        recentWorkspaces.stream()
            .map(UserRecentWorkspace::getWorkspaceId)
            .collect(Collectors.toList());
    assertThat(actualIds).contains(1L, 2L);
  }

  @Test
  public void enforceFirecloudAclsInRecentWorkspaces() {
    long ownedId = workspaceIdIncrementer.getAndIncrement();
    org.pmiops.workbench.db.model.Workspace ownedWorkspace =
        addMockedWorkspace(
            ownedId,
            "owned",
            "owned_namespace",
            WorkspaceAccessLevel.OWNER,
            WorkspaceActiveStatus.ACTIVE);
    workspaceService.updateRecentWorkspaces(ownedWorkspace, USER_ID, Timestamp.from(NOW));

    org.pmiops.workbench.db.model.Workspace sharedWorkspace =
        addMockedWorkspace(
            workspaceIdIncrementer.getAndIncrement(),
            "shared",
            "shared_namespace",
            WorkspaceAccessLevel.NO_ACCESS,
            WorkspaceActiveStatus.ACTIVE);
    workspaceService.updateRecentWorkspaces(sharedWorkspace, USER_ID, Timestamp.from(NOW));

    List<UserRecentWorkspace> recentWorkspaces = workspaceService.getRecentWorkspaces();
    assertThat(recentWorkspaces.size()).isEqualTo(1);
    assertThat(recentWorkspaces.get(0).getWorkspaceId()).isEqualTo(ownedId);
  }
}
