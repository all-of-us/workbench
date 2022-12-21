package org.pmiops.workbench.db.dao;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.Iterables;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.config.CommonConfig;
import org.pmiops.workbench.db.model.DbNewUserSatisfactionSurveyOneTimeCode;
import org.pmiops.workbench.db.model.DbUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@Import({FakeClockConfiguration.class, CommonConfig.class})
@DataJpaTest
public class NewUserSatisfactionSurveyOneTimeCodeDaoTest {
  @Autowired private UserDao userDao;

  @Autowired
  private NewUserSatisfactionSurveyOneTimeCodeDao newUserSatisfactionSurveyOneTimeCodeDao;

  private static final Timestamp CURRENT_TIMESTAMP = Timestamp.from(Instant.now());
  private DbUser user;

  @BeforeEach
  public void setUp() {
    user = userDao.save(new DbUser());
  }

  private DbNewUserSatisfactionSurveyOneTimeCode createValidDbOneTimeCode() {
    return newUserSatisfactionSurveyOneTimeCodeDao.save(
        new DbNewUserSatisfactionSurveyOneTimeCode().setUser(user).setUsedTime(CURRENT_TIMESTAMP));
  }

  @Test
  public void testCRUD() {
    DbNewUserSatisfactionSurveyOneTimeCode oneTimeCode = createValidDbOneTimeCode();

    assertThat(newUserSatisfactionSurveyOneTimeCodeDao.findById(oneTimeCode.getId()).get())
        .isEqualTo(oneTimeCode);

    Timestamp tomorrow = Timestamp.from(CURRENT_TIMESTAMP.toInstant().plus(1, ChronoUnit.DAYS));
    newUserSatisfactionSurveyOneTimeCodeDao.save(oneTimeCode.setUsedTime(tomorrow));
    assertThat(Iterables.size(newUserSatisfactionSurveyOneTimeCodeDao.findAll())).isEqualTo(1);
    assertThat(
            newUserSatisfactionSurveyOneTimeCodeDao
                .findById(oneTimeCode.getId())
                .get()
                .getUsedTime())
        .isEqualTo(tomorrow);

    newUserSatisfactionSurveyOneTimeCodeDao.delete(oneTimeCode);
    assertThat(newUserSatisfactionSurveyOneTimeCodeDao.findById(oneTimeCode.getId()).isPresent())
        .isFalse();
  }
}
