package org.pmiops.workbench.exfiltration;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.pmiops.workbench.exfiltration.EgressEventServiceImpl.NOT_FOUND_WORKSPACE_NAMESPACE;
import static org.pmiops.workbench.utils.TestMockFactory.DEFAULT_GOOGLE_PROJECT;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.ifountain.opsgenie.client.swagger.ApiException;
import com.ifountain.opsgenie.client.swagger.api.AlertApi;
import com.ifountain.opsgenie.client.swagger.model.CreateAlertRequest;
import com.ifountain.opsgenie.client.swagger.model.SuccessResponse;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.pmiops.workbench.JpaFakeDateTimeConfiguration;
import org.pmiops.workbench.actionaudit.auditors.EgressEventAuditor;
import org.pmiops.workbench.cloudtasks.TaskQueueService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.EgressEventDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbEgressEvent;
import org.pmiops.workbench.db.model.DbEgressEvent.EgressEventStatus;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.institution.InstitutionService;
import org.pmiops.workbench.model.Institution;
import org.pmiops.workbench.model.SumologicEgressEvent;
import org.pmiops.workbench.model.User;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.model.WorkspaceAdminView;
import org.pmiops.workbench.model.WorkspaceUserAdminView;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.utils.TestMockFactory;
import org.pmiops.workbench.workspaceadmin.WorkspaceAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;

@DataJpaTest
@Import({FakeClockConfiguration.class, JpaFakeDateTimeConfiguration.class})
public class EgressEventServiceTest {

  private static final Instant NOW = Instant.parse("2020-06-11T01:30:00.02Z");
  private static final String INSTITUTION_2_NAME = "Auburn University";
  private static final String WORKSPACE_NAMEPACE = "aou-namespace";
  private static WorkbenchConfig workbenchConfig;

  @MockBean private AlertApi mockAlertApi;
  @MockBean private EgressEventAuditor egressEventAuditor;
  @MockBean private InstitutionService mockInstitutionService;
  @MockBean private UserService mockUserService;
  @MockBean private WorkspaceAdminService mockWorkspaceAdminService;
  @MockBean private TaskQueueService mockTaskQueueService;
  @Autowired private WorkspaceDao workspaceDao;
  @Autowired private EgressEventDao egressEventDao;
  @Autowired private UserDao userDao;

  @Captor private ArgumentCaptor<CreateAlertRequest> alertRequestCaptor;

  @Autowired private EgressEventService egressEventService;
  @Autowired private FakeClock fakeClock;
  private static final TestMockFactory TEST_MOCK_FACTORY = new TestMockFactory();
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

  private static final String INSTITUTION_1_NAME = "Verily Life Sciences";

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
  static class Configuration {

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    WorkbenchConfig getWorkbenchConfig() {
      return workbenchConfig;
    }
  }

  @BeforeEach
  public void setUp() {
    workbenchConfig = WorkbenchConfig.createEmptyConfig();
    workbenchConfig.server.uiBaseUrl = "https://workbench.researchallofus.org";
    workbenchConfig.featureFlags.enableEgressAlertingV2 = false;

    dbWorkspace = new DbWorkspace();
    dbWorkspace.setWorkspaceNamespace(WORKSPACE_NAMEPACE);
    dbWorkspace.setGoogleProject(DEFAULT_GOOGLE_PROJECT);
    dbWorkspace = workspaceDao.save(dbWorkspace);

    dbUser1 = userDao.save(workspaceAdminUserViewToUser(ADMIN_VIEW_1));
    dbUser2 = userDao.save(workspaceAdminUserViewToUser(ADMIN_VIEW_2));

    final Workspace workspace =
        TEST_MOCK_FACTORY
            .createWorkspace(WORKSPACE_NAMEPACE, "The Whole #!")
            .creator(USER_1.getUserName());
    final WorkspaceAdminView workspaceAdminView =
        new WorkspaceAdminView()
            .workspace(workspace)
            .workspaceDatabaseId(101010L)
            .collaborators(ImmutableList.of(ADMIN_VIEW_1, ADMIN_VIEW_2));
    doReturn(workspaceAdminView).when(mockWorkspaceAdminService).getWorkspaceAdminView(anyString());

    doReturn(Optional.of(dbUser1)).when(mockUserService).getByDatabaseId(dbUser1.getUserId());
    doReturn(Optional.of(dbUser1)).when(mockUserService).getByUsername(dbUser1.getUsername());

    doReturn(Optional.of(dbUser2)).when(mockUserService).getByDatabaseId(dbUser2.getUserId());
    doReturn(Optional.of(dbUser2)).when(mockUserService).getByUsername(dbUser2.getUsername());

    final Institution institution1 = new Institution().displayName(INSTITUTION_1_NAME);
    doReturn(Optional.of(institution1)).when(mockInstitutionService).getByUser(dbUser1);

    final Institution institution2 = new Institution().displayName(INSTITUTION_2_NAME);

    doReturn(Optional.of(institution2)).when(mockInstitutionService).getByUser(dbUser2);
    fakeClock.setInstant(NOW);
  }

