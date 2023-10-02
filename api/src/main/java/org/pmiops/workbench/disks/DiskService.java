package org.pmiops.workbench.disks;

import org.pmiops.workbench.model.Disk;

import java.util.List;

public interface DiskService {


    void deleteDisk(String workspaceNamespace, String diskName);

    void deleteDiskAsService(String workspaceNamespace, String diskName);

    List<Disk> getAllDisksInWorkspaceNamespace(String workspaceNamespace);

    Disk getDisk(String workspaceNamespace, String diskName);

    List<Disk> getOwnedDisksInWorkspace(String workspaceNamespace);

    void updateDisk(String workspaceNamespace, String diskName, Integer diskSize);
}
