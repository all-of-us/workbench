package org.pmiops.workbench.exfiltration;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.pmiops.workbench.utils.TestMockFactory.DEFAULT_GOOGLE_PROJECT;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.FakeJpaDateTimeConfiguration;
import org.pmiops.workbench.actionaudit.auditors.EgressEventAuditor;
import org.pmiops.workbench.cloudtasks.TaskQueueService;
import org.pmiops.workbench.db.dao.EgressEventDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbEgressEvent;
import org.pmiops.workbench.db.model.DbEgressEvent.DbEgressEventStatus;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.leonardo.LeonardoApiClient;
import org.pmiops.workbench.model.*;
import org.pmiops.workbench.test.FakeClock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({FakeClockConfiguration.class, FakeJpaDateTimeConfiguration.class})
public class EgressEventServiceTest {

  private static final Instant NOW = Instant.parse("2020-06-11T01:30:00.02Z");
  private static final String WORKSPACE_NAMEPACE = "aou-namespace";

  @Autowired private WorkspaceDao workspaceDao;
  @Autowired private EgressEventDao egressEventDao;
  @Autowired private UserDao userDao;

  @Autowired private EgressEventAuditor mockEgressEventAuditor;
  @Autowired private TaskQueueService mockTaskQueueService;
  @Autowired private UserService mockUserService;

  @Autowired private EgressEventService egressEventService;
  @Autowired private FakeClock fakeClock;

  @Autowired private LeonardoApiClient leonardoApiClient;

  private static final User USER_1 =
      new User()
          .givenName("Fredward")
          .familyName("Fredrickson")
          .userName("fred@aou.biz")
          .email("freddie@fred.fred.fred.ca");
  private DbUser dbUser1;

  private static final WorkspaceUserAdminView ADMIN_VIEW_1 =
      new WorkspaceUserAdminView()
          .role(WorkspaceAccessLevel.OWNER)
          .userDatabaseId(111L)
          .userModel(USER_1)
          .userAccountCreatedTime(
              OffsetDateTime.parse(
                  "2018-08-30T01:20+02:00", DateTimeFormatter.ISO_OFFSET_DATE_TIME));

  private static final User USER_2 =
      new User()
          .givenName("Theororathy")
          .familyName("Kim")
          .userName("theodorothy@aou.biz")
          .email("theodorothy@fred.fred.fred.org");
  private DbUser dbUser2;

  private static final WorkspaceUserAdminView ADMIN_VIEW_2 =
      new WorkspaceUserAdminView()
          .role(WorkspaceAccessLevel.READER)
          .userDatabaseId(222L)
          .userModel(USER_2)
          .userAccountCreatedTime(OffsetDateTime.parse("2019-03-25T10:30+02:00"));

  private DbWorkspace dbWorkspace;

  @TestConfiguration
  @Import({FakeClockConfiguration.class, EgressEventServiceImpl.class})
  @MockBean({
    EgressEventAuditor.class,
    TaskQueueService.class,
    UserService.class,
    LeonardoApiClient.class
  })
  static class Configuration {}

  @BeforeEach
  public void setUp() {
    dbWorkspace = new DbWorkspace();
    dbWorkspace.setWorkspaceNamespace(WORKSPACE_NAMEPACE);
    dbWorkspace.setGoogleProject(DEFAULT_GOOGLE_PROJECT);
    dbWorkspace = workspaceDao.save(dbWorkspace);

    dbUser1 = userDao.save(workspaceAdminUserViewToUser(ADMIN_VIEW_1));
    dbUser2 = userDao.save(workspaceAdminUserViewToUser(ADMIN_VIEW_2));

    doReturn(Optional.of(dbUser1)).when(mockUserService).getByDatabaseId(dbUser1.getUserId());
    doReturn(Optional.of(dbUser1)).when(mockUserService).getByUsername(dbUser1.getUsername());

    doReturn(Optional.of(dbUser2)).when(mockUserService).getByDatabaseId(dbUser2.getUserId());
    doReturn(Optional.of(dbUser2)).when(mockUserService).getByUsername(dbUser2.getUsername());
  }

  @AfterEach
  public void tearDown() {
    egressEventDao.deleteAll();
    workspaceDao.deleteAll();
  }

