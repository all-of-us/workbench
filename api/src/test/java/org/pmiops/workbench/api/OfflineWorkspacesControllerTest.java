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
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.WorkspaceActiveStatus;
import org.pmiops.workbench.workspaces.WorkspaceUserCacheService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
public class OfflineWorkspacesControllerTest {

  @Mock private WorkspaceUserCacheService mockWorkspaceUserCacheService;
  @Mock private TaskQueueService mockTaskQueueService;

  private OfflineWorkspacesController offlineWorkspacesController;

  private DbWorkspace testWorkspace1;
  private DbWorkspace testWorkspace2;

  @BeforeEach
  public void setUp() {
    offlineWorkspacesController =
        new OfflineWorkspacesController(mockWorkspaceUserCacheService, mockTaskQueueService);

    testWorkspace1 =
        new DbWorkspace()
            .setWorkspaceId(1L)
            .setName("Test Workspace 1")
            .setWorkspaceNamespace("test-ws-1")
            .setFirecloudName("test-ws-1-fc")
            .setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.ACTIVE);

    testWorkspace2 =
        new DbWorkspace()
            .setWorkspaceId(2L)
            .setName("Test Workspace 2")
            .setWorkspaceNamespace("test-ws-2")
            .setFirecloudName("test-ws-2-fc")
            .setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.ACTIVE);
  }

  @Test
  public void testCacheWorkspaceAcls_withWorkspacesNeedingUpdate() {
    List<DbWorkspace> workspacesNeedingUpdate = List.of(testWorkspace1, testWorkspace2);
    when(mockWorkspaceUserCacheService.findAllActiveWorkspacesNeedingCacheUpdate())
        .thenReturn(workspacesNeedingUpdate);

    ResponseEntity<Void> response = offlineWorkspacesController.cacheWorkspaceAcls();

    verify(mockWorkspaceUserCacheService).findAllActiveWorkspacesNeedingCacheUpdate();
    verify(mockWorkspaceUserCacheService).removeInactiveWorkspaces();
    verify(mockTaskQueueService).pushWorkspaceUserCacheTask(workspacesNeedingUpdate);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  public void testCacheWorkspaceAcls_noWorkspacesNeedingUpdate() {
    List<DbWorkspace> emptyList = List.of();
    when(mockWorkspaceUserCacheService.findAllActiveWorkspacesNeedingCacheUpdate())
        .thenReturn(emptyList);

    ResponseEntity<Void> response = offlineWorkspacesController.cacheWorkspaceAcls();

    verify(mockWorkspaceUserCacheService).findAllActiveWorkspacesNeedingCacheUpdate();
    verify(mockWorkspaceUserCacheService).removeInactiveWorkspaces();
    verify(mockTaskQueueService).pushWorkspaceUserCacheTask(emptyList);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }
}
