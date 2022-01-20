package org.pmiops.workbench.access;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.pmiops.workbench.db.model.DbAccessModule.AccessModuleName;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.model.AccessBypassRequest;
import org.pmiops.workbench.model.AccessModule;
import org.pmiops.workbench.model.AccessModuleStatus;

public interface AccessModuleService {
  /** Updates bypass time for a module. */
  void updateBypassTime(long userId, AccessBypassRequest accessBypassRequest);

  /** Updates bypass time for a module. */
  void updateBypassTime(long userId, AccessModule accessModuleName, boolean isBypassed);

  /** Update module status to complete for a user. */
  void updateCompletionTime(DbUser dbUser, AccessModuleName accessModuleName, Timestamp timestamp);

  /** Retrieves all {@link AccessModuleStatus} for a user. */
  List<AccessModuleStatus> getAccessModuleStatus(DbUser user);

  /**
   * Retrieves a specific {@link AccessModuleStatus} for a user.
   *
   * @return
   */
  Optional<AccessModuleStatus> getAccessModuleStatus(DbUser user, AccessModuleName accessModuleName);

  /**
   * Returns true if the access module is compliant.
   *
   * <p>The module can be bypassed OR (completed but not expired).
   */
  boolean isModuleCompliant(DbUser dbUser, AccessModuleName accessModuleName);

  /** Returns true if the access module is bypassable and bypassed */
  boolean isModuleBypassed(DbUser dbUser, AccessModuleName accessModuleName);

  int getCurrentDuccVersion();

  boolean hasUserSignedTheCurrentDucc(DbUser targetUser);
}
