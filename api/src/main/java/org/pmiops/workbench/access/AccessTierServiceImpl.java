package org.pmiops.workbench.access;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.AccessTierDao;
import org.pmiops.workbench.db.dao.UserAccessTierDao;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserAccessTier;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.model.TierAccessStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AccessTierServiceImpl implements AccessTierService {

  // TODO remove?
  private final Provider<WorkbenchConfig> configProvider;
  private final Clock clock;

  private final AccessTierDao accessTierDao;
  private final UserAccessTierDao userAccessTierDao;

  @Autowired
  public AccessTierServiceImpl(
      Provider<WorkbenchConfig> configProvider,
      Clock clock,
      AccessTierDao accessTierDao,
      UserAccessTierDao userAccessTierDao) {
    this.configProvider = configProvider;
    this.accessTierDao = accessTierDao;
    this.userAccessTierDao = userAccessTierDao;
    this.clock = clock;
  }

  /**
   * Return all access tiers in the database
   *
   * @return the List of all DbAccessTiers in the database
   */
  public List<DbAccessTier> getAllTiers() {
    return accessTierDao.findAll();
  }

  /**
   * Return the Registered Tier if it exists in the database
   *
   * @return a DbAccessTier representing the Registered Tier
   * @throws ServerErrorException if there is no Registered Tier
   */
  public DbAccessTier getRegisteredTier() {
    return accessTierDao
        .findOneByShortName(REGISTERED_TIER_SHORT_NAME)
        .orElseThrow(() -> new ServerErrorException("Cannot find Registered Tier in database."));
  }

  /**
   * Add memberships to all tiers for a user if they don't exist by inserting DB row(s) set to
   * ENABLED. For any memberships which exist and are DISABLED, set them to ENABLED.
   *
   * @param user the DbUser in the user-accessTier mappings we're updating
   */
  public void addUserToAllTiers(DbUser user) {
    getAllTiers().forEach(tier -> addUserToTier(user, tier));
  }

  /**
   * Add a Registered Tier membership to a user if none exists by inserting a DB row set to ENABLED.
   * If such a membership exists and is DISABLED, set it to ENABLED.
   *
   * <p>Currently, this does not synchronize Terra Auth Domain group membership, but it will do so
   * when the user_access_tier table is the source of truth for tier membership. The existing method
   * UserServiceImpl.addToRegisteredTierGroupIdempotent() continues to handle group membership until
   * then.
   *
   * @param user the DbUser in the user-accessTier mapping we're updating
   */
  public void addUserToRegisteredTier(DbUser user) {
    addUserToTier(user, getRegisteredTier());
  }

  /**
   * Remove a Registered Tier membership from a user if one exists and is ENABLED by marking that
   * membership as DISABLED. Do nothing if no membership exists.
   *
   * <p>Currently, this does not synchronize Terra Auth Domain group membership, but it will do so
   * when the user_access_tier table is the source of truth for tier membership. The existing method
   * UserServiceImpl.removeFromRegisteredTierGroupIdempotent() continues to handle group membership
   * until then.
   *
   * @param user the DbUser in the user-accessTier mapping we're updating
   */
  public void removeUserFromRegisteredTier(DbUser user) {
    removeUserFromTier(user, getRegisteredTier());
  }

  /**
   * Add a tier membership to a user if none exists by inserting a DB row set to ENABLED. If such a
   * membership exists and is DISABLED, set it to ENABLED.
   *
   * @param user the DbUser in the user-accessTier mapping we're updating
   * @param accessTier the DbAccessTier in the user-accessTier mapping we're updating
   */
  public void addUserToTier(DbUser user, DbAccessTier accessTier) {
    Optional<DbUserAccessTier> existingEntryMaybe =
        userAccessTierDao.getByUserAndAccessTier(user, accessTier);

    if (existingEntryMaybe.isPresent()) {
      final DbUserAccessTier entryToUpdate = existingEntryMaybe.get();

      // don't update if already ENABLED
      if (entryToUpdate.getTierAccessStatusEnum() == TierAccessStatus.DISABLED) {
        userAccessTierDao.save(
            entryToUpdate.setTierAccessStatus(TierAccessStatus.ENABLED).setLastUpdated(now()));
      }
    } else {
      final DbUserAccessTier entryToInsert =
          new DbUserAccessTier()
              .setUser(user)
              .setAccessTier(accessTier)
              .setTierAccessStatus(TierAccessStatus.ENABLED)
              .setFirstEnabled(now())
              .setLastUpdated(now());
      userAccessTierDao.save(entryToInsert);
    }
  }

  /**
   * Remove a tier membership from a user if one exists and is ENABLED by marking that membership as
   * DISABLED. Do nothing if no membership exists.
   *
   * @param user the DbUser in the user-accessTier mapping we're updating
   * @param accessTier the DbAccessTier in the user-accessTier mapping we're updating
   */
  public void removeUserFromTier(DbUser user, DbAccessTier accessTier) {
    userAccessTierDao
        .getByUserAndAccessTier(user, accessTier)
        .filter(entry -> entry.getTierAccessStatusEnum() == TierAccessStatus.ENABLED)
        .ifPresent(
            entryToSoftDelete ->
                userAccessTierDao.save(
                    entryToSoftDelete
                        .setTierAccessStatus(TierAccessStatus.DISABLED)
                        .setLastUpdated(now())));
  }

  /**
   * Return the list of tiers a user has access to: those where a DbUserAccessTier exists with
   * status ENABLED
   *
   * @param user the user whose access we're checking
   * @return The List of DbAccessTiers the DbUser has access to in this environment
   */
  public List<DbAccessTier> getAccessTiersForUser(DbUser user) {
    return userAccessTierDao.getAllByUser(user).stream()
        .filter(uat -> uat.getTierAccessStatusEnum() == TierAccessStatus.ENABLED)
        .map(DbUserAccessTier::getAccessTier)
        .collect(Collectors.toList());
  }

  private Timestamp now() {
    return new Timestamp(clock.instant().toEpochMilli());
  }
}
