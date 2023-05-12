package org.pmiops.workbench.disk;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.pmiops.workbench.utils.TestMockFactory.createLeonardoListPersistentDiskResponse;

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
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.disks.DiskService;
import org.pmiops.workbench.leonardo.LeonardoApiClient;
import org.pmiops.workbench.leonardo.model.LeonardoDiskStatus;
import org.pmiops.workbench.leonardo.model.LeonardoListPersistentDiskResponse;
import org.pmiops.workbench.model.AppType;
import org.pmiops.workbench.model.Disk;
import org.pmiops.workbench.utils.mappers.LeonardoMapper;
import org.pmiops.workbench.workspaces.WorkspaceService;

// @Import({
//  DiskService.class,
//    WorkspaceService.class,
//    LeonardoApiClient.class,
//    LeonardoMapper.class
// })
@ExtendWith(MockitoExtension.class)
public class DiskServiceTest {
  private static final String WORKSPACE_NS = "workspace-ns";
  private static final String GOOGLE_PROJECT_ID = "aou-gcp-id";
  @Mock WorkspaceService mockWorkspaceService;
  @Mock LeonardoApiClient mockLeonardoApiClient;
  @Mock LeonardoMapper mockLeonardoMapper;
  @InjectMocks DiskService diskService;

  private static final String LOGGED_IN_USER_EMAIL = "bob@gmail.com";
  private static final long LOGGED_IN_USER_ID = 123L;
  private static final Instant NOW = Instant.now();

  private DbUser user;

  @BeforeEach
  public void setUp() {
    user = new DbUser().setUsername(LOGGED_IN_USER_EMAIL).setUserId(LOGGED_IN_USER_ID);
  }

  @Test
  public void test_findByWorkspaceNamespace() {
    DbWorkspace dbWorkspace = new DbWorkspace().setGoogleProject(GOOGLE_PROJECT_ID);
    LeonardoListPersistentDiskResponse firstLPDR =
        createLeonardoListPersistentDiskResponse(
            user.generatePDName(),
            LeonardoDiskStatus.READY,
            NOW.minusMillis(200).toString(),
            GOOGLE_PROJECT_ID,
            user,
            AppType.CROMWELL);
    LeonardoListPersistentDiskResponse secondLPDR =
        createLeonardoListPersistentDiskResponse(
            user.generatePDName(),
            LeonardoDiskStatus.READY,
            NOW.minusMillis(20000).toString(),
            GOOGLE_PROJECT_ID,
            user,
            AppType.RSTUDIO);
    LeonardoListPersistentDiskResponse thirdLPDR =
        createLeonardoListPersistentDiskResponse(
            user.generatePDName(),
            LeonardoDiskStatus.READY,
            NOW.minusMillis(2000000).toString(),
            GOOGLE_PROJECT_ID,
            user,
            AppType.CROMWELL);
    List<LeonardoListPersistentDiskResponse> responseList =
        new ArrayList<>(Arrays.asList(firstLPDR, secondLPDR, thirdLPDR));
    Disk firstDisk = new Disk();
    firstDisk.setName(firstLPDR.getName());

    Disk secondDisk = new Disk();
    secondDisk.setName(secondLPDR.getName());

    Disk thirdDisk = new Disk();
    thirdDisk.setName(thirdLPDR.getName());

    when(mockWorkspaceService.lookupWorkspaceByNamespace(anyString())).thenReturn(dbWorkspace);
    when(mockLeonardoApiClient.listDisksByProjectAsService(anyString())).thenReturn(responseList);
    when(mockLeonardoMapper.toApiListDisksResponse(firstLPDR)).thenReturn(firstDisk);
    when(mockLeonardoMapper.toApiListDisksResponse(secondLPDR)).thenReturn(secondDisk);
    when(mockLeonardoMapper.toApiListDisksResponse(thirdLPDR)).thenReturn(thirdDisk);
    assertThat(diskService.findByWorkspaceNamespace(WORKSPACE_NS))
        .containsExactly(firstDisk, secondDisk, thirdDisk);
  }

}
