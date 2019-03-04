package org.pmiops.workbench.db.dao;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Provider;

import org.pmiops.workbench.compliance.ComplianceService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.model.AdminActionHistory;
import org.pmiops.workbench.db.model.CommonStorageEnums;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.model.BillingProjectStatus;
import org.pmiops.workbench.model.DataAccessLevel;
import org.pmiops.workbench.model.EmailVerificationStatus;
import org.pmiops.workbench.moodle.ApiException;
import org.pmiops.workbench.moodle.model.BadgeDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

/**
 * User state manipulation; handles version conflicts.
 */
@Service
public class UserService {

  private final int MAX_RETRIES = 3;

  private final Provider<User> userProvider;
  private final UserDao userDao;
  private final AdminActionHistoryDao adminActionHistoryDao;
  private final Clock clock;
  private final Random random;
  private final FireCloudService fireCloudService;
  private final Provider<WorkbenchConfig> configProvider;
  private final ComplianceService complianceService;
  private static final Logger log = Logger.getLogger(UserService.class.getName());

  @Autowired
  public UserService(Provider<User> userProvider,
      UserDao userDao,
      AdminActionHistoryDao adminActionHistoryDao,
      Clock clock,
      Random random,
      FireCloudService fireCloudService,
      Provider<WorkbenchConfig> configProvider,
      ComplianceService complianceService) {
    this.userProvider = userProvider;
    this.userDao = userDao;
    this.adminActionHistoryDao = adminActionHistoryDao;
    this.clock = clock;
    this.random = random;
    this.fireCloudService = fireCloudService;
    this.configProvider = configProvider;
    this.complianceService = complianceService;
  }

  /**
   * Updates a user record with a modifier function.
   *
   * Ensures that the data access level for the user reflects the state of other fields on the
   * user; handles conflicts with concurrent updates by retrying.
   */
  private User updateWithRetries(Function<User, User> modifyUser) {
    User user = userProvider.get();
    return updateWithRetries(modifyUser, user);
  }

  private User updateWithRetries(Function<User, User> userModifier, User user) {
    int numAttempts = 0;
    while (true) {
      user = userModifier.apply(user);
      updateDataAccessLevel(user);
      try {
        user = userDao.save(user);
        return user;
      } catch (ObjectOptimisticLockingFailureException e) {
        if (numAttempts < MAX_RETRIES) {
          user = userDao.findOne(user.getUserId());
          numAttempts++;
        } else {
          throw new ConflictException(String.format("Could not update user %s", user.getUserId()));
        }
      }
    }
  }

  private void updateDataAccessLevel(User user) {
    Timestamp now = new Timestamp(clock.instant().toEpochMilli());
    boolean shouldBeRegistered = Optional.ofNullable(user.getIdVerificationIsValid()).orElse(false)
        && (!configProvider.get().access.enableEraCommons || user.getEraLinkedNihUsername() != null)
        && user.getDemographicSurveyCompletionTime() != null
        && (!configProvider.get().access.enableComplianceTraining || user.getTrainingCompletionTime() != null)
        && user.getTermsOfServiceCompletionTime() != null
        && !user.getDisabled()
        && EmailVerificationStatus.SUBSCRIBED.equals(user.getEmailVerificationStatusEnum());
    boolean isInGroup = this.fireCloudService.
            isUserMemberOfGroup(configProvider.get().firecloud.registeredDomainName);
    if (shouldBeRegistered) {
      if (!isInGroup) {
        this.fireCloudService.addUserToGroup(user.getEmail(),
            configProvider.get().firecloud.registeredDomainName);
      }
      user.setDataAccessLevelEnum(DataAccessLevel.REGISTERED);
    } else {
      if (isInGroup) {
        this.fireCloudService.removeUserFromGroup(user.getEmail(),
            configProvider.get().firecloud.registeredDomainName);
      }
      user.setDataAccessLevelEnum(DataAccessLevel.UNREGISTERED);
    }
  }

