package org.pmiops.workbench.survey;

import java.time.Instant;
import org.pmiops.workbench.db.model.DbUser;

public interface NewUserSatisfactionSurveyService {
  boolean eligibleToTakeSurvey(DbUser user);

  Instant eligibilityWindowEnd(DbUser user);
}
