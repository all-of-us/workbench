package org.pmiops.workbench.actionaudit.auditors;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Iterables;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.actionaudit.ActionAuditEvent;
import org.pmiops.workbench.actionaudit.ActionAuditService;
import org.pmiops.workbench.actionaudit.ActionType;
import org.pmiops.workbench.actionaudit.AgentType;
import org.pmiops.workbench.actionaudit.targetproperties.EgressEscalationTargetProperty;
import org.pmiops.workbench.actionaudit.targetproperties.EgressEventCommentTargetProperty;
import org.pmiops.workbench.actionaudit.targetproperties.EgressEventTargetProperty;
import org.pmiops.workbench.config.WorkbenchConfig.EgressAlertRemediationPolicy.Escalation;
import org.pmiops.workbench.config.WorkbenchConfig.EgressAlertRemediationPolicy.Escalation.DisableUser;
import org.pmiops.workbench.config.WorkbenchConfig.EgressAlertRemediationPolicy.Escalation.SuspendCompute;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbEgressEvent;
import org.pmiops.workbench.db.model.DbEgressEvent.DbEgressEventStatus;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.model.SumologicEgressEvent;
import org.pmiops.workbench.model.SumologicEgressEventRequest;
import org.pmiops.workbench.model.UserRole;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig
public class EgressEventAuditorTest {

  private static final long USER_ID = 1L;
  private static final String USER_EMAIL = "user@researchallofus.org";
  private static final long WORKSPACE_ID = 1L;
  private static final String WORKSPACE_NAMESPACE = "aou-rw-test-c7dec260";
  private static final String GOOGLE_PROJECT = "gcp-project-id";
  private static final String WORKSPACE_FIRECLOUD_NAME = "mytestworkspacename";
  private static final Timestamp NOW = Timestamp.from(Instant.parse("2000-01-01T00:00:00.00Z"));

  private static final String EGRESS_EVENT_PROJECT_NAME = GOOGLE_PROJECT;
  private static final String EGRESS_EVENT_VM_PREFIX = "all-of-us-" + USER_ID;

  // Pre-built data objects for test.
  private static DbUser dbUser;
  private DbWorkspace dbWorkspace;
  private List<UserRole> firecloudUserRoles = new ArrayList<>();

  @Autowired private EgressEventAuditor egressEventAuditor;

  @MockBean private ActionAuditService mockActionAuditService;
  @MockBean private WorkspaceService mockWorkspaceService;
  @MockBean private WorkspaceDao workspaceDao;
  @MockBean private UserDao mockUserDao;

  @Captor private ArgumentCaptor<Collection<ActionAuditEvent>> eventsCaptor;

  @TestConfiguration
  @Import({
    FakeClockConfiguration.class,
    // Import the impl class to allow autowiring the bean.
    EgressEventAuditorImpl.class,
    // Import common action audit beans.
    ActionAuditTestConfig.class
  })
  static class Configuration {}

  @BeforeEach
  public void setUp() {
    dbUser = new DbUser();
    dbUser.setUserId(USER_ID);
    dbUser.setUsername(USER_EMAIL);
    when(mockUserDao.findUserByUsername(USER_EMAIL)).thenReturn(dbUser);

    dbWorkspace = new DbWorkspace();
    dbWorkspace.setWorkspaceId(WORKSPACE_ID);
    dbWorkspace.setGoogleProject(GOOGLE_PROJECT);
    dbWorkspace.setWorkspaceNamespace(WORKSPACE_NAMESPACE);
    dbWorkspace.setFirecloudName(WORKSPACE_FIRECLOUD_NAME);
    when(workspaceDao.getByGoogleProject(GOOGLE_PROJECT)).thenReturn(Optional.of(dbWorkspace));
    firecloudUserRoles.add(new UserRole().email(USER_EMAIL));
    when(mockWorkspaceService.getFirecloudUserRoles(WORKSPACE_NAMESPACE, WORKSPACE_FIRECLOUD_NAME))
        .thenReturn(firecloudUserRoles);
  }

  @Test
  public void testFireEgressEvent() {
    egressEventAuditor.fireEgressEvent(
        new SumologicEgressEvent()
            .projectName(EGRESS_EVENT_PROJECT_NAME)
            .vmPrefix(EGRESS_EVENT_VM_PREFIX)
            .timeWindowStart(0l)
            .egressMib(12.3)
            .gceEgressMib(0.0)
            .dataprocMasterEgressMib(10.0)
            .dataprocWorkerEgressMib(2.3));
    verify(mockActionAuditService).send(eventsCaptor.capture());
    Collection<ActionAuditEvent> events = eventsCaptor.getValue();

    // Ensure all events have the expected set of constant fields.
    assertThat(events.stream().map(event -> event.getAgentType()).collect(Collectors.toSet()))
        .containsExactly(AgentType.USER);
    assertThat(events.stream().map(event -> event.getActionType()).collect(Collectors.toSet()))
        .containsExactly(ActionType.DETECT_HIGH_EGRESS_EVENT);
    assertThat(events.stream().map(event -> event.getAgentIdMaybe()).collect(Collectors.toSet()))
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
        .containsExactly(EGRESS_EVENT_VM_PREFIX);
  }

