package org.pmiops.workbench.compliance;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.inject.Provider;
import org.pmiops.workbench.google.CloudStorageClient;
import org.pmiops.workbench.moodle.ApiException;
import org.pmiops.workbench.moodle.api.MoodleApi;
import org.pmiops.workbench.moodle.model.BadgeDetailsV2;
import org.pmiops.workbench.moodle.model.UserBadgeResponseV2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ComplianceServiceImpl implements ComplianceService {
  private static final String RESPONSE_FORMAT = "json";
  private static final String DUA_BADGE_NAME = "data_use_agreement";
  private static final String RET_BADGE_NAME =
      "research_ethics_training"; // 'ret' too much like 'return'
  private static final String MOODLE_EXCEPTION = "moodle_exception";
  private static final String MOODLE_USER_NOT_ALLOWED_ERROR_CODE = "guestsarenotallowed";

  private final CloudStorageClient cloudStorageClient;
  private final Provider<MoodleApi> moodleApiProvider;

  private static final Logger logger = Logger.getLogger(ComplianceServiceImpl.class.getName());

  @Autowired
  public ComplianceServiceImpl(
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
  public Map<String, BadgeDetailsV2> getUserBadgesByBadgeName(String email) throws ApiException {
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
    Map<String, BadgeDetailsV2> userBadgesByName = new HashMap<>();
    if (response.getDua() != null) {
      userBadgesByName.put(DUA_BADGE_NAME, response.getDua());
    }
    if (response.getRet() != null) {
      userBadgesByName.put(RET_BADGE_NAME, response.getRet());
    }
    return userBadgesByName;
  }

  public String getResearchEthicsTrainingField() {
    return RET_BADGE_NAME;
  }
}
