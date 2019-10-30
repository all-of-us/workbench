package org.pmiops.workbench.audit;

import java.util.Optional;
import java.util.UUID;

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

  static String newActionId() {
    return UUID.randomUUID().toString();
  }
}
