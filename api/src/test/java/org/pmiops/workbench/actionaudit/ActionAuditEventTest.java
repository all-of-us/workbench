package org.pmiops.workbench.actionaudit;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import org.junit.jupiter.api.Test;

public class ActionAuditEventTest {

  private static final String ACTION_ID = "foo";
  private static final long USER_ID = 333L;
  private static final String USER_EMAIL = "jay@msn.com";
  private static final String TARGET_PROPERTY = "name";
  private static final String NEW_VALUE = "Fred";
  private static final long TIMESTAMP = Instant.parse("2007-01-03T00:00:00.00Z").toEpochMilli();

  @Test
  public void testActionAuditEventBuilder_throwsOnMissingValues() {
    assertThrows(IllegalArgumentException.class, () -> ActionAuditEvent.builder().build());
  }

  @Test
  public void testActionAuditEventBuilder_constructsEvent() {
    ActionAuditEvent event =
        ActionAuditEvent.builder()
            .timestamp(TIMESTAMP)
            .agentType(AgentType.USER)
            .actionId(ACTION_ID)
            .actionType(ActionType.EDIT)
            .agentIdMaybe(USER_ID)
            .agentEmailMaybe(USER_EMAIL)
            .targetType(TargetType.COHORT)
            .targetPropertyMaybe(TARGET_PROPERTY)
            .newValueMaybe(NEW_VALUE)
            .build();
    assertThat(event.timestamp()).isEqualTo(TIMESTAMP);
    assertThat(event.agentType()).isEqualTo(AgentType.USER);
    assertThat(event.actionId()).isEqualTo(ACTION_ID);
    assertThat(event.actionType()).isEqualTo(ActionType.EDIT);
    assertThat(event.agentIdMaybe()).isEqualTo(USER_ID);
    assertThat(event.agentEmailMaybe()).isEqualTo(USER_EMAIL);
    assertThat(event.targetType()).isEqualTo(TargetType.COHORT);
    assertThat(event.targetPropertyMaybe()).isEqualTo(TARGET_PROPERTY);
    assertThat(event.previousValueMaybe()).isNull();
    assertThat(event.newValueMaybe()).isEqualTo(NEW_VALUE);
  }
}
