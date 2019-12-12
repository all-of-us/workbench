package org.pmiops.workbench.actionaudit;

import static com.google.common.truth.Truth.assertThat;

import java.time.Instant;
import org.junit.Test;

public class ActionAuditEventTest {

  private static final String ACTION_ID = "foo";
  private static final long USER_ID = 333L;
  private static final String USER_EMAIL = "jay@msn.com";
  private static final String TARGET_PROPERTY = "name";
  private static final String NEW_VALUE = "Fred";
  private static final long TIMESTAMP = Instant.parse("2007-01-03T00:00:00.00Z").toEpochMilli();

  @Test(expected = IllegalArgumentException.class)
  public void testActionAuditEventBuilder_throwsOnMissingValues() {
    ActionAuditEvent.builder().build();
  }

  @Test
  public void testActionAuditEventBuilder_constructsEvent() {
    ActionAuditEvent event =
        ActionAuditEvent.builder()
            .timestamp(TIMESTAMP)
            .agentType(AgentType.USER)
            .actionId(ACTION_ID)
            .actionType(ActionType.EDIT)
            .agentId(USER_ID)
            .agentEmailMaybe(USER_EMAIL)
            .targetType(TargetType.COHORT)
            .targetPropertyMaybe(TARGET_PROPERTY)
            .newValueMaybe(NEW_VALUE)
            .build();
    assertThat(event.getTimestamp()).isEqualTo(TIMESTAMP);
    assertThat(event.getAgentType()).isEqualTo(AgentType.USER);
    assertThat(event.getActionId()).isEqualTo(ACTION_ID);
    assertThat(event.getActionType()).isEqualTo(ActionType.EDIT);
    assertThat(event.getAgentId()).isEqualTo(USER_ID);
    assertThat(event.getAgentEmailMaybe()).isEqualTo(USER_EMAIL);
    assertThat(event.getTargetType()).isEqualTo(TargetType.COHORT);
    assertThat(event.getTargetPropertyMaybe()).isEqualTo(TARGET_PROPERTY);
    assertThat(event.getPreviousValueMaybe()).isNull();
    assertThat(event.getNewValueMaybe()).isEqualTo(NEW_VALUE);
  }
}
