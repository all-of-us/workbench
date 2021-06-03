package org.pmiops.workbench.utils.mappers;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static org.pmiops.workbench.utils.TimeAssertions.assertTimeApprox;

import com.google.common.collect.ImmutableList;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.SpringTest;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.model.AuditAction;
import org.pmiops.workbench.model.AuditAgent;
import org.pmiops.workbench.model.AuditEventBundle;
import org.pmiops.workbench.model.AuditEventBundleHeader;
import org.pmiops.workbench.model.AuditLogEntry;
import org.pmiops.workbench.model.AuditTargetPropertyChange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

public class AuditLogEntryMapperTest extends SpringTest {

  private static final OffsetDateTime EVENT_TIME_1 = OffsetDateTime.parse("2010-06-30T01:20+02:00");
  private static final OffsetDateTime EVENT_TIME_2 = OffsetDateTime.parse("2015-06-30T01:20+02:00");
  private static final long AGENT_ID = 101L;
  private static final long TARGET_ID = 202L;
  private static final String ACTION_ID_1 = "11111111-1111-1111-1111-111111111111";
  private static final String ACTION_ID_2 = "22222222-2222-2222-2222-222222222222";
  private static final String ACTION_TYPE_CREATE = "CREATE";
  private static final String ACTION_TYPE_DELETE = "DELETE";
  private static final String AGENT_USERNAME = "paul@beatles.info";
  private static final String ETAG = "\"101\"";
  private static final String ETAG_TARGET_PROPERTY = "etag";
  private static final String LOGIN_ACTION_TYPE = "LOGIN";
  private static final String NEW_VALUE = "The Mighty Ducks";
  private static final String PREVIOUS_VALUE = "District 5";
  private static final String TARGET_PROPERTY = "title";
  private static final String USER_AGENT_TYPE = "USER";
  private static final String WORKBENCH_TARGET_TYPE = "WORKBENCH";
  private static final String WORKSPACE_TARGET_TYPE = "WORKSPACE";

  @Autowired private AuditLogEntryMapper auditLogEntryMapper;

  @TestConfiguration
  @Import({AuditLogEntryMapperImpl.class})
  public static class Config {}

  @Test
  public void testLogEntryToAgent() {
    final AuditLogEntry logEntry =
        new AuditLogEntry()
            .actionType("CREATE")
            .agentId(AGENT_ID)
            .agentType(USER_AGENT_TYPE)
            .agentUsername(AGENT_USERNAME);
    final AuditAgent auditAgent = auditLogEntryMapper.logEntryToAgent(logEntry);
    assertThat(auditAgent.getAgentId()).isEqualTo(AGENT_ID);
    assertThat(auditAgent.getAgentType()).isEqualTo(USER_AGENT_TYPE);
    assertThat(auditAgent.getAgentUsername()).isEqualTo(AGENT_USERNAME);
  }

  @Test
  public void testLogEntryToEventBundleHeader() {
    final AuditLogEntry logEntry =
        new AuditLogEntry()
            .actionType(ACTION_TYPE_DELETE)
            .agentId(AGENT_ID)
            .agentType(USER_AGENT_TYPE)
            .agentUsername(AGENT_USERNAME)
            .targetId(TARGET_ID)
            .targetType(WORKSPACE_TARGET_TYPE);
    final AuditEventBundleHeader header = auditLogEntryMapper.logEntryToEventBundleHeader(logEntry);
    assertThat(header.getActionType()).isEqualTo(ACTION_TYPE_DELETE);
    assertThat(header.getAgent().getAgentUsername()).isEqualTo(AGENT_USERNAME);
    assertThat(header.getAgent().getAgentType()).isEqualTo(USER_AGENT_TYPE);
    assertThat(header.getAgent().getAgentId()).isEqualTo(AGENT_ID);
    assertThat(header.getTarget().getTargetId()).isEqualTo(TARGET_ID);
    assertThat(header.getTarget().getTargetType()).isEqualTo(WORKSPACE_TARGET_TYPE);
  }

