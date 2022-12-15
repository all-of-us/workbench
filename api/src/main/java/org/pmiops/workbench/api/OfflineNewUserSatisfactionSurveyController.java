package org.pmiops.workbench.api;

import org.pmiops.workbench.survey.NewUserSatisfactionSurveyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OfflineNewUserSatisfactionSurveyController
    implements OfflineNewUserSatisfactionSurveyApiDelegate {
  private final NewUserSatisfactionSurveyService newUserSatisfactionSurveyService;

  @Autowired
  public OfflineNewUserSatisfactionSurveyController(
      NewUserSatisfactionSurveyService newUserSatisfactionSurveyService) {
    this.newUserSatisfactionSurveyService = newUserSatisfactionSurveyService;
  }

  @Override
  public ResponseEntity<Void> emailNewUserSatisfactionSurveyLinks() {
    newUserSatisfactionSurveyService.emailNewUserSatisfactionSurveyLinks();
    return ResponseEntity.noContent().build();
  }
}
