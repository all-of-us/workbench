package org.pmiops.workbench.api;

import javax.inject.Provider;
import org.pmiops.workbench.db.dao.NewUserSatisfactionSurveyDao;
import org.pmiops.workbench.db.model.DbNewUserSatisfactionSurvey;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.model.CreateNewUserSatisfactionSurvey;
import org.pmiops.workbench.survey.NewUserSatisfactionSurveyMapper;
import org.pmiops.workbench.survey.NewUserSatisfactionSurveyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SurveysController implements SurveysApiDelegate {
  private final NewUserSatisfactionSurveyDao newUserSatisfactionSurveyDao;
  private final Provider<DbUser> userProvider;
  private final NewUserSatisfactionSurveyMapper newUserSatisfactionSurveyMapper;
  private final NewUserSatisfactionSurveyService newUserSatisfactionSurveyService;

  @Autowired
  SurveysController(
      NewUserSatisfactionSurveyDao newUserSatisfactionSurveyDao,
      Provider<DbUser> userProvider,
      NewUserSatisfactionSurveyMapper newUserSatisfactionSurveyMapper,
      NewUserSatisfactionSurveyService newUserSatisfactionSurveyService) {
    this.newUserSatisfactionSurveyDao = newUserSatisfactionSurveyDao;
    this.userProvider = userProvider;
    this.newUserSatisfactionSurveyMapper = newUserSatisfactionSurveyMapper;
    this.newUserSatisfactionSurveyService = newUserSatisfactionSurveyService;
  }

  @Override
  public ResponseEntity<Void> createNewUserSatisfactionSurvey(
      CreateNewUserSatisfactionSurvey createNewUserSatisfactionSurvey) {
    DbUser user = userProvider.get();
    if (!newUserSatisfactionSurveyService.eligibleToTakeSurvey(user)) {
      throw new BadRequestException("User is ineligible to take this survey.");
    }
    DbNewUserSatisfactionSurvey newUserSatisfactionSurvey =
        newUserSatisfactionSurveyMapper.toDbNewUserSatisfactionSurvey(
            createNewUserSatisfactionSurvey, user);
    newUserSatisfactionSurvey = newUserSatisfactionSurveyDao.save(newUserSatisfactionSurvey);
    return new ResponseEntity<>(HttpStatus.OK);
  }
}
