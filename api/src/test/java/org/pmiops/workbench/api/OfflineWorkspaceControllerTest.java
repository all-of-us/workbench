package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.pmiops.workbench.cloudtasks.TaskQueueService;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.pmiops.workbench.workspaces.WorkspaceUserCacheService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
public class OfflineWorkspaceControllerTest {

  @Mock private WorkspaceService workspaceService;
  @Mock private WorkspaceUserCacheService mockWorkspaceUserCacheService;
  @Mock private TaskQueueService mockTaskQueueService;

  private OfflineWorkspaceController offlineWorkspaceController;

  @Mock private WorkspaceDao.WorkspaceUserCacheView testWorkspace1;
  @Mock private WorkspaceDao.WorkspaceUserCacheView testWorkspace2;

  @BeforeEach
  public void setUp() {
    offlineWorkspaceController =
        new OfflineWorkspaceController(
            mockTaskQueueService, workspaceService, mockWorkspaceUserCacheService);
  }

  @Test
  public void testCacheWorkspaceAcls_withWorkspacesNeedingUpdate() {
    List<WorkspaceDao.WorkspaceUserCacheView> workspacesNeedingUpdate =
        List.of(testWorkspace1, testWorkspace2);
    when(mockWorkspaceUserCacheService.findAllActiveWorkspacesNeedingCacheUpdate())
        .thenReturn(workspacesNeedingUpdate);

    ResponseEntity<Void> response = offlineWorkspaceController.cacheWorkspaceAcls();

    verify(mockWorkspaceUserCacheService).findAllActiveWorkspacesNeedingCacheUpdate();
    verify(mockWorkspaceUserCacheService).removeInactiveWorkspaces();
    verify(mockTaskQueueService).pushWorkspaceUserCacheTask(workspacesNeedingUpdate);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  public void testCacheWorkspaceAcls_noWorkspacesNeedingUpdate() {
    List<WorkspaceDao.WorkspaceUserCacheView> emptyList = List.of();
    when(mockWorkspaceUserCacheService.findAllActiveWorkspacesNeedingCacheUpdate())
        .thenReturn(emptyList);

    ResponseEntity<Void> response = offlineWorkspaceController.cacheWorkspaceAcls();

    verify(mockWorkspaceUserCacheService).findAllActiveWorkspacesNeedingCacheUpdate();
    verify(mockWorkspaceUserCacheService).removeInactiveWorkspaces();
    verify(mockTaskQueueService).pushWorkspaceUserCacheTask(emptyList);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }
}
