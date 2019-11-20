package org.pmiops.workbench.db.dao;

import com.google.api.client.http.HttpStatusCodes;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Provider;
import org.hibernate.exception.GenericJDBCException;
import org.pmiops.workbench.actionaudit.adapters.UserServiceAuditAdapter;
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
import org.pmiops.workbench.firecloud.model.NihStatus;
import org.pmiops.workbench.google.DirectoryService;
import org.pmiops.workbench.model.DataAccessLevel;
import org.pmiops.workbench.model.EmailVerificationStatus;
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
public class UserService {

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
  private UserServiceAuditAdapter userServiceAuditAdapter;
  private static final Logger log = Logger.getLogger(UserService.class.getName());

  @Autowired
  public UserService(
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
      UserServiceAuditAdapter userServiceAuditAdapter) {
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
  public DbUser updateUserWithRetries(Function<DbUser, DbUser> userModifier, DbUser user) {
    int objectLockingFailureCount = 0;
    int statementClosedCount = 0;
    while (true) {
      user = userModifier.apply(user);
      updateDataAccessLevel(user);
      try {
        user = userDao.save(user);
        return user;
      } catch (ObjectOptimisticLockingFailureException e) {
        if (objectLockingFailureCount < MAX_RETRIES) {
          user = userDao.findOne(user.getUserId());
          objectLockingFailureCount++;
        } else {
          throw new ConflictException(
              String.format(
                  "Could not update user %s after %d object locking failures",
                  user.getUserId(), objectLockingFailureCount));
        }
      } catch (JpaSystemException e) {
        // We don't know why this happens instead of the object locking failure.
        if (((GenericJDBCException) e.getCause())
            .getSQLException()
            .getMessage()
            .equals("Statement closed.")) {
          if (statementClosedCount < MAX_RETRIES) {
            user = userDao.findOne(user.getUserId());
            statementClosedCount++;
          } else {
            throw new ConflictException(
                String.format(
                    "Could not update user %s after %d statement closes",
                    user.getUserId(), statementClosedCount));
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
      addToGroupIdempotent(dbUser);
      newDataAccessLevel = DataAccessLevel.REGISTERED;
    } else {
      removeFromGroupIdempotent(dbUser);
      newDataAccessLevel = DataAccessLevel.UNREGISTERED;
    }
    dbUser.setDataAccessLevelEnum(newDataAccessLevel);
    userServiceAuditAdapter.fireUpdateDataAccessAction(
        dbUser, newDataAccessLevel, previousDataAccessLevel);
  }

  private void removeFromGroupIdempotent(DbUser dbUser) {
    if (isUserMemberOfFirecloudGroup(dbUser)) {
      this.fireCloudService.removeUserFromGroup(
          dbUser.getEmail(), configProvider.get().firecloud.registeredDomainName);
      log.info(String.format("Removed user %s from registered-tier group.", dbUser.getEmail()));
    }
  }

  private boolean isUserMemberOfFirecloudGroup(DbUser dbUser) {
    return this.fireCloudService.isUserMemberOfGroup(
        dbUser.getEmail(), configProvider.get().firecloud.registeredDomainName);
  }

  private void addToGroupIdempotent(DbUser user) {
    if (!isUserMemberOfFirecloudGroup(user)) {
      this.fireCloudService.addUserToGroup(
          user.getEmail(), configProvider.get().firecloud.registeredDomainName);
      log.info(String.format("Added user %s to registered-tier group.", user.getEmail()));
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
    return configProvider.get().auth.serviceAccountApiUsers.contains(user.getEmail());
  }

  public DbUser createServiceAccountUser(String email) {
    DbUser user = new DbUser();
    user.setDataAccessLevelEnum(DataAccessLevel.PROTECTED);
    user.setEmail(email);
    user.setDisabled(false);
    user.setEmailVerificationStatusEnum(EmailVerificationStatus.UNVERIFIED);
    try {
      userDao.save(user);
    } catch (DataIntegrityViolationException e) {
      user = userDao.findUserByEmail(email);
      if (user == null) {
        throw e;
      }
      // If a user already existed (due to multiple requests trying to create a user simultaneously)
      // just return it.
    }
    return user;
  }

  public DbUser createUser(
      String givenName,
      String familyName,
      String email,
      String contactEmail,
      String currentPosition,
      String organization,
      String areaOfResearch) {
    return createUser(
        givenName,
        familyName,
        email,
        contactEmail,
        currentPosition,
        organization,
        areaOfResearch,
        null,
        null,
        null);
  }

  public DbUser createUser(
      String givenName,
      String familyName,
      String email,
      String contactEmail,
      String currentPosition,
      String organization,
      String areaOfResearch,
      DbAddress address,
      DbDemographicSurvey demographicSurvey,
      List<DbInstitutionalAffiliation> institutionalAffiliations) {
    DbUser user = new DbUser();
    user.setCreationNonce(Math.abs(random.nextLong()));
    user.setDataAccessLevelEnum(DataAccessLevel.UNREGISTERED);
    user.setEmail(email);
    user.setContactEmail(contactEmail);
    user.setCurrentPosition(currentPosition);
    user.setOrganization(organization);
    user.setAreaOfResearch(areaOfResearch);
    user.setFamilyName(familyName);
    user.setGivenName(givenName);
    user.setDisabled(false);
    user.setAboutYou(null);
    user.setEmailVerificationStatusEnum(EmailVerificationStatus.UNVERIFIED);
    user.setAddress(address);
    user.setDemographicSurvey(demographicSurvey);
    // For existing user that do not have address
    if (address != null) {
      address.setUser(user);
    }
    if (demographicSurvey != null) demographicSurvey.setUser(user);
    if (institutionalAffiliations != null) {
      final DbUser u = user;
      institutionalAffiliations.forEach(
          affiliation -> {
            affiliation.setUser(u);
            u.addInstitutionalAffiliation(affiliation);
          });
    }
    try {
      userDao.save(user);
    } catch (DataIntegrityViolationException e) {
      user = userDao.findUserByEmail(email);
      if (user == null) {
        throw e;
      }
      // If a user already existed (due to multiple requests trying to create a user simultaneously)
      // just return it.
    }
    return user;
  }

  public DbUser submitDataUseAgreement(
      DbUser user, Integer dataUseAgreementSignedVersion, String initials) {
    // FIXME: this should not be hardcoded
    if (dataUseAgreementSignedVersion != CURRENT_DATA_USE_AGREEMENT_VERSION) {
      throw new BadRequestException("Data Use Agreement Version is not up to date");
    }
    final Timestamp timestamp = new Timestamp(clock.instant().toEpochMilli());
    DbUserDataUseAgreement dataUseAgreement = new DbUserDataUseAgreement();
    dataUseAgreement.setDataUseAgreementSignedVersion(dataUseAgreementSignedVersion);
    dataUseAgreement.setUserId(user.getUserId());
    dataUseAgreement.setUserFamilyName(user.getFamilyName());
    dataUseAgreement.setUserGivenName(user.getGivenName());
    dataUseAgreement.setUserInitials(initials);
    dataUseAgreement.setCompletionTime(timestamp);
    userDataUseAgreementDao.save(dataUseAgreement);
    // TODO: Teardown/reconcile duplicated state between the user profile and DUA.
    user.setDataUseAgreementCompletionTime(timestamp);
    user.setDataUseAgreementSignedVersion(dataUseAgreementSignedVersion);
    return userDao.save(user);
  }

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

  public DbUser setDataUseAgreementBypassTime(Long userId, Timestamp bypassTime) {
    DbUser user = userDao.findUserByUserId(userId);
    return updateUserWithRetries(
        (u) -> {
          u.setDataUseAgreementBypassTime(bypassTime);
          return u;
        },
        user);
  }

  public DbUser setComplianceTrainingBypassTime(Long userId, Timestamp bypassTime) {
    DbUser user = userDao.findUserByUserId(userId);
    return updateUserWithRetries(
        (u) -> {
          u.setComplianceTrainingBypassTime(bypassTime);
          return u;
        },
        user);
  }

  public DbUser setBetaAccessBypassTime(Long userId, Timestamp bypassTime) {
    DbUser user = userDao.findUserByUserId(userId);
    return updateUserWithRetries(
        (u) -> {
          u.setBetaAccessBypassTime(bypassTime);
          return u;
        },
        user);
  }

  public DbUser setEmailVerificationBypassTime(Long userId, Timestamp bypassTime) {
    DbUser user = userDao.findUserByUserId(userId);
    return updateUserWithRetries(
        (u) -> {
          u.setEmailVerificationBypassTime(bypassTime);
          return u;
        },
        user);
  }

  public DbUser setEraCommonsBypassTime(Long userId, Timestamp bypassTime) {
    DbUser user = userDao.findUserByUserId(userId);
    return updateUserWithRetries(
        (u) -> {
          u.setEraCommonsBypassTime(bypassTime);
          return u;
        },
        user);
  }

  public DbUser setIdVerificationBypassTime(Long userId, Timestamp bypassTime) {
    DbUser user = userDao.findUserByUserId(userId);
    return updateUserWithRetries(
        (u) -> {
          u.setIdVerificationBypassTime(bypassTime);
          return u;
        },
        user);
  }

  public DbUser setTwoFactorAuthBypassTime(Long userId, Timestamp bypassTime) {
    DbUser user = userDao.findUserByUserId(userId);
    return updateUserWithRetries(
        (u) -> {
          u.setTwoFactorAuthBypassTime(bypassTime);
          return u;
        },
        user);
  }

  public DbUser setClusterRetryCount(int clusterRetryCount) {
    return updateUserWithRetries(
        (user) -> {
          user.setClusterCreateRetries(clusterRetryCount);
          return user;
        });
  }

  public DbUser setBillingRetryCount(int billingRetryCount) {
    return updateUserWithRetries(
        (user) -> {
          user.setBillingProjectRetries(billingRetryCount);
          return user;
        });
  }

  public DbUser setDisabledStatus(Long userId, boolean disabled) {
    DbUser user = userDao.findUserByUserId(userId);
    return updateUserWithRetries(
        (u) -> {
          u.setDisabled(disabled);
          return u;
        },
        user);
  }

  public List<DbUser> getAllUsers() {
    return userDao.findUsers();
  }

  public void logAdminUserAction(
      long targetUserId, String targetAction, Object oldValue, Object newValue) {
    logAdminAction(targetUserId, null, targetAction, oldValue, newValue);
  }

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

  public boolean getContactEmailTaken(String contactEmail) {
    return (!userDao.findUserByContactEmail(contactEmail).isEmpty());
  }

  /** Find users matching the user's name or email */
  public List<DbUser> findUsersBySearchString(String term, Sort sort) {
    List<Short> dataAccessLevels =
        Stream.of(DataAccessLevel.REGISTERED, DataAccessLevel.PROTECTED)
            .map(CommonStorageEnums::dataAccessLevelToStorage)
            .collect(Collectors.toList());
    return userDao.findUsersByDataAccessLevelsAndSearchString(dataAccessLevels, term, sort);
  }

  /** Syncs the current user's training status from Moodle. */
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
  public DbUser syncComplianceTrainingStatus(DbUser user)
      throws org.pmiops.workbench.moodle.ApiException, NotFoundException {
    if (isServiceAccount(user)) {
      // Skip sync for service account user rows.
      return user;
    }

    Timestamp now = new Timestamp(clock.instant().toEpochMilli());
    try {
      Integer moodleId = user.getMoodleId();
      if (moodleId == null) {
        moodleId = complianceService.getMoodleId(user.getEmail());
        if (moodleId == null) {
          // User has not yet created/logged into MOODLE
          return user;
        }
        user.setMoodleId(moodleId);
      }

      List<BadgeDetails> badgeResponse = complianceService.getUserBadge(moodleId);
      // The assumption here is that the User will always get 1 badge which will be AoU
      if (badgeResponse != null && badgeResponse.size() > 0) {
        BadgeDetails badge = badgeResponse.get(0);
        Timestamp badgeExpiration =
            badge.getDateexpire() == null
                ? null
                : new Timestamp(Long.parseLong(badge.getDateexpire()));

        if (user.getComplianceTrainingCompletionTime() == null) {
          // This is the user's first time with a Moodle badge response, so we reset the completion
          // time.
          user.setComplianceTrainingCompletionTime(now);
        } else if (badgeExpiration != null
            && !badgeExpiration.equals(user.getComplianceTrainingExpirationTime())) {
          // The badge has a new expiration date, suggesting some sort of course completion change.
          // Reset the completion time.
          user.setComplianceTrainingCompletionTime(now);
        }

        user.setComplianceTrainingExpirationTime(badgeExpiration);
      } else {
        // Moodle has returned zero badges for the given user -- we should clear the user's
        // training completion & expiration time.
        user.setComplianceTrainingCompletionTime(null);
        user.setComplianceTrainingExpirationTime(null);
      }

      return updateUserWithRetries(
          dbUser -> {
            dbUser.setMoodleId(user.getMoodleId());
            dbUser.setComplianceTrainingExpirationTime(user.getComplianceTrainingExpirationTime());
            dbUser.setComplianceTrainingCompletionTime(user.getComplianceTrainingCompletionTime());
            return dbUser;
          },
          user);

    } catch (NumberFormatException e) {
      log.severe("Incorrect date expire format from Moodle");
      throw e;
    } catch (org.pmiops.workbench.moodle.ApiException ex) {
      if (ex.getCode() == HttpStatus.NOT_FOUND.value()) {
        log.severe(
            String.format(
                "Error while retrieving Badge for user %s: %s ",
                user.getUserId(), ex.getMessage()));
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
  private DbUser setEraCommonsStatus(DbUser targetUser, NihStatus nihStatus) {
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
  public DbUser syncEraCommonsStatus() {
    DbUser user = userProvider.get();
    NihStatus nihStatus = fireCloudService.getNihStatus();
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
  public DbUser syncEraCommonsStatusUsingImpersonation(DbUser user)
      throws IOException, org.pmiops.workbench.firecloud.ApiException {
    if (isServiceAccount(user)) {
      // Skip sync for service account user rows.
      return user;
    }

    ApiClient apiClient = fireCloudService.getApiClientWithImpersonation(user.getEmail());
    NihApi api = new NihApi(apiClient);
    try {
      NihStatus nihStatus = api.nihStatus();
      return setEraCommonsStatus(user, nihStatus);
    } catch (org.pmiops.workbench.firecloud.ApiException e) {
      if (e.getCode() == HttpStatusCodes.STATUS_CODE_NOT_FOUND) {
        // We'll catch the NOT_FOUND ApiException here, since we expect many users to have an empty
        // eRA Commons linkage.
        log.info(String.format("NIH Status not found for user %s", user.getEmail()));
        return user;
      } else {
        throw e;
      }
    }
  }

  public void syncTwoFactorAuthStatus() {
    syncTwoFactorAuthStatus(userProvider.get());
  }

  /** */
  public DbUser syncTwoFactorAuthStatus(DbUser targetUser) {
    if (isServiceAccount(targetUser)) {
      // Skip sync for service account user rows.
      return targetUser;
    }

    return updateUserWithRetries(
        user -> {
          boolean isEnrolledIn2FA = directoryService.getUser(user.getEmail()).getIsEnrolledIn2Sv();
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
}
