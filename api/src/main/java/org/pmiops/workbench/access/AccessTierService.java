package org.pmiops.workbench.access;

import java.util.List;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.ServerErrorException;

public interface AccessTierService {
  // TODO remove once we are no longer special-casing the Registered Tier
  String REGISTERED_TIER_SHORT_NAME = "registered";

  /**
   * Return all access tiers in the database
   *
   * @return the List of all DbAccessTiers in the database
   */
  List<DbAccessTier> getAllTiers();

  /**
   * Return the Registered Tier if it exists in the database
   *
   * <p>TODO remove once we are no longer special-casing the Registered Tier
   *
   * @return a DbAccessTier representing the Registered Tier
   * @throws ServerErrorException if there is no Registered Tier
   */
  DbAccessTier getRegisteredTier();

  /**
   * Add memberships to all tiers for a user if they don't exist by inserting DB row(s) set to
   * ENABLED. For any memberships which exist and are DISABLED, set them to ENABLED.
   *
   * @param user the DbUser in the user-accessTier mappings we're updating
   */
  void addUserToAllTiers(DbUser user);

  /**
   * Add a tier membership to a user if none exists by inserting a DB row set to ENABLED. If such a
   * membership exists and is DISABLED, set it to ENABLED.
   *
   * @param user the DbUser in the user-accessTier mapping we're updating
   * @param accessTier the DbAccessTier in the user-accessTier mapping we're updating
   */
  void addUserToTier(DbUser user, DbAccessTier accessTier);

  /**
   * Remove a tier membership from a user if one exists and is ENABLED by marking that membership as
   * DISABLED. Do nothing if no membership exists.
   *
   * @param user the DbUser in the user-accessTier mapping we're updating
   * @param accessTier the DbAccessTier in the user-accessTier mapping we're updating
   */
  void removeUserFromTier(DbUser user, DbAccessTier accessTier);

  /**
   * Remove a Registered Tier membership from a user if one exists and is ENABLED by marking that
   * membership as DISABLED. Do nothing if no membership exists.
   *
   * <p>Currently, this does not synchronize Terra Auth Domain group membership, but it will do so
   * when the user_access_tier table is the source of truth for tier membership. The existing method
   * UserServiceImpl.removeFromRegisteredTierGroupIdempotent() continues to handle group membership
   * until then.
   *
   * <p>TODO remove once we are no longer special-casing the Registered Tier
   *
   * @param user the DbUser in the user-accessTier mapping we're updating
   */
  void removeUserFromRegisteredTier(DbUser user);

  /**
   * Add a Registered Tier membership to a user if none exists by inserting a DB row set to ENABLED.
   * If such a membership exists and is DISABLED, set it to ENABLED.
   *
   * <p>Currently, this does not synchronize Terra Auth Domain group membership, but it will do so
   * when the user_access_tier table is the source of truth for tier membership. The existing method
   * UserServiceImpl.addToRegisteredTierGroupIdempotent() continues to handle group membership until
   * then.
   *
   * <p>TODO remove once we are no longer special-casing the Registered Tier
   *
   * @param user the DbUser in the user-accessTier mapping we're updating
   */
  void addUserToRegisteredTier(DbUser user);

  /**
   * Return the list of tiers a user has access to: those where a DbUserAccessTier exists with
   * status ENABLED
   *
   * @param user the user whose access we're checking
   * @return The List of DbAccessTiers the DbUser has access to in this environment
   */
  List<DbAccessTier> getAccessTiersForUser(DbUser user);
}
