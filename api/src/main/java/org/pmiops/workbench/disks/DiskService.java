package org.pmiops.workbench.disks;

import java.util.List;
import org.broadinstitute.dsde.workbench.client.leonardo.model.ListPersistentDiskResponse;
import org.pmiops.workbench.leonardo.LeonardoApiClient;
import org.pmiops.workbench.leonardo.PersistentDiskUtils;
import org.pmiops.workbench.model.Disk;
import org.pmiops.workbench.utils.mappers.LeonardoMapper;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DiskService {
  private final LeonardoApiClient leonardoApiClient;
  private final LeonardoMapper leonardoMapper;
  private final WorkspaceService workspaceService;

  @Autowired
  public DiskService(
      LeonardoApiClient leonardoApiClient,
      LeonardoMapper leonardoMapper,
      WorkspaceService workspaceService) {
    this.leonardoApiClient = leonardoApiClient;
    this.leonardoMapper = leonardoMapper;
    this.workspaceService = workspaceService;
  }

  public void deleteDisk(String workspaceNamespace, String diskName) {
    String googleProject =
        workspaceService.lookupWorkspaceByNamespace(workspaceNamespace).getGoogleProject();
    leonardoApiClient.deletePersistentDisk(googleProject, diskName);
  }

  public void deleteDiskAsService(String workspaceNamespace, String diskName) {
    String googleProject =
        workspaceService.lookupWorkspaceByNamespace(workspaceNamespace).getGoogleProject();
    leonardoApiClient.deletePersistentDiskAsService(googleProject, diskName);
  }

  public List<Disk> getAllDisksInWorkspaceNamespace(String workspaceNamespace) {
    String googleProject =
        workspaceService.lookupWorkspaceByNamespace(workspaceNamespace).getGoogleProject();
    List<ListPersistentDiskResponse> responseList =
        leonardoApiClient.listDisksByProjectAsService(googleProject);
    return responseList.stream().map(leonardoMapper::toApiListDisksResponse).toList();
  }

  public List<Disk> getOwnedDisksInWorkspace(String workspaceNamespace) {
    String googleProject =
        workspaceService.lookupWorkspaceByNamespace(workspaceNamespace).getGoogleProject();

    List<ListPersistentDiskResponse> responseList =
        leonardoApiClient.listPersistentDiskByProjectCreatedByCreator(googleProject);

    return PersistentDiskUtils.findTheMostRecentActiveDisks(
        responseList.stream().map(leonardoMapper::toApiListDisksResponse).toList());
  }

  public void updateDisk(String workspaceNamespace, String diskName, Integer diskSize) {
    String googleProject =
        workspaceService.lookupWorkspaceByNamespace(workspaceNamespace).getGoogleProject();
    leonardoApiClient.updatePersistentDisk(googleProject, diskName, diskSize);
  }
}
