package org.pmiops.workbench.api;

import java.util.List;
import org.pmiops.workbench.disks.DiskService;
import org.pmiops.workbench.model.Disk;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.ListDisksResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DisksController implements DisksApiDelegate {

  private final DiskService diskService;

  @Autowired
  public DisksController(@Qualifier("multicloudDiskService") DiskService diskService) {
    this.diskService = diskService;
  }

  @Override
  public ResponseEntity<Disk> getDisk(String workspaceNamespace, String diskName) {
    return ResponseEntity.ok(diskService.getDisk(workspaceNamespace, diskName));
  }

  @Override
  public ResponseEntity<EmptyResponse> deleteDisk(String workspaceNamespace, String diskName) {
    diskService.deleteDisk(workspaceNamespace, diskName);
    return ResponseEntity.ok(new EmptyResponse());
  }

  @Override
  public ResponseEntity<EmptyResponse> updateDisk(
      String workspaceNamespace, String diskName, Integer diskSize) {
    diskService.updateDisk(workspaceNamespace, diskName, diskSize);
    return ResponseEntity.ok(new EmptyResponse());
  }

  @Override
  public ResponseEntity<ListDisksResponse> listOwnedDisksInWorkspace(String workspaceNamespace) {
    List<Disk> diskList = diskService.getOwnedDisksInWorkspace(workspaceNamespace);
    ListDisksResponse listDisksResponse = new ListDisksResponse();
    listDisksResponse.addAll(diskList);

    return ResponseEntity.ok(listDisksResponse);
  }
}
