package org.pmiops.workbench.access;

import java.util.List;
import java.util.Optional;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbUser;

public interface AccessTierService {
  // TODO remove once we are no longer special-casing the Registered Tier
  String REGISTERED_TIER_SHORT_NAME = "registered";

  String CONTROLLED_TIER_SHORT_NAME = "controlled";

  /**
   * Return all access tiers in the database, in alphabetical order by shortName.
   *
   * @return the List of all DbAccessTiers in the database
   */
  List<DbAccessTier> getAllTiers();

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

  void addToAuthDomainIdempotent(DbUser dbUser, DbAccessTier accessTier);
  void removeFromAuthDomainIdempotent(DbUser dbUser, DbAccessTier accessTier);
  void propagateAllAuthDomainMembership();
  /**
   * Return the list of tiers a user has access to: those where a DbUserAccessTier exists with
   * status ENABLED
   *
   * @param user the user whose access we're checking
   * @return The List of DbAccessTiers the DbUser has access to in this environment, in alphabetical
   *     order by shortName
   */
  List<DbAccessTier> getAccessTiersForUser(DbUser user);

  /**
   * Return the list of tiers a user has access to, as shortNames
   *
   * @param user the user whose access we're checking
   * @return The List of shortNames of DbAccessTiers the DbUser has access to in this environment,
   *     in alphabetical order
   */
  List<String> getAccessTierShortNamesForUser(DbUser user);

  DbAccessTier getRegisteredTierOrThrow();

  Optional<DbAccessTier> getAccessTierByName(String accessTierShortName);
}
