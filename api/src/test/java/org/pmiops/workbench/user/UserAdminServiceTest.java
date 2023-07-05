package org.pmiops.workbench.user;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.db.dao.UserEgressBypassWindowDao;
import org.pmiops.workbench.db.model.DbUserEgressBypassWindow;
import org.pmiops.workbench.model.EgressBypassWindow;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.utils.mappers.EgressBypassWindowMapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

@DataJpaTest
public class UserAdminServiceTest {
  private static final Long USER_ID = 123L;
  private static final String DESCRIPTION = "description";

  @TestConfiguration
  @Import({
    FakeClockConfiguration.class,
    CommonMappers.class,
    EgressBypassWindowMapperImpl.class,
    UserAdminService.class,
  })
  static class Configuration {}

  @Autowired UserEgressBypassWindowDao userEgressBypassWindowDao;

  @Autowired UserAdminService userAdminService;

  @Test
  public void testCreateEgressByPassWindow() {
    userAdminService.createEgressBypassWindow(
        USER_ID, FakeClockConfiguration.NOW.toInstant(), DESCRIPTION);
    Set<DbUserEgressBypassWindow> dbResults =
        userEgressBypassWindowDao.getByUserIdOrderByStartTimeDesc(USER_ID);
    assertThat(dbResults.size()).isEqualTo(1);
    DbUserEgressBypassWindow dbEntity = dbResults.stream().findFirst().get();
    assertThat(dbEntity.getUserId()).isEqualTo(USER_ID);
    assertThat(dbEntity.getDescription()).isEqualTo(DESCRIPTION);
    assertThat(dbEntity.getStartTime()).isEqualTo(FakeClockConfiguration.NOW);
    assertThat(dbEntity.getEndTime())
        .isEqualTo(Timestamp.from(FakeClockConfiguration.NOW.toInstant().plus(2, ChronoUnit.DAYS)));
  }

  @Test
  public void testGetActiveWindow() {
    Instant startTime = FakeClockConfiguration.NOW.toInstant().minus(1, ChronoUnit.DAYS);
    Instant endTime = FakeClockConfiguration.NOW.toInstant().plus(1, ChronoUnit.DAYS);
    DbUserEgressBypassWindow dbUserEgressBypassWindow =
        new DbUserEgressBypassWindow()
            .setUserId(USER_ID)
            .setStartTime(Timestamp.from(startTime))
            .setEndTime(Timestamp.from(endTime))
            .setDescription(DESCRIPTION);
    userEgressBypassWindowDao.save(dbUserEgressBypassWindow);
    assertThat(userAdminService.getCurrentEgressBypassWindow(USER_ID))
        .isEqualTo(
            new EgressBypassWindow()
                .description(DESCRIPTION)
                .startTime(startTime.toEpochMilli())
                .endTime(endTime.toEpochMilli()));
  }

  @Test
  public void testGetActiveWindow_noActiveWindow_startTimeAfterNow() {
    Instant startTime = FakeClockConfiguration.NOW.toInstant().plus(1, ChronoUnit.DAYS);
    Instant endTime = FakeClockConfiguration.NOW.toInstant().plus(3, ChronoUnit.DAYS);
    DbUserEgressBypassWindow dbUserEgressBypassWindow =
        new DbUserEgressBypassWindow()
            .setUserId(USER_ID)
            .setStartTime(Timestamp.from(startTime))
            .setEndTime(Timestamp.from(endTime))
            .setDescription(DESCRIPTION);
    userEgressBypassWindowDao.save(dbUserEgressBypassWindow);
    assertThat(userAdminService.getCurrentEgressBypassWindow(USER_ID)).isNull();
  }

  @Test
  public void testGetActiveWindow_noActiveWindow_endTimeBeforeNow() {
    Instant startTime = FakeClockConfiguration.NOW.toInstant().minus(3, ChronoUnit.DAYS);
    Instant endTime = FakeClockConfiguration.NOW.toInstant().minus(1, ChronoUnit.DAYS);
    DbUserEgressBypassWindow dbUserEgressBypassWindow =
        new DbUserEgressBypassWindow()
            .setUserId(USER_ID)
            .setStartTime(Timestamp.from(startTime))
            .setEndTime(Timestamp.from(endTime))
            .setDescription(DESCRIPTION);
    userEgressBypassWindowDao.save(dbUserEgressBypassWindow);
    assertThat(userAdminService.getCurrentEgressBypassWindow(USER_ID)).isNull();
  }

  @Test
  public void testGetActiveWindow_noActiveWindow_null() {
    // does not throw
    assertThat(userAdminService.getCurrentEgressBypassWindow(null)).isNull();
  }

  @Test
  public void tesListWindows() {
    Instant startTime = FakeClockConfiguration.NOW.toInstant().minus(1, ChronoUnit.DAYS);
    Instant endTime = FakeClockConfiguration.NOW.toInstant().plus(1, ChronoUnit.DAYS);
    DbUserEgressBypassWindow dbUserEgressBypassWindow1 =
        new DbUserEgressBypassWindow()
            .setUserId(USER_ID)
            .setStartTime(Timestamp.from(startTime))
            .setEndTime(Timestamp.from(endTime))
            .setDescription(DESCRIPTION);
    userEgressBypassWindowDao.saveAll(ImmutableList.of(dbUserEgressBypassWindow1));
    assertThat(userAdminService.listAllEgressBypassWindows(USER_ID))
        .containsExactly(
            new EgressBypassWindow()
                .description(DESCRIPTION)
                .startTime(startTime.toEpochMilli())
                .endTime(endTime.toEpochMilli()));
  }

  @Test
  public void tesListWindows_emptyResult() {
    assertThat(userAdminService.listAllEgressBypassWindows(USER_ID)).isEmpty();
  }
}
