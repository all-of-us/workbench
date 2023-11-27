package org.pmiops.workbench.db.dao;

import static org.pmiops.workbench.access.AccessTierService.CONTROLLED_TIER_SHORT_NAME;
import static org.pmiops.workbench.access.AccessTierService.REGISTERED_TIER_SHORT_NAME;
import static org.pmiops.workbench.access.AccessUtils.getRequiredModulesForControlledTierRenewal;
import static org.pmiops.workbench.access.AccessUtils.getRequiredModulesForRegisteredTierRenewal;

import com.google.api.services.oauth2.model.Userinfo;
import com.google.common.collect.ImmutableList;
import jakarta.mail.MessagingException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.inject.Provider;
import org.hibernate.exception.GenericJDBCException;
import org.pmiops.workbench.access.AccessModuleNameMapper;
import org.pmiops.workbench.access.AccessModuleService;
import org.pmiops.workbench.access.AccessSyncService;
import org.pmiops.workbench.access.AccessTierService;
import org.pmiops.workbench.actionaudit.Agent;
import org.pmiops.workbench.actionaudit.auditors.UserServiceAuditor;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.model.DbAccessModule.DbAccessModuleName;
import org.pmiops.workbench.db.model.DbAddress;
import org.pmiops.workbench.db.model.DbDemographicSurvey;
import org.pmiops.workbench.db.model.DbDemographicSurveyV2;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserCodeOfConductAgreement;
import org.pmiops.workbench.db.model.DbUserTermsOfService;
import org.pmiops.workbench.db.model.DbVerifiedInstitutionalAffiliation;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.FirecloudNihStatus;
import org.pmiops.workbench.google.DirectoryService;
import org.pmiops.workbench.mail.MailService;
import org.pmiops.workbench.model.AccessModuleStatus;
import org.pmiops.workbench.model.Authority;
import org.pmiops.workbench.model.Degree;
import org.pmiops.workbench.model.GeneralDiscoverySource;
import org.pmiops.workbench.model.PartnerDiscoverySource;
import org.pmiops.workbench.monitoring.GaugeDataCollector;
import org.pmiops.workbench.monitoring.MeasurementBundle;
import org.pmiops.workbench.monitoring.labels.MetricLabel;
import org.pmiops.workbench.monitoring.views.GaugeMetric;
import org.pmiops.workbench.profile.DiscoverySourceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * A higher-level service class containing user manipulation and business logic which can't be
 * represented by automatic query generation in UserDao.
 *
 * <p>A large portion of this class is dedicated to:
 *
 * <p>(1) making it easy to consistently modify a subset of fields in a User entry, with retries (2)
 * ensuring we call a single updateUserAccessTiers method whenever a User entry is saved.
 */
@Service
public class UserServiceImpl implements UserService, GaugeDataCollector {
  private static final int MAX_RETRIES = 3;

  private final Provider<WorkbenchConfig> configProvider;
  private final Provider<DbUser> userProvider;
  private final Clock clock;
  private final Random random;
  private final UserServiceAuditor userServiceAuditor;

  private final UserDao userDao;
  private final UserTermsOfServiceDao userTermsOfServiceDao;
  private final VerifiedInstitutionalAffiliationDao verifiedInstitutionalAffiliationDao;

  private final AccessTierService accessTierService;
  private final AccessModuleNameMapper accessModuleNameMapper;
  private final AccessModuleService accessModuleService;
  private final DirectoryService directoryService;
  private final FireCloudService fireCloudService;
  private final MailService mailService;
  private final DiscoverySourceMapper discoverySourceMapper;
  private final AccessSyncService accessSyncService;

  private static final Logger log = Logger.getLogger(UserServiceImpl.class.getName());

