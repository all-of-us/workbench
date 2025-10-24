package org.pmiops.workbench.user;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.db.dao.UserEgressBypassWindowDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserEgressBypassWindow;
import org.pmiops.workbench.model.EgressBypassWindow;
import org.pmiops.workbench.model.EgressVwbBypassWindow;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.utils.mappers.EgressBypassWindowMapperImpl;
import org.pmiops.workbench.utils.mappers.FirecloudMapperImpl;
import org.pmiops.workbench.utils.mappers.UserMapperImpl;
import org.pmiops.workbench.vwb.exfil.ExfilManagerClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

@DataJpaTest
public class UserAdminServiceTest {
  private static final Long USER_ID = 123L;
  private static final String DESCRIPTION = "description";
  private static final String VWB_WORKSPACE_UFID = "vwb-workspace-123";
  private static final String USERNAME = "test@example.com";

  @TestConfiguration
  @Import({
    FakeClockConfiguration.class,
    CommonMappers.class,
    FirecloudMapperImpl.class,
    UserMapperImpl.class,
    EgressBypassWindowMapperImpl.class,
    UserAdminService.class,
  })
  static class Configuration {}

  @Autowired UserEgressBypassWindowDao userEgressBypassWindowDao;

  @Autowired UserAdminService userAdminService;

  @MockBean UserService userService;

  @MockBean ExfilManagerClient exfilManagerClient;

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
  public void testListWindows() {
    Instant startTime1 = FakeClockConfiguration.NOW.toInstant().minus(1, ChronoUnit.DAYS);
    Instant endTime = FakeClockConfiguration.NOW.toInstant().plus(1, ChronoUnit.DAYS);
    Instant startTime2 = FakeClockConfiguration.NOW.toInstant();
    DbUserEgressBypassWindow dbUserEgressBypassWindow1 =
        new DbUserEgressBypassWindow()
            .setUserId(USER_ID)
            .setStartTime(Timestamp.from(startTime1))
            .setEndTime(Timestamp.from(endTime))
            .setDescription(DESCRIPTION);
    DbUserEgressBypassWindow dbUserEgressBypassWindow2 =
        new DbUserEgressBypassWindow()
            .setUserId(USER_ID)
            .setStartTime(Timestamp.from(startTime2))
            .setEndTime(Timestamp.from(endTime))
            .setDescription(DESCRIPTION);
    userEgressBypassWindowDao.saveAll(
        ImmutableList.of(dbUserEgressBypassWindow1, dbUserEgressBypassWindow2));
    assertThat(userAdminService.listAllEgressBypassWindows(USER_ID))
        .containsExactly(
            new EgressBypassWindow()
                .description(DESCRIPTION)
                .startTime(startTime2.toEpochMilli())
                .endTime(endTime.toEpochMilli()),
            new EgressBypassWindow()
                .description(DESCRIPTION)
                .startTime(startTime1.toEpochMilli())
                .endTime(endTime.toEpochMilli()))
        .inOrder();
  }

  @Test
  public void testListWindows_emptyResult() {
    assertThat(userAdminService.listAllEgressBypassWindows(USER_ID)).isEmpty();
  }

  @Test
  public void testCreateVwbEgressBypassWindow() {
    DbUser dbUser = new DbUser();
    dbUser.setUserId(USER_ID);
    dbUser.setUsername(USERNAME);
    when(userService.getByDatabaseId(USER_ID)).thenReturn(Optional.of(dbUser));

    Instant startTime = FakeClockConfiguration.NOW.toInstant();
    Instant expectedEndTime = startTime.plus(2, ChronoUnit.DAYS);

    userAdminService.createVwbEgressBypassWindow(
        USER_ID, startTime, DESCRIPTION, VWB_WORKSPACE_UFID);

    // Verify DB record was created
    Set<DbUserEgressBypassWindow> dbResults =
        userEgressBypassWindowDao.getByUserIdOrderByStartTimeDesc(USER_ID);
    assertThat(dbResults.size()).isEqualTo(1);
    DbUserEgressBypassWindow dbEntity = dbResults.stream().findFirst().get();
    assertThat(dbEntity.getUserId()).isEqualTo(USER_ID);
    assertThat(dbEntity.getDescription()).isEqualTo(DESCRIPTION);
    assertThat(dbEntity.getVwbWorkspaceUfid()).isEqualTo(VWB_WORKSPACE_UFID);
    assertThat(dbEntity.getStartTime()).isEqualTo(Timestamp.from(startTime));
    assertThat(dbEntity.getEndTime()).isEqualTo(Timestamp.from(expectedEndTime));

    // Verify exfil manager was called
    verify(exfilManagerClient)
        .createEgressThresholdOverride(USERNAME, VWB_WORKSPACE_UFID, expectedEndTime, DESCRIPTION);
  }

