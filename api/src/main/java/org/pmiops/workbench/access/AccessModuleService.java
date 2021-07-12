package org.pmiops.workbench.access;

import org.pmiops.workbench.model.AccessModule;

public interface AccessModuleService {
  /** Updates bypass time for a module. */
  void updateBypassTime(long userId, AccessModule accessModuleName, boolean isBypassed);
}
