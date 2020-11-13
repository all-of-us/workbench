package org.pmiops.workbench.access.modules;

import org.pmiops.workbench.db.model.DbUser;

public class BetaAccessModule implements AccessModuleService {

  public BetaAccessModule() {
  }

  @Override
  public AccessModuleKey getKey() {
    return AccessModuleKey.BETA_ACCESS;
  }

  /**
   * BETA Access is always pending.
   * @param user
   * @return
   */
  @Override
  public AccessScore scoreUser(DbUser user) {
    return AccessScore.PENDING;
  }
}