  public User createServiceAccountUser(String email) {
    User user = new User();
    user.setDataAccessLevelEnum(DataAccessLevel.PROTECTED);
    user.setEmail(email);
    user.setDisabled(false);
    user.setEmailVerificationStatusEnum(EmailVerificationStatus.UNVERIFIED);
    user.setFreeTierBillingProjectStatusEnum(BillingProjectStatus.NONE);
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

  public User createUser(String givenName,
      String familyName,
      String email,
      String contactEmail,
      String currentPosition,
      String organization,
      String areaOfResearch) {
    User user = new User();
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
    user.setFreeTierBillingProjectStatusEnum(BillingProjectStatus.NONE);
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

  public User submitTermsOfService() {
    final Timestamp timestamp = new Timestamp(clock.instant().toEpochMilli());
    return updateWithRetries(new Function<User, User>() {
      @Override
      public User apply(User user) {
        user.setTermsOfServiceCompletionTime(timestamp);
        return user;
      }
    });
  }

  public User submitEthicsTraining() {
    final Timestamp timestamp = new Timestamp(clock.instant().toEpochMilli());
    return updateWithRetries(new Function<User, User>() {
      @Override
      public User apply(User user) {
        user.setTrainingCompletionTime(timestamp);
        return user;
      }
    });
  }

  public User submitDemographicSurvey() {
    final Timestamp timestamp = new Timestamp(clock.instant().toEpochMilli());
    return updateWithRetries(new Function<User, User>() {
      @Override
      public User apply(User user) {
        user.setDemographicSurveyCompletionTime(timestamp);
        return user;
      }
    });
  }

  public User setClusterRetryCount(int clusterRetryCount) {
    return updateWithRetries(new Function<User, User>() {
      @Override
      public User apply(User user) {
        user.setClusterCreateRetries(clusterRetryCount);
        return user;
      }
    });
  }

  public User setBillingRetryCount(int billingRetryCount) {
    return updateWithRetries(new Function<User, User>() {
      @Override
      public User apply(User user) {
        user.setBillingProjectRetries(billingRetryCount);
        return user;
      }
    });
  }

  public User setBillingProjectNameAndStatus(String name, BillingProjectStatus status) {
    return updateWithRetries(new Function<User, User>() {
      @Override
      public User apply(User user) {
        user.setFreeTierBillingProjectName(name);
        user.setFreeTierBillingProjectStatusEnum(status);
        return user;
      }
    });
  }

  public User setDisabledStatus(Long userId, boolean disabled) {
    User user = userDao.findUserByUserId(userId);
    return updateWithRetries(new Function<User, User>() {
      @Override
      public User apply(User user) {
        user.setDisabled(disabled);
        return user;
      }
    }, user);
  }

  public List<User> getNonVerifiedUsers() {
    return userDao.findUserNotValidated();
  }

  public List<User> getAllUsers() {
    return userDao.findUsers();
  }

  public User setIdVerificationApproved(Long userId, boolean blockscoreVerificationIsValid) {
    User user = userDao.findUserByUserId(userId);
    return updateWithRetries(new Function<User, User>() {
      @Override
      public User apply(User user) {
        user.setIdVerificationIsValid(blockscoreVerificationIsValid);
        return user;
      }
    }, user);
  }

  public void logAdminUserAction(long targetUserId, String targetAction, Object oldValue, Object newValue) {
    logAdminAction(targetUserId,null, targetAction, oldValue,  newValue);
  }

  public void logAdminWorkspaceAction(long targetWorkspaceId, String targetAction, Object oldValue, Object newValue) {
    logAdminAction(null, targetWorkspaceId, targetAction, oldValue, newValue);
  }

  private void logAdminAction(Long targetUserId, Long targetWorkspaceId, String targetAction, Object oldValue, Object newValue) {
    AdminActionHistory adminActionHistory = new AdminActionHistory();
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

  /**
   * Find users matching the user's name or email
   */
  public List<User> findUsersBySearchString(String term, Sort sort) {
    List<Short> dataAccessLevels;
    if (configProvider.get().firecloud.enforceRegistered) {
      dataAccessLevels = Stream.of(DataAccessLevel.REGISTERED, DataAccessLevel.PROTECTED)
          .map(CommonStorageEnums::dataAccessLevelToStorage)
          .collect(Collectors.toList());
    } else {
      dataAccessLevels = Stream.of(DataAccessLevel.values())
          .map(CommonStorageEnums::dataAccessLevelToStorage)
          .collect(Collectors.toList());
    }
    return userDao.findUsersByDataAccessLevelsAndSearchString(dataAccessLevels, term, sort);
  }

  /**
   * This methods updates user's training status from Moodle.
   * 1. Check if user have moodle_id,
   *    a. if not retrieve it from MOODLE API and save it in the Database
   * 2. Using the MOODLE_ID get user's Badge update the database with
   *    a. training completion time as current time
   *    b. training expiration date with as returned from MOODLE.
   * 3. If there are no badges for a user set training completion time and expiration date as null
   * @param user
   * @return
   */
  public void syncUserTraining(User user) throws ApiException, NotFoundException {
    Timestamp now = new Timestamp(clock.instant().toEpochMilli());
    try {
      Integer moodleId = user.getMoodleId();
      if (moodleId == null) {
        moodleId = complianceService.getMoodleId(user.getEmail());
        if (moodleId == null) {
          // User has not yet created/logged into MOODLE
          return;
        }
        user.setMoodleId(moodleId);
      }
      List<BadgeDetails> badgeResponse = complianceService.getUserBadge(moodleId);
      // The assumption here is that the User will always get 1 badge which will be AoU
      if (badgeResponse != null && badgeResponse.size() > 0) {
        BadgeDetails badge = badgeResponse.get(0);
        if (badge.getDateexpire() == null) {
          //This can happen if date expire is set to never
          user.setTrainingExpirationTime(null);
        } else {
          user.setTrainingExpirationTime(new Timestamp(Long.parseLong(badge.getDateexpire())));
        }
        user.setTrainingCompletionTime(now);
        userDao.save(user);
      }
    } catch (NumberFormatException e) {
      log.severe("Incorrect date expire format from Moodle");
      throw e;
    } catch (ApiException ex) {
      if (ex.getCode() == HttpStatus.NOT_FOUND.value()) {
        log.severe(String.format("Error while retrieving Badge for user %s: %s ",
            user.getUserId(), ex.getMessage()));
        throw new NotFoundException(ex.getMessage());
      }
      throw ex;
    }
  }
}
