package org.pmiops.workbench.survey;

import org.pmiops.workbench.db.model.DbUser;

public interface NewUserSatisfactionSurveyService {
  boolean eligibleToTakeSurvey(DbUser user);
}