  @Test
  public void testFireEgressEventForUser() {
    egressEventAuditor.fireEgressEventForUser(
        new SumologicEgressEvent()
            .projectName(EGRESS_EVENT_PROJECT_NAME)
            .vmPrefix(EGRESS_EVENT_VM_PREFIX)
            .timeWindowStart(0l)
            .egressMib(12.3)
            .gceEgressMib(0.0)
            .dataprocMasterEgressMib(10.0)
            .dataprocWorkerEgressMib(2.3),
        dbUser);
    verify(mockActionAuditService).send(eventsCaptor.capture());
    Collection<ActionAuditEvent> events = eventsCaptor.getValue();

    // Ensure all events have the expected set of constant fields.
    assertThat(events.stream().map(event -> event.getAgentType()).collect(Collectors.toSet()))
        .containsExactly(AgentType.USER);
    assertThat(events.stream().map(event -> event.getActionType()).collect(Collectors.toSet()))
        .containsExactly(ActionType.DETECT_HIGH_EGRESS_EVENT);
    assertThat(events.stream().map(event -> event.getAgentIdMaybe()).collect(Collectors.toSet()))
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
        .containsExactly(EGRESS_EVENT_VM_PREFIX);
  }

  @Test
  public void testNoWorkspaceFound() {
    // When the workspace lookup doesn't succeed, the event is filed w/ a system agent and an
    // empty target ID.
    when(workspaceDao.getByGoogleProject(GOOGLE_PROJECT)).thenReturn(Optional.empty());
    assertThrows(
        BadRequestException.class,
        () ->
            egressEventAuditor.fireEgressEvent(
                new SumologicEgressEvent()
                    .projectName(EGRESS_EVENT_PROJECT_NAME)
                    .vmPrefix(EGRESS_EVENT_VM_PREFIX)));
    verify(mockActionAuditService).send(eventsCaptor.capture());
    Collection<ActionAuditEvent> events = eventsCaptor.getValue();

    // Some of the properties should be nulled out, since we can't identify the target workspace
    // for the egress event.
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
  public void testFireRemediateEgressEvent_noEscalation() {
    egressEventAuditor.fireRemediateEgressEvent(
        new DbEgressEvent()
            .setWorkspace(dbWorkspace)
            .setUser(dbUser)
            .setEgressEventId(1337)
            .setCreationTime(NOW),
        null);
    verify(mockActionAuditService).send(eventsCaptor.capture());
    Collection<ActionAuditEvent> events = eventsCaptor.getValue();

    // Ensure all events have the expected set of constant fields.
    assertThat(events.stream().map(event -> event.getAgentType()).collect(Collectors.toSet()))
        .containsExactly(AgentType.SYSTEM);
    assertThat(events.stream().map(event -> event.getActionType()).collect(Collectors.toSet()))
        .containsExactly(ActionType.REMEDIATE_HIGH_EGRESS_EVENT);
    assertThat(events.stream().map(event -> event.getTargetIdMaybe()).collect(Collectors.toSet()))
        .containsExactly(WORKSPACE_ID);

    // We should have distinct event rows with values from the egress event.
    assertThat(
            events.stream()
                .filter(
                    event ->
                        event.getTargetPropertyMaybe()
                            == EgressEscalationTargetProperty.REMEDIATION.getPropertyName())
                .map(event -> event.getNewValueMaybe())
                .collect(Collectors.toSet()))
        .isEmpty();
    assertThat(
            events.stream()
                .filter(
                    event ->
                        event.getTargetPropertyMaybe()
                            == EgressEscalationTargetProperty.SUSPEND_COMPUTE_DURATION_MIN
                                .getPropertyName())
                .map(event -> event.getNewValueMaybe())
                .collect(Collectors.toSet()))
        .isEmpty();
  }

  @Test
  public void testFireRemediateEgressEvent_suspendCompute() {
    Escalation escalation = new Escalation();
    escalation.suspendCompute = new SuspendCompute();
    escalation.suspendCompute.durationMinutes = 15L;
    egressEventAuditor.fireRemediateEgressEvent(
        new DbEgressEvent()
            .setWorkspace(dbWorkspace)
            .setUser(dbUser)
            .setEgressEventId(1337)
            .setCreationTime(NOW),
        escalation);
    verify(mockActionAuditService).send(eventsCaptor.capture());
    Collection<ActionAuditEvent> events = eventsCaptor.getValue();

    // Ensure all events have the expected set of constant fields.
    assertThat(events.stream().map(event -> event.getAgentType()).collect(Collectors.toSet()))
        .containsExactly(AgentType.SYSTEM);
    assertThat(events.stream().map(event -> event.getActionType()).collect(Collectors.toSet()))
        .containsExactly(ActionType.REMEDIATE_HIGH_EGRESS_EVENT);
    assertThat(events.stream().map(event -> event.getTargetIdMaybe()).collect(Collectors.toSet()))
        .containsExactly(WORKSPACE_ID);

    // We should have distinct event rows with values from the egress event.
    assertThat(
            events.stream()
                .filter(
                    event ->
                        event.getTargetPropertyMaybe()
                            == EgressEscalationTargetProperty.REMEDIATION.getPropertyName())
                .map(event -> event.getNewValueMaybe())
                .collect(Collectors.toSet()))
        .containsExactly("suspend_compute");
    assertThat(
            events.stream()
                .filter(
                    event ->
                        event.getTargetPropertyMaybe()
                            == EgressEscalationTargetProperty.SUSPEND_COMPUTE_DURATION_MIN
                                .getPropertyName())
                .map(event -> event.getNewValueMaybe())
                .collect(Collectors.toSet()))
        .containsExactly("15");
  }

  @Test
  public void testFireRemediateEgressEvent_disableUser() {
    Escalation escalation = new Escalation();
    escalation.disableUser = new DisableUser();
    egressEventAuditor.fireRemediateEgressEvent(
        new DbEgressEvent()
            .setWorkspace(dbWorkspace)
            .setUser(dbUser)
            .setEgressEventId(1337)
            .setCreationTime(NOW),
        escalation);
    verify(mockActionAuditService).send(eventsCaptor.capture());
    Collection<ActionAuditEvent> events = eventsCaptor.getValue();

    // Ensure all events have the expected set of constant fields.
    assertThat(events.stream().map(event -> event.getAgentType()).collect(Collectors.toSet()))
        .containsExactly(AgentType.SYSTEM);
    assertThat(events.stream().map(event -> event.getActionType()).collect(Collectors.toSet()))
        .containsExactly(ActionType.REMEDIATE_HIGH_EGRESS_EVENT);
    assertThat(events.stream().map(event -> event.getTargetIdMaybe()).collect(Collectors.toSet()))
        .containsExactly(WORKSPACE_ID);

    // We should have distinct event rows with values from the egress event.
    assertThat(
            events.stream()
                .filter(
                    event ->
                        event.getTargetPropertyMaybe()
                            == EgressEscalationTargetProperty.REMEDIATION.getPropertyName())
                .map(event -> event.getNewValueMaybe())
                .collect(Collectors.toSet()))
        .containsExactly("disable_user");
    assertThat(
            events.stream()
                .filter(
                    event ->
                        event.getTargetPropertyMaybe()
                            == EgressEscalationTargetProperty.SUSPEND_COMPUTE_DURATION_MIN
                                .getPropertyName())
                .map(event -> event.getNewValueMaybe())
                .filter(Objects::nonNull)
                .collect(Collectors.toList()))
        .isEmpty();
  }

  @Test
  public void testAdminEditEgressEvent() {
    Supplier<DbEgressEvent> newEvent =
        () ->
            new DbEgressEvent()
                .setWorkspace(dbWorkspace)
                .setUser(dbUser)
                .setEgressEventId(1337)
                .setCreationTime(NOW);
    egressEventAuditor.fireAdminEditEgressEvent(
        newEvent.get().setStatus(DbEgressEventStatus.REMEDIATED),
        newEvent.get().setStatus(DbEgressEventStatus.VERIFIED_FALSE_POSITIVE));

    verify(mockActionAuditService).send(eventsCaptor.capture());
    Collection<ActionAuditEvent> events = eventsCaptor.getValue();

    // Ensure all events have the expected set of constant fields.
    assertThat(events.stream().map(event -> event.getAgentType()).collect(Collectors.toSet()))
        .containsExactly(AgentType.ADMINISTRATOR);
    assertThat(events.stream().map(event -> event.getAgentEmailMaybe()).collect(Collectors.toSet()))
        .containsExactly(ActionAuditTestConfig.ADMINISTRATOR_EMAIL);
    assertThat(events.stream().map(event -> event.getActionType()).collect(Collectors.toSet()))
        .containsExactly(ActionType.EDIT);
    assertThat(events.stream().map(event -> event.getTargetIdMaybe()).collect(Collectors.toSet()))
        .containsExactly(WORKSPACE_ID);

    assertThat(events).hasSize(1);
    ActionAuditEvent event = Iterables.getOnlyElement(events);
    assertThat(event.getTargetPropertyMaybe()).isEqualTo("status");
    assertThat(event.getPreviousValueMaybe()).isEqualTo(DbEgressEventStatus.REMEDIATED.toString());
    assertThat(event.getNewValueMaybe())
        .isEqualTo(DbEgressEventStatus.VERIFIED_FALSE_POSITIVE.toString());
  }

  @Test
  public void testFailedParsing() {
    // When the inbound request parsing fails, an event is logged at the system agent.
    when(workspaceDao.getByGoogleProject(GOOGLE_PROJECT)).thenReturn(null);
    egressEventAuditor.fireFailedToParseEgressEventRequest(
        new SumologicEgressEventRequest().eventsJsonArray("asdf"));
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
    when(workspaceDao.getByGoogleProject(GOOGLE_PROJECT)).thenReturn(null);
    egressEventAuditor.fireBadApiKey("ASDF", new SumologicEgressEventRequest());
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
