package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
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
import org.pmiops.workbench.model.AppType;
import org.pmiops.workbench.model.Disk;
import org.pmiops.workbench.model.DiskStatus;

@ExtendWith(MockitoExtension.class)
public class DiskAdminControllerTest {
  private static final String WORKSPACE_NS = "workspace-ns";
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
            user,
            AppType.RSTUDIO);

    Disk cromwellDisk =
        createAppDisk(
            user.generatePDNameForUserApps(AppType.CROMWELL),
            DiskStatus.READY,
            NOW.toString(),
            user,
            AppType.CROMWELL);

    Disk jupyerDisk =
        createRuntimeDisk(
            user.generatePDNameForUserApps(AppType.CROMWELL),
            DiskStatus.READY,
            NOW.toString(),
            user);

    List<Disk> serviceResponse =
        new ArrayList<>(Arrays.asList(rStudioDisk, cromwellDisk, jupyerDisk));

    when(mockDiskService.findByWorkspaceNamespace(anyString())).thenReturn(serviceResponse);
    assertThat(diskAdminController.listDisksInWorkspace(WORKSPACE_NS).getBody())
        .containsExactly(rStudioDisk, cromwellDisk, jupyerDisk);
  }
}
