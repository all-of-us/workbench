package org.pmiops.workbench.access;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.pmiops.workbench.db.model.DbAccessModule.DbAccessModuleName;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.model.AccessBypassRequest;
import org.pmiops.workbench.model.AccessModuleStatus;

public interface AccessModuleService {
  /** Updates bypass time for a module. */
  void updateBypassTime(long userId, AccessBypassRequest accessBypassRequest);

  /** Updates bypass time for a module. */
  void updateBypassTime(long userId, DbAccessModuleName accessModuleName, boolean isBypassed);

  /** Updates bypass times for all modules in the environment. */
  void updateAllBypassTimes(long userId);

  /** Update module status to complete for a user. */
  void updateCompletionTime(
      DbUser dbUser, DbAccessModuleName accessModuleName, Timestamp timestamp);

  /** Retrieves all {@link AccessModuleStatus} for a user. */
  List<AccessModuleStatus> getAccessModuleStatus(DbUser user);

  /**
   * Retrieves a specific {@link AccessModuleStatus} for a user.
   *
   * @return
   */
  Optional<AccessModuleStatus> getAccessModuleStatus(
      DbUser user, DbAccessModuleName accessModuleName);

  /**
   * Returns true if the access module is compliant.
   *
   * <p>The module can be bypassed OR (completed but not expired).
   */
  boolean isModuleCompliant(DbUser dbUser, DbAccessModuleName accessModuleName);

  boolean isSignedDuccVersionCurrent(Integer signedVersion);

  boolean hasUserSignedACurrentDucc(DbUser targetUser);
}
