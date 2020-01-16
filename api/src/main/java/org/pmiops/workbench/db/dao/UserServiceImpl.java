package org.pmiops.workbench.db.dao;

import com.google.api.client.http.HttpStatusCodes;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Provider;
import org.apache.commons.collections4.map.MultiKeyMap;
import org.hibernate.exception.GenericJDBCException;
import org.pmiops.workbench.actionaudit.auditors.UserServiceAuditor;
import org.pmiops.workbench.actionaudit.targetproperties.BypassTimeTargetProperty;
import org.pmiops.workbench.compliance.ComplianceService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.model.CommonStorageEnums;
import org.pmiops.workbench.db.model.DbAddress;
import org.pmiops.workbench.db.model.DbAdminActionHistory;
import org.pmiops.workbench.db.model.DbDemographicSurvey;
import org.pmiops.workbench.db.model.DbInstitutionalAffiliation;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserDataUseAgreement;
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
import org.pmiops.workbench.monitoring.attachments.Attachment;
import org.pmiops.workbench.monitoring.views.GaugeMetric;
import org.pmiops.workbench.moodle.model.BadgeDetails;
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
  private static final int CURRENT_DATA_USE_AGREEMENT_VERSION = 2;

  private final Provider<DbUser> userProvider;
  private final UserDao userDao;
  private final AdminActionHistoryDao adminActionHistoryDao;
  private final UserDataUseAgreementDao userDataUseAgreementDao;
  private final Clock clock;
  private final Random random;
  private final FireCloudService fireCloudService;
  private final Provider<WorkbenchConfig> configProvider;
  private final ComplianceService complianceService;
  private final DirectoryService directoryService;
  private final UserServiceAuditor userServiceAuditAdapter;
  private static final Logger log = Logger.getLogger(UserServiceImpl.class.getName());

  @Autowired
  public UserServiceImpl(
      Provider<DbUser> userProvider,
      UserDao userDao,
      AdminActionHistoryDao adminActionHistoryDao,
      UserDataUseAgreementDao userDataUseAgreementDao,
      Clock clock,
      Random random,
      FireCloudService fireCloudService,
      Provider<WorkbenchConfig> configProvider,
      ComplianceService complianceService,
      DirectoryService directoryService,
      UserServiceAuditor userServiceAuditAdapter) {
    this.userProvider = userProvider;
    this.userDao = userDao;
    this.adminActionHistoryDao = adminActionHistoryDao;
    this.userDataUseAgreementDao = userDataUseAgreementDao;
    this.clock = clock;
    this.random = random;
    this.fireCloudService = fireCloudService;
    this.configProvider = configProvider;
    this.complianceService = complianceService;
    this.directoryService = directoryService;
    this.userServiceAuditAdapter = userServiceAuditAdapter;
  }

  /**
   * Updates the currently-authenticated user with a modifier function.
   *
   * <p>Ensures that the data access level for the user reflects the state of other fields on the
   * user; handles conflicts with concurrent updates by retrying.
   */
  private DbUser updateUserWithRetries(Function<DbUser, DbUser> modifyUser) {
    DbUser user = userProvider.get();
    return updateUserWithRetries(modifyUser, user);
  }

  /**
   * Updates a user record with a modifier function.
   *
   * <p>Ensures that the data access level for the user reflects the state of other fields on the
   * user; handles conflicts with concurrent updates by retrying.
   */
  @Override
  public DbUser updateUserWithRetries(Function<DbUser, DbUser> userModifier, DbUser dbUser) {
    int objectLockingFailureCount = 0;
    int statementClosedCount = 0;
    while (true) {
      dbUser = userModifier.apply(dbUser);
      updateDataAccessLevel(dbUser);
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

  private void updateDataAccessLevel(DbUser dbUser) {
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
      userServiceAuditAdapter.fireUpdateDataAccessAction(
          dbUser, newDataAccessLevel, previousDataAccessLevel);
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
    boolean dataUseAgreementCompliant =
        user.getDataUseAgreementCompletionTime() != null
            || user.getDataUseAgreementBypassTime() != null
            || !configProvider.get().access.enableDataUseAgreement;
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
  public DbUser createUser(
      String givenName,
      String familyName,
      String userName,
      String contactEmail,
      String currentPosition,
      String organization,
      String areaOfResearch) {
    return createUser(
        givenName,
        familyName,
        userName,
        contactEmail,
        currentPosition,
        organization,
        areaOfResearch,
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
      List<Degree> degrees,
      DbAddress address,
      DbDemographicSurvey demographicSurvey,
      List<DbInstitutionalAffiliation> institutionalAffiliations) {
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
    dbUser.setDisabled(false);
    dbUser.setAboutYou(null);
    dbUser.setEmailVerificationStatusEnum(EmailVerificationStatus.UNVERIFIED);
    dbUser.setAddress(address);
    if (degrees != null) {
      dbUser.setDegreesEnum(degrees);
    }
    dbUser.setDemographicSurvey(demographicSurvey);
    Timestamp now = new Timestamp(clock.instant().toEpochMilli());
    dbUser.setCreationTime(now);
    dbUser.setLastModifiedTime(now);

    DbUser user = new DbUser();
    // For existing user that do not have address
    if (address != null) {
      address.setUser(dbUser);
    }
    if (demographicSurvey != null) demographicSurvey.setUser(dbUser);
    if (institutionalAffiliations != null) {
      // We need an "effectively final" variable to be captured in the lambda
      // to pass to forEach.
      final DbUser finalDbUserReference = dbUser;
      institutionalAffiliations.forEach(
          affiliation -> {
            affiliation.setUser(finalDbUserReference);
            finalDbUserReference.addInstitutionalAffiliation(affiliation);
          });
    }
    try {
      dbUser = userDao.save(dbUser);
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
    if (dataUseAgreementSignedVersion != CURRENT_DATA_USE_AGREEMENT_VERSION) {
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
        dbUser);
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
        dbUser);
    userServiceAuditAdapter.fireAdministrativeBypassTime(
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
        user);
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
            .map(CommonStorageEnums::dataAccessLevelToStorage)
            .collect(Collectors.toList());
    return userDao.findUsersByDataAccessLevelsAndSearchString(dataAccessLevels, term, sort);
  }

  /** Syncs the current user's training status from Moodle. */
  @Override
  public DbUser syncComplianceTrainingStatus()
      throws org.pmiops.workbench.moodle.ApiException, NotFoundException {
    return syncComplianceTrainingStatus(userProvider.get());
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
  public DbUser syncComplianceTrainingStatus(DbUser dbUser)
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

      List<BadgeDetails> badgeResponse = complianceService.getUserBadge(moodleId);
      // The assumption here is that the User will always get 1 badge which will be AoU
      if (badgeResponse != null && badgeResponse.size() > 0) {
        BadgeDetails badge = badgeResponse.get(0);
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
          dbUser);

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

  /**
   * Updates the given user's eraCommons-related fields with the NihStatus object returned from FC.
   *
   * <p>This method saves the updated user object to the database and returns it.
   */
  private DbUser setEraCommonsStatus(DbUser targetUser, FirecloudNihStatus nihStatus) {
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
        targetUser);
  }

  /** Syncs the eraCommons access module status for the current user. */
  @Override
  public DbUser syncEraCommonsStatus() {
    DbUser user = userProvider.get();
    FirecloudNihStatus nihStatus = fireCloudService.getNihStatus();
    return setEraCommonsStatus(user, nihStatus);
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
  public DbUser syncEraCommonsStatusUsingImpersonation(DbUser user)
      throws IOException, org.pmiops.workbench.firecloud.ApiException {
    if (isServiceAccount(user)) {
      // Skip sync for service account user rows.
      return user;
    }

    ApiClient apiClient = fireCloudService.getApiClientWithImpersonation(user.getUsername());
    NihApi api = new NihApi(apiClient);
    try {
      FirecloudNihStatus nihStatus = api.nihStatus();
      return setEraCommonsStatus(user, nihStatus);
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
    syncTwoFactorAuthStatus(userProvider.get());
  }

  /** */
  @Override
  public DbUser syncTwoFactorAuthStatus(DbUser targetUser) {
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
        targetUser);
  }

  @Override
  public Collection<MeasurementBundle> getGaugeData() {
    // TODO(jaycarlton): replace this findAll/MultiKeyMap implementation
    //   wtih JdbcTemplate method. I'm told we can't currently do things like this
    //   with our JPA/Spring/MySQL versions.

    List<DbUser> allUsers = userDao.findUsers();
    MultiKeyMap<String, Long> keysToCount = new MultiKeyMap<>();
    // keys will be { accessLevel, disabled }
    for (DbUser user : allUsers) {
      // get keys
      final String dataAccessLevel = user.getDataAccessLevelEnum().toString();
      final String isDisabled = Boolean.valueOf(user.getDisabled()).toString();
      final String isBetaBypassed =
          Boolean.valueOf(user.getBetaAccessBypassTime() != null).toString();

      // find count
      final long count =
          Optional.ofNullable(keysToCount.get(dataAccessLevel, isDisabled, isBetaBypassed))
              .orElse(0L);
      // increment count
      keysToCount.put(dataAccessLevel, isDisabled, isBetaBypassed, count + 1);
    }

    // build bundles for each entry in the map
    return keysToCount.entrySet().stream()
        .map(
            e ->
                MeasurementBundle.builder()
                    .addMeasurement(GaugeMetric.USER_COUNT, e.getValue())
                    .addAttachment(Attachment.DATA_ACCESS_LEVEL, e.getKey().getKey(0))
                    .addAttachment(Attachment.USER_DISABLED, e.getKey().getKey(1))
                    .addAttachment(Attachment.USER_BYPASSED_BETA, e.getKey().getKey(2))
                    .build())
        .collect(ImmutableSet.toImmutableSet());
  }
}
