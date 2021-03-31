package org.pmiops.workbench.access;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.AccessTierDao;
import org.pmiops.workbench.db.dao.UserAccessTierDao;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserAccessTier;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.model.TierAccessStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AccessTierServiceImpl implements AccessTierService {
  private final Provider<WorkbenchConfig> configProvider;
  private final Clock clock;

  private final AccessTierDao accessTierDao;
  private final UserAccessTierDao userAccessTierDao;

  private final FireCloudService fireCloudService;

  private static final Logger log = Logger.getLogger(AccessTierServiceImpl.class.getName());

  @Autowired
  public AccessTierServiceImpl(
      Provider<WorkbenchConfig> configProvider,
      Clock clock,
      AccessTierDao accessTierDao,
      UserAccessTierDao userAccessTierDao,
      FireCloudService fireCloudService) {
    this.configProvider = configProvider;
    this.clock = clock;
    this.accessTierDao = accessTierDao;
    this.userAccessTierDao = userAccessTierDao;
    this.fireCloudService = fireCloudService;
  }

  /**
   * Return all access tiers in the database, in alphabetical order by shortName
   *
   * @return the List of all DbAccessTiers in the database
   */
  @Override
  public List<DbAccessTier> getAllTiers() {
    return accessTierDao.findAll().stream()
        .sorted(Comparator.comparing(DbAccessTier::getShortName))
        .collect(Collectors.toList());
  }

  /**
   * Add memberships to all tiers for a user if they don't exist by inserting DB row(s) set to
   * ENABLED. For any memberships which exist and are DISABLED, set them to ENABLED.
   *
   * @param user the DbUser in the user-accessTier mappings we're updating
   */
  @Override
  public void addUserToAllTiers(DbUser user) {
    getAllTiers().forEach(tier -> addUserToTier(user, tier));
  }

  /**
   * Add a tier membership to a user if none exists by inserting a DB row set to ENABLED. If such a
   * membership exists and is DISABLED, set it to ENABLED.
   *
   * @param user the DbUser in the user-accessTier mapping we're updating
   * @param accessTier the DbAccessTier in the user-accessTier mapping we're updating
   */
  @Override
  public void addUserToTier(DbUser user, DbAccessTier accessTier) {
    addToAuthDomainIdempotent(user, accessTier);

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
  @Override
  public void removeUserFromTier(DbUser user, DbAccessTier accessTier) {
    removeFromAuthDomainIdempotent(user, accessTier);

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

  private void addToAuthDomainIdempotent(DbUser dbUser, DbAccessTier accessTier) {
    final String username = dbUser.getUsername();
    final String authDomainName = accessTier.getAuthDomainName();
    if (!fireCloudService.isUserMemberOfGroup(username, authDomainName)) {
      fireCloudService.addUserToGroup(username, authDomainName);
      log.info(
          String.format(
              "Added user %s to auth domain for tier '%s'", username, accessTier.getShortName()));
    }
  }

  private void removeFromAuthDomainIdempotent(DbUser dbUser, DbAccessTier accessTier) {
    final String username = dbUser.getUsername();
    final String authDomainName = accessTier.getAuthDomainName();
    if (fireCloudService.isUserMemberOfGroup(username, authDomainName)) {
      fireCloudService.removeUserFromGroup(username, authDomainName);
      log.info(
          String.format(
              "Removed user %s from auth domain for tier '%s'",
              username, accessTier.getShortName()));
    }
  }

  /**
   * Return the list of tiers a user has access to: those where a DbUserAccessTier exists with
   * status ENABLED
   *
   * @param user the user whose access we're checking
   * @return The List of DbAccessTiers the DbUser has access to in this environment, in alphabetical
   *     order by shortName
   */
  @Override
  public List<DbAccessTier> getAccessTiersForUser(DbUser user) {
    return userAccessTierDao.getAllByUser(user).stream()
        .filter(uat -> uat.getTierAccessStatusEnum() == TierAccessStatus.ENABLED)
        .map(DbUserAccessTier::getAccessTier)
        .sorted(Comparator.comparing(DbAccessTier::getShortName))
        .collect(Collectors.toList());
  }

  /**
   * Return the list of tiers a user has access to, as shortNames
   *
   * @param user the user whose access we're checking
   * @return The List of shortNames of DbAccessTiers the DbUser has access to in this environment,
   *     in alphabetical order
   */
  @Override
  public List<String> getAccessTierShortNamesForUser(DbUser user) {
    return getAccessTiersForUser(user).stream()
        .map(DbAccessTier::getShortName)
        .collect(Collectors.toList());
  }

  /**
   * Return a list of access tiers which Registered users have access to. Depending on environment,
   * this will either be the Registered Tier or all tiers. This is a temporary measure until we
   * implement Controlled Tier Beta access controls.
   *
   * <p>See https://precisionmedicineinitiative.atlassian.net/browse/RW-6237
   *
   * @return the list of tiers which Registered users have access to.
   */
  @Override
  public List<DbAccessTier> getTiersForRegisteredUsers() {
    // check this regardless of feature flag
    final DbAccessTier registeredTier =
        accessTierDao
            .findOneByShortName(REGISTERED_TIER_SHORT_NAME)
            .orElseThrow(
                () -> new ServerErrorException("Cannot find Registered Tier in database."));

    if (configProvider.get().featureFlags.unsafeAllowAccessToAllTiersForRegisteredUsers) {
      return getAllTiers();
    } else {
      return Collections.singletonList(registeredTier);
    }
  }

  private Timestamp now() {
    return new Timestamp(clock.instant().toEpochMilli());
  }
}
