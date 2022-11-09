package org.pmiops.workbench.survey;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.pmiops.workbench.access.AccessTierService;
import org.pmiops.workbench.db.dao.UserAccessTierDao;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NewUserSatisfactionSurveyServiceImpl implements NewUserSatisfactionSurveyService {

  private final UserAccessTierDao userAccessTierDao;
  private AccessTierService accessTierService;
  private final Clock clock;

  @Autowired
  public NewUserSatisfactionSurveyServiceImpl(
      UserAccessTierDao userAccessTierDao, AccessTierService accessTierService, Clock clock) {
    this.userAccessTierDao = userAccessTierDao;
    this.accessTierService = accessTierService;
    this.clock = clock;
  }

  @Override
  public boolean eligibleToTakeSurvey(DbUser user) {
    if (user.getNewUserSatisfactionSurvey() != null) {
      return false;
    }

    DbAccessTier registeredAccessTier = accessTierService.getRegisteredTierOrThrow();

    return userAccessTierDao
        .getByUserAndAccessTier(user, registeredAccessTier)
        .map(
            userRegisteredAccessTier -> {
              final Instant enabledTime = userRegisteredAccessTier.getFirstEnabled().toInstant();

              final Instant windowStart = enabledTime.plus(2 * 7, ChronoUnit.DAYS);
              final Instant windowEnd = windowStart.plus(61, ChronoUnit.DAYS);

              final Instant now = clock.instant();

              return now.isAfter(windowStart) && now.isBefore(windowEnd);
            })
        .orElse(false);
  }
}
