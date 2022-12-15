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

    final Instant now = clock.instant();
    return now.isAfter(eligibilityWindowStart(user)) && now.isBefore(eligibilityWindowEnd(user));
  }

  private Instant eligibilityWindowStart(DbUser user) {
    final Instant createdTime = user.getCreationTime().toInstant();

    return createdTime.plus(2 * 7, ChronoUnit.DAYS);
  }

  @Override
  public Instant eligibilityWindowEnd(DbUser user) {
    return eligibilityWindowStart(user).plus(61, ChronoUnit.DAYS);
  }
}
