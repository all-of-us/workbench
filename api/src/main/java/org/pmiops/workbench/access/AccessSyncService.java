package org.pmiops.workbench.access;

import org.pmiops.workbench.actionaudit.Agent;
import org.pmiops.workbench.db.model.DbUser;

public interface AccessSyncService {

  /**
   * Ensures that the data access tiers for the user reflect the state of other fields on the user
   */
  DbUser updateUserAccessTiers(DbUser dbUser, Agent agent);
}
