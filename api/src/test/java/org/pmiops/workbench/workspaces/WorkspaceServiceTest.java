package org.pmiops.workbench.workspaces;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.pmiops.workbench.db.dao.CohortCloningService;
import org.pmiops.workbench.db.dao.ConceptSetService;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.Workspace.FirecloudWorkspaceId;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.Workspace;
import org.pmiops.workbench.firecloud.model.WorkspaceResponse;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class WorkspaceServiceTest {

  @TestConfiguration
  @Import({
      WorkspaceMapper.class
  })
  static class Configuration { }

  @Mock
  private CohortCloningService cohortCloningService;
  @Mock
  private ConceptSetService conceptSetService;
  @Mock
  private WorkspaceDao workspaceDao;
  @Autowired
  private WorkspaceMapper workspaceMapper;
  @Mock
  private FireCloudService fireCloudService;
  @Mock
  private Clock clock;

  private WorkspaceService workspaceService;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    workspaceService = new WorkspaceServiceImpl(clock, cohortCloningService, conceptSetService,
        fireCloudService, workspaceDao, workspaceMapper);

    List<WorkspaceResponse> firecloudWorkspaceResponses = Arrays.asList(
        mockFirecloudWorkspaceResponse("reader", WorkspaceAccessLevel.READER),
        mockFirecloudWorkspaceResponse("writer", WorkspaceAccessLevel.WRITER),
        mockFirecloudWorkspaceResponse("owner", WorkspaceAccessLevel.OWNER)
    );
    doReturn(firecloudWorkspaceResponses).when(fireCloudService).getWorkspaces();

    List<org.pmiops.workbench.db.model.Workspace> workspaces = firecloudWorkspaceResponses
        .stream()
        .map(workspaceResponse -> mockDbWorkspace(
            workspaceResponse.getWorkspace().getWorkspaceId(),
            workspaceResponse.getWorkspace().getWorkspaceId()))
        .collect(Collectors.toList());
    doReturn(workspaces).when(workspaceDao).findAllByFirecloudUuidIn(any());
  }

  private WorkspaceResponse mockFirecloudWorkspaceResponse(String workspaceId, WorkspaceAccessLevel accessLevel) {
    Workspace workspace = mock(Workspace.class);
    doReturn(workspaceId).when(workspace).getWorkspaceId();
    WorkspaceResponse workspaceResponse = mock(WorkspaceResponse.class);
    doReturn(workspace).when(workspaceResponse).getWorkspace();
    doReturn(accessLevel.toString()).when(workspaceResponse).getAccessLevel();
    return workspaceResponse;
  }

  private org.pmiops.workbench.db.model.Workspace mockDbWorkspace(String name, String firecloudUuid) {
    org.pmiops.workbench.db.model.Workspace workspace = mock(org.pmiops.workbench.db.model.Workspace.class);
    doReturn(new Timestamp(1000000l)).when(workspace).getLastModifiedTime();
    doReturn(new Timestamp(1000000l)).when(workspace).getCreationTime();
    doReturn(name).when(workspace).getName();
    doReturn(mock(FirecloudWorkspaceId.class)).when(workspace).getFirecloudWorkspaceId();
    doReturn(firecloudUuid).when(workspace).getFirecloudUuid();
    return workspace;
  }
  
  @Test
  public void getWorkspaces() {
    assertThat(workspaceService.getWorkspaces()).hasSize(3);
  }
}
