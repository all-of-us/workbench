package org.pmiops.workbench.survey;

import java.time.Instant;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.model.CreateNewUserSatisfactionSurvey;

public interface NewUserSatisfactionSurveyService {
  boolean eligibleToTakeSurvey(DbUser user);

  Instant eligibilityWindowEnd(DbUser user);

  boolean oneTimeCodeStringValid(String oneTimeCode);

  void createNewUserSatisfactionSurveyWithOneTimeCode(
      CreateNewUserSatisfactionSurvey createNewUserSatisfactionSurvey, String oneTimeCode)
      throws InvalidOneTimeCodeException;
}
