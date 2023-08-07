package org.pmiops.workbench.db.dao;

import static com.google.common.truth.Truth.assertThat;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.config.CommonConfig;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserEgressBypassWindow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;

@Import({FakeClockConfiguration.class, CommonConfig.class})
@DataJpaTest
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
public class UserEgressBypassWindowDaoTest {

  @Autowired UserEgressBypassWindowDao userEgressBypassWindowDao;
  @Autowired UserDao userDao;

  private static final Instant NOW = Instant.now();

  private final DbUser user = new DbUser();

  @BeforeEach
  public void setUp() {
    userDao.save(user);
  }

  @Test
  public void test_getEmptyResult() {
    assertThat(userEgressBypassWindowDao.getByUserIdOrderByStartTimeDesc(user.getUserId()))
        .isEmpty();
  }

  @Test
  public void test_getByUserOrderByStartTimeDesc() {
    DbUserEgressBypassWindow dbUserEgressBypassWindow1 =
        new DbUserEgressBypassWindow()
            .setUserId(user.getUserId())
            .setStartTime(new Timestamp(NOW.plus(1, ChronoUnit.MINUTES).toEpochMilli()))
            .setEndTime(new Timestamp(NOW.plus(2, ChronoUnit.MINUTES).toEpochMilli()))
            .setDescription("I am 1st");
    DbUserEgressBypassWindow dbUserEgressBypassWindow2 =
        new DbUserEgressBypassWindow()
            .setUserId(user.getUserId())
            .setStartTime(new Timestamp(NOW.plus(3, ChronoUnit.MINUTES).toEpochMilli()))
            .setEndTime(new Timestamp(NOW.plus(4, ChronoUnit.MINUTES).toEpochMilli()))
            .setDescription("I am 2nd");
    DbUserEgressBypassWindow dbUserEgressBypassWindow3 =
        new DbUserEgressBypassWindow()
            .setUserId(user.getUserId())
            .setStartTime(new Timestamp(NOW.plus(5, ChronoUnit.MINUTES).toEpochMilli()))
            .setEndTime(new Timestamp(NOW.plus(6, ChronoUnit.MINUTES).toEpochMilli()))
            .setDescription("I am 3rd");

    // Use save instead of saveAll because that is what be used in production.
    userEgressBypassWindowDao.save(dbUserEgressBypassWindow1);
    userEgressBypassWindowDao.save(dbUserEgressBypassWindow2);
    userEgressBypassWindowDao.save(dbUserEgressBypassWindow3);

    assertThat(userEgressBypassWindowDao.getByUserIdOrderByStartTimeDesc(user.getUserId()))
        .containsExactly(
            dbUserEgressBypassWindow3, dbUserEgressBypassWindow2, dbUserEgressBypassWindow1);
  }
}
