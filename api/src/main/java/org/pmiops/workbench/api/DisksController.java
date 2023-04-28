package org.pmiops.workbench.api;

import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.pmiops.workbench.apiclients.leonardo.NewLeonardoApiClient;
import org.pmiops.workbench.apiclients.leonardo.NewLeonardoMapper;
import org.pmiops.workbench.leonardo.LeonardoApiClient;
import org.pmiops.workbench.leonardo.PersistentDiskUtils;
import org.pmiops.workbench.leonardo.model.LeonardoListPersistentDiskResponse;
import org.pmiops.workbench.model.Disk;
import org.pmiops.workbench.model.DiskStatus;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.ListDisksResponse;
import org.pmiops.workbench.utils.mappers.LeonardoMapper;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DisksController implements DisksApiDelegate {
  private static final Logger log = Logger.getLogger(DisksController.class.getName());

  private final LeonardoApiClient leonardoNotebooksClient;
  private final NewLeonardoApiClient newLeonardoClient;
  private final LeonardoMapper leonardoMapper;
  private final NewLeonardoMapper newLeonardoMapper;

  private final WorkspaceService workspaceService;

  @Autowired
  public DisksController(
      LeonardoApiClient leonardoNotebooksClient,
      NewLeonardoApiClient newLeonardoClient,
      LeonardoMapper leonardoMapper,
      NewLeonardoMapper newLeonardoMapper,
      WorkspaceService workspaceService) {
    this.leonardoNotebooksClient = leonardoNotebooksClient;
    this.newLeonardoClient = newLeonardoClient;
    this.leonardoMapper = leonardoMapper;
    this.newLeonardoMapper = newLeonardoMapper;
    this.workspaceService = workspaceService;
  }

  @Override
  public ResponseEntity<Disk> getDisk(String workspaceNamespace, String diskName) {
    String googleProject =
        workspaceService.lookupWorkspaceByNamespace(workspaceNamespace).getGoogleProject();
    Disk disk =
        newLeonardoMapper.toApiGetDiskResponse(
            newLeonardoClient.getPersistentDisk(googleProject, diskName));

    if (DiskStatus.FAILED.equals(disk.getStatus())) {
      log.warning(
          String.format("Observed failed PD %s in workspace %s", diskName, workspaceNamespace));
    }

    return ResponseEntity.ok(disk);
  }

  @Override
  public ResponseEntity<EmptyResponse> deleteDisk(String workspaceNamespace, String diskName) {
    String googleProject =
        workspaceService.lookupWorkspaceByNamespace(workspaceNamespace).getGoogleProject();
    leonardoNotebooksClient.deletePersistentDisk(googleProject, diskName);
    return ResponseEntity.ok(new EmptyResponse());
  }

  @Override
  public ResponseEntity<EmptyResponse> updateDisk(
      String workspaceNamespace, String diskName, Integer diskSize) {
    String googleProject =
        workspaceService.lookupWorkspaceByNamespace(workspaceNamespace).getGoogleProject();
    leonardoNotebooksClient.updatePersistentDisk(googleProject, diskName, diskSize);
    return ResponseEntity.ok(new EmptyResponse());
  }

  @Override
  public ResponseEntity<ListDisksResponse> listDisksInWorkspace(String workspaceNamespace) {
    String googleProject =
        workspaceService.lookupWorkspaceByNamespace(workspaceNamespace).getGoogleProject();

    List<LeonardoListPersistentDiskResponse> responseList =
        leonardoNotebooksClient.listPersistentDiskByProjectCreatedByCreator(googleProject, false);

    List<Disk> diskList =
        PersistentDiskUtils.findTheMostRecentActiveDisks(
            responseList.stream()
                .map(leonardoMapper::toApiListDisksResponse)
                .collect(Collectors.toList()));
    ListDisksResponse listDisksResponse = new ListDisksResponse();
    listDisksResponse.addAll(diskList);

    return ResponseEntity.ok(listDisksResponse);
  }
}
