package org.pmiops.workbench.disks;

import java.util.List;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.Disk;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Qualifier("multicloudDiskService")
public class MulticloudDiskService implements DiskService {

  private final DiskServiceFactory diskServiceFactory;

  private final WorkspaceService workspaceService;

  @Autowired
  public MulticloudDiskService(
      DiskServiceFactory diskServiceFactory, WorkspaceService workspaceService) {
    this.diskServiceFactory = diskServiceFactory;
    this.workspaceService = workspaceService;
  }

  @Override
  public void deleteDisk(String workspaceNamespace, String diskName) {

    DbWorkspace dbWorkspace = workspaceService.lookupWorkspaceByNamespace(workspaceNamespace);
    diskServiceFactory
        .getDiskService(dbWorkspace.getCloudPlatform())
        .deleteDisk(workspaceNamespace, diskName);
  }

  @Override
  public void deleteDiskAsService(String workspaceNamespace, String diskName) {

    DbWorkspace dbWorkspace = workspaceService.lookupWorkspaceByNamespace(workspaceNamespace);
    diskServiceFactory
        .getDiskService(dbWorkspace.getCloudPlatform())
        .deleteDiskAsService(workspaceNamespace, diskName);
  }

  @Override
  public List<Disk> getAllDisksInWorkspaceNamespace(String workspaceNamespace) {
    DbWorkspace dbWorkspace = workspaceService.lookupWorkspaceByNamespace(workspaceNamespace);
    return diskServiceFactory
        .getDiskService(dbWorkspace.getCloudPlatform())
        .getAllDisksInWorkspaceNamespace(workspaceNamespace);
  }

  @Override
  public Disk getDisk(String workspaceNamespace, String diskName) {
    DbWorkspace dbWorkspace = workspaceService.lookupWorkspaceByNamespace(workspaceNamespace);
    return diskServiceFactory
        .getDiskService(dbWorkspace.getCloudPlatform())
        .getDisk(workspaceNamespace, diskName);
  }

  @Override
  public List<Disk> getOwnedDisksInWorkspace(String workspaceNamespace) {
    DbWorkspace dbWorkspace = workspaceService.lookupWorkspaceByNamespace(workspaceNamespace);
    return diskServiceFactory
        .getDiskService(dbWorkspace.getCloudPlatform())
        .getOwnedDisksInWorkspace(workspaceNamespace);
  }

  @Override
  public void updateDisk(String workspaceNamespace, String diskName, Integer diskSize) {
    DbWorkspace dbWorkspace = workspaceService.lookupWorkspaceByNamespace(workspaceNamespace);
    diskServiceFactory
        .getDiskService(dbWorkspace.getCloudPlatform())
        .updateDisk(workspaceNamespace, diskName, diskSize);
  }
}
