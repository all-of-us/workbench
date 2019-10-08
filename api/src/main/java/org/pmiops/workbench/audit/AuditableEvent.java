package org.pmiops.workbench.audit;

import java.util.Optional;

/** temporary "immutable" class until i cna get immutables to work in gradle */
public class AuditableEvent extends AbstractAuditableEvent {

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

  public AuditableEvent(
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

  // todo: replace with smarter builder implementation. Explicitly set optionals to
  // Optional.empty();
  public static class Builder {

    private long timestamp;
    private AgentType agentType;
    private long agentId;
    private Optional<String> agentEmailMaybe;
    private String actionId;
    private ActionType actionType;
    private TargetType targetType;
    private Optional<String> targetPropertyMaybe;
    private Optional<Long> targetIdMaybe;
    private Optional<String> previousValueMaybe;
    private Optional<String> newValueMaybe;

    public Builder() {
      agentEmailMaybe = Optional.empty();
      targetPropertyMaybe = Optional.empty();
      targetIdMaybe = Optional.empty();
      previousValueMaybe = Optional.empty();
      newValueMaybe = Optional.empty();
    }

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

    public Builder setAgentEmailMaybe(Optional<String> agentEmailMaybe) {
      this.agentEmailMaybe = agentEmailMaybe;
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

    public Builder setTargetPropertyMaybe(Optional<String> targetPropertyMaybe) {
      this.targetPropertyMaybe = targetPropertyMaybe;
      return this;
    }

    public Builder setTargetIdMaybe(Optional<Long> targetIdMaybe) {
      this.targetIdMaybe = targetIdMaybe;
      return this;
    }

    public Builder setPreviousValueMaybe(Optional<String> previousValueMaybe) {
      this.previousValueMaybe = previousValueMaybe;
      return this;
    }

    public Builder setNewValueMaybe(Optional<String> newValueMaybe) {
      this.newValueMaybe = newValueMaybe;
      return this;
    }

    public AuditableEvent build() {
      return new AuditableEvent(
          timestamp,
          agentType,
          agentId,
          nullToOptional(agentEmailMaybe),
          actionId,
          actionType,
          targetType,
          nullToOptional(targetPropertyMaybe),
          nullToOptional(targetIdMaybe),
          nullToOptional(previousValueMaybe),
          nullToOptional(newValueMaybe));
    }

    // Since this class doesn't have a constructor and we're storing the fields as Optional,
    // if the values aren't set, we can run into NPEs.
    private <T> Optional<T> nullToOptional(Optional<T> valueMaybe) {
      if (valueMaybe == null) {
        return Optional.empty();
      } else {
        return valueMaybe;
      }
    }
  }
}
