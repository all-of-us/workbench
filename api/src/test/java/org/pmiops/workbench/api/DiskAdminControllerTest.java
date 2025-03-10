package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.pmiops.workbench.utils.TestMockFactory.createAppDisk;
import static org.pmiops.workbench.utils.TestMockFactory.createRuntimeDisk;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.disks.DiskService;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.model.AppType;
import org.pmiops.workbench.model.Disk;
import org.pmiops.workbench.model.DiskStatus;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.ListDisksResponse;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
public class DiskAdminControllerTest {
  private static final String WORKSPACE_NS = "workspace-ns";
  private static final String GOOGLE_PROJECT = "my-project";
  private static final String LOGGED_IN_USER_EMAIL = "bob@gmail.com";
  private static final Instant NOW = Instant.now();

  private static DbUser user = new DbUser();

  @Mock DiskService mockDiskService;

  @InjectMocks DiskAdminController diskAdminController;

  @BeforeEach
  public void setUp() {
    user = new DbUser();
    user.setUsername(LOGGED_IN_USER_EMAIL);
    user.setUserId(123L);
  }

  @Test
  public void listDisksInWorkspace() {
    Disk rStudioDisk =
        createAppDisk(
            user.generatePDNameForUserApps(AppType.RSTUDIO),
            DiskStatus.DELETING,
            NOW.minusMillis(100).toString(),
            GOOGLE_PROJECT,
            user,
            AppType.RSTUDIO);

    Disk cromwellDisk =
        createAppDisk(
            user.generatePDNameForUserApps(AppType.CROMWELL),
            DiskStatus.READY,
            NOW.toString(),
            GOOGLE_PROJECT,
            user,
            AppType.CROMWELL);

    Disk jupyterDisk =
        createRuntimeDisk(
            user.generatePDName(), DiskStatus.READY, NOW.toString(), GOOGLE_PROJECT, user);

    List<Disk> serviceResponse =
        new ArrayList<>(Arrays.asList(rStudioDisk, cromwellDisk, jupyterDisk));

    when(mockDiskService.getAllDisksInWorkspaceNamespace(WORKSPACE_NS)).thenReturn(serviceResponse);

    ResponseEntity<ListDisksResponse> response =
        diskAdminController.listDisksInWorkspace(WORKSPACE_NS);
    assertThat(response.getBody()).containsExactly(rStudioDisk, cromwellDisk, jupyterDisk);
    assertThat(response.getStatusCodeValue()).isEqualTo(200);
  }

  @Test
  public void listDisksInWorkspace_workspaceNotFound() {
    when(mockDiskService.getAllDisksInWorkspaceNamespace(WORKSPACE_NS))
        .thenThrow(new NotFoundException("Workspace not found: " + WORKSPACE_NS));
    assertThrows(
        NotFoundException.class, () -> diskAdminController.listDisksInWorkspace(WORKSPACE_NS));
  }

  @Test
  public void deleteDisk() {
    Disk diskToDelete =
        createAppDisk(
            user.generatePDNameForUserApps(AppType.CROMWELL),
            DiskStatus.READY,
            NOW.toString(),
            GOOGLE_PROJECT,
            user,
            AppType.CROMWELL);
    ResponseEntity<EmptyResponse> response =
        diskAdminController.adminDeleteDisk(WORKSPACE_NS, diskToDelete.getName());
    assertThat(response.getStatusCodeValue()).isEqualTo(200);
  }

  @Test
  public void deleteDisk_workspaceNotFound() {
    doThrow(new NotFoundException("Workspace not found: " + WORKSPACE_NS))
        .when(mockDiskService)
        .deleteDiskAsService(WORKSPACE_NS, "disk name");
    assertThrows(
        NotFoundException.class,
        () -> diskAdminController.adminDeleteDisk(WORKSPACE_NS, "disk name"));
  }
}
