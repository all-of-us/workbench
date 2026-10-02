package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.pmiops.workbench.model.WorkspaceUserCacheQueueWorkspace;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceAccessEntry;
import org.pmiops.workbench.workspaces.WorkspaceAuthService;
import org.pmiops.workbench.workspaces.WorkspaceUserCacheService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
public class CloudTaskWorkspacesControllerTest {
  @Mock private WorkspaceAuthService mockWorkspaceAuthService;
  @Mock private WorkspaceUserCacheService mockWorkspaceUserCacheService;

  @InjectMocks private CloudTaskWorkspacesController controller;

  @Captor
  private ArgumentCaptor<Map<Long, Map<String, RawlsWorkspaceAccessEntry>>> cacheUpdateCaptor;

  @Test
  public void testProcessWorkspaceUserCacheQueueTask_success() {
    WorkspaceUserCacheQueueWorkspace workspace1 =
        new WorkspaceUserCacheQueueWorkspace()
            .workspaceId(1L)
            .workspaceNamespace("test-ws-1")
            .workspaceFirecloudName("test-ws-1-fc");
    WorkspaceUserCacheQueueWorkspace workspace2 =
        new WorkspaceUserCacheQueueWorkspace()
            .workspaceId(2L)
            .workspaceNamespace("test-ws-2")
            .workspaceFirecloudName("test-ws-2-fc");

    Map<String, RawlsWorkspaceAccessEntry> acl1 =
        Map.of(
            "user1@example.com", new RawlsWorkspaceAccessEntry().accessLevel("OWNER"),
            "user2@example.com", new RawlsWorkspaceAccessEntry().accessLevel("READER"));

    Map<String, RawlsWorkspaceAccessEntry> acl2 =
        Map.of("user3@example.com", new RawlsWorkspaceAccessEntry().accessLevel("WRITER"));

    ResponseEntity<Void> response =
        controller.processWorkspaceUserCacheQueueTask(List.of(workspace1, workspace2));

    verify(mockWorkspaceUserCacheService).updateWorkspaceUserCache(cacheUpdateCaptor.capture());

    Map<Long, Map<String, RawlsWorkspaceAccessEntry>> capturedUpdate = cacheUpdateCaptor.getValue();
    assertThat(capturedUpdate).hasSize(2);
    assertThat(capturedUpdate.get(workspace1.getWorkspaceId())).isEqualTo(acl1);
    assertThat(capturedUpdate.get(workspace2.getWorkspaceId())).isEqualTo(acl2);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  public void testProcessWorkspaceUserCacheQueueTask_emptyList() {
    List<WorkspaceUserCacheQueueWorkspace> emptyList = List.of();

    ResponseEntity<Void> response = controller.processWorkspaceUserCacheQueueTask(emptyList);

    verify(mockWorkspaceUserCacheService).updateWorkspaceUserCache(cacheUpdateCaptor.capture());
    Map<Long, Map<String, RawlsWorkspaceAccessEntry>> capturedUpdate = cacheUpdateCaptor.getValue();
    assertThat(capturedUpdate).isEmpty();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }
}
