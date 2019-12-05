package org.pmiops.workbench.actionaudit;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class ActionAuditEventTest {

  private static final long AGENT_ID = 333L;

  @Test(expected = IllegalArgumentException.class)
  public void testActionAuditEventBuilder_throwsOnMissingValues() {
    ActionAuditEvent.builder().build();
  }

  @Test
  public void testActionAuditEventBuilder_constructsEvent() {
    ActionAuditEvent event =
        ActionAuditEvent.builder()
            .timestamp(System.currentTimeMillis())
            .agentType(AgentType.USER)
            .actionId("foo")
            .actionType(ActionType.EDIT)
            .agentId(333L)
            .agentEmailMaybe("jay@msn.com")
            .targetType(TargetType.COHORT)
            .targetIdMaybe(null)
            .targetPropertyMaybe("name")
            .previousValueMaybe(null)
            .newValueMaybe("Fred")
            .build();
    assertThat(event.getAgentId()).isEqualTo(AGENT_ID);
  }
}
