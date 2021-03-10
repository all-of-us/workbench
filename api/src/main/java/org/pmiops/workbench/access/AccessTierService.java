package org.pmiops.workbench.access;

import java.util.List;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbUser;

public interface AccessTierService {
  String REGISTERED_TIER_SHORT_NAME = "registered";

  List<DbAccessTier> getAllTiers();

  DbAccessTier getAccessTier(String shortName);

  DbAccessTier getRegisteredTier();

  void addUserToAllTiers(DbUser user);

  void removeUserFromRegisteredTier(DbUser user);

  void addUserToRegisteredTier(DbUser user);

  List<DbAccessTier> getAccessTiersForUser(DbUser user);
}
