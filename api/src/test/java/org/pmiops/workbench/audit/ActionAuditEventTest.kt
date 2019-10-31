package org.pmiops.workbench.audit

import com.google.common.truth.Truth.assertThat

import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
class ActionAuditEventTest {


    @Test(expected = IllegalArgumentException::class)
    fun testThrowsWhenRequiredStringIsBlank() {
//        ActionAuditEventImpl.builder()
//                .setActionType(ActionType.BYPASS)
//                .setAgentType(AgentType.ADMINISTRATOR)
//                .setAgentId(222L)
//                .setTimestamp(11001001L)
//                .setActionId("")
//                .setTargetType(TargetType.USER)
//                .build()
    }
}
