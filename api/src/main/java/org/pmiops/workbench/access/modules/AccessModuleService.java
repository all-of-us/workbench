package org.pmiops.workbench.access.modules;

import org.pmiops.workbench.db.model.DbUser;

/**
 * Implementations of this service will provide scores for users under various modules.
 */
public interface AccessModuleService {
  AccessModuleKey getKey();
  AccessScore scoreUser(DbUser user);
}
