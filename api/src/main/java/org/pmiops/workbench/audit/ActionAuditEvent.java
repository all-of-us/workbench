package org.pmiops.workbench.audit;

import com.google.cloud.MonitoredResource;
import com.google.cloud.logging.LogEntry;
import com.google.cloud.logging.Payload.JsonPayload;
import com.google.cloud.logging.Severity;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public interface ActionAuditEvent {

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

}
