package org.pmiops.workbench.audit;

import com.google.cloud.MonitoredResource;
import com.google.cloud.logging.LogEntry;
import com.google.cloud.logging.Logging;
import com.google.cloud.logging.LoggingOptions;
import com.google.cloud.logging.Payload.JsonPayload;
import com.google.cloud.logging.Severity;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ActionAuditServiceImpl implements ActionAuditService {
  private final String logName;
  private static final Logger serviceLogger =
      Logger.getLogger(ActionAuditServiceImpl.class.getName());

  private final Logging cloudLogging;
  private MonitoredResource monitoredResource;

  @Autowired
  public ActionAuditServiceImpl(
      Provider<WorkbenchConfig> configProvider) {
    this.cloudLogging = LoggingOptions.getDefaultInstance().getService();
    this.monitoredResource = MonitoredResource.newBuilder(
        configProvider.get().actionAudit.monitoredResourceName)
    .build();
    this.logName = configProvider.get().actionAudit.logName;
  }

  @Override
  public void send(ActionAuditEvent event) {
    send(Collections.singleton(event));
  }

  @Override
  public void send(Collection<ActionAuditEvent> events) {
    try {
      ImmutableList<LogEntry> logEntries =
          events.stream().map(this::auditEventToLogEntry).collect(ImmutableList.toImmutableList());
      cloudLogging.write(logEntries);
    } catch (RuntimeException e) {
      serviceLogger.log(
          Level.SEVERE, e, () -> "Exception encountered writing log entries to Cloud Logging.");
    }
  }

  private LogEntry auditEventToLogEntry(ActionAuditEvent auditEvent) {
    return LogEntry.newBuilder(toJsonPayload(auditEvent))
        .setSeverity(Severity.INFO)
        .setLogName(logName)
        .setResource(monitoredResource)
        .build();
  }

  private JsonPayload toJsonPayload(ActionAuditEvent auditEvent) {
    Map<String, Object> result = new HashMap<>(); // allow null values
    result.put(AuditColumn.TIMESTAMP.name(), auditEvent.timestamp());
    result.put(AuditColumn.ACTION_ID.name(), auditEvent.actionId());
    result.put(AuditColumn.ACTION_TYPE.name(), auditEvent.actionType());
    result.put(AuditColumn.AGENT_TYPE.name(), auditEvent.agentType());
    result.put(AuditColumn.AGENT_ID.name(), auditEvent.agentId());
    result.put(AuditColumn.AGENT_EMAIL.name(), toNullable(auditEvent.agentEmail()));
    result.put(AuditColumn.TARGET_TYPE.name(), auditEvent.targetType());
    result.put(AuditColumn.TARGET_ID.name(), toNullable(auditEvent.targetId()));
    result.put(AuditColumn.AGENT_ID.name(), auditEvent.agentId());
    result.put(AuditColumn.TARGET_PROPERTY.name(), toNullable(auditEvent.targetProperty()));
    result.put(AuditColumn.PREV_VALUE.name(), toNullable(auditEvent.previousValue()));
    result.put(AuditColumn.NEW_VALUE.name(), toNullable(auditEvent.newValue()));
    return JsonPayload.of(result);
  }

  // Inverse of Optional.ofNullable(). Used for JSON api which expects null values for empty
  // columns. Though perhaps we could just leave those out...
  private static <T> T toNullable(Optional<T> opt) {
    return opt.orElse(null);
  }
}
