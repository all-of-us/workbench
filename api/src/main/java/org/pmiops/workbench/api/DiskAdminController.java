package org.pmiops.workbench.api;

import java.util.List;
import org.pmiops.workbench.annotations.AuthorityRequired;
import org.pmiops.workbench.disks.DiskService;
import org.pmiops.workbench.model.Authority;
import org.pmiops.workbench.model.Disk;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.ListDisksResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DiskAdminController implements DiskAdminApiDelegate {

  private final DiskService diskService;

  @Autowired
  public DiskAdminController(DiskService diskService) {
    this.diskService = diskService;
  }

  @Override
  @AuthorityRequired({Authority.RESEARCHER_DATA_VIEW})
  public ResponseEntity<EmptyResponse> deleteDisk(String workspaceNamespace, String diskName){
    diskService.deleteDiskAsService(workspaceNamespace, diskName);
    return ResponseEntity.ok(new EmptyResponse());
  }

  @Override
  @AuthorityRequired({Authority.RESEARCHER_DATA_VIEW})
  public ResponseEntity<ListDisksResponse> listDisksInWorkspace(String workspaceNamespace) {
    List<Disk> diskList = diskService.findByWorkspaceNamespace(workspaceNamespace);
    ListDisksResponse listDisksResponse = new ListDisksResponse();
    listDisksResponse.addAll(diskList);
    return ResponseEntity.ok(listDisksResponse);
  }
}
