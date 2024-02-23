package org.pmiops.workbench.actionaudit;

public record ActionAuditEvent(
    long timestamp,
    AgentType agentType,
    Long agentIdMaybe,
    String agentEmailMaybe,
    String actionId,
    ActionType actionType,
    TargetType targetType,
    String targetPropertyMaybe,
    Long targetIdMaybe,
    String previousValueMaybe,
    String newValueMaybe) {}
