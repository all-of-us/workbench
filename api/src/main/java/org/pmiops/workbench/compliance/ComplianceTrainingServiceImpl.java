package org.pmiops.workbench.compliance;

import org.pmiops.workbench.moodle.ApiException;
import org.pmiops.workbench.moodle.api.MoodleApi;
import org.pmiops.workbench.moodle.model.MoodleUserResponse;
import org.pmiops.workbench.moodle.model.UserBadgeResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ComplianceTrainingServiceImpl implements ComplianceTrainingService {

  private MoodleApi api = new MoodleApi();
  private static final String RESPONSE_FORMAT = "json";
  private static final String TOKEN = "d106acbb7eb050a10e9b200eb82b8be8";
  private static final String GET_MOODLE_ID_SEARCH_FIELD = "email";
  private static final String GET_MOODLE_ID_FUNCTION = "core_user_get_users_by_field";
  private static final String MOODLE_EXCEPTION = "moodle_exception";
  private static final String MOODLE_ERROR_CODE = "guestsarenotallowed";

  @Override
  public int getMoodleId(String email) throws ApiException {
    List<MoodleUserResponse> response = api.getMoodleId(GET_MOODLE_ID_FUNCTION, RESPONSE_FORMAT,
        TOKEN, GET_MOODLE_ID_SEARCH_FIELD, email);
    if (response.size() == 0) {
      throw new ApiException(HttpStatus.NOT_FOUND.value(),
          "User not found while trying to retrieve moodle Id");
    }
    return response.get(0).getId();
  }

  @Override
  public Map<String, Timestamp> getUserBadge(int userId) throws ApiException {
    UserBadgeResponse response = api.getMoodleBadge(RESPONSE_FORMAT, TOKEN, userId);
    if (response.getException() != null && response.getException().equals(MOODLE_EXCEPTION)) {
      if (response.getErrorcode().equals(MOODLE_ERROR_CODE))
        throw new ApiException(HttpStatus.NOT_FOUND.value(), response.getMessage());
      else
        throw new ApiException(response.getMessage());
    }
    Map<String, Timestamp> badgeDateExpiryMap = new HashMap<>();
    if (response.getBadges() != null ) {
      response.getBadges().forEach(badge -> {
        badgeDateExpiryMap.put(
            badge.getName(),
            badge.getDateexpire() != null ? new Timestamp(Long.parseLong(badge.getDateexpire())) : null
        );
      });
    }

    return badgeDateExpiryMap;
  }
}
