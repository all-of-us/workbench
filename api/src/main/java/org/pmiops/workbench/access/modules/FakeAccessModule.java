package org.pmiops.workbench.access.modules;

import org.pmiops.workbench.db.model.DbUser;

/** Access Module test fake. */
public class FakeAccessModule implements AccessModuleService {

  private final AccessModuleKey accessModuleKey;
  private final AccessScore constantAccessScore;

  public FakeAccessModule(AccessModuleKey accessModuleKey, AccessScore accessScore) {
    this.accessModuleKey = accessModuleKey;
    this.constantAccessScore = accessScore;
  }

  @Override
  public AccessModuleKey getKey() {
    return accessModuleKey;
  }

  @Override
  public AccessScore scoreUser(DbUser user) {
    return constantAccessScore;
  }
}
