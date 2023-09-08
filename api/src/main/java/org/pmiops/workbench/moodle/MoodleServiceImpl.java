package org.pmiops.workbench.moodle;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.inject.Provider;
import org.pmiops.workbench.google.CloudStorageClient;
import org.pmiops.workbench.moodle.api.MoodleApi;
import org.pmiops.workbench.moodle.model.BadgeDetailsV2;
import org.pmiops.workbench.moodle.model.UserBadgeResponseV2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class MoodleServiceImpl implements MoodleService {
  private static final String RESPONSE_FORMAT = "json";
  private static final String MOODLE_EXCEPTION = "moodle_exception";
  private static final String MOODLE_USER_NOT_ALLOWED_ERROR_CODE = "guestsarenotallowed";

  private final CloudStorageClient cloudStorageClient;
  private final Provider<MoodleApi> moodleApiProvider;

  private static final Logger logger = Logger.getLogger(MoodleServiceImpl.class.getName());

  @Autowired
  public MoodleServiceImpl(
      CloudStorageClient cloudStorageClient, Provider<MoodleApi> moodleApiProvider) {
    this.cloudStorageClient = cloudStorageClient;
    this.moodleApiProvider = moodleApiProvider;
  }

  private String getToken() {
    return this.cloudStorageClient.getMoodleApiKey();
  }

  /**
   * Returns a map of Moodle badge names to Moodle badge details for the given Moodle user email.
   *
   * @param email The research-aou.org email for the user
   * @return A map of badge name to badge details.
   * @throws ApiException if the Moodle API call returns an error because the email is not yet
   *     registered in Moodle.
   */
  @Override
  public Map<BadgeName, BadgeDetailsV2> getUserBadgesByBadgeName(String email) throws ApiException {
    UserBadgeResponseV2 response =
        moodleApiProvider.get().getMoodleBadgeV2(RESPONSE_FORMAT, getToken(), email);
    if (response.getException() != null && response.getException().equals(MOODLE_EXCEPTION)) {
      logger.warning(response.getMessage());
      if (response.getErrorcode().equals(MOODLE_USER_NOT_ALLOWED_ERROR_CODE)) {
        throw new ApiException(HttpStatus.NOT_FOUND.value(), response.getMessage());
      } else {
        throw new ApiException(response.getMessage());
      }
    }
    Map<BadgeName, BadgeDetailsV2> userBadgesByName = new HashMap<>();
    // "dua" and "ret" are confusingly named, but according to Moodle team this mapping is correct.
    // See RW-7438 for details.
    if (response.getDua() != null) {
      userBadgesByName.put(BadgeName.CONTROLLED_TIER_TRAINING, response.getDua());
    }
    if (response.getRet() != null) {
      userBadgesByName.put(BadgeName.REGISTERED_TIER_TRAINING, response.getRet());
    }
    return userBadgesByName;
  }
}
