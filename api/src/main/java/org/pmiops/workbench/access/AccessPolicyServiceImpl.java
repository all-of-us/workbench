package org.pmiops.workbench.access;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.function.Function;
import org.pmiops.workbench.db.model.DbUser;

public abstract class AccessPolicyServiceImpl implements AccessPolicyService {

  private Map<AccessModuleKey, AccessModuleService> moduleKeyAccessModuleService;
  private final InvalidKeyAccessModule invalidKeyAccessModule = new InvalidKeyAccessModule();

  public AccessPolicyServiceImpl(Map<AccessModuleKey, AccessModuleService> moduleKeyAccessModuleService) {
    this.moduleKeyAccessModuleService = moduleKeyAccessModuleService;
  }

  /**
   * Ask each module to rate this user and return a map. TODO(jaycarlton): include human-readable
   * descriptions for error conditions
   * @param dbUser user to be evalluated.
   * @return map from module key to score.
   */
  @Override
  public Map<AccessModuleKey, AccessScore> evaluateUser(DbUser dbUser) {
    return getAccessModules().stream()
        .collect(ImmutableMap.toImmutableMap(
            Function.identity(),
            accessModuleKey -> scoreUser(accessModuleKey, dbUser)));
  }

  public AccessScore scoreUser(AccessModuleKey key, DbUser dbUser) {
    return moduleKeyAccessModuleService.getOrDefault(key, invalidKeyAccessModule).scoreUser(dbUser);
  }
}
