package org.pmiops.workbench.environments;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.broadinstitute.dsde.workbench.client.leonardo.model.AuditInfo;
import org.broadinstitute.dsde.workbench.client.leonardo.model.DiskStatus;
import org.broadinstitute.dsde.workbench.client.leonardo.model.ListPersistentDiskResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.WorkbenchException;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoAuditInfo;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoListRuntimeResponse;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoRuntimeStatus;
import org.pmiops.workbench.leonardo.LeonardoApiClient;
import org.pmiops.workbench.model.AppStatus;
import org.pmiops.workbench.model.UserAppEnvironment;
import org.pmiops.workbench.model.UserRole;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.pmiops.workbench.workspaces.WorkspaceUserCacheService;

@ExtendWith(MockitoExtension.class)
public class EnvironmentsAdminServiceTest {

  @Mock private WorkspaceService mockWorkspaceService;
  @Mock private LeonardoApiClient mockLeonardoApiClient;
  @Mock private WorkspaceUserCacheService mockWorkspaceUserCacheService;

  private EnvironmentsAdminServiceImpl environmentsAdminService;

  private static final String USER_EMAIL_1 = "user1@example.com";
  private static final String USER_EMAIL_2 = "user2@example.com";
  private static final String CREATOR_EMAIL = "creator@example.com";

  @BeforeEach
  void setUp() {
    environmentsAdminService =
        new EnvironmentsAdminServiceImpl(
            mockWorkspaceService, mockLeonardoApiClient, mockWorkspaceUserCacheService);
  }

  @Test
  void testDeleteUnsharedWorkspaceEnvironmentsBatch_noResources() {
    DbWorkspace workspace = createMockWorkspace();
    when(mockWorkspaceService.lookupWorkspacesByNamespace(
            List.of(workspace.getWorkspaceNamespace())))
        .thenReturn(List.of(workspace));

    when(mockWorkspaceUserCacheService.getWorkspaceUsers(workspace.getWorkspaceId()))
        .thenReturn(Set.of(USER_EMAIL_1, USER_EMAIL_2));

    when(mockLeonardoApiClient.listRuntimesByProjectAsService(workspace.getGoogleProject()))
        .thenReturn(Collections.emptyList());
    when(mockLeonardoApiClient.listAppsInProjectAsService(workspace.getGoogleProject()))
        .thenReturn(Collections.emptyList());
    when(mockLeonardoApiClient.listDisksByProjectAsService(workspace.getGoogleProject()))
        .thenReturn(Collections.emptyList());

    long failures =
        environmentsAdminService.deleteUnsharedWorkspaceEnvironmentsBatch(
            List.of(workspace.getWorkspaceNamespace()));

    assertEquals(0, failures);
    verify(mockWorkspaceService)
        .lookupWorkspacesByNamespace(List.of(workspace.getWorkspaceNamespace()));
    verify(mockWorkspaceService, never()).getFirecloudUserRoles(any(), any());
    verifyNoDeleteCalls();
  }

  @Test
  void testDeleteUnsharedWorkspaceEnvironmentsBatch_withFailures() {
    DbWorkspace workspace = createMockWorkspace();
    when(mockWorkspaceService.lookupWorkspacesByNamespace(
            List.of(workspace.getWorkspaceNamespace())))
        .thenReturn(List.of(workspace));

    when(mockWorkspaceUserCacheService.getWorkspaceUsers(workspace.getWorkspaceId()))
        .thenReturn(Set.of(USER_EMAIL_1));

    when(mockLeonardoApiClient.listRuntimesByProjectAsService(workspace.getGoogleProject()))
        .thenReturn(List.of(createMockRuntime("runtime-1", CREATOR_EMAIL)));
    when(mockLeonardoApiClient.listAppsInProjectAsService(workspace.getGoogleProject()))
        .thenReturn(Collections.emptyList());

    when(mockWorkspaceService.getFirecloudUserRoles(
            workspace.getWorkspaceNamespace(), workspace.getFirecloudName()))
        .thenThrow(new WorkbenchException("Failed to get user roles"));

    Long failures =
        environmentsAdminService.deleteUnsharedWorkspaceEnvironmentsBatch(
            List.of(workspace.getWorkspaceNamespace()));

    assertEquals(1, failures);
    verifyNoDeleteCalls();
  }