  @Test
  public void testLogEntryToTargetPropertyChange() {
    final AuditLogEntry logEntry =
        new AuditLogEntry()
            .targetProperty(TARGET_PROPERTY)
            .previousValue(PREVIOUS_VALUE)
            .newValue(NEW_VALUE);
    final Optional<AuditTargetPropertyChange> propertyMaybe =
        auditLogEntryMapper.logEntryToTargetPropertyChange(logEntry);
    assertThat(propertyMaybe).isPresent();
    assertThat(propertyMaybe.map(AuditTargetPropertyChange::getTargetProperty))
        .hasValue(TARGET_PROPERTY);
    assertThat(propertyMaybe.map(AuditTargetPropertyChange::getPreviousValue))
        .hasValue(PREVIOUS_VALUE);
    assertThat(propertyMaybe.map(AuditTargetPropertyChange::getNewValue)).hasValue(NEW_VALUE);
  }

  @Test
  public void testLogEntryToTargetPropertyChange_createdValue() {
    final AuditLogEntry logEntry =
        new AuditLogEntry().targetProperty(TARGET_PROPERTY).previousValue(null).newValue(NEW_VALUE);
    final Optional<AuditTargetPropertyChange> propertyMaybe =
        auditLogEntryMapper.logEntryToTargetPropertyChange(logEntry);
    assertThat(propertyMaybe).isPresent();
    assertThat(propertyMaybe.map(AuditTargetPropertyChange::getTargetProperty))
        .hasValue(TARGET_PROPERTY);
    assertThat(propertyMaybe.map(AuditTargetPropertyChange::getPreviousValue)).isEmpty();
    assertThat(propertyMaybe.map(AuditTargetPropertyChange::getNewValue)).hasValue(NEW_VALUE);
  }

  /**
   * At the moment, and historically, we write empty stings to the audit stream for things like
   * workspace properties (when empty). Make sure they propagate, unless/until we tighten things
   * upstream.
   */
  @Test
  public void testLogEntryToTargetPropertyChange_toleratesEmptyString() {
    final AuditLogEntry logEntry =
        new AuditLogEntry().targetProperty(TARGET_PROPERTY).previousValue(null).newValue("");
    final Optional<AuditTargetPropertyChange> propertyMaybe =
        auditLogEntryMapper.logEntryToTargetPropertyChange(logEntry);
    assertThat(propertyMaybe).isPresent();
    assertThat(propertyMaybe.map(AuditTargetPropertyChange::getTargetProperty))
        .hasValue(TARGET_PROPERTY);
    assertThat(propertyMaybe.get().getPreviousValue()).isNull();
    assertThat(propertyMaybe.map(AuditTargetPropertyChange::getNewValue)).hasValue("");
  }

  @Test
  public void testLogEntryToTargetPropertyChange_emptyIfNoProperty() {
    assertThat(auditLogEntryMapper.logEntryToTargetPropertyChange(new AuditLogEntry())).isEmpty();
  }

