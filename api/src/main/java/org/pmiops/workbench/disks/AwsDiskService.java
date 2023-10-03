package org.pmiops.workbench.disks;

import bio.terra.workspace.api.ResourceApi;
import bio.terra.workspace.model.ResourceList;
import bio.terra.workspace.model.ResourceType;
import bio.terra.workspace.model.StewardshipType;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.inject.Provider;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.WorkbenchException;
import org.pmiops.workbench.model.Disk;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Qualifier("awsDiskService")
public class AwsDiskService implements DiskService {

  private final WorkspaceService workspaceService;
  private final Provider<ResourceApi> resourceApiProvider;

  @Autowired
  public AwsDiskService(
      WorkspaceService workspaceService, Provider<ResourceApi> resourceApiProvider) {
    this.workspaceService = workspaceService;
    this.resourceApiProvider = resourceApiProvider;
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
          resourceApiProvider
              .get()
              .enumerateResources(
                  UUID.fromString(dbWorkspace.getFirecloudUuid()),
                  0,
                  10,
                  ResourceType.AWS_S3_STORAGE_FOLDER,
                  StewardshipType.CONTROLLED);
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