  @Test
  void testDeleteUnsharedWorkspaceEnvironmentsBatch_noWorkspacesFound() {
    when(mockWorkspaceService.lookupWorkspacesByNamespace(List.of("nonexistent-namespace")))
        .thenReturn(Collections.emptyList());

    long failures =
        environmentsAdminService.deleteUnsharedWorkspaceEnvironmentsBatch(
            List.of("nonexistent-namespace"));

    assertEquals(0, failures);
    verify(mockWorkspaceService).lookupWorkspacesByNamespace(List.of("nonexistent-namespace"));
    verifyNoDeleteCalls();
  }

  @Test
  void testDeleteUnsharedWorkspaceEnvironmentsBatch_multipleWorkspaces() {
    DbWorkspace workspace1 = createMockWorkspace();
    DbWorkspace workspace2 = createMockWorkspace();
    workspace2.setWorkspaceId(456L);
    workspace2.setWorkspaceNamespace("another-namespace");
    workspace2.setFirecloudName("another-name");
    workspace2.setGoogleProject("another-project");

    when(mockWorkspaceService.lookupWorkspacesByNamespace(
            List.of(workspace1.getWorkspaceNamespace(), workspace2.getWorkspaceNamespace())))
        .thenReturn(List.of(workspace1, workspace2));

    when(mockWorkspaceUserCacheService.getWorkspaceUsers(workspace1.getWorkspaceId()))
        .thenReturn(Set.of(USER_EMAIL_1));
    when(mockWorkspaceUserCacheService.getWorkspaceUsers(workspace2.getWorkspaceId()))
        .thenReturn(Set.of(USER_EMAIL_2));

    var runtime1 = createMockRuntime("runtime-1", CREATOR_EMAIL);
    when(mockLeonardoApiClient.listRuntimesByProjectAsService(workspace1.getGoogleProject()))
        .thenReturn(List.of(createMockRuntime("runtime-1", CREATOR_EMAIL)));
    when(mockLeonardoApiClient.listAppsInProjectAsService(workspace1.getGoogleProject()))
        .thenReturn(Collections.emptyList());

    var runtime2 = createMockRuntime("runtime-2", CREATOR_EMAIL);
    when(mockLeonardoApiClient.listRuntimesByProjectAsService(workspace2.getGoogleProject()))
        .thenReturn(List.of(createMockRuntime("runtime-2", CREATOR_EMAIL)));
    when(mockLeonardoApiClient.listAppsInProjectAsService(workspace2.getGoogleProject()))
        .thenReturn(Collections.emptyList());

    when(mockWorkspaceService.getFirecloudUserRoles(
            workspace1.getWorkspaceNamespace(), workspace1.getFirecloudName()))
        .thenReturn(List.of(new UserRole().email(USER_EMAIL_1)));

    when(mockWorkspaceService.getFirecloudUserRoles(
            workspace2.getWorkspaceNamespace(), workspace2.getFirecloudName()))
        .thenReturn(List.of(new UserRole().email(USER_EMAIL_2)));

    long failures =
        environmentsAdminService.deleteUnsharedWorkspaceEnvironmentsBatch(
            List.of(workspace1.getWorkspaceNamespace(), workspace2.getWorkspaceNamespace()));

    assertEquals(0, failures);
    verify(mockLeonardoApiClient)
        .deleteRuntimeAsService(workspace1.getGoogleProject(), runtime1.getRuntimeName(), true);
    verify(mockLeonardoApiClient)
        .deleteRuntimeAsService(workspace2.getGoogleProject(), runtime2.getRuntimeName(), true);
  }

