package org.pmiops.workbench.exfiltration;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.pmiops.workbench.utils.TestMockFactory.DEFAULT_GOOGLE_PROJECT;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.sql.Timestamp;
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
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.*;
import org.pmiops.workbench.test.FakeClock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@DataJpaTest
@Import({FakeClockConfiguration.class, FakeJpaDateTimeConfiguration.class})
public class EgressEventServiceTest {

  @MockitoBean private EgressEventAuditor egressEventAuditor;
  @MockitoBean private TaskQueueService taskQueueService;
  @MockitoBean private UserService userService;

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
  public void testHandleVwbEgressEvent() {
    VwbEgressEventRequest vwbEvent =
        new VwbEgressEventRequest()
            .userEmail(dbUser1.getUsername())
            .vwbWorkspaceId("testWorkspaceId")
            .vmName("testVmName")
            .incidentCount(1L)
            .egressMib(500.0)
            .egressMibThreshold(100.0)
            .gcpProjectId("test-gcp-project")
            .timeWindowDuration(600L)
            .timeWindowStart(NOW.toEpochMilli());

    doReturn(Optional.of(dbUser1)).when(mockUserService).getByDatabaseId(dbUser1.getUserId());

    egressEventService.handleVwbEvent(vwbEvent);

    verify(mockEgressEventAuditor).fireVwbEgressEvent(vwbEvent, dbUser1);
    verify(mockTaskQueueService).pushEgressEventTask(anyLong(), anyBoolean());

    List<DbEgressEvent> dbEvents = ImmutableList.copyOf(egressEventDao.findAll());
    assertThat(dbEvents).hasSize(1);
    DbEgressEvent dbEvent = Iterables.getOnlyElement(dbEvents);
    assertThat(dbEvent.getUser()).isEqualTo(dbUser1);
    assertThat(dbEvent.getVwbWorkspaceId()).isEqualTo("testWorkspaceId");
    assertThat(dbEvent.getVwbVmName()).isEqualTo("testVmName");
    assertThat(dbEvent.getVwbIncidentCount()).isEqualTo(1);
    assertThat(dbEvent.getEgressMegabytes())
        .isEqualTo((float) (500 * ((1 << 20) / 1e6))); // Convert MiB
    assertThat(dbEvent.getGcpProjectId()).isEqualTo("test-gcp-project");
    assertThat(dbEvent.getEgressWindowSeconds()).isEqualTo(600L);
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
