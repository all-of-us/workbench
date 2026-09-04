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
import org.pmiops.workbench.workspaces.migration.WorkspaceMigrationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
public class OfflineWorkspaceControllerTest {
  private static final String NAMESPACE = "test-ns";
  private static final String TERRA_NAME = "test-ws";

  @Mock private WorkspaceService workspaceService;
  @Mock private WorkspaceDao workspaceDao;
  @Mock private WorkspaceMigrationService workspaceMigrationService;
  @Mock private WorkspaceUserCacheService mockWorkspaceUserCacheService;
  @Mock private TaskQueueService mockTaskQueueService;

  private OfflineWorkspaceController offlineWorkspaceController;

  @Mock private WorkspaceDao.WorkspaceUserCacheView testWorkspace1;
  @Mock private WorkspaceDao.WorkspaceUserCacheView testWorkspace2;
  @Mock private WorkspaceDao.WorkspaceArchiveView workspaceArchiveView;
  @Mock private WorkspaceDao.WorkspaceDeletionView workspaceDeletionView;

  @BeforeEach
  public void setUp() {
    offlineWorkspaceController =
        new OfflineWorkspaceController(
            mockTaskQueueService,
            workspaceService,
            workspaceDao,
            mockWorkspaceUserCacheService,
            workspaceMigrationService);
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

  @Test
  public void testDeleteNextLegacyWorkspace() {
    when(workspaceDeletionView.getWorkspaceNamespace()).thenReturn(NAMESPACE);
    when(workspaceDeletionView.getFirecloudName()).thenReturn(TERRA_NAME);
    when(workspaceDao.findNextWorkspaceToDelete()).thenReturn(workspaceDeletionView);
    ResponseEntity<Void> response = offlineWorkspaceController.deleteNextLegacyWorkspace();

    verify(mockTaskQueueService).pushDeleteLegacyWorkspaceTask(NAMESPACE, TERRA_NAME);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
  }

  @Test
  public void testArchiveNextLegacyWorkspace() {
    when(workspaceArchiveView.getWorkspaceNamespace()).thenReturn("ns");
    when(workspaceArchiveView.getFirecloudName()).thenReturn("terra");
    when(workspaceMigrationService.getNextWorkspaceToArchive()).thenReturn(workspaceArchiveView);

    ResponseEntity<Void> response = offlineWorkspaceController.archiveNextLegacyWorkspace();

    verify(workspaceMigrationService).startWorkspaceArchive("ns", "terra");
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
  }
}
