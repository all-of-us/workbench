package org.pmiops.workbench.utils.mappers;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static org.pmiops.workbench.actionaudit.auditors.ActionAuditTestConfig.ACTION_ID;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.model.AuditAction;
import org.pmiops.workbench.model.AuditAgent;
import org.pmiops.workbench.model.AuditEventBundle;
import org.pmiops.workbench.model.AuditEventBundleHeader;
import org.pmiops.workbench.model.AuditLogEntry;
import org.pmiops.workbench.model.AuditTargetPropertyChange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class AuditLogEntryMapperTest {

  public static final long AGENT_ID = 101L;
  public static final String USER_AGENT_TYPE = "USER";
  public static final String AGENT_USERNAME = "paul@beatles.info";
  public static final long TARGET_ID = 202L;
  public static final String ACTION_TYPE_DELETE = "DELETE";
  public static final String TARGET_TYPE_WORKSPACE = "WORKSPACE";
  public static final String TARGET_PROPERTY = "title";
  public static final String PREVIOUS_VALUE = "District 5";
  public static final String NEW_VALUE = "The Mighty Ducks";
  public static final String LOGIN_ACTION_TYPE = "LOGIN";
  public static final DateTime EVENT_TIME = new DateTime(1579013840545L);
  public static final String WORKBENCH_TARGET_TYPE = "WORKBENCH";
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
            .agentId(AGENT_ID)
            .agentUsername(AGENT_USERNAME)
            .agentType(USER_AGENT_TYPE)
            .actionType(ACTION_TYPE_DELETE)
            .targetId(TARGET_ID)
            .targetType(TARGET_TYPE_WORKSPACE);
    final AuditEventBundleHeader header = auditLogEntryMapper.logEntryToEventBundleHeader(logEntry);
    assertThat(header.getActionType()).isEqualTo(ACTION_TYPE_DELETE);
    assertThat(header.getAgent().getAgentUsername()).isEqualTo(AGENT_USERNAME);
    assertThat(header.getAgent().getAgentType()).isEqualTo(USER_AGENT_TYPE);
    assertThat(header.getAgent().getAgentId()).isEqualTo(AGENT_ID);
    assertThat(header.getTarget().getTargetId()).isEqualTo(TARGET_ID);
    assertThat(header.getTarget().getTargetType()).isEqualTo(TARGET_TYPE_WORKSPACE);
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
    assertThat(propertyMaybe.get().getPreviousValue()).isNull();
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
                    .actionId(ACTION_ID)
                    .actionType(LOGIN_ACTION_TYPE)
                    .agentId(AGENT_ID)
                    .agentType(USER_AGENT_TYPE)
                    .agentUsername(AGENT_USERNAME)
                    .eventTime(EVENT_TIME)
                    .newValue(null)
                    .previousValue(null)
                    .targetId(null)
                    .targetProperty(null)
                    .targetType(WORKBENCH_TARGET_TYPE)));
    assertThat(auditAction.getActionId()).isEqualTo(ACTION_ID);
    assertThat(auditAction.getActionTime()).isEqualTo(EVENT_TIME);

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
}