  @Test
  public void testListAllVwbEgressBypassWindows() {
    Instant startTime1 = FakeClockConfiguration.NOW.toInstant().minus(1, ChronoUnit.DAYS);
    Instant endTime = FakeClockConfiguration.NOW.toInstant().plus(1, ChronoUnit.DAYS);
    Instant startTime2 = FakeClockConfiguration.NOW.toInstant();

    // Create VWB bypass window (has vwbWorkspaceUfid)
    DbUserEgressBypassWindow vwbWindow1 =
        new DbUserEgressBypassWindow()
            .setUserId(USER_ID)
            .setStartTime(Timestamp.from(startTime1))
            .setEndTime(Timestamp.from(endTime))
            .setDescription(DESCRIPTION)
            .setVwbWorkspaceUfid(VWB_WORKSPACE_UFID);

    // Create another VWB bypass window
    DbUserEgressBypassWindow vwbWindow2 =
        new DbUserEgressBypassWindow()
            .setUserId(USER_ID)
            .setStartTime(Timestamp.from(startTime2))
            .setEndTime(Timestamp.from(endTime))
            .setDescription(DESCRIPTION)
            .setVwbWorkspaceUfid(VWB_WORKSPACE_UFID);

    // Create regular bypass window (no vwbWorkspaceUfid) - should be filtered out
    DbUserEgressBypassWindow regularWindow =
        new DbUserEgressBypassWindow()
            .setUserId(USER_ID)
            .setStartTime(Timestamp.from(startTime1))
            .setEndTime(Timestamp.from(endTime))
            .setDescription(DESCRIPTION);

    userEgressBypassWindowDao.saveAll(ImmutableList.of(vwbWindow1, vwbWindow2, regularWindow));

    // Should only return VWB windows (with vwbWorkspaceUfid)
    assertThat(userAdminService.listAllVwbEgressBypassWindows(USER_ID))
        .containsExactly(
            new EgressVwbBypassWindow()
                .description(DESCRIPTION)
                .startTime(startTime2.toEpochMilli())
                .endTime(endTime.toEpochMilli())
                .vwbWorkspaceUfid(VWB_WORKSPACE_UFID),
            new EgressVwbBypassWindow()
                .description(DESCRIPTION)
                .startTime(startTime1.toEpochMilli())
                .endTime(endTime.toEpochMilli())
                .vwbWorkspaceUfid(VWB_WORKSPACE_UFID))
        .inOrder();
  }

  @Test
  public void testListAllVwbEgressBypassWindows_emptyResult() {
    assertThat(userAdminService.listAllVwbEgressBypassWindows(USER_ID)).isEmpty();
  }

  @Test
  public void testListAllVwbEgressBypassWindows_onlyRegularWindows() {
    // Create only regular bypass windows (no vwbWorkspaceUfid)
    Instant startTime = FakeClockConfiguration.NOW.toInstant();
    Instant endTime = FakeClockConfiguration.NOW.toInstant().plus(1, ChronoUnit.DAYS);
    DbUserEgressBypassWindow regularWindow =
        new DbUserEgressBypassWindow()
            .setUserId(USER_ID)
            .setStartTime(Timestamp.from(startTime))
            .setEndTime(Timestamp.from(endTime))
            .setDescription(DESCRIPTION);

    userEgressBypassWindowDao.save(regularWindow);

    // Should return empty list since no VWB windows exist
    assertThat(userAdminService.listAllVwbEgressBypassWindows(USER_ID)).isEmpty();
  }
}
