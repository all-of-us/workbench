package org.pmiops.workbench.db.dao;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.Iterables;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.config.CommonConfig;
import org.pmiops.workbench.db.model.DbNewUserSatisfactionSurvey;
import org.pmiops.workbench.db.model.DbNewUserSatisfactionSurvey.Satisfaction;
import org.pmiops.workbench.db.model.DbUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@Import({FakeClockConfiguration.class, CommonConfig.class})
@DataJpaTest
public class NewUserSatisfactionSurveyDaoTest {
  @Autowired private UserDao userDao;
  @Autowired NewUserSatisfactionSurveyDao newUserSatisfactionSurveyDao;

  private DbUser user;
  private DbNewUserSatisfactionSurvey newUserSatisfactionSurvey;

  @BeforeEach
  public void setUp() {
    user = userDao.save(new DbUser());
  }

  @Test
  public void testCRUD() {
    newUserSatisfactionSurvey =
        new DbNewUserSatisfactionSurvey()
            .setUser(user)
            .setSatisfaction(Satisfaction.SATISFIED)
            .setAdditionalInfo("");
    newUserSatisfactionSurveyDao.save(newUserSatisfactionSurvey);

    assertThat(newUserSatisfactionSurveyDao.findById(newUserSatisfactionSurvey.getId()).get())
        .isEqualTo(newUserSatisfactionSurvey);

    newUserSatisfactionSurveyDao.save(
        newUserSatisfactionSurvey.setSatisfaction(Satisfaction.NEUTRAL));
    assertThat(Iterables.size(newUserSatisfactionSurveyDao.findAll())).isEqualTo(1);
    assertThat(
            newUserSatisfactionSurveyDao
                .findById(newUserSatisfactionSurvey.getId())
                .get()
                .getSatisfaction())
        .isEqualTo(Satisfaction.NEUTRAL);

    newUserSatisfactionSurveyDao.delete(newUserSatisfactionSurvey);
    assertThat(newUserSatisfactionSurveyDao.findById(newUserSatisfactionSurvey.getId()).isPresent())
        .isFalse();
  }

  @Test
  public void testGetByUser() {
    newUserSatisfactionSurvey =
        new DbNewUserSatisfactionSurvey()
            .setUser(user)
            .setSatisfaction(Satisfaction.SATISFIED)
            .setAdditionalInfo("");

    newUserSatisfactionSurveyDao.save(newUserSatisfactionSurvey);

    assertThat(newUserSatisfactionSurveyDao.findByUser(user).get())
        .isEqualTo(newUserSatisfactionSurvey);
  }

  @Test
  public void testGetByUserNotPresent() {
    assertThat(newUserSatisfactionSurveyDao.findByUser(user).isPresent()).isEqualTo(false);
  }
}
