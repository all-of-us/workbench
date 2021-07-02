package org.pmiops.workbench.access;

import java.util.List;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.model.AccessBypassRequest;

public interface AccessModuleService {
  /**
   * Updates bypass time for a module.
   */
  void updateBypassTime(long userDatabaseId, AccessBypassRequest accessBypassRequest);
}
