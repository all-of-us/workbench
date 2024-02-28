package org.pmiops.workbench.actionaudit;

import com.google.cloud.MonitoredResource;
import com.google.cloud.logging.LogEntry;
import com.google.cloud.logging.Logging;
import com.google.cloud.logging.Payload.JsonPayload;
import com.google.cloud.logging.Severity;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ActionAuditServiceImpl implements ActionAuditService {
  private static final String MONITORED_RESOURCE_TYPE = "global";
  private static final MonitoredResource MONITORED_RESOURCE =
      MonitoredResource.newBuilder(MONITORED_RESOURCE_TYPE).build();

  private static final Logger LOG = Logger.getLogger(ActionAuditServiceImpl.class.getName());

  private final Provider<WorkbenchConfig> configProvider;
  private final Logging cloudLogging;

  @Autowired
  public ActionAuditServiceImpl(Provider<WorkbenchConfig> configProvider, Logging cloudLogging) {
    this.configProvider = configProvider;
    this.cloudLogging = cloudLogging;
  }

  @Override
  public void send(Collection<ActionAuditEvent> events) {
    try {
      List<LogEntry> logEntries = events.stream().map(this::auditEventToLogEntry).toList();
      if (!logEntries.isEmpty()) {
        cloudLogging.write(logEntries);
      }
    } catch (RuntimeException e) {
      LOG.log(Level.SEVERE, "Exception encountered writing log entries to Cloud Logging.", e);
    }
  }

  private LogEntry auditEventToLogEntry(ActionAuditEvent auditEvent) {
    WorkbenchConfig.ActionAuditConfig actionAuditConfig = configProvider.get().actionAudit;
    return LogEntry.newBuilder(toJsonPayload(auditEvent))
        .setSeverity(Severity.INFO)
        .setLogName(actionAuditConfig.logName)
        .setResource(MONITORED_RESOURCE)
        .build();
  }

  private JsonPayload toJsonPayload(ActionAuditEvent auditEvent) {
    HashMap<String, Object> jsonValuesByColumn = new HashMap<>();
    jsonValuesByColumn.put(AuditColumn.TIMESTAMP.name(), auditEvent.timestamp());
    jsonValuesByColumn.put(AuditColumn.ACTION_ID.name(), auditEvent.actionId());
    jsonValuesByColumn.put(AuditColumn.ACTION_TYPE.name(), auditEvent.actionType());
    jsonValuesByColumn.put(AuditColumn.AGENT_TYPE.name(), auditEvent.agentType());
    jsonValuesByColumn.put(AuditColumn.AGENT_ID.name(), auditEvent.agentIdMaybe());
    jsonValuesByColumn.put(AuditColumn.AGENT_EMAIL.name(), auditEvent.agentEmailMaybe());
    jsonValuesByColumn.put(AuditColumn.TARGET_TYPE.name(), auditEvent.targetType());
    jsonValuesByColumn.put(AuditColumn.TARGET_ID.name(), auditEvent.targetIdMaybe());
    jsonValuesByColumn.put(AuditColumn.TARGET_PROPERTY.name(), auditEvent.targetPropertyMaybe());
    jsonValuesByColumn.put(AuditColumn.PREV_VALUE.name(), auditEvent.previousValueMaybe());
    jsonValuesByColumn.put(AuditColumn.NEW_VALUE.name(), auditEvent.newValueMaybe());
    return JsonPayload.of(jsonValuesByColumn);
  }
}
