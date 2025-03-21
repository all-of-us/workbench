package org.pmiops.workbench.disk;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.pmiops.workbench.utils.TestMockFactory.createLeonardoRuntimePDResponse;
import static org.pmiops.workbench.utils.TestMockFactory.createListPersistentDiskResponse;

import java.time.Instant;
import java.util.List;
import org.broadinstitute.dsde.workbench.client.leonardo.model.DiskStatus;
import org.broadinstitute.dsde.workbench.client.leonardo.model.ListPersistentDiskResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.disks.DiskService;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.exceptions.WorkbenchException;
import org.pmiops.workbench.leonardo.LeonardoApiClient;
import org.pmiops.workbench.model.AppType;
import org.pmiops.workbench.model.Disk;
import org.pmiops.workbench.utils.mappers.LeonardoMapper;
import org.pmiops.workbench.workspaces.WorkspaceService;

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
  public void test_getAllDisksInWorkspaceNamespace() {
    DbWorkspace dbWorkspace = new DbWorkspace().setGoogleProject(GOOGLE_PROJECT_ID);
    ListPersistentDiskResponse firstLPDR =
        createListPersistentDiskResponse(
            user.generatePDName(),
            DiskStatus.READY,
            NOW.minusMillis(200).toString(),
            GOOGLE_PROJECT_ID,
            user,
            AppType.CROMWELL);
    ListPersistentDiskResponse secondLPDR =
        createListPersistentDiskResponse(
            user.generatePDName(),
            DiskStatus.READY,
            NOW.minusMillis(20000).toString(),
            GOOGLE_PROJECT_ID,
            user,
            AppType.RSTUDIO);
    ListPersistentDiskResponse thirdLPDR =
        createLeonardoRuntimePDResponse(
            user.generatePDName(),
            DiskStatus.READY,
            NOW.minusMillis(2000000).toString(),
            GOOGLE_PROJECT_ID,
            user);
    List<ListPersistentDiskResponse> responseList = List.of(firstLPDR, secondLPDR, thirdLPDR);
    Disk firstDisk = new Disk();
    firstDisk.setName(firstLPDR.getName());

    Disk secondDisk = new Disk();
    secondDisk.setName(secondLPDR.getName());

    Disk thirdDisk = new Disk();
    thirdDisk.setName(thirdLPDR.getName());

    when(mockWorkspaceService.lookupWorkspaceByNamespace(anyString())).thenReturn(dbWorkspace);
    when(mockLeonardoApiClient.listDisksByProjectAsService(anyString())).thenReturn(responseList);
    when(mockLeonardoMapper.toApiDisk(firstLPDR)).thenReturn(firstDisk);
    when(mockLeonardoMapper.toApiDisk(secondLPDR)).thenReturn(secondDisk);
    when(mockLeonardoMapper.toApiDisk(thirdLPDR)).thenReturn(thirdDisk);
    assertThat(diskService.getAllDisksInWorkspaceNamespace(WORKSPACE_NS))
        .containsExactly(firstDisk, secondDisk, thirdDisk);
  }

  @Test
  public void deleteDisk() {
    String diskName = user.generatePDName();
    DbWorkspace workspace = new DbWorkspace().setGoogleProject(GOOGLE_PROJECT_ID);
    when(mockWorkspaceService.lookupWorkspaceByNamespace(WORKSPACE_NS)).thenReturn(workspace);
    diskService.deleteDisk(WORKSPACE_NS, diskName);
    verify(mockLeonardoApiClient).deletePersistentDisk(GOOGLE_PROJECT_ID, diskName);
  }

  @Test
  public void deleteDisk_workspaceNotFound() {
    String diskName = user.generatePDName();

    doThrow(new NotFoundException())
        .when(mockWorkspaceService)
        .lookupWorkspaceByNamespace(WORKSPACE_NS);

    assertThrows(NotFoundException.class, () -> diskService.deleteDisk(WORKSPACE_NS, diskName));
  }

  @Test
  public void deleteDiskAsService() {
    String diskName = user.generatePDName();
    DbWorkspace workspace = new DbWorkspace().setGoogleProject(GOOGLE_PROJECT_ID);
    when(mockWorkspaceService.lookupWorkspaceByNamespace(WORKSPACE_NS)).thenReturn(workspace);
    diskService.deleteDiskAsService(WORKSPACE_NS, diskName);
    verify(mockLeonardoApiClient).deletePersistentDiskAsService(GOOGLE_PROJECT_ID, diskName);
  }

  @Test
  public void deleteDiskAsService_leonardoWorkbenchException() {
    String diskName = user.generatePDName();
    DbWorkspace workspace = new DbWorkspace().setGoogleProject(GOOGLE_PROJECT_ID);
    when(mockWorkspaceService.lookupWorkspaceByNamespace(WORKSPACE_NS)).thenReturn(workspace);
    doThrow(new WorkbenchException())
        .when(mockLeonardoApiClient)
        .deletePersistentDiskAsService(GOOGLE_PROJECT_ID, diskName);
    assertThrows(
        WorkbenchException.class, () -> diskService.deleteDiskAsService(WORKSPACE_NS, diskName));
  }
}