  @Test
  public void testCreateEgressEventAlert() throws Exception {
    SumologicEgressEvent event = recentEgressEventForUser(dbUser1);
    egressEventService.handleEvent(event);
    verify(mockEgressEventAuditor).fireEgressEventForUser(event, dbUser1);
    verify(mockTaskQueueService).pushEgressEventTask(anyLong());

    List<DbEgressEvent> dbEvents = ImmutableList.copyOf(egressEventDao.findAll());
    assertThat(dbEvents).hasSize(1);
    DbEgressEvent dbEvent = Iterables.getOnlyElement(dbEvents);
    assertThat(dbEvent.getUser()).isEqualTo(dbUser1);
    assertThat(dbEvent.getWorkspace()).isEqualTo(dbWorkspace);
    assertThat(dbEvent.getCreationTime()).isNotNull();
    assertThat(dbEvent.getLastModifiedTime()).isNotNull();
    assertThat(dbEvent.getSumologicEvent()).isNotNull();
    assertThat(dbEvent.getEgressWindowSeconds()).isEqualTo(event.getTimeWindowDuration());
  }

  @Test
  public void testAppCreateEgressEventAlert() {

    SumologicEgressEvent event = recentAppEgressEvent(dbUser1, AppType.RSTUDIO);

    doReturn(Optional.of(dbUser1)).when(mockUserService).getByDatabaseId(dbUser1.getUserId());

    egressEventService.handleEvent(event);
    verify(mockEgressEventAuditor).fireEgressEventForUser(event, dbUser1);
    verify(mockTaskQueueService).pushEgressEventTask(anyLong());

    List<DbEgressEvent> dbEvents = ImmutableList.copyOf(egressEventDao.findAll());
    assertThat(dbEvents).hasSize(1);
    DbEgressEvent dbEvent = Iterables.getOnlyElement(dbEvents);
    assertThat(dbEvent.getUser()).isEqualTo(dbUser1);
    assertThat(dbEvent.getWorkspace()).isEqualTo(dbWorkspace);
    assertThat(dbEvent.getCreationTime()).isNotNull();
    assertThat(dbEvent.getLastModifiedTime()).isNotNull();
    assertThat(dbEvent.getSumologicEvent()).isNotNull();
    assertThat(dbEvent.getEgressWindowSeconds()).isEqualTo(event.getTimeWindowDuration());
  }

  @Test
  public void testAppCreateEgressEventAlert_skipCromwell() {

    SumologicEgressEvent event = recentAppEgressEvent(dbUser1, AppType.CROMWELL);

    doReturn(Optional.of(dbUser1)).when(mockUserService).getByDatabaseId(dbUser1.getUserId());

    egressEventService.handleEvent(event);
    verify(mockEgressEventAuditor).fireEgressEventForUser(event, dbUser1);
    verify(mockTaskQueueService).pushEgressEventTask(anyLong());

    List<DbEgressEvent> dbEvents = ImmutableList.copyOf(egressEventDao.findAll());
    assertThat(dbEvents).hasSize(1);
    DbEgressEvent dbEvent = Iterables.getOnlyElement(dbEvents);
    assertThat(dbEvent.getUser()).isEqualTo(dbUser1);
    assertThat(dbEvent.getWorkspace()).isEqualTo(dbWorkspace);
    assertThat(dbEvent.getCreationTime()).isNotNull();
    assertThat(dbEvent.getLastModifiedTime()).isNotNull();
    assertThat(dbEvent.getSumologicEvent()).isNotNull();
    assertThat(dbEvent.getEgressWindowSeconds()).isEqualTo(event.getTimeWindowDuration());
  }

  @Test
  public void testCreateEgressEventAlert_stalePersistedEvent() {
    SumologicEgressEvent oldEgressEvent =
        recentEgressEventForUser(dbUser1)
            .timeWindowDuration(60 * 60L)
            .timeWindowStart(NOW.minus(Duration.ofMinutes(125)).toEpochMilli());

    // Persist an existing copy of this event into the database.
    fakeClock.setInstant(NOW.minus(Duration.ofHours(1L)));
    egressEventDao.save(
        new DbEgressEvent()
            .setEgressWindowSeconds(oldEgressEvent.getTimeWindowDuration())
            .setUser(dbUser1)
            .setWorkspace(dbWorkspace)
            .setStatus(DbEgressEventStatus.PENDING));

    fakeClock.setInstant(NOW);
    egressEventService.handleEvent(oldEgressEvent);
    verifyNoInteractions(mockTaskQueueService);

    Iterable<DbEgressEvent> dbEvents = egressEventDao.findAll();
    assertThat(dbEvents).hasSize(1);
  }

