package org.pmiops.workbench.access;

import java.util.Map;
import java.util.Set;
import org.pmiops.workbench.db.model.DbUser;

public interface AccessPolicyService {
  Set<AccessModuleKey> getAccessModules();
  Map<AccessModuleKey, AccessScore> evaluateUser(DbUser dbUser);
}