  @Autowired
  public UserServiceImpl(
      Provider<WorkbenchConfig> configProvider,
      Provider<DbUser> userProvider,
      Clock clock,
      Random random,
      UserServiceAuditor userServiceAuditor,
      UserDao userDao,
      UserTermsOfServiceDao userTermsOfServiceDao,
      VerifiedInstitutionalAffiliationDao verifiedInstitutionalAffiliationDao,
      AccessModuleNameMapper accessModuleNameMapper,
      AccessModuleService accessModuleService,
      FireCloudService fireCloudService,
      DirectoryService directoryService,
      AccessTierService accessTierService,
      MailService mailService,
      AccessSyncService accessSyncService,
      DiscoverySourceMapper discoverySourceMapper) {
    this.configProvider = configProvider;
    this.userProvider = userProvider;
    this.clock = clock;
    this.random = random;
    this.userServiceAuditor = userServiceAuditor;
    this.userDao = userDao;
    this.userTermsOfServiceDao = userTermsOfServiceDao;
    this.verifiedInstitutionalAffiliationDao = verifiedInstitutionalAffiliationDao;
    this.accessModuleNameMapper = accessModuleNameMapper;
    this.accessModuleService = accessModuleService;
    this.fireCloudService = fireCloudService;
    this.directoryService = directoryService;
    this.accessTierService = accessTierService;
    this.mailService = mailService;
    this.accessSyncService = accessSyncService;
    this.discoverySourceMapper = discoverySourceMapper;
  }

  /**
   * Updates a user record with a modifier function.
   *
   * <p>Ensures that the data access tiers for the user reflect the state of other fields on the
   * user; handles conflicts with concurrent updates by retrying.
   */
  @Override
  public DbUser updateUserWithRetries(
      Function<DbUser, DbUser> userModifier, DbUser dbUser, Agent agent) {
    int objectLockingFailureCount = 0;
    int statementClosedCount = 0;
    while (true) {
      dbUser = userModifier.apply(dbUser);
      dbUser = accessSyncService.updateUserAccessTiers(dbUser, agent);
      try {
        return userDao.save(dbUser);
      } catch (ObjectOptimisticLockingFailureException e) {
        if (objectLockingFailureCount < MAX_RETRIES) {
          long userId = dbUser.getUserId();
          dbUser =
              userDao
                  .findById(userId)
                  .orElseThrow(
                      () ->
                          new BadRequestException(
                              String.format("User with ID %s not found", userId)));
          objectLockingFailureCount++;
        } else {
          throw new ConflictException(
              String.format(
                  "Could not update user %s after %d object locking failures",
                  dbUser.getUserId(), objectLockingFailureCount));
        }
      } catch (JpaSystemException e) {
        // We don't know why this happens instead of the object locking failure.
        if (((GenericJDBCException) e.getCause())
            .getSQLException()
            .getMessage()
            .equals("Statement closed.")) {
          if (statementClosedCount < MAX_RETRIES) {
            long userId = dbUser.getUserId();
            dbUser =
                userDao
                    .findById(userId)
                    .orElseThrow(
                        () ->
                            new BadRequestException(
                                String.format("User with ID %s not found", userId)));
            statementClosedCount++;
          } else {
            throw new ConflictException(
                String.format(
                    "Could not update user %s after %d statement closes",
                    dbUser.getUserId(), statementClosedCount));
          }
        } else {
          throw e;
        }
      }
    }
  }

  @Override
  public boolean isServiceAccount(DbUser user) {
    return configProvider.get().auth.serviceAccountApiUsers.contains(user.getUsername());
  }

  @Override
  public DbUser createServiceAccountUser(String username) {
    DbUser user = new DbUser();
    user.setUsername(username);
    user.setContactEmail(username);
    user.setDisabled(false);
    try {
      user = userDao.save(user);
    } catch (DataIntegrityViolationException e) {
      // For certain test workflows, it's possible to have concurrent user creation.
      // We attempt to handle that gracefully here.
      final DbUser userByUserName = userDao.findUserByUsername(username);
      if (userByUserName == null) {
        log.log(
            Level.WARNING,
            String.format(
                "While creating new user with email %s due to "
                    + "DataIntegrityViolationException. No user matching this username was found "
                    + "and none exists in the database",
                username),
            e);
        throw e;
      } else {
        log.log(
            Level.WARNING,
            String.format(
                "While creating new user with email %s due to "
                    + "DataIntegrityViolationException. User %d is present however, "
                    + "indicating possible concurrent creation.",
                username, userByUserName.getUserId()),
            e);
        user = userByUserName;
      }
    }

    // record the Service Account's access level as belonging to all tiers in user_access_tier
    // which will eventually serve as the source of truth (TODO)
    // this needs to occur after the user has been saved to the DB
    accessTierService.addUserToAllTiers(user);
    return user;
  }

