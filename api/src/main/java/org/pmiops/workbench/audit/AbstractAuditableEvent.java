package org.pmiops.workbench.audit;

import static org.pmiops.workbench.api.StatusController.LOGGING_RESOURCE;

import com.google.cloud.logging.LogEntry;
import com.google.cloud.logging.Payload.JsonPayload;
import com.google.cloud.logging.Severity;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public abstract class AbstractAuditableEvent {

  private static final String LOG_NAME = "action-audit-test-2";

  public abstract long timestamp();

  public abstract String actionId();

  public abstract AgentType agentType();

  public abstract long agentId();

  public abstract Optional<String> agentEmail();

  public abstract ActionType actionType();

  public abstract TargetType targetType();

  public abstract Optional<Long> targetId();

  public abstract Optional<String> targetProperty();

  public abstract Optional<String> previousValue();

  public abstract Optional<String> newValue();

  private JsonPayload toJsonPayload() {
    Map<String, Object> result = new HashMap<>();
    result.put(AuditColumn.TIMESTAMP.name(), timestamp());
    result.put(AuditColumn.ACTION_ID.name(), actionId());
    result.put(AuditColumn.ACTION_TYPE.name(), actionType());
    result.put(AuditColumn.AGENT_TYPE.name(), agentType());
    result.put(AuditColumn.AGENT_ID.name(), agentId());
    result.put(AuditColumn.AGENT_EMAIL.name(), agentEmail().orElse(null));
    result.put(AuditColumn.TARGET_TYPE.name(), targetType());
    result.put(AuditColumn.TARGET_ID.name(), targetId().orElse(null));
    result.put(AuditColumn.AGENT_ID.name(), targetId().orElse(null));
    result.put(AuditColumn.TARGET_PROPERTY.name(), targetProperty().orElse(null));
    result.put(AuditColumn.PREV_VALUE.name(), previousValue().orElse(null));
    result.put(AuditColumn.NEW_VALUE.name(), newValue().orElse(null));
    return JsonPayload.of(result);
  }

  public LogEntry toLogEntry() {
    return LogEntry.newBuilder(toJsonPayload())
        .setSeverity(Severity.INFO)
        .setLogName(LOG_NAME)
        .setResource(LOGGING_RESOURCE)
        .build();
  }
}
