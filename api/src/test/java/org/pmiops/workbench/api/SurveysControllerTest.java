package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.db.dao.NewUserSatisfactionSurveyDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.model.DbNewUserSatisfactionSurvey;
import org.pmiops.workbench.db.model.DbNewUserSatisfactionSurvey.Satisfaction;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.model.CreateNewUserSatisfactionSurvey;
import org.pmiops.workbench.model.CreateNewUserSatisfactionSurveyWithOneTimeCode;
import org.pmiops.workbench.model.NewUserSatisfactionSurveySatisfaction;
import org.pmiops.workbench.survey.NewUserSatisfactionSurveyMapperImpl;
import org.pmiops.workbench.survey.NewUserSatisfactionSurveyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;

@DataJpaTest
public class SurveysControllerTest {
  @MockBean NewUserSatisfactionSurveyService newUserSatisfactionSurveyService;

  @TestConfiguration
  @Import({
    FakeClockConfiguration.class,
    SurveysController.class,
    NewUserSatisfactionSurveyMapperImpl.class
  })
  static class Configuration {
    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    DbUser user() {
      return user;
    }
  }

  private static DbUser user;

  @BeforeEach
  public void setUp() {
    user = new DbUser();
    user = userDao.save(user);
  }

  @Autowired UserDao userDao;
  @Autowired SurveysController surveysController;
  @Autowired NewUserSatisfactionSurveyDao newUserSatisfactionSurveyDao;
  @Autowired EntityManager entityManager;

  private CreateNewUserSatisfactionSurvey createValidFormData() {
    return new CreateNewUserSatisfactionSurvey()
        .satisfaction(NewUserSatisfactionSurveySatisfaction.VERY_SATISFIED)
        .additionalInfo("Love it!");
  }

  private CreateNewUserSatisfactionSurveyWithOneTimeCode createValidFormDataWithOneTimeCode() {
    return new CreateNewUserSatisfactionSurveyWithOneTimeCode()
        .createNewUserSatisfactionSurvey(createValidFormData())
        .oneTimeCode("abc");
  }

  @Test
  public void testCreateNewUserSatisfactionSurvey() {
    final CreateNewUserSatisfactionSurvey createNewUserSatisfactionSurvey = createValidFormData();
    when(newUserSatisfactionSurveyService.eligibleToTakeSurvey(user)).thenReturn(true);

    surveysController.createNewUserSatisfactionSurvey(createNewUserSatisfactionSurvey);
    entityManager.refresh(user);

    DbNewUserSatisfactionSurvey newUserSatisfactionSurvey = user.getNewUserSatisfactionSurvey();
    assertThat(newUserSatisfactionSurvey.getSatisfaction()).isEqualTo(Satisfaction.VERY_SATISFIED);
    assertThat(newUserSatisfactionSurvey.getAdditionalInfo())
        .isEqualTo(createNewUserSatisfactionSurvey.getAdditionalInfo());
    assertThat(newUserSatisfactionSurvey.getUser()).isEqualTo(user);
  }

  @Test
  public void testCreateNewUserSatisfactionSurvey_failsIfUserIneligible() {
    final CreateNewUserSatisfactionSurvey createNewUserSatisfactionSurvey = createValidFormData();
    when(newUserSatisfactionSurveyService.eligibleToTakeSurvey(user)).thenReturn(false);

    assertThrows(
        BadRequestException.class,
        () -> surveysController.createNewUserSatisfactionSurvey(createNewUserSatisfactionSurvey));
  }

  @Test
  public void testValidateOneTimeCodeForNewUserSatisfactionSurvey() {
    final String oneTimeCode = "abc";
    when(newUserSatisfactionSurveyService.oneTimeCodeStringValid(oneTimeCode)).thenReturn(true);
    assertThat(
            surveysController
                .validateOneTimeCodeForNewUserSatisfactionSurvey(oneTimeCode)
                .getBody())
        .isEqualTo(true);
  }

  @Test
  public void testCreateNewUserSatisfactionSurveyWithOneTimeCode() {
    final CreateNewUserSatisfactionSurveyWithOneTimeCode requestBody =
        createValidFormDataWithOneTimeCode();

    surveysController.createNewUserSatisfactionSurveyWithOneTimeCode(requestBody);

    verify(newUserSatisfactionSurveyService)
        .createNewUserSatisfactionSurveyWithOneTimeCode(
            requestBody.getCreateNewUserSatisfactionSurvey(), requestBody.getOneTimeCode());
  }
}
