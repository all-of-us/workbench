package org.pmiops.workbench.actionaudit.auditors;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.pmiops.workbench.actionaudit.ActionAuditEvent;
import org.pmiops.workbench.actionaudit.ActionAuditService;
import org.pmiops.workbench.actionaudit.ActionType;
import org.pmiops.workbench.actionaudit.AgentType;
import org.pmiops.workbench.actionaudit.targetproperties.EgressEventCommentTargetProperty;
import org.pmiops.workbench.actionaudit.targetproperties.EgressEventTargetProperty;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.BadRequestException;
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
public class EgressEventAuditorTest {

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

  @Autowired private EgressEventAuditor egressEventAuditor;

  @MockBean private ActionAuditService mockActionAuditService;
  @MockBean private WorkspaceService mockWorkspaceService;
  @MockBean private UserDao mockUserDao;

  @Captor private ArgumentCaptor<Collection<ActionAuditEvent>> eventsCaptor;

  @Rule public final ExpectedException exception = ExpectedException.none();

  @TestConfiguration
  @Import({
    // Import the impl class to allow autowiring the bean.
    EgressEventAuditorImpl.class,
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
    when(mockWorkspaceService.getByNamespace(WORKSPACE_NAMESPACE))
        .thenReturn(Optional.of(dbWorkspace));
    firecloudUserRoles.add(new UserRole().email(USER_EMAIL));
    when(mockWorkspaceService.getFirecloudUserRoles(WORKSPACE_NAMESPACE, WORKSPACE_FIRECLOUD_NAME))
        .thenReturn(firecloudUserRoles);
  }

  Set<String> extractValuesFromEvents(
      Collection<ActionAuditEvent> events, Function<ActionAuditEvent, String> fn) {
    return events.stream().map(fn).collect(Collectors.toSet());
  }

  @Test
  public void testFireEgressEvent() {
    egressEventAuditor.fireEgressEvent(
        new EgressEvent()
            .projectName(EGRESS_EVENT_PROJECT_NAME)
            .vmName(EGRESS_EVENT_VM_NAME)
            .timeWindowStart(0l)
            .egressMib(12.3));
    verify(mockActionAuditService).send(eventsCaptor.capture());
    Collection<ActionAuditEvent> events = eventsCaptor.getValue();

    // Ensure all events have the expected set of constant fields.
    assertThat(events.stream().map(event -> event.getAgentType()).collect(Collectors.toSet()))
        .containsExactly(AgentType.USER);
    assertThat(events.stream().map(event -> event.getActionType()).collect(Collectors.toSet()))
        .containsExactly(ActionType.EGRESS_EVENT);
    assertThat(events.stream().map(event -> event.getAgentId()).collect(Collectors.toSet()))
        .containsExactly(USER_ID);
    assertThat(events.stream().map(event -> event.getAgentEmailMaybe()).collect(Collectors.toSet()))
        .containsExactly(USER_EMAIL);
    assertThat(events.stream().map(event -> event.getTargetIdMaybe()).collect(Collectors.toSet()))
        .containsExactly(WORKSPACE_ID);

    // We should have distinct event rows with values from the egress event.
    assertThat(
            events.stream()
                .filter(
                    event ->
                        event.getTargetPropertyMaybe()
                            == EgressEventTargetProperty.EGRESS_MIB.getPropertyName())
                .map(event -> event.getNewValueMaybe())
                .collect(Collectors.toSet()))
        .containsExactly("12.3");
    assertThat(
            events.stream()
                .filter(
                    event ->
                        event.getTargetPropertyMaybe()
                            == EgressEventTargetProperty.VM_NAME.getPropertyName())
                .map(event -> event.getNewValueMaybe())
                .collect(Collectors.toSet()))
        .containsExactly(EGRESS_EVENT_VM_NAME);
  }

  @Test
  public void testNoWorkspaceFound() {
    exception.expect(BadRequestException.class);

    // When the workspace lookup doesn't succeed, the event is filed w/ a system agent and an
    // empty target ID.
    when(mockWorkspaceService.getByNamespace(WORKSPACE_NAMESPACE)).thenReturn(Optional.empty());
    egressEventAuditor.fireEgressEvent(
        new EgressEvent().projectName(EGRESS_EVENT_PROJECT_NAME).vmName(EGRESS_EVENT_VM_NAME));
    verify(mockActionAuditService).send(eventsCaptor.capture());
    Collection<ActionAuditEvent> events = eventsCaptor.getValue();

    // Some of the properties should be nulled out, since we can't identify the target workspace
    // for the egress event.
    assertThat(events.stream().map(event -> event.getAgentId()).collect(Collectors.toSet()))
        .containsExactly(0);
    assertThat(
            events.stream()
                .map(event -> event.getAgentEmailMaybe())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()))
        .isEmpty();
    assertThat(
            events.stream()
                .map(event -> event.getTargetIdMaybe())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()))
        .isEmpty();

    // We expect to see an audit event row with a comment describing the issue encountered when
    // trying to handle the high-egress message.
    assertThat(
            events.stream()
                .filter(
                    event ->
                        event.getTargetPropertyMaybe()
                            == EgressEventCommentTargetProperty.COMMENT.getPropertyName())
                .map(event -> event.getNewValueMaybe())
                .findFirst()
                .get())
        .contains("Failed to find workspace");
  }

  @Test
  public void testFailedParsing() {
    // When the inbound request parsing fails, an event is logged at the system agent.
    when(mockWorkspaceService.getByNamespace(WORKSPACE_NAMESPACE)).thenReturn(null);
    egressEventAuditor.fireFailedToParseEgressEventRequest(
        new EgressEventRequest().eventsJsonArray("asdf"));
    verify(mockActionAuditService).send(eventsCaptor.capture());
    Collection<ActionAuditEvent> events = eventsCaptor.getValue();

    assertThat(
            events.stream()
                .filter(
                    event ->
                        EgressEventCommentTargetProperty.COMMENT
                            .getPropertyName()
                            .equals(event.getTargetPropertyMaybe()))
                .map(event -> event.getNewValueMaybe())
                .findFirst()
                .get())
        .contains("Failed to parse egress event");
  }

  @Test
  public void testBadApiKey() {
    // When the inbound request parsing fails, an event is logged at the system agent.
    when(mockWorkspaceService.getByNamespace(WORKSPACE_NAMESPACE)).thenReturn(null);
    egressEventAuditor.fireBadApiKey("ASDF", new EgressEventRequest());
    verify(mockActionAuditService).send(eventsCaptor.capture());
    Collection<ActionAuditEvent> events = eventsCaptor.getValue();

    assertThat(
            events.stream()
                .filter(
                    event ->
                        EgressEventCommentTargetProperty.COMMENT
                            .getPropertyName()
                            .equals(event.getTargetPropertyMaybe()))
                .map(event -> event.getNewValueMaybe())
                .findFirst()
                .get())
        .contains("Received bad API key");
  }
}