  @Test
  void testDeleteUnsharedWorkspaceEnvironmentsBatch_creatorsStillHaveAccess() {
    DbWorkspace workspace = createMockWorkspace();
    when(mockWorkspaceService.lookupWorkspacesByNamespace(
            List.of(workspace.getWorkspaceNamespace())))
        .thenReturn(List.of(workspace));

    when(mockWorkspaceUserCacheService.getWorkspaceUsers(workspace.getWorkspaceId()))
        .thenReturn(Set.of(USER_EMAIL_1, USER_EMAIL_2));

    when(mockLeonardoApiClient.listRuntimesByProjectAsService(workspace.getGoogleProject()))
        .thenReturn(List.of(createMockRuntime("runtime-1", USER_EMAIL_1)));
    when(mockLeonardoApiClient.listAppsInProjectAsService(workspace.getGoogleProject()))
        .thenReturn(List.of(createMockApp("app-1", USER_EMAIL_2)));
    when(mockLeonardoApiClient.listDisksByProjectAsService(workspace.getGoogleProject()))
        .thenReturn(List.of(createMockDisk("disk-1", USER_EMAIL_1)));

    long failures =
        environmentsAdminService.deleteUnsharedWorkspaceEnvironmentsBatch(
            List.of(workspace.getWorkspaceNamespace()));

    assertEquals(0, failures);
    verify(mockWorkspaceService, never()).getFirecloudUserRoles(any(), any());
    verifyNoDeleteCalls();
  }

  @Test
  void testDeleteUnsharedWorkspaceEnvironmentsBatch_handlesCacheMiss() {
    DbWorkspace workspace = createMockWorkspace();
    when(mockWorkspaceService.lookupWorkspacesByNamespace(
            List.of(workspace.getWorkspaceNamespace())))
        .thenReturn(List.of(workspace));

    // Cache returns no users, simulating a cache miss
    when(mockWorkspaceUserCacheService.getWorkspaceUsers(workspace.getWorkspaceId()))
        .thenReturn(Set.of());
    // User still has access in Terra
    when(mockWorkspaceService.getFirecloudUserRoles(
            workspace.getWorkspaceNamespace(), workspace.getFirecloudName()))
        .thenReturn(List.of(new UserRole().email(USER_EMAIL_1)));

    when(mockLeonardoApiClient.listRuntimesByProjectAsService(workspace.getGoogleProject()))
        .thenReturn(List.of(createMockRuntime("runtime-1", USER_EMAIL_1)));
    when(mockLeonardoApiClient.listAppsInProjectAsService(workspace.getGoogleProject()))
        .thenReturn(List.of(createMockApp("app-1", USER_EMAIL_1)));
    when(mockLeonardoApiClient.listDisksByProjectAsService(workspace.getGoogleProject()))
        .thenReturn(List.of(createMockDisk("disk-1", USER_EMAIL_1)));

    long failures =
        environmentsAdminService.deleteUnsharedWorkspaceEnvironmentsBatch(
            List.of(workspace.getWorkspaceNamespace()));

    assertEquals(0, failures);
    // User still has access, so no deletions should occur
    verifyNoDeleteCalls();
  }

  @Test
  void testDeleteUnsharedWorkspaceEnvironmentsBatch_deletesUnsharedRuntimes() {
    DbWorkspace workspace = createMockWorkspace();
    when(mockWorkspaceService.lookupWorkspacesByNamespace(
            List.of(workspace.getWorkspaceNamespace())))
        .thenReturn(List.of(workspace));

    when(mockWorkspaceUserCacheService.getWorkspaceUsers(workspace.getWorkspaceId()))
        .thenReturn(Set.of(USER_EMAIL_1));

    LeonardoListRuntimeResponse runtime = createMockRuntime("runtime-1", CREATOR_EMAIL);
    when(mockLeonardoApiClient.listRuntimesByProjectAsService(workspace.getGoogleProject()))
        .thenReturn(List.of(runtime));
    when(mockLeonardoApiClient.listAppsInProjectAsService(workspace.getGoogleProject()))
        .thenReturn(Collections.emptyList());
    when(mockLeonardoApiClient.listDisksByProjectAsService(workspace.getGoogleProject()))
        .thenReturn(Collections.emptyList());

    UserRole userRole = new UserRole().email(USER_EMAIL_1);
    when(mockWorkspaceService.getFirecloudUserRoles(
            workspace.getWorkspaceNamespace(), workspace.getFirecloudName()))
        .thenReturn(List.of(userRole));

    long failures =
        environmentsAdminService.deleteUnsharedWorkspaceEnvironmentsBatch(
            List.of(workspace.getWorkspaceNamespace()));

    assertEquals(0, failures);
    verify(mockWorkspaceService)
        .getFirecloudUserRoles(workspace.getWorkspaceNamespace(), workspace.getFirecloudName());
    verify(mockLeonardoApiClient)
        .deleteRuntimeAsService(workspace.getGoogleProject(), runtime.getRuntimeName(), true);
  }