  @Test
  public void testBuildAuditAction_loginAction() {
    final AuditAction auditAction =
        auditLogEntryMapper.buildAuditAction(
            ImmutableList.of(
                new AuditLogEntry()
                    .actionId(ACTION_ID_1)
                    .actionType(LOGIN_ACTION_TYPE)
                    .agentId(AGENT_ID)
                    .agentType(USER_AGENT_TYPE)
                    .agentUsername(AGENT_USERNAME)
                    .eventTime(EVENT_TIME_1.toInstant().toEpochMilli())
                    .newValue(null)
                    .previousValue(null)
                    .targetId(null)
                    .targetProperty(null)
                    .targetType(WORKBENCH_TARGET_TYPE)));
    assertThat(auditAction.getActionId()).isEqualTo(ACTION_ID_1);
    assertTimeApprox(auditAction.getActionTime(), EVENT_TIME_1);

    assertThat(auditAction.getEventBundles()).hasSize(1);

    final AuditEventBundle eventBundle = auditAction.getEventBundles().get(0);
    assertThat(eventBundle.getHeader().getTarget().getTargetType())
        .isEqualTo(WORKBENCH_TARGET_TYPE);
    assertThat(eventBundle.getHeader().getTarget().getTargetId()).isNull();
    assertThat(eventBundle.getHeader().getAgent().getAgentId()).isEqualTo(AGENT_ID);
    assertThat(eventBundle.getHeader().getAgent().getAgentType()).isEqualTo(USER_AGENT_TYPE);
    assertThat(eventBundle.getHeader().getAgent().getAgentUsername()).isEqualTo(AGENT_USERNAME);

    assertThat(eventBundle.getPropertyChanges()).isEmpty();
  }

  @Test
  public void testBuildAuditAction_multipleRowAction() {
    List<AuditLogEntry> logEntries =
        ImmutableList.of(
            new AuditLogEntry()
                .actionId(ACTION_ID_1)
                .actionType(ACTION_TYPE_CREATE)
                .agentId(AGENT_ID)
                .agentType(USER_AGENT_TYPE)
                .agentUsername(AGENT_USERNAME)
                .eventTime(EVENT_TIME_1.toInstant().toEpochMilli())
                .newValue("The Grapes of Wrath")
                .previousValue(null)
                .targetId(TARGET_ID)
                .targetProperty(TARGET_PROPERTY)
                .targetType(WORKSPACE_TARGET_TYPE),
            new AuditLogEntry()
                .actionId(ACTION_ID_1)
                .actionType(ACTION_TYPE_CREATE)
                .agentId(AGENT_ID)
                .agentType(USER_AGENT_TYPE)
                .agentUsername(AGENT_USERNAME)
                .eventTime(EVENT_TIME_1.toInstant().toEpochMilli())
                .newValue(ETAG)
                .previousValue(null)
                .targetId(TARGET_ID)
                .targetProperty(ETAG_TARGET_PROPERTY)
                .targetType(WORKSPACE_TARGET_TYPE));

    final AuditAction auditAction = auditLogEntryMapper.buildAuditAction(logEntries);
    assertTimeApprox(auditAction.getActionTime(), EVENT_TIME_1);
    assertThat(auditAction.getEventBundles()).hasSize(1);

    final AuditEventBundle eventBundle = auditAction.getEventBundles().get(0);
    final AuditEventBundleHeader header = eventBundle.getHeader();
    assertThat(header.getAgent().getAgentId()).isEqualTo(AGENT_ID);
    assertThat(header.getAgent().getAgentType()).isEqualTo(USER_AGENT_TYPE);
    assertThat(header.getAgent().getAgentUsername()).isEqualTo(AGENT_USERNAME);

    assertThat(header.getTarget().getTargetType()).isEqualTo(WORKSPACE_TARGET_TYPE);
    assertThat(header.getTarget().getTargetId()).isEqualTo(TARGET_ID);

    assertThat(header.getActionType()).isEqualTo(ACTION_TYPE_CREATE);
  }

