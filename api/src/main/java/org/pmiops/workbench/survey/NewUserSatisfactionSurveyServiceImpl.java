package org.pmiops.workbench.survey;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.pmiops.workbench.access.AccessTierService;
import org.pmiops.workbench.db.dao.UserAccessTierDao;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserAccessTier;
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

    DbAccessTier registeredAccessTier = this.accessTierService.getRegisteredTierOrThrow();

    Optional<DbUserAccessTier> userRegisteredAccessTier =
        this.userAccessTierDao.getByUserAndAccessTier(user, registeredAccessTier);

    if (!userRegisteredAccessTier.isPresent()) {
      return false;
    }

    Instant enabledTime = userRegisteredAccessTier.get().getFirstEnabled().toInstant();

    final Instant twoWeeksAgo = clock.instant().minus(2 * 7, ChronoUnit.DAYS);
    final boolean enabledMoreThanTwoWeeksAgo = enabledTime.isBefore(twoWeeksAgo);

    final Instant twoMonthsTwoWeeksAgo = twoWeeksAgo.minus(61, ChronoUnit.DAYS);
    final boolean eligibleForLessThanTwoMonths = enabledTime.isAfter(twoMonthsTwoWeeksAgo);

    return enabledMoreThanTwoWeeksAgo && eligibleForLessThanTwoMonths;
  }
}