  @AfterEach
  public void tearDown() {
    egressEventDao.deleteAll();
    workspaceDao.deleteAll();
  }

  @Test
  public void testCreateEgressEventAlert() throws ApiException {
    when(mockAlertApi.createAlert(any())).thenReturn(new SuccessResponse().requestId("12345"));

    SumologicEgressEvent event = recentEgressEventForUser(dbUser1);
    egressEventService.handleEvent(event);
    verify(mockAlertApi).createAlert(alertRequestCaptor.capture());
    verify(egressEventAuditor).fireEgressEvent(event);

    String vmPrefix = "all-of-us-" + dbUser1.getUserId();

    final CreateAlertRequest request = alertRequestCaptor.getValue();
    assertThat(request.getDescription())
        .contains("Terra Billing Project/Firecloud Namespace: " + WORKSPACE_NAMEPACE);
    assertThat(request.getDescription()).contains("Google Project Id: " + DEFAULT_GOOGLE_PROJECT);
    assertThat(request.getDescription())
        .contains(
            "https://workbench.researchallofus.org/admin/workspaces/" + WORKSPACE_NAMEPACE + "/");
    assertThat(request.getDescription())
        .containsMatch(
            Pattern.compile("Institution:\\s+Verily\\s+Life\\s+Sciences,\\s+Account\\s+Age:"));
    assertThat(request.getAlias()).isEqualTo(WORKSPACE_NAMEPACE + " | " + vmPrefix);

    verifyZeroInteractions(mockTaskQueueService);
  }

  @Test
  public void testCreateEgressEventAlert_v2() throws ApiException {
    workbenchConfig.featureFlags.enableEgressAlertingV2 = true;
    SumologicEgressEvent event = recentEgressEventForUser(dbUser1);
    egressEventService.handleEvent(event);
    verify(egressEventAuditor).fireEgressEvent(event);
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

    verifyZeroInteractions(mockAlertApi);
  }

  @Test
  public void testCreateEgressEventAlert_institutionNotFound() throws ApiException {
    when(mockAlertApi.createAlert(any())).thenReturn(new SuccessResponse().requestId("12345"));
    doReturn(Optional.empty()).when(mockInstitutionService).getByUser(any(DbUser.class));

    egressEventService.handleEvent(recentEgressEventForUser(dbUser1));
    verify(mockAlertApi).createAlert(alertRequestCaptor.capture());

    final CreateAlertRequest request = alertRequestCaptor.getValue();
    assertThat(request.getDescription())
        .contains("Terra Billing Project/Firecloud Namespace: " + WORKSPACE_NAMEPACE);
    assertThat(request.getDescription()).contains("Google Project Id: " + DEFAULT_GOOGLE_PROJECT);
    assertThat(request.getDescription())
        .contains(
            "https://workbench.researchallofus.org/admin/workspaces/" + WORKSPACE_NAMEPACE + "/");
    assertThat(request.getDescription())
        .containsMatch(Pattern.compile("Institution:\\s+not\\s+found,\\s+Account\\s+Age:"));
    assertThat(request.getAlias())
        .isEqualTo(WORKSPACE_NAMEPACE + " | all-of-us-" + dbUser1.getUserId());

    verifyZeroInteractions(mockTaskQueueService);
  }

