package org.pmiops.workbench.compliance;

import java.util.List;
import javax.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.google.CloudStorageService;
import org.pmiops.workbench.moodle.ApiException;
import org.pmiops.workbench.moodle.api.MoodleApi;
import org.pmiops.workbench.moodle.model.BadgeDetails;
import org.pmiops.workbench.moodle.model.MoodleUserResponse;
import org.pmiops.workbench.moodle.model.UserBadgeResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ComplianceServiceImpl implements ComplianceService {

  private MoodleApi api = new MoodleApi();
  private static final String RESPONSE_FORMAT = "json";
  private static final String GET_MOODLE_ID_SEARCH_FIELD = "email";
  private static final String MOODLE_EXCEPTION = "moodle_exception";
  private static final String MOODLE_USER_NOT_ALLOWED_ERROR_CODE = "guestsarenotallowed";
  private CloudStorageService cloudStorageService;
  private Provider<WorkbenchConfig> configProvider;

  private Provider<MoodleApi> moodleApiProvider;

  @Autowired
  public ComplianceServiceImpl(
      CloudStorageService cloudStorageService,
      Provider<WorkbenchConfig> configProvider,
      Provider<MoodleApi> moodleApiProvider) {
    this.cloudStorageService = cloudStorageService;
    this.configProvider = configProvider;
    this.moodleApiProvider = moodleApiProvider;
  }

  private String getToken() {
    return this.cloudStorageService.getMoodleApiKey();
  }

  private boolean enableMoodleCalls() {
    return configProvider.get().moodle.enableMoodleBackend;
  }

  /**
   * Returns the Moodle ID corresponding to the given AoU user email address.
   *
   * <p>Returns null if no Moodle user ID was found.
   */
  @Override
  public Integer getMoodleId(String email) throws ApiException {
    if (!enableMoodleCalls()) {
      return null;
    }
    List<MoodleUserResponse> response =
        moodleApiProvider.get().getMoodleId(getToken(), GET_MOODLE_ID_SEARCH_FIELD, email);
    if (response.size() == 0) {
      return null;
    }
    return response.get(0).getId();
  }

  /**
   * Returns the Moodle user badge for the given Moodle user ID.
   *
   * <p>Throws a NOT_FOUND API exception if the Moodle API call returns an error because the given
   * Moodle user ID does not exist.
   */
  @Override
  public List<BadgeDetails> getUserBadge(int userMoodleId) throws ApiException {
    if (!enableMoodleCalls()) {
      return null;
    }
    UserBadgeResponse response;
    if(configProvider.get().featureFlags.enableMoodleV2Api) {
      response = moodleApiProvider.get().getMoodleBadge(RESPONSE_FORMAT, getToken(), userMoodleId);
    }
    else {
      response = moodleApiProvider.get().getMoodleBadgeV1(RESPONSE_FORMAT, getToken(), userMoodleId);
    }
    System.err.println(response);
    if (response.getException() != null && response.getException().equals(MOODLE_EXCEPTION)) {
      if (response.getErrorcode().equals(MOODLE_USER_NOT_ALLOWED_ERROR_CODE)) {
        throw new ApiException(HttpStatus.NOT_FOUND.value(), response.getMessage());
      } else {
        throw new ApiException(response.getMessage());
      }
    }
    return response.getBadges();
  }
}
