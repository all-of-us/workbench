package org.pmiops.workbench.disks;

import bio.terra.workspace.model.ResourceList;
import bio.terra.workspace.model.ResourceType;
import java.util.ArrayList;
import java.util.List;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.WorkbenchException;
import org.pmiops.workbench.model.Disk;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.pmiops.workbench.wsm.WsmClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Qualifier("awsDiskService")
public class AwsDiskService implements DiskService {

  private final WorkspaceService workspaceService;

  private final WsmClient wsmClient;

  @Autowired
  public AwsDiskService(WorkspaceService workspaceService, WsmClient wsmClient) {
    this.workspaceService = workspaceService;
    this.wsmClient = wsmClient;
  }

  @Override
  public void deleteDisk(String workspaceNamespace, String diskName) {}

  @Override
  public void deleteDiskAsService(String workspaceNamespace, String diskName) {}

  @Override
  public List<Disk> getAllDisksInWorkspaceNamespace(String workspaceNamespace) {
    return null;
  }

  @Override
  public Disk getDisk(String workspaceNamespace, String diskName) {
    return null;
  }

  @Override
  public List<Disk> getOwnedDisksInWorkspace(String workspaceNamespace) {
    DbWorkspace dbWorkspace = workspaceService.lookupWorkspaceByNamespace(workspaceNamespace);
    ResourceList resourceList;
    List<Disk> disks = new ArrayList<>();
    try {
      resourceList =
          wsmClient.getResourcesInWorkspace(
              dbWorkspace.getFirecloudUuid(), 10, ResourceType.AWS_S3_STORAGE_FOLDER);

    } catch (bio.terra.workspace.client.ApiException e) {
      throw new WorkbenchException(e);
    }
    resourceList
        .getResources()
        .forEach(
            resource -> {
              Disk disk = new Disk().name(resource.getMetadata().getName());
              disks.add(disk);
            });
    return disks;
  }

  @Override
  public void updateDisk(String workspaceNamespace, String diskName, Integer diskSize) {}
}
