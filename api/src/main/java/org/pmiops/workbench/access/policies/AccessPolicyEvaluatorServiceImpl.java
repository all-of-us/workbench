package org.pmiops.workbench.access.policies;

import java.util.Map;
import java.util.Optional;
import org.pmiops.workbench.access.modules.AccessModuleKey;
import org.pmiops.workbench.access.modules.AccessModuleService;
import org.pmiops.workbench.access.modules.AccessScore;
import org.pmiops.workbench.db.model.DbUser;
import org.springframework.stereotype.Service;

@Service
public class AccessPolicyEvaluatorServiceImpl implements AccessPolicyEvaluatorService {

  private final Map<AccessModuleKey, AccessModuleService> moduleKeyAccessModuleService;

  public AccessPolicyEvaluatorServiceImpl(
      Map<AccessModuleKey, AccessModuleService> moduleKeyAccessModuleService) {
    this.moduleKeyAccessModuleService = moduleKeyAccessModuleService;
  }

  @Override
  public AccessScore scoreUser(AccessModuleKey key, DbUser dbUser) {
    return Optional.ofNullable(moduleKeyAccessModuleService.get(key))
        .map(m -> m.scoreUser(dbUser))
        .orElse(AccessScore.INVALID_ACCESS_MODULE);
  }
}
