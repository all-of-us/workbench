package org.pmiops.workbench.opsgenie;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.pmiops.workbench.opsgenie.EgressEventServiceImpl.NOT_FOUND_WORKSPACE_NAMESPACE;
import static org.pmiops.workbench.utils.TestMockFactory.DEFAULT_GOOGLE_PROJECT;

import com.google.common.collect.ImmutableList;
import com.ifountain.opsgenie.client.swagger.ApiException;
import com.ifountain.opsgenie.client.swagger.api.AlertApi;
import com.ifountain.opsgenie.client.swagger.model.CreateAlertRequest;
import com.ifountain.opsgenie.client.swagger.model.SuccessResponse;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.pmiops.workbench.SpringTest;
import org.pmiops.workbench.actionaudit.auditors.EgressEventAuditor;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.institution.InstitutionService;
import org.pmiops.workbench.model.EgressEvent;
import org.pmiops.workbench.model.Institution;
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
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;

public class EgressEventServiceTest extends SpringTest {

  private static final Instant NOW = Instant.parse("2020-06-11T01:30:00.02Z");
  private static final String INSTITUTION_2_NAME = "Auburn University";
  private static final String WORKSPACE_NAMEPACE = "aou-namespace";
  private static WorkbenchConfig workbenchConfig;
  private static final EgressEvent RECENT_EGRESS_EVENT =
      new EgressEvent()
          .projectName(DEFAULT_GOOGLE_PROJECT)
          .vmPrefix("all-of-us-111")
          .egressMib(120.7)
          .egressMibThreshold(100.0)
          .timeWindowStart(NOW.minusSeconds(630).toEpochMilli())
          .timeWindowDuration(600L);
  private static final Clock CLOCK = new FakeClock(NOW);

  @MockBean private AlertApi mockAlertApi;
  @MockBean private EgressEventAuditor egressEventAuditor;
  @MockBean private InstitutionService mockInstitutionService;
  @MockBean private UserService mockUserService;
  @MockBean private WorkspaceAdminService mockWorkspaceAdminService;
  @MockBean private WorkspaceDao workspaceDao;

  @Captor private ArgumentCaptor<CreateAlertRequest> alertRequestCaptor;

  @Autowired private EgressEventService egressEventService;
  private static final TestMockFactory TEST_MOCK_FACTORY = new TestMockFactory();
  private static final User USER_1 =
      new User()
          .givenName("Fredward")
          .familyName("Fredrickson")
          .userName("fred@aou.biz")
          .email("freddie@fred.fred.fred.ca");

  private static final WorkspaceUserAdminView ADMIN_VIEW_1 =
      new WorkspaceUserAdminView()
          .role(WorkspaceAccessLevel.OWNER)
          .userDatabaseId(111L)
          .userModel(USER_1)
          .userAccountCreatedTime(
              OffsetDateTime.parse(
                  "2018-08-30T01:20+02:00", DateTimeFormatter.ISO_OFFSET_DATE_TIME));

  private static final String INSTITUTION_1_NAME = "Verily Life Sciences";
  private static final DbUser DB_USER_1 = workspaceAdminUserViewToUser(ADMIN_VIEW_1);

  private static final User USER_2 =
      new User()
          .givenName("Theororathy")
          .familyName("Kim")
          .userName("theodorothy@aou.biz")
          .email("theodorothy@fred.fred.fred.org");
  private static final WorkspaceUserAdminView ADMIN_VIEW_2 =
      new WorkspaceUserAdminView()
          .role(WorkspaceAccessLevel.READER)
          .userDatabaseId(222L)
          .userModel(USER_2)
          .userAccountCreatedTime(OffsetDateTime.parse("2019-03-25T10:30+02:00"));
  private static final DbUser DB_USER_2 = workspaceAdminUserViewToUser(ADMIN_VIEW_2);

  @TestConfiguration
  @Import({EgressEventServiceImpl.class})
  static class Configuration {

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    WorkbenchConfig getWorkbenchConfig() {
      return workbenchConfig;
    }

    @Bean
    public Clock clock() {
      return CLOCK;
    }
  }

