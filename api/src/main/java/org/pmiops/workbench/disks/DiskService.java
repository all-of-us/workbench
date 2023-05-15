package org.pmiops.workbench.disks;

import java.util.List;
import java.util.stream.Collectors;
import org.pmiops.workbench.leonardo.LeonardoApiClient;
import org.pmiops.workbench.leonardo.model.LeonardoListPersistentDiskResponse;
import org.pmiops.workbench.model.Disk;
import org.pmiops.workbench.utils.mappers.LeonardoMapper;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DiskService {
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

  public List<Disk> findByWorkspaceNamespace(String workspaceNamespace) {
    String googleProject =
        workspaceService.lookupWorkspaceByNamespace(workspaceNamespace).getGoogleProject();
    List<LeonardoListPersistentDiskResponse> responseList =
        leonardoNotebooksClient.listDisksByProjectAsService(googleProject);
    return responseList.stream()
        .map(leonardoMapper::toApiListDisksResponse)
        .collect(Collectors.toList());
  }
}
