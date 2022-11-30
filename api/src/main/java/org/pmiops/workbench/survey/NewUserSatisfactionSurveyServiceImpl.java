package org.pmiops.workbench.survey;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.pmiops.workbench.db.model.DbUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NewUserSatisfactionSurveyServiceImpl implements NewUserSatisfactionSurveyService {
  private final Clock clock;

  @Autowired
  public NewUserSatisfactionSurveyServiceImpl(Clock clock) {
    this.clock = clock;
  }

  @Override
  public boolean eligibleToTakeSurvey(DbUser user) {
    if (user.getNewUserSatisfactionSurvey() != null) {
      return false;
    }

    final Instant createdTime = user.getCreationTime().toInstant();

    final Instant windowStart = createdTime.plus(2 * 7, ChronoUnit.DAYS);
    final Instant windowEnd = windowStart.plus(61, ChronoUnit.DAYS);

    final Instant now = clock.instant();

    return now.isAfter(windowStart) && now.isBefore(windowEnd);
  }
}