  @Test
  void testDeleteUnsharedWorkspaceEnvironmentsBatch_deletesUnsharedApps() {
    DbWorkspace workspace = createMockWorkspace();
    when(mockWorkspaceService.lookupWorkspacesByNamespace(
            List.of(workspace.getWorkspaceNamespace())))
        .thenReturn(List.of(workspace));

    when(mockWorkspaceUserCacheService.getWorkspaceUsers(workspace.getWorkspaceId()))
        .thenReturn(Set.of(USER_EMAIL_1));

    UserAppEnvironment app = createMockApp("app-1", CREATOR_EMAIL);
    when(mockLeonardoApiClient.listRuntimesByProjectAsService(workspace.getGoogleProject()))
        .thenReturn(Collections.emptyList());
    when(mockLeonardoApiClient.listAppsInProjectAsService(workspace.getGoogleProject()))
        .thenReturn(List.of(app));
    when(mockLeonardoApiClient.listDisksByProjectAsService(workspace.getGoogleProject()))
        .thenReturn(Collections.emptyList());

    UserRole userRole = new UserRole().email(USER_EMAIL_1);
    when(mockWorkspaceService.getFirecloudUserRoles(
            workspace.getWorkspaceNamespace(), workspace.getFirecloudName()))
        .thenReturn(List.of(userRole));

    long failures =
        environmentsAdminService.deleteUnsharedWorkspaceEnvironmentsBatch(
            List.of(workspace.getWorkspaceNamespace()));

    assertEquals(0, failures);
    verify(mockWorkspaceService)
        .getFirecloudUserRoles(workspace.getWorkspaceNamespace(), workspace.getFirecloudName());
    verify(mockLeonardoApiClient).deleteAppAsService(app.getAppName(), workspace, true);
  }

  @Test
  void testDeleteUnsharedWorkspaceEnvironmentsBatch_deletesUnsharedDisks() {
    DbWorkspace workspace = createMockWorkspace();
    when(mockWorkspaceService.lookupWorkspacesByNamespace(
            List.of(workspace.getWorkspaceNamespace())))
        .thenReturn(List.of(workspace));

    when(mockWorkspaceUserCacheService.getWorkspaceUsers(workspace.getWorkspaceId()))
        .thenReturn(Set.of(USER_EMAIL_1));

    ListPersistentDiskResponse disk = createMockDisk("disk-1", CREATOR_EMAIL);
    when(mockLeonardoApiClient.listRuntimesByProjectAsService(workspace.getGoogleProject()))
        .thenReturn(Collections.emptyList());
    when(mockLeonardoApiClient.listAppsInProjectAsService(workspace.getGoogleProject()))
        .thenReturn(Collections.emptyList());
    when(mockLeonardoApiClient.listDisksByProjectAsService(workspace.getGoogleProject()))
        .thenReturn(List.of(disk));

    UserRole userRole = new UserRole().email(USER_EMAIL_1);
    when(mockWorkspaceService.getFirecloudUserRoles(
            workspace.getWorkspaceNamespace(), workspace.getFirecloudName()))
        .thenReturn(List.of(userRole));

    long failures =
        environmentsAdminService.deleteUnsharedWorkspaceEnvironmentsBatch(
            List.of(workspace.getWorkspaceNamespace()));

    assertEquals(0, failures);
    verify(mockWorkspaceService, times(1))
        .getFirecloudUserRoles(workspace.getWorkspaceNamespace(), workspace.getFirecloudName());
    verify(mockLeonardoApiClient)
        .deletePersistentDiskAsService(workspace.getGoogleProject(), disk.getName());
  }

