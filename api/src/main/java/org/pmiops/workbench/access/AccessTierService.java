package org.pmiops.workbench.access;

import java.util.List;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbUser;

public interface AccessTierService {
  String REGISTERED_TIER_SHORT_NAME = "registered";

  List<DbAccessTier> getAccessTiersForUser(DbUser user);

  // currently the required tier in some contexts
  DbAccessTier getRegisteredTier();

  DbAccessTier getAccessTier(String shortName);

  List<DbAccessTier> getAllTiers();
}
