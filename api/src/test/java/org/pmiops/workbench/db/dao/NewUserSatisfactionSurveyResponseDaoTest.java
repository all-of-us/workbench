package org.pmiops.workbench.db.dao;

import static com.google.common.truth.Truth.assertThat;

import javax.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.config.CommonConfig;
import org.pmiops.workbench.db.model.DbNewUserSatisfactionSurveyResponse;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.utils.TestMockFactory;
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

  @Autowired private EntityManager entityManager;

  @BeforeEach
  public void setUp() {
    user = userDao.save(new DbUser());

    newUserSatisfactionSurveyResponse =
        TestMockFactory.createDefaultNewUserSatisfactionSurveyResponse(user);
    newUserSatisfactionSurveyResponseDao.save(newUserSatisfactionSurveyResponse);

    entityManager.refresh(user);
  }

  @Test
  public void testGetByUser() {
    assertThat(newUserSatisfactionSurveyResponseDao.findByUser(user).get())
        .isEqualTo(newUserSatisfactionSurveyResponse);
  }

  @Test
  public void testGetByUserNotPresent() {
    user = userDao.save(new DbUser());
    assertThat(newUserSatisfactionSurveyResponseDao.findByUser(user).isPresent()).isEqualTo(false);
  }
}