  @Test
  void testDeleteUnsharedWorkspaceEnvironmentsBatch_reusesCurrentUsersForDisks() {
    DbWorkspace workspace = createMockWorkspace();
    when(mockWorkspaceService.lookupWorkspacesByNamespace(
            List.of(workspace.getWorkspaceNamespace())))
        .thenReturn(List.of(workspace));

    when(mockWorkspaceUserCacheService.getWorkspaceUsers(workspace.getWorkspaceId()))
        .thenReturn(Set.of(USER_EMAIL_1));

    LeonardoListRuntimeResponse runtime = createMockRuntime("runtime-1", CREATOR_EMAIL);
    ListPersistentDiskResponse disk = createMockDisk("disk-1", CREATOR_EMAIL);

    when(mockLeonardoApiClient.listRuntimesByProjectAsService(workspace.getGoogleProject()))
        .thenReturn(List.of(runtime));
    when(mockLeonardoApiClient.listAppsInProjectAsService(workspace.getGoogleProject()))
        .thenReturn(Collections.emptyList());
    when(mockLeonardoApiClient.listDisksByProjectAsService(workspace.getGoogleProject()))
        .thenReturn(List.of(disk));

    UserRole userRole = new UserRole().email(USER_EMAIL_1);
    when(mockWorkspaceService.getFirecloudUserRoles(
            workspace.getWorkspaceNamespace(), workspace.getFirecloudName()))
        .thenReturn(List.of(userRole));

    long failures =
        environmentsAdminService.deleteUnsharedWorkspaceEnvironmentsBatch(
            List.of(workspace.getWorkspaceNamespace()));

    assertEquals(0, failures);
    // Should only call getCurrentUsers once (for runtime/app phase, then reuse for disks)
    verify(mockWorkspaceService, times(1))
        .getFirecloudUserRoles(workspace.getWorkspaceNamespace(), workspace.getFirecloudName());
    verify(mockLeonardoApiClient)
        .deleteRuntimeAsService(workspace.getGoogleProject(), runtime.getRuntimeName(), true);
    verify(mockLeonardoApiClient)
        .deletePersistentDiskAsService(workspace.getGoogleProject(), disk.getName());
  }

  @Test
  void testDeleteUnsharedWorkspaceEnvironmentsBatch_handlesRuntimeDeletionFailure() {
    DbWorkspace workspace = createMockWorkspace();
    when(mockWorkspaceService.lookupWorkspacesByNamespace(
            List.of(workspace.getWorkspaceNamespace())))
        .thenReturn(List.of(workspace));

    when(mockWorkspaceUserCacheService.getWorkspaceUsers(workspace.getWorkspaceId()))
        .thenReturn(Set.of(USER_EMAIL_1));

    LeonardoListRuntimeResponse runtimeFailure =
        createMockRuntime("runtime-failure", CREATOR_EMAIL);
    LeonardoListRuntimeResponse runtimeSuccess =
        createMockRuntime("runtime-success", CREATOR_EMAIL);
    when(mockLeonardoApiClient.listRuntimesByProjectAsService(workspace.getGoogleProject()))
        .thenReturn(List.of(runtimeFailure, runtimeSuccess));
    when(mockLeonardoApiClient.listAppsInProjectAsService(workspace.getGoogleProject()))
        .thenReturn(Collections.emptyList());
    when(mockLeonardoApiClient.listDisksByProjectAsService(workspace.getGoogleProject()))
        .thenReturn(Collections.emptyList());

    UserRole userRole = new UserRole().email(USER_EMAIL_1);
    when(mockWorkspaceService.getFirecloudUserRoles(
            workspace.getWorkspaceNamespace(), workspace.getFirecloudName()))
        .thenReturn(List.of(userRole));

    doThrow(new WorkbenchException("Deletion failed"))
        .when(mockLeonardoApiClient)
        .deleteRuntimeAsService(any(), eq(runtimeFailure.getRuntimeName()), anyBoolean());

    long failures =
        environmentsAdminService.deleteUnsharedWorkspaceEnvironmentsBatch(
            List.of(workspace.getWorkspaceNamespace()));

    assertEquals(0, failures);
    verify(mockLeonardoApiClient)
        .deleteRuntimeAsService(
            workspace.getGoogleProject(), runtimeFailure.getRuntimeName(), true);
    verify(mockLeonardoApiClient)
        .deleteRuntimeAsService(
            workspace.getGoogleProject(), runtimeSuccess.getRuntimeName(), true);
  }

