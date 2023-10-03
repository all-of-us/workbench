package org.pmiops.workbench.disks;

import org.pmiops.workbench.db.model.DbWorkspace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class DiskServiceFactory {

  private final DiskService gcpDiskService;
  private final DiskService awsDiskService;

  @Autowired
  public DiskServiceFactory(
      DiskService gcpDiskService, @Qualifier("awsDiskService") DiskService awsDiskService) {
    this.gcpDiskService = gcpDiskService;
    this.awsDiskService = awsDiskService;
  }

  public DiskService getDiskService(DbWorkspace dbWorkspace) {
    if (dbWorkspace.isAws()) return awsDiskService;
    else return gcpDiskService;
  }
}