  @BeforeEach
  public void setUp() {
    workbenchConfig = WorkbenchConfig.createEmptyConfig();
    workbenchConfig.server.uiBaseUrl = "https://workbench.researchallofus.org";
    DbWorkspace dbWorkspace = new DbWorkspace();
    dbWorkspace.setWorkspaceNamespace(WORKSPACE_NAMEPACE);
    dbWorkspace.setGoogleProject(DEFAULT_GOOGLE_PROJECT);
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

    doReturn(Optional.of(DB_USER_1)).when(mockUserService).getByDatabaseId(DB_USER_1.getUserId());
    doReturn(Optional.of(DB_USER_1)).when(mockUserService).getByUsername(DB_USER_1.getUsername());

    doReturn(Optional.of(DB_USER_2)).when(mockUserService).getByDatabaseId(DB_USER_2.getUserId());
    doReturn(Optional.of(DB_USER_2)).when(mockUserService).getByUsername(DB_USER_2.getUsername());

    final Institution institution1 = new Institution().displayName(INSTITUTION_1_NAME);
    doReturn(Optional.of(institution1)).when(mockInstitutionService).getByUser(DB_USER_1);

    final Institution institution2 = new Institution().displayName(INSTITUTION_2_NAME);

    doReturn(Optional.of(institution2)).when(mockInstitutionService).getByUser(DB_USER_2);
    doReturn(Optional.of(dbWorkspace))
        .when(workspaceDao)
        .getByGoogleProject(DEFAULT_GOOGLE_PROJECT);
  }

  @Test
  public void testCreateEgressEventAlert() throws ApiException {
    when(mockAlertApi.createAlert(any())).thenReturn(new SuccessResponse().requestId("12345"));

    egressEventService.handleEvent(RECENT_EGRESS_EVENT);
    verify(mockAlertApi).createAlert(alertRequestCaptor.capture());
    verify(egressEventAuditor).fireEgressEvent(RECENT_EGRESS_EVENT);

    final CreateAlertRequest request = alertRequestCaptor.getValue();
    assertThat(request.getDescription())
        .contains("Terra Billing Project/Firecloud Namespace: " + WORKSPACE_NAMEPACE);
    assertThat(request.getDescription()).contains("Google Project Id: " + DEFAULT_GOOGLE_PROJECT);
    assertThat(request.getDescription())
        .contains(
            "https://workbench.researchallofus.org/admin/workspaces/" + WORKSPACE_NAMEPACE + "/");
    assertThat(request.getDescription())
        .containsMatch(
            Pattern.compile(
                "user_id:\\s+111,\\s+Institution:\\s+Verily\\s+Life\\s+Sciences,\\s+Account\\s+Age:\\s+651\\s+days"));
    assertThat(request.getAlias()).isEqualTo(WORKSPACE_NAMEPACE + " | all-of-us-111");
  }

  @Test
  public void testCreateEgressEventAlert_institutionNotFound() throws ApiException {
    when(mockAlertApi.createAlert(any())).thenReturn(new SuccessResponse().requestId("12345"));
    doReturn(Optional.empty()).when(mockInstitutionService).getByUser(any(DbUser.class));

    egressEventService.handleEvent(RECENT_EGRESS_EVENT);
    verify(mockAlertApi).createAlert(alertRequestCaptor.capture());

    final CreateAlertRequest request = alertRequestCaptor.getValue();
    assertThat(request.getDescription())
        .contains("Terra Billing Project/Firecloud Namespace: " + WORKSPACE_NAMEPACE);
    assertThat(request.getDescription()).contains("Google Project Id: " + DEFAULT_GOOGLE_PROJECT);
    assertThat(request.getDescription())
        .contains(
            "https://workbench.researchallofus.org/admin/workspaces/" + WORKSPACE_NAMEPACE + "/");
    assertThat(request.getDescription())
        .containsMatch(
            Pattern.compile(
                "user_id:\\s+111,\\s+Institution:\\s+not\\s+found,\\s+Account\\s+Age:\\s+651\\s+days"));
    assertThat(request.getAlias()).isEqualTo(WORKSPACE_NAMEPACE + " | all-of-us-111");
  }

