package org.pmiops.workbench.workspaces;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import javax.inject.Provider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.pmiops.workbench.cohorts.CohortCloningService;
import org.pmiops.workbench.conceptset.ConceptSetService;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.db.model.Workspace.FirecloudWorkspaceId;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.Workspace;
import org.pmiops.workbench.firecloud.model.WorkspaceACL;
import org.pmiops.workbench.firecloud.model.WorkspaceAccessEntry;
import org.pmiops.workbench.firecloud.model.WorkspaceResponse;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.model.WorkspaceActiveStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class WorkspaceServiceTest {
  public static String workspaceNamespace = "namespace";

  @TestConfiguration
  @Import({WorkspaceMapper.class})
  static class Configuration {}

  @Mock private CohortCloningService cohortCloningService;
  @Mock private ConceptSetService conceptSetService;
  @Mock private WorkspaceDao workspaceDao;
  @Mock private UserDao userDao;
  @Mock private Provider<User> userProvider;
  @Autowired private WorkspaceMapper workspaceMapper;
  @Mock private FireCloudService fireCloudService;
  @Mock private Clock clock;

  private WorkspaceService workspaceService;

  private List<WorkspaceResponse> workspaceResponses = new ArrayList<>();
  private List<org.pmiops.workbench.db.model.Workspace> workspaces = new ArrayList<>();

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    workspaceService =
        new WorkspaceServiceImpl(
            clock,
            cohortCloningService,
            conceptSetService,
            fireCloudService,
            userDao,
            userProvider,
            workspaceDao,
            workspaceMapper);

    doReturn(workspaceResponses).when(fireCloudService).getWorkspaces();
    doReturn(workspaces).when(workspaceDao).findAllByFirecloudUuidIn(any());

    workspaceResponses.clear();
    workspaces.clear();
    addMockedWorkspace("reader", WorkspaceAccessLevel.READER, WorkspaceActiveStatus.ACTIVE);
    addMockedWorkspace("writer", WorkspaceAccessLevel.WRITER, WorkspaceActiveStatus.ACTIVE);
    addMockedWorkspace("owner", WorkspaceAccessLevel.OWNER, WorkspaceActiveStatus.ACTIVE);
  }

  private WorkspaceResponse mockFirecloudWorkspaceResponse(
      String workspaceId, WorkspaceAccessLevel accessLevel) {
    Workspace workspace = mock(Workspace.class);
    doReturn(workspaceNamespace).when(workspace).getNamespace();
    doReturn(workspaceId).when(workspace).getWorkspaceId();
    WorkspaceResponse workspaceResponse = mock(WorkspaceResponse.class);
    doReturn(workspace).when(workspaceResponse).getWorkspace();
    doReturn(accessLevel.toString()).when(workspaceResponse).getAccessLevel();
    return workspaceResponse;
  }

  private org.pmiops.workbench.db.model.Workspace mockDbWorkspace(
      String name, String firecloudUuid, WorkspaceActiveStatus activeStatus) {
    org.pmiops.workbench.db.model.Workspace workspace =
        spy(org.pmiops.workbench.db.model.Workspace.class);
    doReturn(mock(Timestamp.class)).when(workspace).getLastModifiedTime();
    doReturn(mock(Timestamp.class)).when(workspace).getCreationTime();
    doReturn(name).when(workspace).getName();
    workspace.setWorkspaceActiveStatusEnum(activeStatus);
    doReturn(mock(FirecloudWorkspaceId.class)).when(workspace).getFirecloudWorkspaceId();
    doReturn(firecloudUuid).when(workspace).getFirecloudUuid();
    return workspace;
  }

  private void addMockedWorkspace(
      String workspaceId, WorkspaceAccessLevel accessLevel, WorkspaceActiveStatus activeStatus) {

    WorkspaceACL workspaceAccessLevelResponse = spy(WorkspaceACL.class);
    HashMap<String, WorkspaceAccessEntry> acl = spy(HashMap.class);
    WorkspaceAccessEntry accessLevelEntry =
        new WorkspaceAccessEntry().accessLevel(accessLevel.toString());
    doReturn(acl).when(workspaceAccessLevelResponse).getAcl();
    doReturn(accessLevelEntry).when(acl).get(anyString());
    workspaceAccessLevelResponse.setAcl(acl);
    WorkspaceResponse workspaceResponse = mockFirecloudWorkspaceResponse(workspaceId, accessLevel);
    doReturn(workspaceAccessLevelResponse)
        .when(fireCloudService)
        .getWorkspaceAcl(workspaceNamespace, workspaceId);
    workspaceResponses.add(workspaceResponse);

    workspaces.add(
        mockDbWorkspace(
            workspaceResponse.getWorkspace().getWorkspaceId(),
            workspaceResponse.getWorkspace().getWorkspaceId(),
            activeStatus));
  }

  @Test
  public void getWorkspaces() {
    assertThat(workspaceService.getWorkspaces()).hasSize(3);
  }

  @Test
  public void getWorkspaces_skipPending() {
    int currentWorkspacesSize = workspaceService.getWorkspaces().size();

    addMockedWorkspace(
        "inactive",
        WorkspaceAccessLevel.OWNER,
        WorkspaceActiveStatus.PENDING_DELETION_POST_1PPW_MIGRATION);
    assertThat(workspaceService.getWorkspaces().size()).isEqualTo(currentWorkspacesSize);
  }

  @Test
  public void getWorkspaces_skipDeleted() {
    int currentWorkspacesSize = workspaceService.getWorkspaces().size();

    addMockedWorkspace("deleted", WorkspaceAccessLevel.OWNER, WorkspaceActiveStatus.DELETED);
    assertThat(workspaceService.getWorkspaces().size()).isEqualTo(currentWorkspacesSize);
  }

  @Test
  public void activeStatus() {
    EnumSet.allOf(WorkspaceActiveStatus.class)
        .forEach(
            status ->
                assertThat(mockDbWorkspace("1", "1", status).getWorkspaceActiveStatusEnum())
                    .isEqualTo(status));
  }
}
