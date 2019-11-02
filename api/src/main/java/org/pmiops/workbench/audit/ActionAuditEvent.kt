package org.pmiops.workbench.audit

import java.util.Optional
import java.util.UUID

interface ActionAuditEvent {

    fun timestamp(): Long

    fun actionId(): String

    fun agentType(): AgentType

    fun agentId(): Long

    fun agentEmail(): Optional<String>

    fun actionType(): ActionType

    fun targetType(): TargetType

    fun targetId(): Optional<Long>

    fun targetProperty(): Optional<String>

    fun previousValue(): Optional<String>

    fun newValue(): Optional<String>

    companion object {

        fun newActionId(): String {
            return UUID.randomUUID().toString()
        }
    }
}
