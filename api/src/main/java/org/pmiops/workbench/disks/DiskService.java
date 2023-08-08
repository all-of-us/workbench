package org.pmiops.workbench.disks;

import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.pmiops.workbench.leonardo.LeonardoApiClient;
import org.pmiops.workbench.leonardo.PersistentDiskUtils;
import org.pmiops.workbench.leonardo.model.ListPersistentDiskResponse;
import org.pmiops.workbench.model.Disk;
import org.pmiops.workbench.model.DiskStatus;
import org.pmiops.workbench.utils.mappers.LeonardoMapper;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DiskService {
  private static final Logger log = Logger.getLogger(DiskService.class.getName());
  private final LeonardoMapper leonardoMapper;
  private final LeonardoApiClient leonardoNotebooksClient;
  private final WorkspaceService workspaceService;

  @Autowired
  public DiskService(
      LeonardoMapper leonardoMapper,
      LeonardoApiClient leonardoNotebooksClient,
      WorkspaceService workspaceService) {
    this.leonardoMapper = leonardoMapper;
    this.leonardoNotebooksClient = leonardoNotebooksClient;
    this.workspaceService = workspaceService;
  }

  public void deleteDisk(String workspaceNamespace, String diskName) {
    String googleProject =
        workspaceService.lookupWorkspaceByNamespace(workspaceNamespace).getGoogleProject();
    leonardoNotebooksClient.deletePersistentDisk(googleProject, diskName);
  }

  public void deleteDiskAsService(String workspaceNamespace, String diskName) {
    String googleProject =
        workspaceService.lookupWorkspaceByNamespace(workspaceNamespace).getGoogleProject();
    leonardoNotebooksClient.deletePersistentDiskAsService(googleProject, diskName);
  }

  public List<Disk> getAllDisksInWorkspaceNamespace(String workspaceNamespace) {
    String googleProject =
        workspaceService.lookupWorkspaceByNamespace(workspaceNamespace).getGoogleProject();
    List<ListPersistentDiskResponse> responseList =
        leonardoNotebooksClient.listDisksByProjectAsService(googleProject);
    return responseList.stream()
        .map(leonardoMapper::toApiListDisksResponse)
        .collect(Collectors.toList());
  }

  public Disk getDisk(String workspaceNamespace, String diskName) {
    String googleProject =
        workspaceService.lookupWorkspaceByNamespace(workspaceNamespace).getGoogleProject();
    Disk disk =
        leonardoMapper.toApiGetDiskResponse(
            leonardoNotebooksClient.getPersistentDisk(googleProject, diskName));

    if (DiskStatus.FAILED.equals(disk.getStatus())) {
      log.warning(
          String.format("Observed failed PD %s in workspace %s", diskName, workspaceNamespace));
    }
    return disk;
  }

  public List<Disk> getOwnedDisksInWorkspace(String workspaceNamespace) {
    String googleProject =
        workspaceService.lookupWorkspaceByNamespace(workspaceNamespace).getGoogleProject();

    List<ListPersistentDiskResponse> responseList =
        leonardoNotebooksClient.listPersistentDiskByProjectCreatedByCreator(googleProject);

    return PersistentDiskUtils.findTheMostRecentActiveDisks(
        responseList.stream()
            .map(leonardoMapper::toApiListDisksResponse)
            .collect(Collectors.toList()));
  }

  public void updateDisk(String workspaceNamespace, String diskName, Integer diskSize) {
    String googleProject =
        workspaceService.lookupWorkspaceByNamespace(workspaceNamespace).getGoogleProject();
    leonardoNotebooksClient.updatePersistentDisk(googleProject, diskName, diskSize);
  }
}