  @Override
  public DbUser createUser(
      final Userinfo userInfo,
      final String contactEmail,
      DbVerifiedInstitutionalAffiliation dbVerifiedAffiliation) {
    return createUser(
        userInfo.getGivenName(),
        userInfo.getFamilyName(),
        // This GSuite primary email address is what RW refers to as `username`.
        userInfo.getEmail(),
        contactEmail,
        null,
        null,
        List.of(),
        null,
        List.of(),
        null,
        null,
        null,
        null,
        null,
        dbVerifiedAffiliation);
  }

  // TODO: move this and the one above to UserMapper
  @Override
  public DbUser createUser(
      String givenName,
      String familyName,
      String username,
      String contactEmail,
      String areaOfResearch,
      String professionalUrl,
      List<GeneralDiscoverySource> generalDiscoverySources,
      String generalDiscoverySourceOtherText,
      List<PartnerDiscoverySource> partnerDiscoverySources,
      String partnerDiscoverySourceOtherText,
      List<Degree> degrees,
      DbAddress dbAddress,
      DbDemographicSurvey dbDemographicSurvey,
      DbDemographicSurveyV2 dbDemographicSurveyV2,
      DbVerifiedInstitutionalAffiliation dbVerifiedAffiliation) {
    DbUser dbUser = new DbUser();
    dbUser.setCreationNonce(Math.abs(random.nextLong()));
    dbUser.setUsername(username);
    dbUser.setContactEmail(contactEmail);
    dbUser.setAreaOfResearch(areaOfResearch);
    dbUser.setFamilyName(familyName);
    dbUser.setGivenName(givenName);
    dbUser.setProfessionalUrl(professionalUrl);
    setDiscoverySources(
        dbUser,
        generalDiscoverySources,
        generalDiscoverySourceOtherText,
        partnerDiscoverySources,
        partnerDiscoverySourceOtherText);
    dbUser.setDisabled(false);
    dbUser.setAddress(dbAddress);
    if (degrees != null) {
      dbUser.setDegreesEnum(degrees);
    }
    dbUser.setDemographicSurvey(dbDemographicSurvey);
    dbUser.setDemographicSurveyV2(dbDemographicSurveyV2);

    // For existing user that do not have address
    if (dbAddress != null) {
      dbAddress.setUser(dbUser);
    }
    if (dbDemographicSurvey != null) {
      dbDemographicSurvey.setUser(dbUser);
    }
    if (dbDemographicSurveyV2 != null) {
      dbDemographicSurveyV2.setUser(dbUser);
    }

    Timestamp now = clockNow();
    try {
      dbUser = userDao.save(dbUser);
      dbVerifiedAffiliation.setUser(dbUser);
      verifiedInstitutionalAffiliationDao.save(dbVerifiedAffiliation);
      accessModuleService.updateCompletionTime(
          dbUser, DbAccessModuleName.PUBLICATION_CONFIRMATION, now);
      accessModuleService.updateCompletionTime(
          dbUser, DbAccessModuleName.PROFILE_CONFIRMATION, now);
    } catch (DataIntegrityViolationException e) {
      dbUser = userDao.findUserByUsername(username);
      if (dbUser == null) {
        throw e;
      }
      // If a user already existed (due to multiple requests trying to create a user simultaneously)
      // just return it.
    }
    return dbUser;
  }