  @Test
  public void testCreateEgressEventAlert_workspaceNotFound() throws ApiException {
    when(mockAlertApi.createAlert(any())).thenReturn(new SuccessResponse().requestId("12345"));

    String notFoundProjectName = "NOT_FOUND123";
    SumologicEgressEvent event = recentEgressEventForUser(dbUser1).projectName(notFoundProjectName);
    egressEventService.handleEvent(event);
    verify(mockAlertApi).createAlert(alertRequestCaptor.capture());
    verify(egressEventAuditor).fireEgressEvent(event);

    final CreateAlertRequest request = alertRequestCaptor.getValue();
    assertThat(request.getDescription())
        .contains("Terra Billing Project/Firecloud Namespace: " + NOT_FOUND_WORKSPACE_NAMESPACE);
    assertThat(request.getDescription()).contains("Google Project Id: " + notFoundProjectName);
    assertThat(request.getDescription())
        .contains(
            "https://workbench.researchallofus.org/admin/workspaces/"
                + NOT_FOUND_WORKSPACE_NAMESPACE
                + "/");
    assertThat(request.getDescription())
        .containsMatch(
            Pattern.compile("\\s+Institution:\\s+Verily\\s+Life\\s+Sciences,\\s+Account\\s+Age:"));
    assertThat(request.getAlias())
        .isEqualTo(NOT_FOUND_WORKSPACE_NAMESPACE + " | all-of-us-" + dbUser1.getUserId());
  }

  @Test
  public void testCreateEgressEventAlert_staleEvent() throws ApiException {
    when(mockAlertApi.createAlert(any())).thenReturn(new SuccessResponse().requestId("12345"));

    SumologicEgressEvent oldEgressEvent =
        recentEgressEventForUser(dbUser1)
            .timeWindowDuration(60 * 60L)
            .timeWindowStart(NOW.minus(Duration.ofMinutes(125)).toEpochMilli());

    egressEventService.handleEvent(oldEgressEvent);
    verify(mockAlertApi).createAlert(alertRequestCaptor.capture());
    verify(egressEventAuditor).fireEgressEvent(oldEgressEvent);

    final CreateAlertRequest request = alertRequestCaptor.getValue();
    assertThat(request.getMessage()).contains("[>60 mins old] High-egress event");
    assertThat(request.getNote()).contains("[>60 mins old] Time window");

    Iterable<DbEgressEvent> dbEvents = egressEventDao.findAll();
    assertThat(dbEvents).hasSize(1);
  }

  @Test
  public void testCreateEgressEventAlert_stalePersistedEvent_v2() {
    workbenchConfig.featureFlags.enableEgressAlertingV2 = true;

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
            .setStatus(EgressEventStatus.PENDING));

    fakeClock.setInstant(NOW);
    egressEventService.handleEvent(oldEgressEvent);
    verifyZeroInteractions(mockTaskQueueService);

    Iterable<DbEgressEvent> dbEvents = egressEventDao.findAll();
    assertThat(dbEvents).hasSize(1);
  }

  @Test
  public void testCreateEgressEventAlert_staleEventsMultiwindow_v2() {
    workbenchConfig.featureFlags.enableEgressAlertingV2 = true;

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
            .setStatus(EgressEventStatus.PENDING));

    fakeClock.setInstant(NOW);
    egressEventService.handleEvent(oldEgressEvent);
    verify(mockTaskQueueService).pushEgressEventTask(anyLong());

    Iterable<DbEgressEvent> dbEvents = egressEventDao.findAll();
    assertThat(dbEvents).hasSize(2);
  }

  @Test
  public void testCreateEgressEventAlert_staleEventsDifferentUsers_v2() {
    workbenchConfig.featureFlags.enableEgressAlertingV2 = true;

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
            .setStatus(EgressEventStatus.PENDING));

    fakeClock.setInstant(NOW);
    egressEventService.handleEvent(oldEgressEvent);
    verify(egressEventAuditor).fireEgressEvent(oldEgressEvent);
    verify(mockTaskQueueService).pushEgressEventTask(anyLong());

    Iterable<DbEgressEvent> dbEvents = egressEventDao.findAll();
    assertThat(dbEvents).hasSize(2);
  }

  @Test
  public void testCreateEgressEventAlert_staleEventShortWindowPersisted_v2() {
    workbenchConfig.featureFlags.enableEgressAlertingV2 = true;

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
            .setStatus(EgressEventStatus.PENDING));

    fakeClock.setInstant(NOW);
    egressEventService.handleEvent(oldEgressEvent);
    verify(egressEventAuditor).fireEgressEvent(oldEgressEvent);
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
