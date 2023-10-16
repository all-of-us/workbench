package org.pmiops.workbench.disks;

import java.util.Map;
import org.pmiops.workbench.model.CloudPlatform;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class DiskServiceFactory {

  private final Map<CloudPlatform, DiskService> diskServices;
  private final DiskService defaultDiskService;

  public DiskServiceFactory(
      DiskService gcpDiskService, @Qualifier("awsDiskService") DiskService awsDiskService) {
    defaultDiskService = gcpDiskService;
    this.diskServices =
        Map.of(CloudPlatform.GCP, gcpDiskService, CloudPlatform.AWS, awsDiskService);
  }

  public DiskService getDiskService(CloudPlatform cloudPlatform) {
    return diskServices.getOrDefault(cloudPlatform, defaultDiskService);
  }
}
