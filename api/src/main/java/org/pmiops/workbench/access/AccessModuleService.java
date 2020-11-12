package org.pmiops.workbench.access;

import org.pmiops.workbench.db.model.DbUser;

/**
 * Implementations of this service will provide scores for users under various modules.
 */
public interface AccessModuleService {
  AccessScore scoreUser(DbUser user);
}
