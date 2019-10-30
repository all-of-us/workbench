package org.pmiops.workbench.audit

import com.google.cloud.MonitoredResource
import com.google.cloud.logging.LogEntry
import com.google.cloud.logging.Payload.JsonPayload
import com.google.cloud.logging.Severity
import java.util.HashMap
import java.util.Optional

abstract class AbstractAuditableEvent {

    abstract fun timestamp(): Long

    abstract fun actionId(): String

    abstract fun agentType(): AgentType

    abstract fun agentId(): Long

    abstract fun agentEmail(): Optional<String>

    abstract fun actionType(): ActionType

    abstract fun targetType(): TargetType

    abstract fun targetId(): Optional<Long>

    abstract fun targetProperty(): Optional<String>

    abstract fun previousValue(): Optional<String>

    abstract fun newValue(): Optional<String>

    private fun toJsonPayload(): JsonPayload {
        val result = HashMap<String, Any>() // allow null values
        result[AuditColumn.TIMESTAMP.name] = timestamp()
        result[AuditColumn.ACTION_ID.name] = actionId()
        result[AuditColumn.ACTION_TYPE.name] = actionType()
        result[AuditColumn.AGENT_TYPE.name] = agentType()
        result[AuditColumn.AGENT_ID.name] = agentId()
        result[AuditColumn.AGENT_EMAIL.name] = toNullable(agentEmail())
        result[AuditColumn.TARGET_TYPE.name] = targetType()
        result[AuditColumn.TARGET_ID.name] = toNullable(targetId())
        result[AuditColumn.AGENT_ID.name] = agentId()
        result[AuditColumn.TARGET_PROPERTY.name] = toNullable(targetProperty())
        result[AuditColumn.PREV_VALUE.name] = toNullable(previousValue())
        result[AuditColumn.NEW_VALUE.name] = toNullable(newValue())
        return JsonPayload.of(result)
    }

    fun toLogEntry(): LogEntry {

        return LogEntry.newBuilder(toJsonPayload())
                .setSeverity(Severity.INFO)
                .setLogName(LOG_NAME)
                .setResource(MONITORED_RESOURCE)
                .build()
    }

    companion object {
        private val MONITORED_RESOURCE = MonitoredResource.newBuilder("global").build()
        private val LOG_NAME = "action-audit-test-3"

        // Inverse of Optional.ofNullable(). Used for JSON api which expects null values for empty
        // columns. Though perhaps we could just leave those out...
        private fun <T> toNullable(opt: Optional<T>): T {
            return opt.orElse(null)
        }
    }
}