  private void setDiscoverySources(
      DbUser dbUser,
      List<GeneralDiscoverySource> generalDiscoverySources,
      String generalDiscoverySourceOtherText,
      List<PartnerDiscoverySource> partnerDiscoverySources,
      String partnerDiscoverySourceOtherText) {
    dbUser.setGeneralDiscoverySources(
        discoverySourceMapper.toDbGeneralDiscoverySources(generalDiscoverySources));
    dbUser.setGeneralDiscoverySourceOtherText(generalDiscoverySourceOtherText);
    if (dbUser.getGeneralDiscoverySources().contains(DbUser.DbGeneralDiscoverySource.OTHER)
        && generalDiscoverySourceOtherText == null) {
      throw new BadRequestException(
          "General discovery source other text is required when the OTHER general discovery source is provided");
    }
    if (!dbUser.getGeneralDiscoverySources().contains(DbUser.DbGeneralDiscoverySource.OTHER)
        && generalDiscoverySourceOtherText != null) {
      throw new BadRequestException(
          "General discovery source other text must be null when the OTHER general discovery source is not provided");
    }

    dbUser.setPartnerDiscoverySources(
        discoverySourceMapper.toDbPartnerDiscoverySources(partnerDiscoverySources));
    dbUser.setPartnerDiscoverySourceOtherText(partnerDiscoverySourceOtherText);
    if (dbUser.getPartnerDiscoverySources().contains(DbUser.DbPartnerDiscoverySource.OTHER)
        && partnerDiscoverySourceOtherText == null) {
      throw new BadRequestException(
          "Partner discovery source other text is required when the OTHER partner discovery source is provided");
    }
    if (!dbUser.getPartnerDiscoverySources().contains(DbUser.DbPartnerDiscoverySource.OTHER)
        && partnerDiscoverySourceOtherText != null) {
      throw new BadRequestException(
          "Partner discovery source other text must be null when the OTHER partner discovery source is not provided");
    }
  }

  @Override
  public DbUser submitDUCC(DbUser dbUser, Integer duccSignedVersion, String initials) {
    if (!accessModuleService.isSignedDuccVersionCurrent(duccSignedVersion)) {
      throw new BadRequestException("Data User Code of Conduct Version is not up to date");
    }
    final Timestamp timestamp = clockNow();
    return updateUserWithRetries(
        (user) -> {
          accessModuleService.updateCompletionTime(
              user, DbAccessModuleName.DATA_USER_CODE_OF_CONDUCT, timestamp);
          return updateDuccAgreement(user, duccSignedVersion, initials, timestamp);
        },
        dbUser,
        Agent.asUser(dbUser));
  }

  private DbUser updateDuccAgreement(
      DbUser dbUser, Integer duccSignedVersion, String initials, Timestamp timestamp) {
    DbUserCodeOfConductAgreement ducc =
        Optional.ofNullable(dbUser.getDuccAgreement())
            .orElseGet(
                () -> {
                  DbUserCodeOfConductAgreement d = new DbUserCodeOfConductAgreement();

                  // TODO not sure if we strictly need both of these, but it shouldn't hurt
                  d.setUser(dbUser);
                  dbUser.setDuccAgreement(d);

                  return d;
                });
    ducc.setSignedVersion(duccSignedVersion);
    ducc.setUserFamilyName(dbUser.getFamilyName());
    ducc.setUserGivenName(dbUser.getGivenName());
    ducc.setUserInitials(initials);
    ducc.setCompletionTime(timestamp);
    ducc.setUserNameOutOfDate(false);
    return dbUser;
  }

  /**
   * This method is used by create Account flow, it throws exception if user has not accepted AoU
   * Terms of Service at all or has accepted the incorrect version
   *
   * @param tosVersion
   */
  @Override
  public void validateAllOfUsTermsOfService(Integer tosVersion) {
    if (tosVersion == null) {
      throw new BadRequestException("All of Us Terms of Service version is NULL");
    }
    if (tosVersion != configProvider.get().termsOfService.latestAouVersion) {
      throw new BadRequestException("All of Us Terms of Service version is not up to date");
    }
  }