  @Test
  public void testCreateEgressEventAlert_workspaceNotFound() throws ApiException {
    doReturn(Optional.empty()).when(workspaceDao).getByGoogleProject(DEFAULT_GOOGLE_PROJECT);
    when(mockAlertApi.createAlert(any())).thenReturn(new SuccessResponse().requestId("12345"));

    egressEventService.handleEvent(RECENT_EGRESS_EVENT);
    verify(mockAlertApi).createAlert(alertRequestCaptor.capture());
    verify(egressEventAuditor).fireEgressEvent(RECENT_EGRESS_EVENT);

    final CreateAlertRequest request = alertRequestCaptor.getValue();
    assertThat(request.getDescription())
        .contains("Terra Billing Project/Firecloud Namespace: " + NOT_FOUND_WORKSPACE_NAMESPACE);
    assertThat(request.getDescription()).contains("Google Project Id: " + DEFAULT_GOOGLE_PROJECT);
    assertThat(request.getDescription())
        .contains(
            "https://workbench.researchallofus.org/admin/workspaces/"
                + NOT_FOUND_WORKSPACE_NAMESPACE
                + "/");
    assertThat(request.getDescription())
        .containsMatch(
            Pattern.compile(
                "user_id:\\s+111,\\s+Institution:\\s+Verily\\s+Life\\s+Sciences,\\s+Account\\s+Age:\\s+651\\s+days"));
    assertThat(request.getAlias()).isEqualTo(NOT_FOUND_WORKSPACE_NAMESPACE + " | all-of-us-111");
  }

  @Test
  public void testCreateEgressEventAlert_staleEvent() throws ApiException {
    doReturn(Optional.empty()).when(workspaceDao).getByGoogleProject(DEFAULT_GOOGLE_PROJECT);
    when(mockAlertApi.createAlert(any())).thenReturn(new SuccessResponse().requestId("12345"));

    EgressEvent oldEgressEvent =
        new EgressEvent()
            .projectName(DEFAULT_GOOGLE_PROJECT)
            .vmPrefix("all-of-us-111")
            .egressMib(120.7)
            .egressMibThreshold(100.0)
            // > 2 windows into the past
            .timeWindowStart(NOW.minus(Duration.ofMinutes(125)).toEpochMilli())
            .timeWindowDuration(Duration.ofMinutes(60).getSeconds());

    egressEventService.handleEvent(oldEgressEvent);
    verify(mockAlertApi).createAlert(alertRequestCaptor.capture());
    verify(egressEventAuditor).fireEgressEvent(oldEgressEvent);

    final CreateAlertRequest request = alertRequestCaptor.getValue();
    assertThat(request.getMessage()).contains("[>60 mins old] High-egress event");
    assertThat(request.getNote()).contains("[>60 mins old] Time window");
  }

  @Test
  public void testCreateEgressEventAlert_staleEventShortWindow() throws ApiException {
    doReturn(Optional.empty()).when(workspaceDao).getByGoogleProject(DEFAULT_GOOGLE_PROJECT);
    when(mockAlertApi.createAlert(any())).thenReturn(new SuccessResponse().requestId("12345"));

    EgressEvent oldEgressEvent =
        new EgressEvent()
            .projectName(DEFAULT_GOOGLE_PROJECT)
            .vmPrefix("all-of-us-111")
            .egressMib(120.7)
            .egressMibThreshold(100.0)
            // > 2 windows into the past
            .timeWindowStart(NOW.minus(Duration.ofMinutes(3)).toEpochMilli())
            .timeWindowDuration(Duration.ofMinutes(1).getSeconds());

    egressEventService.handleEvent(oldEgressEvent);
    verify(mockAlertApi).createAlert(alertRequestCaptor.capture());
    verify(egressEventAuditor).fireEgressEvent(oldEgressEvent);

    final CreateAlertRequest request = alertRequestCaptor.getValue();
    assertThat(request.getMessage()).startsWith("High-egress event");
    assertThat(request.getNote()).startsWith("Time window");
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
