package org.pmiops.workbench.access.policies;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.pmiops.workbench.access.modules.AccessModuleKey;

/**
 * POJO class defining the access modules to be evaluated. TODO(jaycarlton): support different
 * strategies
 */
public class AccessPolicy {

  private final ImmutableSet<AccessModuleKey> accessModuleKeys;

  public AccessPolicy(Set<AccessModuleKey> accessModuleKeys) {
    this.accessModuleKeys = ImmutableSet.copyOf(accessModuleKeys);
  }

  public Set<AccessModuleKey> getAccessModuleKeys() {
    return accessModuleKeys;
  }
}
