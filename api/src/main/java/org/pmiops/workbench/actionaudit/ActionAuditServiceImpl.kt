package org.pmiops.workbench.actionaudit

import com.google.cloud.MonitoredResource
import com.google.cloud.logging.LogEntry
import com.google.cloud.logging.Logging
import com.google.cloud.logging.Payload.JsonPayload
import com.google.cloud.logging.Severity
import org.pmiops.workbench.config.WorkbenchConfig
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.HashMap
import java.util.logging.Level
import java.util.logging.Logger
import javax.inject.Provider

@Service
class ActionAuditServiceImpl @Autowired
constructor(private val configProvider: Provider<WorkbenchConfig>, private val cloudLogging: Logging) : ActionAuditService {

    override fun send(events: Collection<ActionAuditEvent>) {
        try {
            val logEntries: List<LogEntry> = events
                    .map { this.auditEventToLogEntry(it) }
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
                .setResource(monitoredResource)
                .build()
    }

    private fun toJsonPayload(auditEvent: ActionAuditEvent): JsonPayload {
        // allow null values so that JSON columns are populated and BQ schema
        // doesn't have gaps that need to be added later via daily migration
        val jsonValuesByColumn = HashMap<String, Any?>()
        jsonValuesByColumn[AuditColumn.TIMESTAMP.name] = auditEvent.timestamp
        jsonValuesByColumn[AuditColumn.ACTION_ID.name] = auditEvent.actionId
        jsonValuesByColumn[AuditColumn.ACTION_TYPE.name] = auditEvent.actionType
        jsonValuesByColumn[AuditColumn.AGENT_TYPE.name] = auditEvent.agentType
        jsonValuesByColumn[AuditColumn.AGENT_ID.name] = auditEvent.agentIdMaybe
        jsonValuesByColumn[AuditColumn.AGENT_EMAIL.name] = auditEvent.agentEmailMaybe
        jsonValuesByColumn[AuditColumn.TARGET_TYPE.name] = auditEvent.targetType
        jsonValuesByColumn[AuditColumn.TARGET_ID.name] = auditEvent.targetIdMaybe
        jsonValuesByColumn[AuditColumn.AGENT_ID.name] = auditEvent.agentIdMaybe
        jsonValuesByColumn[AuditColumn.TARGET_PROPERTY.name] = auditEvent.targetPropertyMaybe
        jsonValuesByColumn[AuditColumn.PREV_VALUE.name] = auditEvent.previousValueMaybe
        jsonValuesByColumn[AuditColumn.NEW_VALUE.name] = auditEvent.newValueMaybe
        return JsonPayload.of(jsonValuesByColumn)
    }

    override fun logRuntimeException(logger: Logger, exception: RuntimeException) {
        logger.log(Level.WARNING, "Exception encountered during audit.", exception)
    }

    companion object {
        private val serviceLogger = Logger.getLogger(ActionAuditServiceImpl::class.java.name)
        private const val MONITORED_RESOURCE_TYPE = "global"
        private val monitoredResource: MonitoredResource = MonitoredResource.newBuilder(MONITORED_RESOURCE_TYPE).build()
    }
}
