package org.pmiops.workbench.db.dao;

import com.google.api.client.http.HttpStatusCodes;
import com.google.api.services.oauth2.model.Userinfoplus;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Provider;
import org.hibernate.exception.GenericJDBCException;
import org.pmiops.workbench.actionaudit.Agent;
import org.pmiops.workbench.actionaudit.auditors.UserServiceAuditor;
import org.pmiops.workbench.actionaudit.targetproperties.BypassTimeTargetProperty;
import org.pmiops.workbench.compliance.ComplianceService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserDao.UserCountGaugeLabelsAndValue;
import org.pmiops.workbench.db.model.DbAddress;
import org.pmiops.workbench.db.model.DbAdminActionHistory;
import org.pmiops.workbench.db.model.DbDemographicSurvey;
import org.pmiops.workbench.db.model.DbInstitutionalAffiliation;
import org.pmiops.workbench.db.model.DbStorageEnums;
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
import org.pmiops.workbench.model.DataAccessLevel;
import org.pmiops.workbench.model.Degree;
import org.pmiops.workbench.model.EmailVerificationStatus;
import org.pmiops.workbench.monitoring.GaugeDataCollector;
import org.pmiops.workbench.monitoring.MeasurementBundle;
import org.pmiops.workbench.monitoring.labels.MetricLabel;
import org.pmiops.workbench.monitoring.views.GaugeMetric;
import org.pmiops.workbench.moodle.model.BadgeDetailsV1;
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
 * ensuring we call a single updateDataAccessLevel method whenever a User entry is saved.
 */
@Service
public class UserServiceImpl implements UserService, GaugeDataCollector {

  private final int MAX_RETRIES = 3;
  private static final int CURRENT_TERMS_OF_SERVICE_VERSION = 1;

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

