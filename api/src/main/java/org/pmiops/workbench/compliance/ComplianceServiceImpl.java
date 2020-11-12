package org.pmiops.workbench.compliance;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import javax.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.google.CloudStorageService;
import org.pmiops.workbench.moodle.ApiException;
import org.pmiops.workbench.moodle.api.MoodleApi;
import org.pmiops.workbench.moodle.model.BadgeDetailsV1;
import org.pmiops.workbench.moodle.model.BadgeDetailsV2;
import org.pmiops.workbench.moodle.model.MoodleUserResponse;
import org.pmiops.workbench.moodle.model.UserBadgeResponseV1;
import org.pmiops.workbench.moodle.model.UserBadgeResponseV2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ComplianceServiceImpl implements ComplianceService {
  private static final Logger logger = Logger.getLogger(ComplianceServiceImpl.class.getName());

  private static final String RESPONSE_FORMAT = "json";
  private static final String GET_MOODLE_ID_SEARCH_FIELD = "email";
  private static final String MOODLE_EXCEPTION = "moodle_exception";
  private static final String MOODLE_USER_NOT_ALLOWED_ERROR_CODE = "guestsarenotallowed";

  private final CloudStorageService cloudStorageService;
  private final Provider<WorkbenchConfig> configProvider;
  private final Provider<MoodleApi> moodleApiProvider;


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
  @Deprecated
  public List<BadgeDetailsV1> getUserBadgeV1(int userMoodleId) throws ApiException {
    if (!enableMoodleCalls()) {
      return null;
    }

    UserBadgeResponseV1 response =
        moodleApiProvider.get().getMoodleBadgeV1(RESPONSE_FORMAT, getToken(), userMoodleId);
    if (response.getException() != null && response.getException().equals(MOODLE_EXCEPTION)) {
      if (response.getErrorcode().equals(MOODLE_USER_NOT_ALLOWED_ERROR_CODE)) {
        throw new ApiException(HttpStatus.NOT_FOUND.value(), response.getMessage());
      } else {
        throw new ApiException(response.getMessage());
      }
    }
    return response.getBadges();
  }

  /**
   * Returns a map of Moodle badge names to Moodle badge details for the given Moodle user email.
   *
   * @param username The research-aou.org email for the user
   * @return A map of badge name to badge details.
   * @throws ApiException if the Moodle API call returns an error because the email is not yet
   *     registered in Moodle.
   */
  @Override
  public Map<MoodleBadge, BadgeDetailsV2> getUserBadgesByBadgeName(String username) throws ApiException {
    if (!enableMoodleCalls()) {
      return new HashMap<>();
    }

    final UserBadgeResponseV2 response =
        moodleApiProvider.get().getMoodleBadgeV2(RESPONSE_FORMAT, getToken(), username);
    if (response.getException() != null && response.getException().equals(MOODLE_EXCEPTION)) {
      logger.warning(response.getMessage());
      if (response.getErrorcode().equals(MOODLE_USER_NOT_ALLOWED_ERROR_CODE)) {
        throw new ApiException(HttpStatus.NOT_FOUND.value(), response.getMessage());
      } else {
        throw new ApiException(response.getMessage());
      }
    }
    Map<MoodleBadge, BadgeDetailsV2> userBadgesByName = new HashMap<>();
    if (response.getDua() != null) {
      userBadgesByName.put(MoodleBadge.DATA_USE_AGREEMENT, response.getDua());
    }
    if (response.getRet() != null) {
      userBadgesByName.put(MoodleBadge.RESEARCH_ETHICS_TRAINING, response.getRet());
    }
    return userBadgesByName;
  }

  @Override
  public Optional<BadgeDetailsV2> getUserBadgeDetails(String username, MoodleBadge moodleBadge) throws ApiException {
    return Optional.ofNullable(getUserBadgesByBadgeName(username).get(moodleBadge));
  }
}
