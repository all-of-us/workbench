package org.pmiops.workbench.db.dao;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.Iterables;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.config.CommonConfig;
import org.pmiops.workbench.db.model.DbNewUserSatisfactionSurveyResponse;
import org.pmiops.workbench.db.model.DbNewUserSatisfactionSurveyResponse.Satisfaction;
import org.pmiops.workbench.db.model.DbUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@Import({FakeClockConfiguration.class, CommonConfig.class})
@DataJpaTest
public class NewUserSatisfactionSurveyResponseDaoTest {
  @Autowired private UserDao userDao;
  @Autowired NewUserSatisfactionSurveyResponseDao newUserSatisfactionSurveyResponseDao;

  private DbUser user;
  private DbNewUserSatisfactionSurveyResponse newUserSatisfactionSurveyResponse;

  @Test
  public void testCRUD() {
    user = userDao.save(new DbUser());
    newUserSatisfactionSurveyResponse =
        new DbNewUserSatisfactionSurveyResponse()
            .setUser(user)
            .setSatisfaction(Satisfaction.SATISFIED)
            .setAdditionalInfo("");
    newUserSatisfactionSurveyResponseDao.save(newUserSatisfactionSurveyResponse);

    assertThat(
            newUserSatisfactionSurveyResponseDao
                .findById(newUserSatisfactionSurveyResponse.getId())
                .get())
        .isEqualTo(newUserSatisfactionSurveyResponse);

    newUserSatisfactionSurveyResponseDao.save(
        newUserSatisfactionSurveyResponse.setSatisfaction(Satisfaction.NEUTRAL));
    assertThat(Iterables.size(newUserSatisfactionSurveyResponseDao.findAll())).isEqualTo(1);
    assertThat(
            newUserSatisfactionSurveyResponseDao
                .findById(newUserSatisfactionSurveyResponse.getId())
                .get()
                .getSatisfaction())
        .isEqualTo(Satisfaction.NEUTRAL);

    newUserSatisfactionSurveyResponseDao.delete(newUserSatisfactionSurveyResponse);
    assertThat(
            newUserSatisfactionSurveyResponseDao
                .findById(newUserSatisfactionSurveyResponse.getId())
                .isPresent())
        .isFalse();
  }

  @Test
  public void testGetByUser() {
    user = userDao.save(new DbUser());

    newUserSatisfactionSurveyResponse =
        new DbNewUserSatisfactionSurveyResponse()
            .setUser(user)
            .setSatisfaction(Satisfaction.SATISFIED)
            .setAdditionalInfo("");

    newUserSatisfactionSurveyResponseDao.save(newUserSatisfactionSurveyResponse);

    assertThat(newUserSatisfactionSurveyResponseDao.findByUser(user).get())
        .isEqualTo(newUserSatisfactionSurveyResponse);
  }

  @Test
  public void testGetByUserNotPresent() {
    user = userDao.save(new DbUser());
    assertThat(newUserSatisfactionSurveyResponseDao.findByUser(user).isPresent()).isEqualTo(false);
  }
}