  @Override
  public boolean hasSignedLatestAoUTermsOfService(@Nonnull DbUser dbUser) {
    return userTermsOfServiceDao
        .findFirstByUserIdOrderByTosVersionDesc(dbUser.getUserId())
        .map(u -> u.getTosVersion() == configProvider.get().termsOfService.latestAouVersion)
        .orElse(false);
  }

  @Override
  public boolean hasSignedLatestTermsOfServiceBoth(@Nonnull DbUser dbUser) {
    return hasSignedLatestAoUTermsOfService(dbUser) && hasSignedLatestTerraTermsOfService(dbUser);
  }

  private boolean hasSignedLatestTerraTermsOfService(@Nonnull DbUser dbUser) {
    try {
      return fireCloudService.hasUserAcceptedLatestTerraToS(dbUser);
    } catch (Exception e) {
      log.log(
          Level.SEVERE,
          String.format(
              "Error while getting Terra Terms of Service status for user %s",
              dbUser.getUsername()));
      throw new ServerErrorException(e);
    }
  }

  @Override
  @Transactional
  public void submitAouTermsOfService(@Nonnull DbUser dbUser, @Nonnull Integer tosVersion) {
    long userId = dbUser.getUserId();
    userTermsOfServiceDao.save(
        userTermsOfServiceDao
            .findFirstByUserIdOrderByTosVersionDesc(userId)
            .orElse(new DbUserTermsOfService().setUserId(userId))
            // set or update the aou tos version and agreement time
            .setTosVersion(tosVersion)
            .setAouAgreementTime(clockNow()));
    userServiceAuditor.fireAcknowledgeTermsOfService(dbUser, tosVersion);
  }

  @Override
  public void acceptTerraTermsOfService(@Nonnull DbUser dbUser) {
    fireCloudService.acceptTermsOfServiceDeprecated();
    userTermsOfServiceDao.save(
        userTermsOfServiceDao
            .findByUserIdOrThrow(dbUser.getUserId())
            .setTerraAgreementTime(clockNow()));
  }

  @Override
  public DbUser setDisabledStatus(Long userId, boolean disabled) {
    DbUser user = userDao.findUserByUserId(userId);
    return updateUserWithRetries(
        (u) -> {
          u.setDisabled(disabled);
          return u;
        },
        user,
        Agent.asAdmin(userProvider.get()));
  }

  @Override
  public List<Long> getAllUserIds() {
    return userDao.findUserIds();
  }

  @Override
  public List<DbUser> getAllUsers() {
    return userDao.findUsers();
  }

  @Override
  public List<DbUser> getAllUsersExcludingDisabled() {
    return userDao.findUsersExcludingDisabled();
  }

  /**
   * Find users whose name or username match the supplied search terms and who have the appropriate
   * access tier.
   *
   * @param term User-supplied search term
   * @param sort Option(s) for ordering query results
   * @param accessTierShortName the shortName of the access tier to check
   * @return the List of DbUsers which meet the search and access requirements
   */
  @Override
  public List<DbUser> findUsersBySearchString(String term, Sort sort, String accessTierShortName) {
    return userDao.findUsersBySearchStringAndTier(term, sort, accessTierShortName);
  }

  @Override
  public List<DbUser> findUsersByUsernames(List<String> usernames) {
    return userDao.findUserByUsernameIn(usernames);
  }

  @Override
  public Set<DbUser> findActiveUsersByUsernames(List<String> usernames) {
    return userDao.findUserByUsernameInAndDisabledFalse(usernames);
  }