  @Test
  void testDeleteUnsharedWorkspaceEnvironmentsBatch_handlesAppDeletionFailure() {
    DbWorkspace workspace = createMockWorkspace();
    when(mockWorkspaceService.lookupWorkspacesByNamespace(
            List.of(workspace.getWorkspaceNamespace())))
        .thenReturn(List.of(workspace));

    when(mockWorkspaceUserCacheService.getWorkspaceUsers(workspace.getWorkspaceId()))
        .thenReturn(Set.of(USER_EMAIL_1));

    UserAppEnvironment appFailure = createMockApp("app-failure", CREATOR_EMAIL);
    UserAppEnvironment appSuccess = createMockApp("app-success", CREATOR_EMAIL);
    when(mockLeonardoApiClient.listRuntimesByProjectAsService(workspace.getGoogleProject()))
        .thenReturn(Collections.emptyList());
    when(mockLeonardoApiClient.listAppsInProjectAsService(workspace.getGoogleProject()))
        .thenReturn(List.of(appFailure, appSuccess));
    when(mockLeonardoApiClient.listDisksByProjectAsService(workspace.getGoogleProject()))
        .thenReturn(Collections.emptyList());

    UserRole userRole = new UserRole().email(USER_EMAIL_1);
    when(mockWorkspaceService.getFirecloudUserRoles(
            workspace.getWorkspaceNamespace(), workspace.getFirecloudName()))
        .thenReturn(List.of(userRole));

    doThrow(new WorkbenchException("Deletion failed"))
        .when(mockLeonardoApiClient)
        .deleteAppAsService(appFailure.getAppName(), workspace, true);

    long failures =
        environmentsAdminService.deleteUnsharedWorkspaceEnvironmentsBatch(
            List.of(workspace.getWorkspaceNamespace()));

    assertEquals(0, failures);
    verify(mockLeonardoApiClient).deleteAppAsService(appFailure.getAppName(), workspace, true);
    verify(mockLeonardoApiClient).deleteAppAsService(appSuccess.getAppName(), workspace, true);
  }

  @Test
  void testDeleteUnsharedWorkspaceEnvironmentsBatch_handlesDiskDeletionFailure() {
    DbWorkspace workspace = createMockWorkspace();
    when(mockWorkspaceService.lookupWorkspacesByNamespace(
            List.of(workspace.getWorkspaceNamespace())))
        .thenReturn(List.of(workspace));

    when(mockWorkspaceUserCacheService.getWorkspaceUsers(workspace.getWorkspaceId()))
        .thenReturn(Set.of(USER_EMAIL_1));

    ListPersistentDiskResponse diskFailure = createMockDisk("disk-failure", CREATOR_EMAIL);
    ListPersistentDiskResponse diskSuccess = createMockDisk("disk-success", CREATOR_EMAIL);
    when(mockLeonardoApiClient.listRuntimesByProjectAsService(workspace.getGoogleProject()))
        .thenReturn(Collections.emptyList());
    when(mockLeonardoApiClient.listAppsInProjectAsService(workspace.getGoogleProject()))
        .thenReturn(Collections.emptyList());
    when(mockLeonardoApiClient.listDisksByProjectAsService(workspace.getGoogleProject()))
        .thenReturn(List.of(diskFailure, diskSuccess));

    UserRole userRole = new UserRole().email(USER_EMAIL_1);
    when(mockWorkspaceService.getFirecloudUserRoles(
            workspace.getWorkspaceNamespace(), workspace.getFirecloudName()))
        .thenReturn(List.of(userRole));

    doThrow(new WorkbenchException("Deletion failed"))
        .when(mockLeonardoApiClient)
        .deletePersistentDiskAsService(workspace.getGoogleProject(), diskFailure.getName());

    long failures =
        environmentsAdminService.deleteUnsharedWorkspaceEnvironmentsBatch(
            List.of(workspace.getWorkspaceNamespace()));

    assertEquals(0, failures);
    verify(mockLeonardoApiClient)
        .deletePersistentDiskAsService(workspace.getGoogleProject(), diskFailure.getName());
    verify(mockLeonardoApiClient)
        .deletePersistentDiskAsService(workspace.getGoogleProject(), diskSuccess.getName());
  }