  @Test
  public void testCreateEgressEventAlert_staleEventsMultiwindow() {
    SumologicEgressEvent oldEgressEvent =
        recentEgressEventForUser(dbUser1)
            .timeWindowDuration(60 * 60L)
            .timeWindowStart(NOW.minus(Duration.ofMinutes(125)).toEpochMilli());

    fakeClock.setInstant(NOW.minus(Duration.ofHours(1L)));
    egressEventDao.save(
        new DbEgressEvent()
            // Different window; otherwise metadata matches.
            .setEgressWindowSeconds(10 * 60L)
            .setUser(dbUser1)
            .setWorkspace(dbWorkspace)
            .setStatus(DbEgressEventStatus.PENDING));

    fakeClock.setInstant(NOW);
    egressEventService.handleEvent(oldEgressEvent);
    verify(mockTaskQueueService).pushEgressEventTask(anyLong());

    Iterable<DbEgressEvent> dbEvents = egressEventDao.findAll();
    assertThat(dbEvents).hasSize(2);
  }

  @Test
  public void testCreateEgressEventAlert_staleEventsDifferentUsers() {
    SumologicEgressEvent oldEgressEvent =
        recentEgressEventForUser(dbUser1)
            .timeWindowDuration(60 * 60L)
            .timeWindowStart(NOW.minus(Duration.ofMinutes(125)).toEpochMilli());

    fakeClock.setInstant(NOW.minus(Duration.ofHours(1L)));
    egressEventDao.save(
        new DbEgressEvent()
            .setEgressWindowSeconds(oldEgressEvent.getTimeWindowDuration())
            // Different user, otherwise metadata matches
            .setUser(dbUser2)
            .setWorkspace(dbWorkspace)
            .setStatus(DbEgressEventStatus.PENDING));

    fakeClock.setInstant(NOW);
    egressEventService.handleEvent(oldEgressEvent);
    verify(mockEgressEventAuditor).fireEgressEventForUser(oldEgressEvent, dbUser1);
    verify(mockTaskQueueService).pushEgressEventTask(anyLong());

    Iterable<DbEgressEvent> dbEvents = egressEventDao.findAll();
    assertThat(dbEvents).hasSize(2);
  }

  @Test
  public void testCreateEgressEventAlert_staleEventShortWindowPersisted() {
    SumologicEgressEvent oldEgressEvent =
        recentEgressEventForUser(dbUser1)
            // > 2 windows into the past
            .timeWindowStart(NOW.minus(Duration.ofMinutes(3)).toEpochMilli())
            .timeWindowDuration(Duration.ofMinutes(1).getSeconds());

    // Persist an existing copy of this event into the database.
    fakeClock.setInstant(NOW.minus(Duration.ofMinutes(2L)));
    egressEventDao.save(
        new DbEgressEvent()
            .setEgressWindowSeconds(oldEgressEvent.getTimeWindowDuration())
            .setUser(dbUser1)
            .setWorkspace(dbWorkspace)
            .setStatus(DbEgressEventStatus.PENDING));

    fakeClock.setInstant(NOW);
    egressEventService.handleEvent(oldEgressEvent);
    verify(mockEgressEventAuditor).fireEgressEventForUser(oldEgressEvent, dbUser1);
    verify(mockTaskQueueService).pushEgressEventTask(anyLong());

    Iterable<DbEgressEvent> dbEvents = egressEventDao.findAll();
    assertThat(dbEvents).hasSize(2);
  }

  private static SumologicEgressEvent recentEgressEventForUser(DbUser user) {
    return new SumologicEgressEvent()
        .projectName(DEFAULT_GOOGLE_PROJECT)
        .vmPrefix("all-of-us-" + user.getUserId())
        .egressMib(120.7)
        .egressMibThreshold(100.0)
        .timeWindowStart(NOW.minusSeconds(630).toEpochMilli())
        .timeWindowDuration(600L);
  }

  private static SumologicEgressEvent recentAppEgressEvent(DbUser user, AppType appType) {
    return new SumologicEgressEvent()
        .projectName(DEFAULT_GOOGLE_PROJECT)
        .vmName("some-vm")
        .srcGkeServiceName(
            "all-of-us-" + user.getUserId() + appType.toString().toLowerCase() + "random-abc")
        .egressMib(120.7)
        .egressMibThreshold(100.0)
        .timeWindowStart(NOW.minusSeconds(630).toEpochMilli())
        .timeWindowDuration(600L);
  }

  // I thought about adding this to a mapper, but it's such a backwards, test-only conversion,
  // and there are 20 unmapped properties, so it's not worth it.
  private static DbUser workspaceAdminUserViewToUser(WorkspaceUserAdminView adminView) {
    final User userModel = adminView.getUserModel();
    final DbUser result = new DbUser();
    result.setUserId(adminView.getUserDatabaseId());
    result.setGivenName(userModel.getGivenName());
    result.setFamilyName(userModel.getFamilyName());
    result.setUsername(userModel.getUserName());
    result.setContactEmail(userModel.getEmail());
    result.setCreationTime(Timestamp.from(adminView.getUserAccountCreatedTime().toInstant()));
    return result;
  }
}
