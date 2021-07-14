package org.pmiops.workbench.access;

import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.model.AccessModule;

public interface AccessModuleService {
  /** Updates bypass time for a module. */
  void updateBypassTime(long userId, AccessModule accessModuleName, boolean isBypassed);

  /** Update module status to complete for a user. */
  void completeModule(DbUser dbUser, AccessModule accessModuleName);
}
