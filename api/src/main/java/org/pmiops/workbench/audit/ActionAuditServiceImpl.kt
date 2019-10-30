package org.pmiops.workbench.audit

import com.google.cloud.MonitoredResource
import com.google.cloud.logging.LogEntry
import com.google.cloud.logging.Logging
import com.google.cloud.logging.Payload.JsonPayload
import com.google.cloud.logging.Severity
import com.google.common.collect.ImmutableList
import java.util.Collections
import java.util.HashMap
import java.util.Optional
import java.util.logging.Level
import java.util.logging.Logger
import javax.inject.Provider
import org.pmiops.workbench.config.WorkbenchConfig
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ActionAuditServiceImpl @Autowired
constructor(private val configProvider: Provider<WorkbenchConfig>, private val cloudLogging: Logging) : ActionAuditService {

    override fun send(event: ActionAuditEvent) {
        send(setOf(event))
    }

    override fun send(events: Collection<ActionAuditEvent>) {
        try {
            val logEntries = events.stream().map<LogEntry>(Function<ActionAuditEvent, LogEntry> { this.auditEventToLogEntry(it) }).collect<ImmutableList<LogEntry>, Any>(ImmutableList.toImmutableList())
            if (!logEntries.isEmpty()) {
                cloudLogging.write(logEntries)
            }
        } catch (e: RuntimeException) {
            serviceLogger.log(
                    Level.SEVERE, e) { "Exception encountered writing log entries to Cloud Logging." }
        }

    }

    private fun auditEventToLogEntry(auditEvent: ActionAuditEvent): LogEntry {
        val actionAuditConfig = configProvider.get().actionAudit
        return LogEntry.newBuilder(toJsonPayload(auditEvent))
                .setSeverity(Severity.INFO)
                .setLogName(actionAuditConfig.logName)
                .setResource(MONITORED_RESOURCE)
                .build()
    }

    private fun toJsonPayload(auditEvent: ActionAuditEvent): JsonPayload {
        val result = HashMap<String, Any>() // allow null values
        result[AuditColumn.TIMESTAMP.name] = auditEvent.timestamp()
        result[AuditColumn.ACTION_ID.name] = auditEvent.actionId()
        result[AuditColumn.ACTION_TYPE.name] = auditEvent.actionType()
        result[AuditColumn.AGENT_TYPE.name] = auditEvent.agentType()
        result[AuditColumn.AGENT_ID.name] = auditEvent.agentId()
        result[AuditColumn.AGENT_EMAIL.name] = toNullable(auditEvent.agentEmail())
        result[AuditColumn.TARGET_TYPE.name] = auditEvent.targetType()
        result[AuditColumn.TARGET_ID.name] = toNullable(auditEvent.targetId())
        result[AuditColumn.AGENT_ID.name] = auditEvent.agentId()
        result[AuditColumn.TARGET_PROPERTY.name] = toNullable(auditEvent.targetProperty())
        result[AuditColumn.PREV_VALUE.name] = toNullable(auditEvent.previousValue())
        result[AuditColumn.NEW_VALUE.name] = toNullable(auditEvent.newValue())
        return JsonPayload.of(result)
    }

    companion object {

        private val serviceLogger = Logger.getLogger(ActionAuditServiceImpl::class.java.name)

        private val MONITORED_RESOURCE_TYPE = "Global"
        val MONITORED_RESOURCE = MonitoredResource.newBuilder(MONITORED_RESOURCE_TYPE).build()

        // Inverse of Optional.ofNullable(). Used for JSON api which expects null values for empty
        // columns. Though perhaps we could just leave those out...
        private fun <T> toNullable(opt: Optional<T>): T {
            return opt.orElse(null)
        }
    }
}
