package org.pmiops.workbench.audit;

import java.util.Optional;

public class ActionAuditEventImpl implements ActionAuditEvent {

  private final long timestamp;
  private final AgentType agentType;
  private final long agentId;
  private final String actionId;
  private Optional<String> agentEmail;
  private final ActionType actionType;
  private final TargetType targetType;
  private final Optional<String> targetPropertyMaybe;
  private Optional<Long> targetIdMaybe;
  private final Optional<String> previousValueMaybe;
  private final Optional<String> newValueMaybe;

  public ActionAuditEventImpl(
      long timestamp,
      AgentType agentType,
      long agentId,
      Optional<String> agentEmail,
      String actionId,
      ActionType actionType,
      TargetType targetType,
      Optional<String> targetPropertyMaybe,
      Optional<Long> targetIdMaybe,
      Optional<String> previousValueMaybe,
      Optional<String> newValueMaybe) {
    this.timestamp = timestamp;
    this.agentType = agentType;
    this.agentId = agentId;
    this.agentEmail = agentEmail;
    this.actionId = actionId;
    this.actionType = actionType;
    this.targetType = targetType;
    this.targetPropertyMaybe = targetPropertyMaybe;
    this.targetIdMaybe = targetIdMaybe;
    this.previousValueMaybe = previousValueMaybe;
    this.newValueMaybe = newValueMaybe;
  }

  @Override
  public long timestamp() {
    return timestamp;
  }

  @Override
  public AgentType agentType() {
    return agentType;
  }

  @Override
  public long agentId() {
    return agentId;
  }

  @Override
  public Optional<String> agentEmail() {
    return agentEmail;
  }

  @Override
  public String actionId() {
    return actionId;
  }

  @Override
  public ActionType actionType() {
    return actionType;
  }

  @Override
  public TargetType targetType() {
    return targetType;
  }

  @Override
  public Optional<Long> targetId() {
    return targetIdMaybe; // check if this is null somehow
  }

  @Override
  public Optional<String> targetProperty() {
    return targetPropertyMaybe;
  }

  @Override
  public Optional<String> previousValue() {
    return previousValueMaybe;
  }

  @Override
  public Optional<String> newValue() {
    return newValueMaybe;
  }

  public static Builder builder() {
    return new Builder();
  }
  // This builder handles null inputs for optional fields, simplifying code at the call site.
  // For example setAgentEmail() can be called safely with null (or just not called it's not known).
  public static class Builder {

    private long timestamp;
    private AgentType agentType;
    private long agentId;
    private String agentEmail;
    private String actionId;
    private ActionType actionType;
    private TargetType targetType;
    private String targetProperty;
    private Long targetId;
    private String previousValue;
    private String newValue;

    public Builder() {}

    public Builder setTimestamp(long timestamp) {
      this.timestamp = timestamp;
      return this;
    }

    public Builder setAgentType(AgentType agentType) {
      this.agentType = agentType;
      return this;
    }

    public Builder setAgentId(long agentId) {
      this.agentId = agentId;
      return this;
    }

    public Builder setAgentEmail(String agentEmail) {
      this.agentEmail = agentEmail;
      return this;
    }

    public Builder setActionId(String actionId) {
      this.actionId = actionId;
      return this;
    }

    public Builder setActionType(ActionType actionType) {
      this.actionType = actionType;
      return this;
    }

    public Builder setTargetType(TargetType targetType) {
      this.targetType = targetType;
      return this;
    }

    public Builder setTargetProperty(String targetProperty) {
      this.targetProperty = targetProperty;
      return this;
    }

    public Builder setTargetId(Long targetId) {
      this.targetId = targetId;
      return this;
    }

    public Builder setPreviousValue(String previousValue) {
      this.previousValue = previousValue;
      return this;
    }

    public Builder setNewValue(String newValue) {
      this.newValue = newValue;
      return this;
    }

    public ActionAuditEventImpl build() {
      return new ActionAuditEventImpl(
          timestamp,
          agentType,
          agentId,
          Optional.ofNullable(agentEmail),
          actionId,
          actionType,
          targetType,
          Optional.ofNullable(targetProperty),
          Optional.ofNullable(targetId),
          Optional.ofNullable(previousValue),
          Optional.ofNullable(newValue));
    }
  }
}
