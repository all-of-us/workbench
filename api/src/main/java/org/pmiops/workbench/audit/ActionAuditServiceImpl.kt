package org.pmiops.workbench.audit

import com.google.cloud.MonitoredResource
import com.google.cloud.logging.LogEntry
import com.google.cloud.logging.Logging
import com.google.cloud.logging.Payload.JsonPayload
import com.google.cloud.logging.Severity
import java.util.HashMap
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
            val logEntries: List<LogEntry> = events
                    .map { this.auditEventToLogEntry(it) }
                    .toList()
            if (logEntries.isNotEmpty()) {
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
        result[AuditColumn.TIMESTAMP.name] = auditEvent.timestamp
        result[AuditColumn.ACTION_ID.name] = auditEvent.actionId
        result[AuditColumn.ACTION_TYPE.name] = auditEvent.actionType
        result[AuditColumn.AGENT_TYPE.name] = auditEvent.agentType
        result[AuditColumn.AGENT_ID.name] = auditEvent.agentId
        if (auditEvent.agentEmailMaybe != null) {
            result[AuditColumn.AGENT_EMAIL.name] = auditEvent.agentEmailMaybe
        }
        result[AuditColumn.TARGET_TYPE.name] = auditEvent.targetType
        if (auditEvent.targetIdMaybe != null) {
            result[AuditColumn.TARGET_ID.name] = auditEvent.targetIdMaybe
        }

        result[AuditColumn.AGENT_ID.name] = auditEvent.agentId
        if (auditEvent.targetPropertyMaybe != null) {
            result[AuditColumn.TARGET_PROPERTY.name] = auditEvent.targetPropertyMaybe
        }

        if (auditEvent.previousValueMaybe != null) {
            result[AuditColumn.PREV_VALUE.name] = auditEvent.previousValueMaybe
        }
        if (auditEvent.newValueMaybe != null) {
            result[AuditColumn.NEW_VALUE.name] = auditEvent.newValueMaybe
        }
        return JsonPayload.of(result)
    }

    companion object {
        private val serviceLogger = Logger.getLogger(ActionAuditServiceImpl::class.java.name)
        private val MONITORED_RESOURCE_TYPE = "global"
        val MONITORED_RESOURCE = MonitoredResource.newBuilder(MONITORED_RESOURCE_TYPE).build()
    }
}