  @Test
  void testDeleteUnsharedWorkspaceEnvironmentsBatch_usesCurrentWorkspaceUsersToDelete() {
    DbWorkspace workspace = createMockWorkspace();
    when(mockWorkspaceService.lookupWorkspacesByNamespace(
            List.of(workspace.getWorkspaceNamespace())))
        .thenReturn(List.of(workspace));

    when(mockWorkspaceUserCacheService.getWorkspaceUsers(workspace.getWorkspaceId()))
        .thenReturn(Set.of(USER_EMAIL_1));
    // Simulate a stale cache and CREATOR_EMAIL does actually have access
    when(mockWorkspaceService.getFirecloudUserRoles(
            workspace.getWorkspaceNamespace(), workspace.getFirecloudName()))
        .thenReturn(
            List.of(new UserRole().email(USER_EMAIL_1), new UserRole().email(CREATOR_EMAIL)));

    var runtime = createMockRuntime("runtime-1", CREATOR_EMAIL);
    when(mockLeonardoApiClient.listRuntimesByProjectAsService(workspace.getGoogleProject()))
        .thenReturn(List.of(runtime));
    when(mockLeonardoApiClient.listAppsInProjectAsService(workspace.getGoogleProject()))
        .thenReturn(Collections.emptyList());
    when(mockLeonardoApiClient.listDisksByProjectAsService(workspace.getGoogleProject()))
        .thenReturn(Collections.emptyList());

    environmentsAdminService.deleteUnsharedWorkspaceEnvironmentsBatch(
        List.of(workspace.getWorkspaceNamespace()));

    verify(mockWorkspaceService)
        .getFirecloudUserRoles(workspace.getWorkspaceNamespace(), workspace.getFirecloudName());
    verifyNoDeleteCalls();
  }

  private void verifyNoDeleteCalls() {
    verify(mockLeonardoApiClient, never()).deleteRuntimeAsService(any(), any(), anyBoolean());
    verify(mockLeonardoApiClient, never()).deleteAppAsService(any(), any(), anyBoolean());
    verify(mockLeonardoApiClient, never()).deletePersistentDiskAsService(any(), any());
  }

  private DbWorkspace createMockWorkspace() {
    DbWorkspace workspace = new DbWorkspace();
    workspace.setWorkspaceId(123L);
    workspace.setWorkspaceNamespace("test-namespace");
    workspace.setFirecloudName("test-workspace");
    workspace.setGoogleProject("test-project");
    return workspace;
  }

  private LeonardoListRuntimeResponse createMockRuntime(String name, String creator) {
    return new LeonardoListRuntimeResponse()
        .runtimeName(name)
        .auditInfo(new LeonardoAuditInfo().creator(creator))
        .status(LeonardoRuntimeStatus.RUNNING);
  }

  private UserAppEnvironment createMockApp(String name, String creator) {
    return new UserAppEnvironment().appName(name).creator(creator).status(AppStatus.RUNNING);
  }

  private ListPersistentDiskResponse createMockDisk(String name, String creator) {
    return new ListPersistentDiskResponse()
        .name(name)
        .auditInfo(new AuditInfo().creator(creator))
        .status(DiskStatus.READY);
  }
}
