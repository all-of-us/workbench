package org.pmiops.workbench.access;

import java.util.List;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.model.AccessModule;
import org.pmiops.workbench.model.AccessModuleStatus;

public interface AccessModuleService {
  /** Updates bypass time for a module. */
  void updateBypassTime(long userId, AccessModule accessModuleName, boolean isBypassed);

  /** Retrieves all {@link AccessModuleStatus} for a user. */
  List<AccessModuleStatus> getClientAccessModuleStatus(DbUser user);
}
