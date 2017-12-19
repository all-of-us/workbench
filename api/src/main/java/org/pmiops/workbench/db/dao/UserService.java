package org.pmiops.workbench.db.dao;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.function.Function;
import javax.inject.Provider;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.model.DataAccessLevel;
import org.springframework.beans.factory.annotation.Autowired;
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

  @Autowired
  public UserService(Provider<User> userProvider, UserDao userDao, Clock clock) {
    this.userProvider = userProvider;
    this.userDao = userDao;
    this.clock = clock;
  }

  /**
   * Updates a user record with a modifier function.
   *
   * Ensures that the data access level for the user reflects the state of other fields on the
   * user; handles conflicts with concurrent updates by retrying.
   */
  User updateWithRetries(Function<User, User> userModifier) {
    User user = userProvider.get();
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
      if (user.getBlockscoreVerificationIsValid()
        && user.getDemographicSurveyCompletionTime() != null
        && user.getEthicsTrainingCompletionTime() != null
        && user.getTermsOfServiceCompletionTime() != null) {
        user.setDataAccessLevel(DataAccessLevel.REGISTERED);
      }
    }
  }

  public User setIdVerification(String blockscoreId, boolean blockscoreVerificationIsValid) {
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
    return updateWithRetries(new Function<User, User>() {
      @Override
      public User apply(User user) {
        user.setTermsOfServiceCompletionTime(new Timestamp(clock.instant().toEpochMilli()));
        return user;
      }
    });
  }

  public User submitEthicsTraining() {
    return updateWithRetries(new Function<User, User>() {
      @Override
      public User apply(User user) {
        user.setEthicsTrainingCompletionTime(new Timestamp(clock.instant().toEpochMilli()));
        return user;
      }
    });
  }

  public User submitDemographicSurvey() {
    return updateWithRetries(new Function<User, User>() {
      @Override
      public User apply(User user) {
        user.setDemographicSurveyCompletionTime(new Timestamp(clock.instant().toEpochMilli()));
        return user;
      }
    });
  }
}
