package org.pmiops.workbench.audit

import com.google.common.truth.Truth.assertThat

import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
class ActionAuditEventTest {

    @Test(expected = IllegalArgumentException::class)
    fun testThrowsWhenRequiredParameterMissing() {
        ActionAuditEventImpl.builder().build()
    }

    @Test
    fun testMinimumRequiredPropertiesDoesNotthrow() {
        val event = ActionAuditEventImpl.builder()
                .setActionType(ActionType.BYPASS)
                .setAgentType(AgentType.ADMINISTRATOR)
                .setAgentId(222L)
                .setTimestamp(11001001L)
                .setActionId(ActionAuditEvent.newActionId())
                .setTargetType(TargetType.USER)
                .build()
        assertThat(event.targetType()).isEqualTo(TargetType.USER)
        assertThat(event.targetProperty().isPresent).isFalse()
        assertThat(event.previousValue().isPresent).isFalse()
        assertThat(event.newValue().isPresent).isFalse()
        assertThat(event.agentEmail().isPresent).isFalse()
    }

    @Test(expected = IllegalArgumentException::class)
    fun testThrowsWhenRequiredStringIsBlank() {
        ActionAuditEventImpl.builder()
                .setActionType(ActionType.BYPASS)
                .setAgentType(AgentType.ADMINISTRATOR)
                .setAgentId(222L)
                .setTimestamp(11001001L)
                .setActionId("")
                .setTargetType(TargetType.USER)
                .build()
    }
}
