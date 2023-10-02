package org.pmiops.workbench.disks;

import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.Disk;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Qualifier("multicloudDiskService")
public class MulticloudDiskService implements DiskService {

    private final DiskService gcpDiskService;
    private final DiskService awsDiskService;

    private final WorkspaceService workspaceService;

    @Autowired
    public MulticloudDiskService(DiskService gcpDiskService, @Qualifier("awsDiskService") DiskService awsDiskService, WorkspaceService workspaceService) {
        this.gcpDiskService = gcpDiskService;
        this.awsDiskService = awsDiskService;
        this.workspaceService = workspaceService;
    }

    @Override
    public void deleteDisk(String workspaceNamespace, String diskName) {

    }

    @Override
    public void deleteDiskAsService(String workspaceNamespace, String diskName) {

    }

    @Override
    public List<Disk> getAllDisksInWorkspaceNamespace(String workspaceNamespace) {
        DbWorkspace dbWorkspace = workspaceService.lookupWorkspaceByNamespace(workspaceNamespace);
        diskServiceFactory.getDiskService(dbWorkspace.getCloudPlatform());
    }

    @Override
    public Disk getDisk(String workspaceNamespace, String diskName) {
        return null;
    }

    @Override
    public List<Disk> getOwnedDisksInWorkspace(String workspaceNamespace) {
        return null;
    }

    @Override
    public void updateDisk(String workspaceNamespace, String diskName, Integer diskSize) {

    }
}
