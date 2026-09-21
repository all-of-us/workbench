package org.pmiops.workbench.disks;

import java.util.List;
import java.util.logging.Logger;
import org.pmiops.workbench.model.Disk;
import org.springframework.stereotype.Service;

@Service
public class DiskAdminService {
  private static final Logger log = Logger.getLogger(DiskAdminService.class.getName());

  public DiskAdminService() {}

  public void checkPersistentDisks(List<Disk> disks) {
    log.info(String.format("Removed disk check with Terra calls, skipping check on %d disks", disks.size()));
  }
}
