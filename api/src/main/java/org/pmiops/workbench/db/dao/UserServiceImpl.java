package org.pmiops.workbench.db.dao;

import com.google.api.client.http.HttpStatusCodes;
import com.google.api.services.oauth2.model.Userinfoplus;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.inject.Provider;
import javax.mail.MessagingException;
import org.hibernate.exception.GenericJDBCException;
import org.javers.common.collections.Lists;
import org.pmiops.workbench.access.AccessTierService;
import org.pmiops.workbench.actionaudit.Agent;
import org.pmiops.workbench.actionaudit.auditors.UserServiceAuditor;
import org.pmiops.workbench.actionaudit.targetproperties.BypassTimeTargetProperty;
import org.pmiops.workbench.compliance.ComplianceService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.config.WorkbenchConfig.AccessConfig;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbAddress;
import org.pmiops.workbench.db.model.DbAdminActionHistory;
import org.pmiops.workbench.db.model.DbDemographicSurvey;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserDataUseAgreement;
import org.pmiops.workbench.db.model.DbUserTermsOfService;
import org.pmiops.workbench.db.model.DbVerifiedInstitutionalAffiliation;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.firecloud.ApiClient;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.api.NihApi;
import org.pmiops.workbench.firecloud.model.FirecloudNihStatus;
import org.pmiops.workbench.google.DirectoryService;
import org.pmiops.workbench.mail.MailService;
import org.pmiops.workbench.model.AccessBypassRequest;
import org.pmiops.workbench.model.Authority;
import org.pmiops.workbench.model.Degree;
import org.pmiops.workbench.model.RenewableAccessModuleStatus;
import org.pmiops.workbench.model.RenewableAccessModuleStatus.ModuleNameEnum;
import org.pmiops.workbench.model.UserAccessExpiration;
import org.pmiops.workbench.monitoring.GaugeDataCollector;
import org.pmiops.workbench.monitoring.MeasurementBundle;
import org.pmiops.workbench.monitoring.labels.MetricLabel;
import org.pmiops.workbench.monitoring.views.GaugeMetric;
import org.pmiops.workbench.moodle.model.BadgeDetailsV2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
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
  private static final int CURRENT_TERMS_OF_SERVICE_VERSION = 1;
  private static final long MIN_ACCESS_EXPIRATION_EPOCH_MS =
      Instant.parse("2021-07-01T00:00:00.00Z").toEpochMilli();

  private final Provider<WorkbenchConfig> configProvider;
  private final Provider<DbUser> userProvider;
  private final Clock clock;
  private final Random random;
  private final UserServiceAuditor userServiceAuditor;

  private final UserDao userDao;
  private final AdminActionHistoryDao adminActionHistoryDao;
  private final UserDataUseAgreementDao userDataUseAgreementDao;
  private final UserTermsOfServiceDao userTermsOfServiceDao;
  private final VerifiedInstitutionalAffiliationDao verifiedInstitutionalAffiliationDao;

  private final AccessTierService accessTierService;
  private final ComplianceService complianceService;
  private final DirectoryService directoryService;
  private final FireCloudService fireCloudService;
  private final MailService mailService;

  private static final Logger log = Logger.getLogger(UserServiceImpl.class.getName());

  @Autowired
  public UserServiceImpl(
      Provider<WorkbenchConfig> configProvider,
      Provider<DbUser> userProvider,
      Clock clock,
      Random random,
      UserServiceAuditor userServiceAuditor,
      UserDao userDao,
      AdminActionHistoryDao adminActionHistoryDao,
      UserDataUseAgreementDao userDataUseAgreementDao,
      UserTermsOfServiceDao userTermsOfServiceDao,
      VerifiedInstitutionalAffiliationDao verifiedInstitutionalAffiliationDao,
      FireCloudService fireCloudService,
      ComplianceService complianceService,
      DirectoryService directoryService,
      AccessTierService accessTierService,
      MailService mailService) {
    this.configProvider = configProvider;
    this.userProvider = userProvider;
    this.clock = clock;
    this.random = random;
    this.userServiceAuditor = userServiceAuditor;
    this.userDao = userDao;
    this.adminActionHistoryDao = adminActionHistoryDao;
    this.userDataUseAgreementDao = userDataUseAgreementDao;
    this.userTermsOfServiceDao = userTermsOfServiceDao;
    this.verifiedInstitutionalAffiliationDao = verifiedInstitutionalAffiliationDao;
    this.fireCloudService = fireCloudService;
    this.complianceService = complianceService;
    this.directoryService = directoryService;
    this.accessTierService = accessTierService;
    this.mailService = mailService;
  }

  @VisibleForTesting
  @Override
  public int getCurrentDuccVersion() {
    return configProvider.get().featureFlags.enableV3DataUserCodeOfConduct ? 3 : 2;
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
      updateUserAccessTiers(dbUser, agent);
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

  private void updateUserAccessTiers(DbUser dbUser, Agent agent) {
    final List<DbAccessTier> previousAccessTiers = accessTierService.getAccessTiersForUser(dbUser);

    // TODO for Controlled Tier Beta: different access module evaluation criteria
    // For Controlled Tier Alpha, we simply evaluate whether the user is qualified for
    // Registered Tier and set RT+CT or RT only based on the feature flag

    final List<DbAccessTier> newAccessTiers =
        shouldUserBeRegistered(dbUser)
            ? accessTierService.getTiersForRegisteredUsers()
            : Collections.emptyList();

    if (!newAccessTiers.equals(previousAccessTiers)) {
      userServiceAuditor.fireUpdateAccessTiersAction(
          dbUser, previousAccessTiers, newAccessTiers, agent);
    }

    // add user to each Access Tier DB table and the tiers' Terra Auth Domains
    newAccessTiers.forEach(tier -> accessTierService.addUserToTier(dbUser, tier));

    // remove user from all other Access Tier DB tables and the tiers' Terra Auth Domains
    final List<DbAccessTier> tiersForRemoval =
        Lists.difference(accessTierService.getAllTiers(), newAccessTiers);
    tiersForRemoval.forEach(tier -> accessTierService.removeUserFromTier(dbUser, tier));
  }

  private class ModuleTimes {
    public Optional<Timestamp> completion;
    public Optional<Timestamp> bypass;

    public ModuleTimes(Timestamp nullableCompletion, Timestamp nullableBypass) {
      completion = Optional.ofNullable(nullableCompletion);
      bypass = Optional.ofNullable(nullableBypass);
    }

    public boolean isBypassed() {
      return bypass.isPresent();
    }

    public boolean isComplete() {
      return completion.isPresent();
    }

    public Optional<Timestamp> getExpiration() {
      if (isBypassed() || !configProvider.get().access.enableAccessRenewal) {
        return Optional.empty();
      }
      Long expiryDays = configProvider.get().accessRenewal.expiryDays;
      Preconditions.checkNotNull(
          expiryDays, "expected value for config key accessRenewal.expiryDays.expiryDays");
      long expiryDaysInMs = TimeUnit.MILLISECONDS.convert(expiryDays, TimeUnit.DAYS);
      return completion.map(
          c ->
              new Timestamp(
                  Math.max(c.getTime() + expiryDaysInMs, MIN_ACCESS_EXPIRATION_EPOCH_MS)));
    }

    public boolean hasExpired() {
      Preconditions.checkArgument(
          isComplete(), "Cannot check expiration on module that has not been completed");
      final Timestamp now = new Timestamp(clock.millis());
      if (now.before(new Timestamp(MIN_ACCESS_EXPIRATION_EPOCH_MS))) {
        return false;
      }
      return getExpiration().map(x -> x.before(now)).orElse(false);
    }
  }

  // TODO split into registered tier and controlled tier versions, when available
  private Map<ModuleNameEnum, ModuleTimes> getRenewableAccessModules(DbUser user) {
    return ImmutableMap.of(
        ModuleNameEnum.COMPLIANCETRAINING,
            new ModuleTimes(
                user.getComplianceTrainingCompletionTime(), user.getComplianceTrainingBypassTime()),
        ModuleNameEnum.DATAUSEAGREEMENT,
            new ModuleTimes(
                user.getDataUseAgreementCompletionTime(), user.getDataUseAgreementBypassTime()),
        ModuleNameEnum.PROFILECONFIRMATION,
            new ModuleTimes(user.getProfileLastConfirmedTime(), null),
        ModuleNameEnum.PUBLICATIONCONFIRMATION,
            new ModuleTimes(user.getPublicationsLastConfirmedTime(), null));
  }

  private RenewableAccessModuleStatus mkStatus(ModuleNameEnum name, ModuleTimes times) {
    return new RenewableAccessModuleStatus()
        .moduleName(name)
        .expirationEpochMillis(times.getExpiration().map(Timestamp::getTime).orElse(null))
        .hasExpired(times.isComplete() && times.hasExpired());
  }

  public List<RenewableAccessModuleStatus> getRenewableAccessModuleStatus(DbUser user) {
    return getRenewableAccessModules(user).entrySet().stream()
        .map(x -> mkStatus(x.getKey(), x.getValue()))
        .collect(Collectors.toList());
  }

  private boolean isDataUseAgreementCompliant(DbUser user) {
    final ModuleTimes duaTimes =
        new ModuleTimes(
            user.getDataUseAgreementCompletionTime(), user.getDataUseAgreementBypassTime());
    if (!configProvider.get().access.enableDataUseAgreement) {
      return true;
    }
    if (duaTimes.isBypassed()) {
      return true;
    }
    final Integer signedVersion = user.getDataUseAgreementSignedVersion();
    if (signedVersion == null || signedVersion != getCurrentDuccVersion()) {
      return false;
    }
    return duaTimes.isComplete() && !duaTimes.hasExpired();
  }

  // Checking for annual completion time is not a part of this module
  private boolean isEraCommonsCompliant(DbUser user) {
    return user.getEraCommonsBypassTime() != null
        || !configProvider.get().access.enableEraCommons
        || user.getEraCommonsCompletionTime() != null;
  }

  private boolean isComplianceTrainingCompliant(DbUser user) {
    if (!configProvider.get().access.enableComplianceTraining) {
      return true;
    }
    final ModuleTimes ctTimes =
        new ModuleTimes(
            user.getComplianceTrainingCompletionTime(), user.getComplianceTrainingBypassTime());
    return ctTimes.isBypassed() || (ctTimes.isComplete() && !ctTimes.hasExpired());
  }

  // these 2 modules will not be checked at all until we flip the feature flag

  private boolean isProfileConfirmationCompliant(DbUser user) {
    if (!configProvider.get().access.enableAccessRenewal) {
      return true;
    }

    final ModuleTimes profileTimes = new ModuleTimes(user.getProfileLastConfirmedTime(), null);
    return profileTimes.isComplete() && !profileTimes.hasExpired();
  }

  private boolean isPublicationConfirmationCompliant(DbUser user) {
    if (!configProvider.get().access.enableAccessRenewal) {
      return true;
    }

    final ModuleTimes publicationTimes =
        new ModuleTimes(user.getPublicationsLastConfirmedTime(), null);
    return publicationTimes.isComplete() && !publicationTimes.hasExpired();
  }

  private boolean shouldUserBeRegistered(DbUser user) {
    // 2FA does not need to be checked for annual renewal
    boolean twoFactorAuthComplete =
        user.getTwoFactorAuthCompletionTime() != null || user.getTwoFactorAuthBypassTime() != null;
    // TODO: can take out other checks once we're entirely moved over to the 'module' columns
    return !user.getDisabled()
        && isComplianceTrainingCompliant(user)
        && isEraCommonsCompliant(user)
        && twoFactorAuthComplete
        && isDataUseAgreementCompliant(user)
        && isPublicationConfirmationCompliant(user)
        && isProfileConfirmationCompliant(user);
  }

  private boolean isServiceAccount(DbUser user) {
    return configProvider.get().auth.serviceAccountApiUsers.contains(user.getUsername());
  }

  @Override
  public DbUser createServiceAccountUser(String username) {
    DbUser user = new DbUser();
    user.setUsername(username);
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
      final Userinfoplus userInfo,
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
      List<Degree> degrees,
      DbAddress dbAddress,
      DbDemographicSurvey dbDemographicSurvey,
      DbVerifiedInstitutionalAffiliation dbVerifiedAffiliation) {
    DbUser dbUser = new DbUser();
    Timestamp now = new Timestamp(clock.instant().toEpochMilli());
    dbUser.setCreationNonce(Math.abs(random.nextLong()));
    dbUser.setUsername(username);
    dbUser.setContactEmail(contactEmail);
    dbUser.setAreaOfResearch(areaOfResearch);
    dbUser.setFamilyName(familyName);
    dbUser.setGivenName(givenName);
    dbUser.setProfessionalUrl(professionalUrl);
    dbUser.setDisabled(false);
    dbUser.setAboutYou(null);
    dbUser.setAddress(dbAddress);
    dbUser.setProfileLastConfirmedTime(now);
    dbUser.setPublicationsLastConfirmedTime(now);
    if (degrees != null) {
      dbUser.setDegreesEnum(degrees);
    }
    dbUser.setDemographicSurvey(dbDemographicSurvey);

    // For existing user that do not have address
    if (dbAddress != null) {
      dbAddress.setUser(dbUser);
    }
    if (dbDemographicSurvey != null) {
      dbDemographicSurvey.setUser(dbUser);
    }

    try {
      dbUser = userDao.save(dbUser);
      dbVerifiedAffiliation.setUser(dbUser);
      verifiedInstitutionalAffiliationDao.save(dbVerifiedAffiliation);
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

  /**
   * Save updated dbUser object and transform ObjectOptimisticLockingFailureException into
   * ConflictException
   *
   * @param dbUser
   * @return
   */
  @Override
  public DbUser updateUserWithConflictHandling(DbUser dbUser) {
    try {
      dbUser = userDao.save(dbUser);
    } catch (ObjectOptimisticLockingFailureException e) {
      log.log(Level.WARNING, "version conflict for user update", e);
      throw new ConflictException("Failed due to concurrent modification");
    }
    return dbUser;
  }

  @Override
  public DbUser submitDataUseAgreement(
      DbUser dbUser, Integer dataUseAgreementSignedVersion, String initials) {
    // FIXME: this should not be hardcoded
    if (dataUseAgreementSignedVersion != getCurrentDuccVersion()) {
      throw new BadRequestException("Data Use Agreement Version is not up to date");
    }
    final Timestamp timestamp = new Timestamp(clock.instant().toEpochMilli());
    DbUserDataUseAgreement dataUseAgreement = new DbUserDataUseAgreement();
    dataUseAgreement.setDataUseAgreementSignedVersion(dataUseAgreementSignedVersion);
    dataUseAgreement.setUserId(dbUser.getUserId());
    dataUseAgreement.setUserFamilyName(dbUser.getFamilyName());
    dataUseAgreement.setUserGivenName(dbUser.getGivenName());
    dataUseAgreement.setUserInitials(initials);
    dataUseAgreement.setCompletionTime(timestamp);
    userDataUseAgreementDao.save(dataUseAgreement);
    return updateUserWithRetries(
        (user) -> {
          // TODO: Teardown/reconcile duplicated state between the user profile and DUA.
          user.setDataUseAgreementCompletionTime(timestamp);
          user.setDataUseAgreementSignedVersion(dataUseAgreementSignedVersion);
          return user;
        },
        dbUser,
        Agent.asUser(dbUser));
  }

  @Override
  @Transactional
  public void setDataUseAgreementNameOutOfDate(String newGivenName, String newFamilyName) {
    List<DbUserDataUseAgreement> dataUseAgreements =
        userDataUseAgreementDao.findByUserIdOrderByCompletionTimeDesc(
            userProvider.get().getUserId());
    dataUseAgreements.forEach(
        dua ->
            dua.setUserNameOutOfDate(
                !dua.getUserGivenName().equalsIgnoreCase(newGivenName)
                    || !dua.getUserFamilyName().equalsIgnoreCase(newFamilyName)));
    userDataUseAgreementDao.saveAll(dataUseAgreements);
  }

  @Override
  @Transactional
  public void submitTermsOfService(DbUser dbUser, @Nonnull Integer tosVersion) {

    // Validates a given tosVersion, by running all validation checks.
    if (tosVersion == null) {
      throw new BadRequestException("Terms of Service version is NULL");
    }
    if (tosVersion != CURRENT_TERMS_OF_SERVICE_VERSION) {
      throw new BadRequestException("Terms of Service version is not up to date");
    }

    DbUserTermsOfService userTermsOfService = new DbUserTermsOfService();
    userTermsOfService.setTosVersion(tosVersion);
    userTermsOfService.setUserId(dbUser.getUserId());
    userTermsOfServiceDao.save(userTermsOfService);
    userServiceAuditor.fireAcknowledgeTermsOfService(dbUser, tosVersion);
  }

  @Override
  public void setDataUseAgreementBypassTime(
      Long userId, Timestamp previousBypassTime, Timestamp newBypassTime) {
    setBypassTimeWithRetries(
        userId,
        previousBypassTime,
        newBypassTime,
        DbUser::setDataUseAgreementBypassTime,
        BypassTimeTargetProperty.DATA_USE_AGREEMENT_BYPASS_TIME);
  }

  @Override
  public void setComplianceTrainingBypassTime(
      Long userId, Timestamp previousBypassTime, Timestamp newBypassTime) {
    setBypassTimeWithRetries(
        userId,
        previousBypassTime,
        newBypassTime,
        DbUser::setComplianceTrainingBypassTime,
        BypassTimeTargetProperty.COMPLIANCE_TRAINING_BYPASS_TIME);
  }

  @Override
  public void setEraCommonsBypassTime(
      Long userId, Timestamp previousBypassTime, Timestamp newBypassTime) {
    setBypassTimeWithRetries(
        userId,
        previousBypassTime,
        newBypassTime,
        DbUser::setEraCommonsBypassTime,
        BypassTimeTargetProperty.ERA_COMMONS_BYPASS_TIME);
  }

  @Override
  public void setTwoFactorAuthBypassTime(
      Long userId, Timestamp previousBypassTime, Timestamp newBypassTime) {
    setBypassTimeWithRetries(
        userId,
        previousBypassTime,
        newBypassTime,
        DbUser::setTwoFactorAuthBypassTime,
        BypassTimeTargetProperty.TWO_FACTOR_AUTH_BYPASS_TIME);
  }

  @Override
  public void setRasLinkLoginGovBypassTime(
      Long userId, Timestamp previousBypassTime, Timestamp newBypassTime) {
    setBypassTimeWithRetries(
        userId,
        previousBypassTime,
        newBypassTime,
        DbUser::setRasLinkLoginGovBypassTime,
        BypassTimeTargetProperty.RAS_LINK_LOGIN_GOV);
  }

  /**
   * Functional bypass time column setter, using retry logic.
   *
   * @param userId id of user getting bypassed
   * @param previousBypassTime time of bypass, before update
   * @param newBypassTime time of bypass
   * @param setter void-returning method to call to set the particular bypass field. Should
   *     typically be a method reference on DbUser, e.g.
   * @param targetProperty BypassTimeTargetProperty enum value, for auditing
   */
  private void setBypassTimeWithRetries(
      long userId,
      Timestamp previousBypassTime,
      Timestamp newBypassTime,
      BiConsumer<DbUser, Timestamp> setter,
      BypassTimeTargetProperty targetProperty) {
    setBypassTimeWithRetries(
        userDao.findUserByUserId(userId),
        previousBypassTime,
        newBypassTime,
        targetProperty,
        setter);
  }

  private void setBypassTimeWithRetries(
      DbUser dbUser,
      Timestamp previousBypassTime,
      Timestamp newBypassTime,
      BypassTimeTargetProperty targetProperty,
      BiConsumer<DbUser, Timestamp> setter) {
    updateUserWithRetries(
        (u) -> {
          setter.accept(u, newBypassTime);
          return u;
        },
        dbUser,
        Agent.asAdmin(userProvider.get()));
    userServiceAuditor.fireAdministrativeBypassTime(
        dbUser.getUserId(),
        targetProperty,
        Optional.ofNullable(previousBypassTime).map(Timestamp::toInstant),
        Optional.ofNullable(newBypassTime).map(Timestamp::toInstant));
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

  @Override
  public void logAdminUserAction(
      long targetUserId, String targetAction, Object oldValue, Object newValue) {
    logAdminAction(targetUserId, null, targetAction, oldValue, newValue);
  }

  @Override
  public void logAdminWorkspaceAction(
      long targetWorkspaceId, String targetAction, Object oldValue, Object newValue) {
    logAdminAction(null, targetWorkspaceId, targetAction, oldValue, newValue);
  }

  private void logAdminAction(
      Long targetUserId,
      Long targetWorkspaceId,
      String targetAction,
      Object oldValue,
      Object newValue) {
    DbAdminActionHistory adminActionHistory = new DbAdminActionHistory();
    adminActionHistory.setTargetUserId(targetUserId);
    adminActionHistory.setTargetWorkspaceId(targetWorkspaceId);
    adminActionHistory.setTargetAction(targetAction);
    adminActionHistory.setOldValue(oldValue == null ? "null" : oldValue.toString());
    adminActionHistory.setNewValue(newValue == null ? "null" : newValue.toString());
    adminActionHistory.setAdminUserId(userProvider.get().getUserId());
    adminActionHistory.setTimestamp();
    adminActionHistoryDao.save(adminActionHistory);
  }

  /**
   * Find users with Registered Tier access whose name or username match the supplied search terms.
   *
   * @param term User-supplied search term
   * @param sort Option(s) for ordering query results
   * @return the List of DbUsers which meet the search and access requirements
   * @deprecated use {@link UserService#findUsersBySearchString(String, Sort, String)} instead.
   */
  @Deprecated
  @Override
  public List<DbUser> findUsersBySearchString(String term, Sort sort) {
    return findUsersBySearchString(term, sort, accessTierService.REGISTERED_TIER_SHORT_NAME);
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

  /** Syncs the current user's training status from Moodle. */
  @Override
  public DbUser syncComplianceTrainingStatusV2()
      throws org.pmiops.workbench.moodle.ApiException, NotFoundException {
    DbUser user = userProvider.get();
    return syncComplianceTrainingStatusV2(user, Agent.asUser(user));
  }

  /**
   * Updates the given user's training status from Moodle.
   *
   * <p>We can fetch Moodle data for arbitrary users since we use an API key to access Moodle,
   * rather than user-specific OAuth tokens.
   *
   * <p>Using the user's email, we can get their badges from Moodle's APIs. If the badges are marked
   * valid, we store their completion/expiration dates in the database. If they are marked invalid,
   * we clear the completion/expiration dates from the database as the user will need to complete a
   * new training.
   */
  @Override
  public DbUser syncComplianceTrainingStatusV2(DbUser dbUser, Agent agent)
      throws org.pmiops.workbench.moodle.ApiException, NotFoundException {
    // Skip sync for service account user rows.
    if (isServiceAccount(dbUser)) {
      return dbUser;
    }

    try {
      Timestamp now = new Timestamp(clock.instant().toEpochMilli());
      final Timestamp newComplianceTrainingCompletionTime;
      final Timestamp newComplianceTrainingExpirationTime;
      Map<String, BadgeDetailsV2> userBadgesByName =
          complianceService.getUserBadgesByBadgeName(dbUser.getUsername());
      if (userBadgesByName.containsKey(complianceService.getResearchEthicsTrainingField())) {
        BadgeDetailsV2 complianceBadge =
            userBadgesByName.get(complianceService.getResearchEthicsTrainingField());
        if (complianceBadge.getValid()) {
          if (dbUser.getComplianceTrainingCompletionTime() == null) {
            // The badge was previously invalid and is now valid.
            newComplianceTrainingCompletionTime = now;
          } else if (!dbUser
              .getComplianceTrainingExpirationTime()
              .equals(Timestamp.from(Instant.ofEpochSecond(complianceBadge.getDateexpire())))) {
            // The badge was previously valid, but has a new expiration date (and so is a new
            // training)
            newComplianceTrainingCompletionTime = now;
          } else {
            // The badge status has not changed since the last time the status was synced.
            newComplianceTrainingCompletionTime = dbUser.getComplianceTrainingCompletionTime();
          }
          // Always update the expiration time if the training badge is valid
          newComplianceTrainingExpirationTime =
              Timestamp.from(Instant.ofEpochSecond(complianceBadge.getDateexpire()));
        } else {
          // The current badge is invalid or expired, the training must be completed or retaken.
          newComplianceTrainingCompletionTime = null;
          newComplianceTrainingExpirationTime = null;
        }
      } else {
        // There is no record of this person having taken the training.
        newComplianceTrainingCompletionTime = null;
        newComplianceTrainingExpirationTime = null;
      }

      return updateUserWithRetries(
          u -> {
            u.setComplianceTrainingCompletionTime(newComplianceTrainingCompletionTime);
            u.setComplianceTrainingExpirationTime(newComplianceTrainingExpirationTime);
            return u;
          },
          dbUser,
          agent);
    } catch (NumberFormatException e) {
      log.severe("Incorrect date expire format from Moodle");
      throw e;
    } catch (org.pmiops.workbench.moodle.ApiException ex) {
      if (ex.getCode() == HttpStatus.NOT_FOUND.value()) {
        log.severe(
            String.format(
                "Error while querying Moodle for badges for %s: %s ",
                dbUser.getUsername(), ex.getMessage()));
        throw new NotFoundException(ex.getMessage());
      } else {
        log.severe(String.format("Error while syncing compliance training: %s", ex.getMessage()));
      }
      throw ex;
    }
  }

  /**
   * Updates the given user's eraCommons-related fields with the NihStatus object returned from FC.
   *
   * <p>This method saves the updated user object to the database and returns it.
   */
  private DbUser setEraCommonsStatus(DbUser targetUser, FirecloudNihStatus nihStatus, Agent agent) {
    Timestamp now = new Timestamp(clock.instant().toEpochMilli());

    return updateUserWithRetries(
        user -> {
          if (nihStatus != null) {
            Timestamp eraCommonsCompletionTime = user.getEraCommonsCompletionTime();
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
            user.setEraCommonsCompletionTime(eraCommonsCompletionTime);
          } else {
            user.setEraCommonsLinkedNihUsername(null);
            user.setEraCommonsLinkExpireTime(null);
            user.setEraCommonsCompletionTime(null);
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

  /**
   * Syncs the eraCommons access module status for an arbitrary user.
   *
   * <p>This uses impersonated credentials and should only be called in the context of a cron job or
   * a request from a user with elevated privileges.
   *
   * <p>Returns the updated User object.
   */
  @Override
  public DbUser syncEraCommonsStatusUsingImpersonation(DbUser user, Agent agent)
      throws IOException, org.pmiops.workbench.firecloud.ApiException {
    if (isServiceAccount(user)) {
      // Skip sync for service account user rows.
      return user;
    }

    ApiClient apiClient = fireCloudService.getApiClientWithImpersonation(user.getUsername());
    NihApi api = new NihApi(apiClient);
    try {
      FirecloudNihStatus nihStatus = api.nihStatus();
      return setEraCommonsStatus(user, nihStatus, agent);
    } catch (org.pmiops.workbench.firecloud.ApiException e) {
      if (e.getCode() == HttpStatusCodes.STATUS_CODE_NOT_FOUND) {
        // We'll catch the NOT_FOUND ApiException here, since we expect many users to have an empty
        // eRA Commons linkage.
        log.info(String.format("NIH Status not found for user %s", user.getUsername()));
        return user;
      } else {
        throw e;
      }
    }
  }

  @Override
  public void syncTwoFactorAuthStatus() {
    DbUser user = userProvider.get();
    syncTwoFactorAuthStatus(user, Agent.asUser(user));
  }

  @Override
  public DbUser syncTwoFactorAuthStatus(DbUser targetUser, Agent agent) {
    return syncTwoFactorAuthStatus(
        targetUser, agent, directoryService.getUser(targetUser.getUsername()).getIsEnrolledIn2Sv());
  }

  @Override
  public DbUser syncTwoFactorAuthStatus(DbUser targetUser, Agent agent, boolean isEnrolledIn2FA) {
    if (isServiceAccount(targetUser)) {
      // Skip sync for service account user rows.
      return targetUser;
    }

    return updateUserWithRetries(
        user -> {
          if (isEnrolledIn2FA) {
            if (user.getTwoFactorAuthCompletionTime() == null) {
              user.setTwoFactorAuthCompletionTime(new Timestamp(clock.instant().toEpochMilli()));
            }
          } else {
            user.setTwoFactorAuthCompletionTime(null);
          }
          return user;
        },
        targetUser,
        agent);
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
  public void updateBypassTime(long userDatabaseId, AccessBypassRequest accessBypassRequest) {
    final DbUser user =
        getByDatabaseId(userDatabaseId)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        String.format("User with database ID %d not found", userDatabaseId)));

    final Timestamp previousBypassTime;
    final Timestamp newBypassTime;

    final Boolean isBypassed = accessBypassRequest.getIsBypassed();
    if (isBypassed) {
      newBypassTime = new Timestamp(clock.instant().toEpochMilli());
    } else {
      newBypassTime = null;
    }
    switch (accessBypassRequest.getModuleName()) {
      case DATA_USE_AGREEMENT:
        previousBypassTime = user.getDataUseAgreementBypassTime();
        setDataUseAgreementBypassTime(userDatabaseId, previousBypassTime, newBypassTime);
        break;
      case COMPLIANCE_TRAINING:
        previousBypassTime = user.getComplianceTrainingBypassTime();
        setComplianceTrainingBypassTime(userDatabaseId, previousBypassTime, newBypassTime);
        break;
      case ERA_COMMONS:
        previousBypassTime = user.getEraCommonsBypassTime();
        setEraCommonsBypassTime(userDatabaseId, previousBypassTime, newBypassTime);
        break;
      case TWO_FACTOR_AUTH:
        previousBypassTime = user.getTwoFactorAuthBypassTime();
        setTwoFactorAuthBypassTime(userDatabaseId, previousBypassTime, newBypassTime);
        break;
      case RAS_LINK_LOGIN_GOV:
        previousBypassTime = user.getRasLinkLoginGovBypassTime();
        setRasLinkLoginGovBypassTime(userDatabaseId, previousBypassTime, newBypassTime);
        break;
      default:
        throw new BadRequestException(
            "There is no access module named: " + accessBypassRequest.getModuleName().toString());
    }
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
  public DbUser updateRasLinkLoginGovStatus(String loginGovUserName) {
    DbUser dbUser = userProvider.get();

    return updateUserWithRetries(
        user -> {
          user.setRasLinkLoginGovUsername(loginGovUserName);
          user.setRasLinkLoginGovCompletionTime(new Timestamp(clock.instant().toEpochMilli()));
          // TODO(RW-6480): Determine if need to set link expiration time.
          return user;
        },
        dbUser,
        Agent.asUser(dbUser));
  }

  /** Confirm that a user's profile is up to date, for annual renewal compliance purposes. */
  @Override
  public DbUser confirmProfile(DbUser dbUser) {
    return updateUserWithRetries(
        user -> {
          user.setProfileLastConfirmedTime(new Timestamp(clock.instant().toEpochMilli()));
          return user;
        },
        dbUser,
        Agent.asUser(dbUser));
  }

  /** Confirm that a user has either reported any AoU-related publications, or has none. */
  @Override
  public DbUser confirmPublications() {
    final DbUser dbUser = userProvider.get();
    return updateUserWithRetries(
        user -> {
          user.setPublicationsLastConfirmedTime(new Timestamp(clock.instant().toEpochMilli()));
          return user;
        },
        dbUser,
        Agent.asUser(dbUser));
  }

  /** Send an Access Renewal Expiration or Warning email to the user, if appropriate */
  @Override
  public void maybeSendAccessExpirationEmail(DbUser user) {
    // TODO combine with CT expiration logic when available to send AT MOST ONE email

    final Optional<Timestamp> rtExpiration = getRegisteredTierExpirationForEmails(user);
    rtExpiration.ifPresent(expiration -> maybeSendRegisteredTierExpirationEmail(user, expiration));
  }

  /**
   * Return a mapping of users to their Annual Access Renewal expiration date for Registered Tier,
   * for users who have them
   */
  @Override
  public List<UserAccessExpiration> getRegisteredTierExpirations() {
    // restrict to current RT users
    return accessTierService.getAllRegisteredTierUsers().stream()
        .flatMap(this::maybeAccessExpiration)
        .collect(Collectors.toList());
  }

  // streams a UserAccessExpiration object, if the user has an expiration
  private Stream<UserAccessExpiration> maybeAccessExpiration(DbUser user) {
    return getRegisteredTierExpirationForEmails(user)
        .map(
            exp ->
                Stream.of(
                    new UserAccessExpiration()
                        .userName(user.getUsername())
                        .contactEmail(user.getContactEmail())
                        .givenName(user.getGivenName())
                        .familyName(user.getFamilyName())
                        // converts to UTC
                        .expirationDate(exp.toInstant().toString())))
        .orElse(Stream.empty());
  }

  /**
   * Return the user's registered tier access expiration time, for the purpose of sending an access
   * renewal reminder or expiration email.
   *
   * <p>First: ignore any modules which have been disabled in this environment.
   *
   * <p>Next: ignore any bypassed modules. These are in compliance and do not need to be renewed.
   *
   * <p>Finally: do all enabled un-bypassed modules have expiration times? If yes, return the min
   * (earliest). If no, either the AAR feature flag is not set or the user does not have access for
   * reasons other than access renewal compliance. In either negative case, we should not send an
   * email.
   *
   * <p>Note that this method may return EMPTY for both valid and invalid users, so this method
   * SHOULD NOT BE USED FOR ACCESS DECISIONS.
   */
  private Optional<Timestamp> getRegisteredTierExpirationForEmails(DbUser user) {
    // Collection<Optional<T>> is usually a code smell.
    // Here we do need to know if any are EMPTY, for the next step.
    final Set<Optional<Timestamp>> expirations =
        getRenewableAccessModules(user).entrySet().stream()
            .filter(entry -> isModuleEnabledInEnvironment(entry.getKey()))
            .filter(entry -> !entry.getValue().isBypassed())
            .map(entry -> entry.getValue().getExpiration())
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
          .min(Timestamp::compareTo);
    }
  }

  private boolean isModuleEnabledInEnvironment(ModuleNameEnum moduleName) {
    final AccessConfig accessConfig = configProvider.get().access;

    switch (moduleName) {
      case COMPLIANCETRAINING:
        return accessConfig.enableComplianceTraining;
      case DATAUSEAGREEMENT:
        return accessConfig.enableDataUseAgreement;
      default:
        return true;
    }
  }

  private void maybeSendRegisteredTierExpirationEmail(DbUser user, Timestamp expiration) {
    long millisRemaining = expiration.getTime() - clock.millis();
    long daysRemaining = TimeUnit.DAYS.convert(millisRemaining, TimeUnit.MILLISECONDS);

    final List<Long> thresholds = configProvider.get().accessRenewal.expiryDaysWarningThresholds;
    try {
      // we only want to send the expiration email on the day of the actual expiration
      if (millisRemaining < 0 && daysRemaining == 0) {
        mailService.alertUserRegisteredTierExpiration(user, expiration.toInstant());
      } else {
        if (thresholds.contains(daysRemaining)) {
          mailService.alertUserRegisteredTierWarningThreshold(
              user, daysRemaining, expiration.toInstant());
        }
      }
    } catch (final MessagingException e) {
      log.log(Level.WARNING, e.getMessage());
    }
  }
}
