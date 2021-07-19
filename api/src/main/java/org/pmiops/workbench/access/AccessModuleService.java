package org.pmiops.workbench.access;

import java.sql.Timestamp;
import java.util.List;
import org.pmiops.workbench.db.model.DbAccessModule.AccessModuleName;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.model.AccessModule;
import org.pmiops.workbench.model.AccessModuleStatus;

public interface AccessModuleService {
  /** Updates bypass time for a module. */
  void updateBypassTime(long userId, AccessModule accessModuleName, boolean isBypassed);

  /** Update module status to complete for a user. */
  void updateCompletionTime(DbUser dbUser, AccessModuleName accessModuleName, Timestamp timestamp);

  /** Retrieves all {@link AccessModuleStatus} for a user. */
  List<AccessModuleStatus> getClientAccessModuleStatus(DbUser user);
}
