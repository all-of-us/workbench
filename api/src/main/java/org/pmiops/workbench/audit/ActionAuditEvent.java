package org.pmiops.workbench.audit;

import java.util.Optional;


public interface ActionAuditEvent {
  long timestamp();
  String actionId();
  AgentType agentType();
  long agentId();
  Optional<String> agentEmail();
  ActionType actionType();
  TargetType targetType();
  Optional<Long> targetId();
  Optional<String> targetProperty();
  Optional<String> previousValue();
  Optional<String> newValue();
}
