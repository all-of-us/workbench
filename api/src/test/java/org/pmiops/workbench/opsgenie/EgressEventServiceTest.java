package org.pmiops.workbench.opsgenie;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.ifountain.opsgenie.client.swagger.ApiException;
import com.ifountain.opsgenie.client.swagger.api.AlertApi;
import com.ifountain.opsgenie.client.swagger.model.CreateAlertRequest;
import com.ifountain.opsgenie.client.swagger.model.SuccessResponse;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.pmiops.workbench.actionaudit.auditors.EgressEventAuditor;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.model.EgressEvent;
import org.pmiops.workbench.model.User;
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
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class EgressEventServiceTest {

  private static final Instant NOW = Instant
      .ofEpochMilli(DateTime.parse("2020-06-11T01:30+02:00").getMillis());
  private static WorkbenchConfig workbenchConfig;
  private static final EgressEvent EGRESS_EVENT_1 =
      new EgressEvent()
          .projectName("aou-rw-test-c7dec260")
          .vmName("aou-rw-111-m")
          .egressMib(120.7)
          .egressMibThreshold(100.0)
          .timeWindowDuration(600L);
  private static final Clock CLOCK = new FakeClock(NOW);

  @MockBean private AlertApi mockAlertApi;
  @MockBean private EgressEventAuditor egressEventAuditor;
  @MockBean private UserService mockUserService;
  @MockBean private WorkspaceAdminService mockWorkspaceAdminService;

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
          .userAccountCreatedTime(DateTime.parse("2018-08-30T01:20+02:00"));

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
          .userAccountCreatedTime(DateTime.parse("2019-03-25T10:30+02:00"));
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

  @Before
  public void setUp() {
    workbenchConfig = WorkbenchConfig.createEmptyConfig();
    workbenchConfig.server.uiBaseUrl = "https://workbench.researchallofus.org";

    final WorkspaceAdminView workspaceAdminView =
        new WorkspaceAdminView()
            .workspace(TEST_MOCK_FACTORY.createWorkspace("aou-rw-33116581", "The Whole #!"))
            .workspaceDatabaseId(101010L)
            .collaborators(ImmutableList.of(ADMIN_VIEW_1, ADMIN_VIEW_2));
    doReturn(workspaceAdminView).when(mockWorkspaceAdminService).getWorkspaceAdminView(anyString());

    doReturn(Optional.of(DB_USER_1)).when(mockUserService).getByDatabaseId(DB_USER_1.getUserId());
    doReturn(Optional.of(DB_USER_1)).when(mockUserService).getByUsername(DB_USER_1.getUsername());

    doReturn(Optional.of(DB_USER_2)).when(mockUserService).getByDatabaseId(DB_USER_2.getUserId());
    doReturn(Optional.of(DB_USER_2)).when(mockUserService).getByUsername(DB_USER_2.getUsername());
  }

  @Test
  public void createEgressEventAlert() throws ApiException {
    when(mockAlertApi.createAlert(any())).thenReturn(new SuccessResponse().requestId("12345"));

    egressEventService.handleEvent(EGRESS_EVENT_1);
    verify(mockAlertApi).createAlert(alertRequestCaptor.capture());
    verify(egressEventAuditor).fireEgressEvent(EGRESS_EVENT_1);

    final CreateAlertRequest request = alertRequestCaptor.getValue();
    assertThat(request.getDescription()).contains("GCP Billing Project/Firecloud Namespace: aou-rw-test-c7dec260");
    assertThat(request.getDescription())
        .contains("https://workbench.researchallofus.org/admin/workspaces/aou-rw-test-c7dec260/");
    assertThat(request.getAlias()).isEqualTo("aou-rw-test-c7dec260 | aou-rw-111-m");
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
    result.setCreationTime(new Timestamp(adminView.getUserAccountCreatedTime().getMillis()));
    return result;
  }
}