  /**
   * Updates the given user's eraCommons-related fields with the NihStatus object returned from FC.
   *
   * <p>This method saves the updated user object to the database and returns it.
   */
  private DbUser setEraCommonsStatus(DbUser targetUser, FirecloudNihStatus nihStatus, Agent agent) {
    Timestamp now = clockNow();

    return updateUserWithRetries(
        user -> {
          if (nihStatus != null) {
            Timestamp eraCommonsCompletionTime =
                accessModuleService
                    .getAccessModuleStatus(user, DbAccessModuleName.ERA_COMMONS)
                    .map(AccessModuleStatus::getCompletionEpochMillis)
                    .map(Timestamp::new)
                    .orElse(null);

            Timestamp nihLinkExpireTime =
                Timestamp.from(Instant.ofEpochSecond(nihStatus.getLinkExpireTime()));

            // NihStatus should never come back from firecloud with an empty linked username.
            // If that is the case, there is an error with FC, because we should get a 404
            // in that case. Leaving the null checking in for code safety reasons

            if (nihStatus.getLinkedNihUsername() == null) {
              // If FireCloud says we have no NIH link, always clear the completion time.
              eraCommonsCompletionTime = null;
            } else if (!nihLinkExpireTime.equals(user.getEraCommonsLinkExpireTime())) {
              // If the link expiration time has changed, we treat this as a "new" completion of the
              // access requirement.
              eraCommonsCompletionTime = now;
            } else if (nihStatus.getLinkedNihUsername() != null
                && !nihStatus
                    .getLinkedNihUsername()
                    .equals(user.getEraCommonsLinkedNihUsername())) {
              // If the linked username has changed, we treat this as a new completion time.
              eraCommonsCompletionTime = now;
            } else if (eraCommonsCompletionTime == null) {
              // If the user hasn't yet completed this access requirement, set the time to now.
              eraCommonsCompletionTime = now;
            }

            user.setEraCommonsLinkedNihUsername(nihStatus.getLinkedNihUsername());
            user.setEraCommonsLinkExpireTime(nihLinkExpireTime);
            accessModuleService.updateCompletionTime(
                user, DbAccessModuleName.ERA_COMMONS, eraCommonsCompletionTime);
          } else {
            user.setEraCommonsLinkedNihUsername(null);
            user.setEraCommonsLinkExpireTime(null);
            accessModuleService.updateCompletionTime(user, DbAccessModuleName.ERA_COMMONS, null);
          }
          return user;
        },
        targetUser,
        agent);
  }

  /** Syncs the eraCommons access module status for the current user. */
  @Override
  public DbUser syncEraCommonsStatus() {
    DbUser user = userProvider.get();
    FirecloudNihStatus nihStatus = fireCloudService.getNihStatus();
    return setEraCommonsStatus(user, nihStatus, Agent.asUser(user));
  }

  @Override
  public void syncTwoFactorAuthStatus() {
    DbUser user = userProvider.get();
    syncTwoFactorAuthStatus(user, Agent.asUser(user));
  }

  @Override
  public DbUser syncTwoFactorAuthStatus(DbUser targetUser, Agent agent) {
    return syncTwoFactorAuthStatus(
        targetUser,
        agent,
        directoryService.getUserOrThrow(targetUser.getUsername()).getIsEnrolledIn2Sv());
  }

  @Override
  public DbUser syncTwoFactorAuthStatus(DbUser targetUser, Agent agent, boolean isEnrolledIn2FA) {
    if (isServiceAccount(targetUser)) {
      // Skip sync for service account user rows.
      return targetUser;
    }

    if (isEnrolledIn2FA) {
      final boolean needsDbCompletionUpdate =
          accessModuleService
              .getAccessModuleStatus(targetUser, DbAccessModuleName.TWO_FACTOR_AUTH)
              .map(status -> status.getCompletionEpochMillis() == null)
              .orElse(true);

      if (needsDbCompletionUpdate) {
        accessModuleService.updateCompletionTime(
            targetUser, DbAccessModuleName.TWO_FACTOR_AUTH, clockNow());
      }
    } else {
      accessModuleService.updateCompletionTime(
          targetUser, DbAccessModuleName.TWO_FACTOR_AUTH, null);
    }

    return accessSyncService.updateUserAccessTiers(targetUser, agent);
  }

  @Override
  public DbUser syncDuccVersionStatus(DbUser targetUser, Agent agent) {
    if (isServiceAccount(targetUser)) {
      // Skip sync for service account user rows.
      return targetUser;
    }

    if (!accessModuleService.hasUserSignedACurrentDucc(targetUser)) {
      accessModuleService.updateCompletionTime(
          targetUser, DbAccessModuleName.DATA_USER_CODE_OF_CONDUCT, null);
    }

    return accessSyncService.updateUserAccessTiers(targetUser, agent);
  }

