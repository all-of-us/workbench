package org.pmiops.workbench.audit;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class ActionAuditEventTest {

  @Test(expected = IllegalArgumentException.class)
  public void testThrowsWhenRequiredParameterMissing() {
    ActionAuditEventImpl.builder().build();
  }

  @Test
  public void testMinimumRequiredPropertiesDoesNotthrow() {
    final ActionAuditEvent event =
        ActionAuditEventImpl.builder()
            .setActionType(ActionType.BYPASS)
            .setAgentType(AgentType.ADMINISTRATOR)
            .setAgentId(222L)
            .setTimestamp(11001001L)
            .setActionId(ActionAuditEvent.newActionId())
            .setTargetType(TargetType.USER)
            .build();
    assertThat(event.targetType()).isEqualTo(TargetType.USER);
    assertThat(event.targetProperty().isPresent()).isFalse();
    assertThat(event.previousValue().isPresent()).isFalse();
    assertThat(event.newValue().isPresent()).isFalse();
    assertThat(event.agentEmail().isPresent()).isFalse();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testThrowsWhenRequiredStringIsBlank() {
    ActionAuditEventImpl.builder()
        .setActionType(ActionType.BYPASS)
        .setAgentType(AgentType.ADMINISTRATOR)
        .setAgentId(222L)
        .setTimestamp(11001001L)
        .setActionId("")
        .setTargetType(TargetType.USER)
        .build();
  }
}