  @Test
  public void testLogEntriesToActions() {
    List<AuditLogEntry> logEntries =
        ImmutableList.of(
            new AuditLogEntry()
                .actionId(ACTION_ID_1)
                .actionType(ACTION_TYPE_CREATE)
                .agentId(AGENT_ID)
                .agentType(USER_AGENT_TYPE)
                .agentUsername(AGENT_USERNAME)
                .eventTime(EVENT_TIME_1.toInstant().toEpochMilli())
                .newValue("The Grapes of Wrath")
                .previousValue(null)
                .targetId(TARGET_ID)
                .targetProperty(TARGET_PROPERTY)
                .targetType(WORKSPACE_TARGET_TYPE),
            new AuditLogEntry()
                .actionId(ACTION_ID_1)
                .actionType(ACTION_TYPE_CREATE)
                .agentId(AGENT_ID)
                .agentType(USER_AGENT_TYPE)
                .agentUsername(AGENT_USERNAME)
                .eventTime(EVENT_TIME_1.toInstant().toEpochMilli())
                .newValue(ETAG)
                .previousValue(null)
                .targetId(TARGET_ID)
                .targetProperty(ETAG_TARGET_PROPERTY)
                .targetType(WORKSPACE_TARGET_TYPE),
            new AuditLogEntry()
                .actionId(ACTION_ID_2)
                .actionType(ACTION_TYPE_DELETE)
                .agentId(AGENT_ID)
                .agentType(USER_AGENT_TYPE)
                .agentUsername(AGENT_USERNAME)
                .eventTime(EVENT_TIME_2.toInstant().toEpochMilli())
                .newValue(ETAG)
                .previousValue(null)
                .targetId(TARGET_ID)
                .targetProperty(null)
                .targetType(WORKSPACE_TARGET_TYPE));

    final List<AuditAction> actions = auditLogEntryMapper.logEntriesToActions(logEntries);

    assertThat(actions).hasSize(2);
    final AuditAction action1 =
        actions.stream()
            .filter(a -> a.getActionId().equals(ACTION_ID_1))
            .findFirst()
            .orElseThrow(() -> new NotFoundException("Action not found"));
    assertTimeApprox(action1.getActionTime(), EVENT_TIME_1);
    assertThat(action1.getEventBundles()).hasSize(1);
    assertTimeApprox(action1.getActionTime(), EVENT_TIME_1);
    assertThat(action1.getEventBundles()).hasSize(1);

    final AuditEventBundle eventBundle1 = action1.getEventBundles().get(0);
    final AuditEventBundleHeader header1 = eventBundle1.getHeader();
    assertThat(header1.getAgent().getAgentId()).isEqualTo(AGENT_ID);
    assertThat(header1.getAgent().getAgentType()).isEqualTo(USER_AGENT_TYPE);
    assertThat(header1.getAgent().getAgentUsername()).isEqualTo(AGENT_USERNAME);

    assertThat(header1.getTarget().getTargetType()).isEqualTo(WORKSPACE_TARGET_TYPE);
    assertThat(header1.getTarget().getTargetId()).isEqualTo(TARGET_ID);

    assertThat(header1.getActionType()).isEqualTo(ACTION_TYPE_CREATE);
    assertThat(eventBundle1.getPropertyChanges()).hasSize(2);

    final AuditAction action2 =
        actions.stream()
            .filter(a -> a.getActionId().equals(ACTION_ID_2))
            .findFirst()
            .orElseThrow(() -> new NotFoundException("Action not found"));
    assertTimeApprox(action2.getActionTime(), EVENT_TIME_2.toInstant().toEpochMilli());
    final AuditEventBundle bundle2 = action2.getEventBundles().get(0);
    assertThat(bundle2.getHeader().getActionType()).isEqualTo(ACTION_TYPE_DELETE);

    final AuditEventBundle eventBundle2 = action2.getEventBundles().get(0);
    final AuditEventBundleHeader header2 = eventBundle2.getHeader();
    assertThat(header2.getAgent().getAgentId()).isEqualTo(AGENT_ID);
    assertThat(header2.getAgent().getAgentType()).isEqualTo(USER_AGENT_TYPE);
    assertThat(header2.getAgent().getAgentUsername()).isEqualTo(AGENT_USERNAME);
    assertThat(header2.getTarget().getTargetType()).isEqualTo(WORKSPACE_TARGET_TYPE);
    assertThat(header2.getTarget().getTargetId()).isEqualTo(TARGET_ID);

    assertThat(header2.getActionType()).isEqualTo(ACTION_TYPE_DELETE);
    assertThat(eventBundle2.getPropertyChanges()).hasSize(1);
  }
}