  private final FireCloudService fireCloudService;
  private final ComplianceService complianceService;
  private final DirectoryService directoryService;

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
      DirectoryService directoryService) {
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
  }

  @VisibleForTesting
  @Override
  public int getCurrentDuccVersion() {
    return configProvider.get().featureFlags.enableV3DataUserCodeOfConduct ? 3 : 2;
  }

  /**
   * Updates the currently-authenticated user with a modifier function.
   *
   * <p>Ensures that the data access level for the user reflects the state of other fields on the
   * user; handles conflicts with concurrent updates by retrying.
   */
  private DbUser updateUserWithRetries(Function<DbUser, DbUser> modifyUser) {
    DbUser user = userProvider.get();
    return updateUserWithRetries(modifyUser, user, Agent.asUser(user));
  }

  /**
   * Updates a user record with a modifier function.
   *
   * <p>Ensures that the data access level for the user reflects the state of other fields on the
   * user; handles conflicts with concurrent updates by retrying.
   */
  @Override
  public DbUser updateUserWithRetries(
      Function<DbUser, DbUser> userModifier, DbUser dbUser, Agent agent) {
    int objectLockingFailureCount = 0;
    int statementClosedCount = 0;
    while (true) {
      dbUser = userModifier.apply(dbUser);
      updateDataAccessLevel(dbUser, agent);
      Timestamp now = new Timestamp(clock.instant().toEpochMilli());
      dbUser.setLastModifiedTime(now);
      try {
        return userDao.save(dbUser);
      } catch (ObjectOptimisticLockingFailureException e) {
        if (objectLockingFailureCount < MAX_RETRIES) {
          dbUser = userDao.findOne(dbUser.getUserId());
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
            dbUser = userDao.findOne(dbUser.getUserId());
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

  private void updateDataAccessLevel(DbUser dbUser, Agent agent) {
    final DataAccessLevel previousDataAccessLevel = dbUser.getDataAccessLevelEnum();
    final DataAccessLevel newDataAccessLevel;
    if (shouldUserBeRegistered(dbUser)) {
      addToRegisteredTierGroupIdempotent(dbUser);
      newDataAccessLevel = DataAccessLevel.REGISTERED;

      // if this is the first time the user has completed registration, record it
      // this starts the Free Tier Credits countdown clock
      if (dbUser.getFirstRegistrationCompletionTime() == null) {
        dbUser.setFirstRegistrationCompletionTime();
      }
    } else {
      removeFromRegisteredTierGroupIdempotent(dbUser);
      newDataAccessLevel = DataAccessLevel.UNREGISTERED;
    }
    if (!newDataAccessLevel.equals(previousDataAccessLevel)) {
      dbUser.setDataAccessLevelEnum(newDataAccessLevel);
      userServiceAuditor.fireUpdateDataAccessAction(
          dbUser, newDataAccessLevel, previousDataAccessLevel, agent);
    }
  }

  private void removeFromRegisteredTierGroupIdempotent(DbUser dbUser) {
    if (isUserMemberOfRegisteredTierGroup(dbUser)) {
      this.fireCloudService.removeUserFromGroup(
          dbUser.getUsername(), configProvider.get().firecloud.registeredDomainName);
      log.info(String.format("Removed user %s from registered-tier group.", dbUser.getUsername()));
    }
  }

  private boolean isUserMemberOfRegisteredTierGroup(DbUser dbUser) {
    return this.fireCloudService.isUserMemberOfGroup(
        dbUser.getUsername(), configProvider.get().firecloud.registeredDomainName);
  }

  private void addToRegisteredTierGroupIdempotent(DbUser user) {
    if (!isUserMemberOfRegisteredTierGroup(user)) {
      this.fireCloudService.addUserToGroup(
          user.getUsername(), configProvider.get().firecloud.registeredDomainName);
      log.info(String.format("Added user %s to registered-tier group.", user.getUsername()));
    }
  }

  private boolean shouldUserBeRegistered(DbUser user) {
    boolean dataUseAgreementCompliant = false;
    if (user.getDataUseAgreementBypassTime() != null
        || !configProvider.get().access.enableDataUseAgreement) {
      // Data use agreement version may be ignored, since it's bypassed on the user or env level.
      dataUseAgreementCompliant = true;
    } else if (user.getDataUseAgreementSignedVersion() == getCurrentDuccVersion()) {
      // User has signed the most-recent DUCC version.
      dataUseAgreementCompliant = true;
    }
    boolean eraCommonsCompliant =
        user.getEraCommonsBypassTime() != null
            || !configProvider.get().access.enableEraCommons
            || user.getEraCommonsCompletionTime() != null;
    boolean complianceTrainingCompliant =
        user.getComplianceTrainingCompletionTime() != null
            || user.getComplianceTrainingBypassTime() != null
            || !configProvider.get().access.enableComplianceTraining;
    boolean betaAccessGranted =
        user.getBetaAccessBypassTime() != null || !configProvider.get().access.enableBetaAccess;
    boolean twoFactorAuthComplete =
        user.getTwoFactorAuthCompletionTime() != null || user.getTwoFactorAuthBypassTime() != null;

    // TODO: can take out other checks once we're entirely moved over to the 'module' columns
    return !user.getDisabled()
        && complianceTrainingCompliant
        && eraCommonsCompliant
        && betaAccessGranted
        && twoFactorAuthComplete
        && dataUseAgreementCompliant
        && EmailVerificationStatus.SUBSCRIBED.equals(user.getEmailVerificationStatusEnum());
  }

  private boolean isServiceAccount(DbUser user) {
    return configProvider.get().auth.serviceAccountApiUsers.contains(user.getUsername());
  }

  @Override
  public DbUser createServiceAccountUser(String username) {
    DbUser user = new DbUser();
    user.setDataAccessLevelEnum(DataAccessLevel.PROTECTED);
    user.setUsername(username);
    user.setDisabled(false);
    user.setEmailVerificationStatusEnum(EmailVerificationStatus.UNVERIFIED);
    try {
      return userDao.save(user);
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
        return userByUserName;
      }
    }
  }

  @Override
  public DbUser createUser(final Userinfoplus oAuth2Userinfo) {
    return createUser(
        oAuth2Userinfo.getGivenName(),
        oAuth2Userinfo.getFamilyName(),
        oAuth2Userinfo.getEmail(),
        oAuth2Userinfo.getEmail(),
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null);
  }

  @Override
  public DbUser createUser(
      String givenName,
      String familyName,
      String userName,
      String contactEmail,
      String currentPosition,
      String organization,
      String areaOfResearch,
      String professionalUrl,
      List<Degree> degrees,
      DbAddress dbAddress,
      DbDemographicSurvey dbDemographicSurvey,
      List<DbInstitutionalAffiliation> dbAffiliations,
      DbVerifiedInstitutionalAffiliation dbVerifiedAffiliation) {
    DbUser dbUser = new DbUser();
    dbUser.setCreationNonce(Math.abs(random.nextLong()));
    dbUser.setDataAccessLevelEnum(DataAccessLevel.UNREGISTERED);
    dbUser.setUsername(userName);
    dbUser.setContactEmail(contactEmail);
    dbUser.setCurrentPosition(currentPosition);
    dbUser.setOrganization(organization);
    dbUser.setAreaOfResearch(areaOfResearch);
    dbUser.setFamilyName(familyName);
    dbUser.setGivenName(givenName);
    dbUser.setProfessionalUrl(professionalUrl);
    dbUser.setDisabled(false);
    dbUser.setAboutYou(null);
    dbUser.setEmailVerificationStatusEnum(EmailVerificationStatus.UNVERIFIED);
    dbUser.setAddress(dbAddress);
    if (degrees != null) {
      dbUser.setDegreesEnum(degrees);
    }
    dbUser.setDemographicSurvey(dbDemographicSurvey);
    Timestamp now = new Timestamp(clock.instant().toEpochMilli());
    dbUser.setCreationTime(now);
    dbUser.setLastModifiedTime(now);

    // For existing user that do not have address
    if (dbAddress != null) {
      dbAddress.setUser(dbUser);
    }
    if (dbDemographicSurvey != null) dbDemographicSurvey.setUser(dbUser);
    // set via the older Institutional Affiliation flow, from the Demographic Survey
    if (dbAffiliations != null) {
      // We need an "effectively final" variable to be captured in the lambda
      // to pass to forEach.
      final DbUser finalDbUserReference = dbUser;
      dbAffiliations.forEach(
          affiliation -> {
            affiliation.setUser(finalDbUserReference);
            finalDbUserReference.addInstitutionalAffiliation(affiliation);
          });
    }
    // set via the newer Verified Institutional Affiliation flow
    boolean requireInstitutionalVerification =
        configProvider.get().featureFlags.requireInstitutionalVerification;

    try {
      dbUser = userDao.save(dbUser);
      if (requireInstitutionalVerification) {
        dbVerifiedAffiliation.setUser(dbUser);
        this.verifiedInstitutionalAffiliationDao.save(dbVerifiedAffiliation);
      }
    } catch (DataIntegrityViolationException e) {
      dbUser = userDao.findUserByUsername(userName);
      if (dbUser == null) {
        throw e;
      }
      // If a user already existed (due to multiple requests trying to create a user simultaneously)
      // just return it.
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
    userDataUseAgreementDao.save(dataUseAgreements);
  }

  @Override
  @Transactional
  public void submitTermsOfService(DbUser dbUser, Integer tosVersion) {
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
  public void setDataUseAgreementBypassTime(Long userId, Timestamp bypassTime) {
    setBypassTimeWithRetries(
        userId,
        bypassTime,
        DbUser::setDataUseAgreementBypassTime,
        BypassTimeTargetProperty.DATA_USE_AGREEMENT_BYPASS_TIME);
  }

  @Override
  public void setComplianceTrainingBypassTime(Long userId, Timestamp bypassTime) {
    setBypassTimeWithRetries(
        userId,
        bypassTime,
        DbUser::setComplianceTrainingBypassTime,
        BypassTimeTargetProperty.COMPLIANCE_TRAINING_BYPASS_TIME);
  }

  @Override
  public void setBetaAccessBypassTime(Long userId, Timestamp bypassTime) {
    setBypassTimeWithRetries(
        userId,
        bypassTime,
        DbUser::setBetaAccessBypassTime,
        BypassTimeTargetProperty.BETA_ACCESS_BYPASS_TIME);
  }

  @Override
  public void setEraCommonsBypassTime(Long userId, Timestamp bypassTime) {
    setBypassTimeWithRetries(
        userId,
        bypassTime,
        DbUser::setEraCommonsBypassTime,
        BypassTimeTargetProperty.ERA_COMMONS_BYPASS_TIME);
  }

  @Override
  public void setTwoFactorAuthBypassTime(Long userId, Timestamp bypassTime) {
    setBypassTimeWithRetries(
        userId,
        bypassTime,
        DbUser::setTwoFactorAuthBypassTime,
        BypassTimeTargetProperty.TWO_FACTOR_AUTH_BYPASS_TIME);
  }

  /**
   * Functional bypass time column setter, using retry logic.
   *
   * @param userId id of user getting bypassed
   * @param bypassTime type of bypass
   * @param setter void-returning method to call to set the particular bypass field. Should
   *     typically be a method reference on DbUser, e.g.
   * @param targetProperty BypassTimeTargetProperty enum value, for auditing
   */
  private void setBypassTimeWithRetries(
      long userId,
      Timestamp bypassTime,
      BiConsumer<DbUser, Timestamp> setter,
      BypassTimeTargetProperty targetProperty) {
    setBypassTimeWithRetries(userDao.findUserByUserId(userId), bypassTime, targetProperty, setter);
  }

  private void setBypassTimeWithRetries(
      DbUser dbUser,
      Timestamp bypassTime,
      BypassTimeTargetProperty targetProperty,
      BiConsumer<DbUser, Timestamp> setter) {
    updateUserWithRetries(
        (u) -> {
          setter.accept(u, bypassTime);
          return u;
        },
        dbUser,
        Agent.asAdmin(userProvider.get()));
    userServiceAuditor.fireAdministrativeBypassTime(
        dbUser.getUserId(),
        targetProperty,
        Optional.ofNullable(bypassTime).map(Timestamp::toInstant));
  }

  @Override
  public void setClusterRetryCount(int clusterRetryCount) {
    updateUserWithRetries(
        (user) -> {
          user.setClusterCreateRetries(clusterRetryCount);
          return user;
        });
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
  public List<DbUser> getAllUsers() {
    return userDao.findUsers();
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

  /** Find users matching the user's name or email */
  @Override
  public List<DbUser> findUsersBySearchString(String term, Sort sort) {
    List<Short> dataAccessLevels =
        Stream.of(DataAccessLevel.REGISTERED, DataAccessLevel.PROTECTED)
            .map(DbStorageEnums::dataAccessLevelToStorage)
            .collect(Collectors.toList());
    return userDao.findUsersByDataAccessLevelsAndSearchString(dataAccessLevels, term, sort);
  }

  /** Syncs the current user's training status from Moodle. */
  @Override
  @Deprecated
  public DbUser syncComplianceTrainingStatusV1()
      throws org.pmiops.workbench.moodle.ApiException, NotFoundException {
    DbUser user = userProvider.get();
    return syncComplianceTrainingStatusV1(user, Agent.asUser(user));
  }

  /**
   * Updates the given user's training status from Moodle.
   *
   * <p>We can fetch Moodle data for arbitrary users since we use an API key to access Moodle,
   * rather than user-specific OAuth tokens.
   *
   * <p>Overall flow: 1. Check if user have moodle_id, a. if not retrieve it from MOODLE API and
   * save it in the Database 2. Using the MOODLE_ID get user's Badge update the database with a.
   * training completion time as current time b. training expiration date with as returned from
   * MOODLE. 3. If there are no badges for a user set training completion time and expiration date
   * as null
   */
  @Override
  @Deprecated
  public DbUser syncComplianceTrainingStatusV1(DbUser dbUser, Agent agent)
      throws org.pmiops.workbench.moodle.ApiException, NotFoundException {
    if (isServiceAccount(dbUser)) {
      // Skip sync for service account user rows.
      return dbUser;
    }

    Timestamp now = new Timestamp(clock.instant().toEpochMilli());
    try {
      Integer moodleId = dbUser.getMoodleId();
      if (moodleId == null) {
        moodleId = complianceService.getMoodleId(dbUser.getUsername());
        if (moodleId == null) {
          // User has not yet created/logged into MOODLE
          return dbUser;
        }
        dbUser.setMoodleId(moodleId);
      }

      List<BadgeDetailsV1> badgeResponse = complianceService.getUserBadgeV1(moodleId);
      // The assumption here is that the User will always get 1 badge which will be AoU
      if (badgeResponse != null && badgeResponse.size() > 0) {
        BadgeDetailsV1 badge = badgeResponse.get(0);
        Timestamp badgeExpiration =
            badge.getDateexpire() == null
                ? null
                : new Timestamp(Long.parseLong(badge.getDateexpire()));

        if (dbUser.getComplianceTrainingCompletionTime() == null) {
          // This is the user's first time with a Moodle badge response, so we reset the completion
          // time.
          dbUser.setComplianceTrainingCompletionTime(now);
        } else if (badgeExpiration != null
            && !badgeExpiration.equals(dbUser.getComplianceTrainingExpirationTime())) {
          // The badge has a new expiration date, suggesting some sort of course completion change.
          // Reset the completion time.
          dbUser.setComplianceTrainingCompletionTime(now);
        }

        dbUser.setComplianceTrainingExpirationTime(badgeExpiration);
      } else {
        // Moodle has returned zero badges for the given user -- we should clear the user's
        // training completion & expiration time.
        dbUser.setComplianceTrainingCompletionTime(null);
        dbUser.setComplianceTrainingExpirationTime(null);
      }

      return updateUserWithRetries(
          u -> {
            u.setMoodleId(u.getMoodleId());
            u.setComplianceTrainingExpirationTime(u.getComplianceTrainingExpirationTime());
            u.setComplianceTrainingCompletionTime(u.getComplianceTrainingCompletionTime());
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
                "Error while retrieving Badge for user %s: %s ",
                dbUser.getUserId(), ex.getMessage()));
        throw new NotFoundException(ex.getMessage());
      }
      throw ex;
    }
  }

  /** Syncs the current user's training status from Moodle. */
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

  /** */
  @Override
  public DbUser syncTwoFactorAuthStatus(DbUser targetUser, Agent agent) {
    if (isServiceAccount(targetUser)) {
      // Skip sync for service account user rows.
      return targetUser;
    }

    return updateUserWithRetries(
        user -> {
          boolean isEnrolledIn2FA =
              directoryService.getUser(user.getUsername()).getIsEnrolledIn2Sv();
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

    final List<UserCountGaugeLabelsAndValue> rows = userDao.getUserCountGaugeData();
    return rows.stream()
        .map(
            row ->
                MeasurementBundle.builder()
                    .addMeasurement(GaugeMetric.USER_COUNT, row.getUserCount())
                    .addTag(
                        MetricLabel.DATA_ACCESS_LEVEL,
                        DbStorageEnums.dataAccessLevelFromStorage(row.getDataAccessLevel())
                            .toString())
                    .addTag(MetricLabel.USER_DISABLED, row.getDisabled().toString())
                    .addTag(MetricLabel.USER_BYPASSED_BETA, row.getBetaIsBypassed().toString())
                    .build())
        .collect(ImmutableList.toImmutableList());
  }
}
