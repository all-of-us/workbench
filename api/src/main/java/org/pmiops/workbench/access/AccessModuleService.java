package org.pmiops.workbench.access;

import org.pmiops.workbench.model.AccessBypassRequest;

public interface AccessModuleService {
  /** Updates bypass time for a module. */
  void updateBypassTime(long userDatabaseId, AccessBypassRequest accessBypassRequest);
}
