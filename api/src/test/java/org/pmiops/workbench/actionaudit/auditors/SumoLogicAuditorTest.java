package org.pmiops.workbench.actionaudit.auditors;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.pmiops.workbench.actionaudit.ActionAuditEvent;
import org.pmiops.workbench.actionaudit.ActionAuditService;
import org.pmiops.workbench.actionaudit.ActionType;
import org.pmiops.workbench.actionaudit.AgentType;
import org.pmiops.workbench.actionaudit.TargetType;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.EgressEvent;
import org.pmiops.workbench.model.EgressEventRequest;
import org.pmiops.workbench.model.UserRole;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class SumoLogicAuditorTest {

  private static final long USER_ID = 1L;
  private static final String USER_EMAIL = "user@researchallofus.org";
  private static final long WORKSPACE_ID = 1L;
  private static final String WORKSPACE_NAMESPACE = "aou-rw-test-c7dec260";
  private static final String WORKSPACE_FIRECLOUD_NAME = "mytestworkspacename";

  private static final String EGRESS_EVENT_PROJECT_NAME = WORKSPACE_NAMESPACE;
  private static final String EGRESS_EVENT_VM_NAME = "all-of-us-" + USER_ID + "-m";

  // Pre-built data objects for test.
  private DbUser dbUser;
  private DbWorkspace dbWorkspace;
  private List<UserRole> firecloudUserRoles = new ArrayList<>();

  @Autowired private SumoLogicAuditor sumoLogicAuditor;

  @MockBean private ActionAuditService mockActionAuditService;
  @MockBean private WorkspaceService mockWorkspaceService;
  @MockBean private UserDao mockUserDao;

  @Captor private ArgumentCaptor<Collection<ActionAuditEvent>> eventsCaptor;
  @Captor private ArgumentCaptor<String> stringCaptor;

  @TestConfiguration
  @Import({
    // Import the impl class to allow autowiring the bean.
    SumoLogicAuditorImpl.class,
    // Import common action audit beans.
    ActionAuditTestConfig.class
  })
  static class Configuration {}

  @Before
  public void setUp() {
    dbUser = new DbUser();
    dbUser.setUserId(USER_ID);
    dbUser.setUsername(USER_EMAIL);
    when(mockUserDao.findUserByUsername(USER_EMAIL)).thenReturn(dbUser);

    dbWorkspace = new DbWorkspace();
    dbWorkspace.setWorkspaceId(WORKSPACE_ID);
    dbWorkspace.setWorkspaceNamespace(WORKSPACE_NAMESPACE);
    dbWorkspace.setFirecloudName(WORKSPACE_FIRECLOUD_NAME);
    when(mockWorkspaceService.getByNamespace(WORKSPACE_NAMESPACE)).thenReturn(dbWorkspace);
    firecloudUserRoles.add(new UserRole().email(USER_EMAIL));
    when(mockWorkspaceService.getFirecloudUserRoles(WORKSPACE_NAMESPACE, WORKSPACE_FIRECLOUD_NAME))
        .thenReturn(firecloudUserRoles);
  }

  @Test
  public void testFireEgressEvent() {
    sumoLogicAuditor.fireEgressEvent(
        new EgressEvent()
            .projectName(EGRESS_EVENT_PROJECT_NAME)
            .vmName(EGRESS_EVENT_VM_NAME)
            .egressMib(12.3));
    verify(mockActionAuditService).sendWithComment(eventsCaptor.capture(), stringCaptor.capture());

    assertThat(eventsCaptor.getValue().size()).isEqualTo(1);
    final ActionAuditEvent event = eventsCaptor.getValue().stream().findFirst().get();
    assertThat(event.getActionType()).isEqualTo(ActionType.HIGH_EGRESS);
    assertThat(event.getAgentType()).isEqualTo(AgentType.USER);
    assertThat(event.getAgentId()).isEqualTo(USER_ID);
    assertThat(event.getAgentEmailMaybe()).isEqualTo(USER_EMAIL);
    assertThat(event.getTargetType()).isEqualTo(TargetType.WORKSPACE);
    assertThat(event.getTargetIdMaybe()).isEqualTo(WORKSPACE_ID);

    assertThat(stringCaptor.getValue()).contains("Detected 12.30Mb egress");
  }

  @Test
  public void testFireEgressNoWorkspaceFound() {
    // When the workspace lookup doesn't succeed, the event is filed w/ a system agent and an
    // empty target ID.
    when(mockWorkspaceService.getByNamespace(WORKSPACE_NAMESPACE)).thenReturn(null);
    sumoLogicAuditor.fireEgressEvent(
        new EgressEvent().projectName(EGRESS_EVENT_PROJECT_NAME).vmName(EGRESS_EVENT_VM_NAME));
    verify(mockActionAuditService).sendWithComment(eventsCaptor.capture(), stringCaptor.capture());

    assertThat(eventsCaptor.getValue().size()).isEqualTo(1);
    final ActionAuditEvent event = eventsCaptor.getValue().stream().findFirst().get();
    assertThat(event.getActionType()).isEqualTo(ActionType.HIGH_EGRESS);
    assertThat(event.getAgentType()).isEqualTo(AgentType.SYSTEM);
    assertThat(event.getAgentId()).isEqualTo(0);
    assertThat(event.getAgentEmailMaybe()).isEqualTo(null);
    assertThat(event.getTargetType()).isEqualTo(TargetType.WORKSPACE);
    assertThat(event.getTargetIdMaybe()).isEqualTo(null);
  }

  @Test
  public void testFireFailedParsing() {
    // When the inbound request parsing fails, an event is logged at the system agent.
    when(mockWorkspaceService.getByNamespace(WORKSPACE_NAMESPACE)).thenReturn(null);
    sumoLogicAuditor.fireFailedToParseEgressEvent(new EgressEventRequest().eventsJsonArray("asdf"));
    verify(mockActionAuditService).sendWithComment(eventsCaptor.capture(), stringCaptor.capture());

    assertThat(eventsCaptor.getValue().size()).isEqualTo(1);
    final ActionAuditEvent event = eventsCaptor.getValue().stream().findFirst().get();

    assertThat(event.getActionType()).isEqualTo(ActionType.HIGH_EGRESS);
    assertThat(event.getAgentType()).isEqualTo(AgentType.SYSTEM);
    assertThat(event.getAgentId()).isEqualTo(0);
    assertThat(event.getAgentEmailMaybe()).isEqualTo(null);
    assertThat(event.getTargetType()).isEqualTo(TargetType.WORKSPACE);
    assertThat(event.getTargetIdMaybe()).isEqualTo(null);

    assertThat(stringCaptor.getValue()).contains("asdf");
  }
}