  @Override
  public Collection<MeasurementBundle> getGaugeData() {
    return userDao.getUserCountGaugeData().stream()
        .map(
            row ->
                MeasurementBundle.builder()
                    .addMeasurement(GaugeMetric.USER_COUNT, row.getUserCount())
                    .addTag(MetricLabel.USER_DISABLED, row.getDisabled().toString())
                    .addTag(MetricLabel.ACCESS_TIER_SHORT_NAMES, row.getAccessTierShortNames())
                    .build())
        .collect(ImmutableList.toImmutableList());
  }

  @Override
  public Optional<DbUser> getByUsername(String username) {
    return Optional.ofNullable(userDao.findUserByUsername(username));
  }

  @Override
  public DbUser getByUsernameOrThrow(String username) {
    return getByUsername(username)
        .orElseThrow(() -> new NotFoundException("User '" + username + "' not found"));
  }

  @Override
  public Optional<DbUser> getByDatabaseId(long databaseId) {
    return Optional.ofNullable(userDao.findUserByUserId(databaseId));
  }

  @Override
  public boolean hasAuthority(long userId, Authority required) {
    final Set<Authority> userAuthorities =
        userDao.findUserWithAuthorities(userId).getAuthoritiesEnum();

    // DEVELOPER is the super-authority which subsumes all others
    return userAuthorities.contains(Authority.DEVELOPER) || userAuthorities.contains(required);
  }

  @Override
  public Optional<DbUser> findUserWithAuthoritiesAndPageVisits(long userId) {
    return Optional.ofNullable(userDao.findUserWithAuthoritiesAndPageVisits(userId));
  }

  @Override
  public DbUser updateIdentityStatus(String userName) {
    DbUser dbUser = userProvider.get();

    return updateUserWithRetries(
        user -> {
          user.setRasLinkUsername(userName);
          accessModuleService.updateCompletionTime(user, DbAccessModuleName.IDENTITY, clockNow());
          return user;
        },
        dbUser,
        Agent.asUser(dbUser));
  }

  @Override
  public DbUser updateRasLinkLoginGovStatus(String loginGovUserName) {
    DbUser dbUser = userProvider.get();

    return updateUserWithRetries(
        user -> {
          user.setRasLinkUsername(loginGovUserName);
          accessModuleService.updateCompletionTime(
              user, DbAccessModuleName.RAS_LOGIN_GOV, clockNow());
          return user;
        },
        dbUser,
        Agent.asUser(dbUser));
  }

  @Override
  public DbUser updateRasLinkEraStatus(String eRACommonsUsername) {
    DbUser dbUser = userProvider.get();

    return updateUserWithRetries(
        user -> {
          user.setEraCommonsLinkedNihUsername(eRACommonsUsername);
          accessModuleService.updateCompletionTime(
              user, DbAccessModuleName.ERA_COMMONS, clockNow());
          return user;
        },
        dbUser,
        Agent.asUser(dbUser));
  }

  /** Confirm that a user's profile is up to date, for annual renewal compliance purposes. */
  @Override
  public DbUser confirmProfile(DbUser dbUser) {
    accessModuleService.updateCompletionTime(
        dbUser, DbAccessModuleName.PROFILE_CONFIRMATION, clockNow());

    return accessSyncService.updateUserAccessTiers(dbUser, Agent.asUser(dbUser));
  }

  /** Confirm that a user has either reported any AoU-related publications, or has none. */
  @Override
  public DbUser confirmPublications() {
    final DbUser dbUser = userProvider.get();
    accessModuleService.updateCompletionTime(
        dbUser, DbAccessModuleName.PUBLICATION_CONFIRMATION, clockNow());

    return accessSyncService.updateUserAccessTiers(dbUser, Agent.asUser(dbUser));
  }

