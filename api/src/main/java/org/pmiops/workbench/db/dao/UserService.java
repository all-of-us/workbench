package org.pmiops.workbench.db.dao;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.List;
import java.util.function.Function;
import javax.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.firecloud.ApiException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.model.DataAccessLevel;
import org.pmiops.workbench.model.EmailVerificationStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
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
  private final Clock clock;
  private final FireCloudService fireCloudService;
  private final Provider<WorkbenchConfig> configProvider;

  @Autowired
  public UserService(Provider<User> userProvider,
      UserDao userDao,
      Clock clock,
      FireCloudService fireCloudService,
      Provider<WorkbenchConfig> configProvider) {
    this.userProvider = userProvider;
    this.userDao = userDao;
    this.clock = clock;
    this.fireCloudService = fireCloudService;
    this.configProvider = configProvider;
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
    if (user.getDataAccessLevel() == DataAccessLevel.UNREGISTERED) {
      if (user.getBlockscoreVerificationIsValid() != null
          && user.getBlockscoreVerificationIsValid()
          && user.getDemographicSurveyCompletionTime() != null
          && user.getEthicsTrainingCompletionTime() != null
          && user.getTermsOfServiceCompletionTime() != null
          && user.getEmailVerificationStatus().equals(EmailVerificationStatus.VERIFIED)) {
        try {
          this.fireCloudService.addUserToGroup(user.getEmail(),
              configProvider.get().firecloud.registeredDomainName);
        } catch (ApiException e) {
          if (e.getCode() == 403) {
            throw new ForbiddenException(e.getResponseBody());
          } else if (e.getCode() == 404) {
            throw new NotFoundException(e.getResponseBody());
          } else {
            throw new ServerErrorException(e.getResponseBody());
          }
        }
        user.setDataAccessLevel(DataAccessLevel.REGISTERED);
      }
    }
  }

  public User createUser(String givenName, String familyName, String email, String contactEmail) {
    User user = new User();
    user.setDataAccessLevel(DataAccessLevel.UNREGISTERED);
    user.setEmail(email);
    user.setContactEmail(contactEmail);
    user.setFamilyName(familyName);
    user.setGivenName(givenName);
    user.setDisabled(false);
    user.setEmailVerificationStatus(EmailVerificationStatus.UNVERIFIED);
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

  public User setBlockscoreIdVerification(String blockscoreId, boolean blockscoreVerificationIsValid) {
    return updateWithRetries(new Function<User, User>() {
      @Override
      public User apply(User user) {
        user.setBlockscoreId(blockscoreId);
        user.setBlockscoreVerificationIsValid(blockscoreVerificationIsValid);
        return user;
      }
    });
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
        user.setEthicsTrainingCompletionTime(timestamp);
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

  public List<User> getNonVerifiedUsers() {
    return userDao.findUserNotValidated();
  }

  public User setIdVerificationApproved(Long userId, boolean blockscoreVerificationIsValid) {
    User user = userDao.findUserByUserId(userId);
    return updateWithRetries(new Function<User, User>() {
      @Override
      public User apply(User user) {
        user.setBlockscoreVerificationIsValid(blockscoreVerificationIsValid);
        return user;
      }
    }, user);
  }
}
