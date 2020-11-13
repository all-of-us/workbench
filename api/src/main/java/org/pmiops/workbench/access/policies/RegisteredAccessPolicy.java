package org.pmiops.workbench.access.policies;

import com.google.common.collect.ImmutableSet;
import java.util.Map;
import java.util.Set;
import org.pmiops.workbench.access.modules.AccessModuleKey;
import org.pmiops.workbench.access.modules.AccessModuleService;

public class RegisteredAccessPolicy extends AccessPolicyServiceAbstractImpl {

  public RegisteredAccessPolicy(
      Map<AccessModuleKey, AccessModuleService> moduleKeyAccessModuleService) {
    super(moduleKeyAccessModuleService);
  }

  @Override
  public Set<AccessModuleKey> getAccessModuleKeys() {
    return ImmutableSet.of(AccessModuleKey.DUA_TRAINING, AccessModuleKey.BETA_ACCESS);
  }
}