  // Send Access Renewal Expiration or Warning emails to the user, if appropriate
  @Override
  public void maybeSendAccessTierExpirationEmails(DbUser user) {
    maybeSendAccessTierExpirationEmail(user, REGISTERED_TIER_SHORT_NAME);
    maybeSendAccessTierExpirationEmail(user, CONTROLLED_TIER_SHORT_NAME);
  }

  @Override
  public void signOut(DbUser user) {
    directoryService.signOut(user.getUsername());
  }

  /**
   * Return the user's access expiration time for a given set of modules, for the purpose of sending
   * an access renewal reminder or expiration email.
   *
   * <p>First: ignore any bypassed modules. These are in compliance and do not need to be renewed.
   *
   * <p>Next: do all un-bypassed modules have expiration times? If yes, return the min (earliest).
   * If no, the user does not have access for reasons other than access renewal compliance. In this
   * case, we should not send an email.
   *
   * <p>Note that this method may return EMPTY for both valid and invalid users, so this method
   * SHOULD NOT BE USED FOR ACCESS DECISIONS.
   */
  private Optional<Timestamp> getTierExpirationForEmails(
      DbUser user, Collection<DbAccessModuleName> requiredModules) {
    // Collection<Optional<T>> is usually a code smell.
    // Here we do need to know if any are EMPTY, for the next step.
    Set<Optional<Long>> expirations =
        accessModuleService.getAccessModuleStatus(user).stream()
            .filter(a -> a.getBypassEpochMillis() == null)
            .filter(
                a ->
                    requiredModules.contains(
                        accessModuleNameMapper.clientAccessModuleToStorage(a.getModuleName())))
            .map(a -> Optional.ofNullable(a.getExpirationEpochMillis()))
            .collect(Collectors.toSet());

    // if any un-bypassed modules are incomplete, we know:
    // * this user does not currently have access
    // * this user has never previously had access
    // therefore: the user is neither "expired" nor "expiring" and we should not send an email

    if (!expirations.stream().allMatch(Optional::isPresent)) {
      return Optional.empty();
    } else {
      return expirations.stream()
          .map(Optional::get)
          // note: min() returns EMPTY if the stream is empty at this point,
          // which is also an indicator that we should not send an email
          .min(Long::compareTo)
          .map(t -> Timestamp.from(Instant.ofEpochMilli(t)));
    }
  }

  private void maybeSendAccessTierExpirationEmail(
      DbUser user, Timestamp expiration, String tierShortName) {
    long millisRemaining = expiration.getTime() - clock.millis();
    long daysRemaining = TimeUnit.DAYS.convert(millisRemaining, TimeUnit.MILLISECONDS);

    final List<Long> thresholds = configProvider.get().access.renewal.expiryDaysWarningThresholds;
    try {
      // we only want to send the expiration email on the day of the actual expiration
      if (millisRemaining < 0 && daysRemaining == 0) {
        mailService.alertUserAccessTierExpiration(user, expiration.toInstant(), tierShortName);
      } else {
        if (thresholds.contains(daysRemaining)) {
          mailService.alertUserAccessTierWarningThreshold(
              user, daysRemaining, expiration.toInstant(), tierShortName);
        }
      }
    } catch (final MessagingException e) {
      log.log(Level.WARNING, e.getMessage());
    }
  }

  private Timestamp clockNow() {
    return new Timestamp(clock.instant().toEpochMilli());
  }

  // Send an Access Renewal Expiration or Warning email to the user, if appropriate
  private void maybeSendAccessTierExpirationEmail(DbUser user, String tierShortName) {
    final Collection<DbAccessModuleName> moduleNames;
    switch (tierShortName) {
      case REGISTERED_TIER_SHORT_NAME:
        moduleNames = getRequiredModulesForRegisteredTierRenewal();
        break;
      case CONTROLLED_TIER_SHORT_NAME:
        moduleNames = getRequiredModulesForControlledTierRenewal();
        break;
      default:
        moduleNames = Collections.emptyList();
    }

    getTierExpirationForEmails(user, moduleNames)
        .ifPresent(
            expiration -> maybeSendAccessTierExpirationEmail(user, expiration, tierShortName));
  }
}
