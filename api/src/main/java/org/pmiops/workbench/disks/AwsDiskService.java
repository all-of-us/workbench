package org.pmiops.workbench.disks;

import org.pmiops.workbench.model.Disk;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Qualifier("awsDiskService")
public class AwsDiskService implements DiskService{
    @Override
    public void deleteDisk(String workspaceNamespace, String diskName) {

    }

    @Override
    public void deleteDiskAsService(String workspaceNamespace, String diskName) {

    }

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
        return null;
    }

    @Override
    public void updateDisk(String workspaceNamespace, String diskName, Integer diskSize) {

    }
}
