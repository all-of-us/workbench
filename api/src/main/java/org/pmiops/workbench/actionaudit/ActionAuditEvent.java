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
    String newValueMaybe) {

  public static class Builder {
    private Long timestamp;
    private AgentType agentType;
    private Long agentIdMaybe;
    private String agentEmailMaybe;
    private String actionId;
    private ActionType actionType;
    private TargetType targetType;
    private String targetPropertyMaybe;
    private Long targetIdMaybe;
    private String previousValueMaybe;
    private String newValueMaybe;

    public Builder timestamp(Long timestamp) {
      this.timestamp = timestamp;
      return this;
    }

    public Builder agentType(AgentType agentType) {
      this.agentType = agentType;
      return this;
    }

    public Builder agentIdMaybe(Long agentIdMaybe) {
      this.agentIdMaybe = agentIdMaybe;
      return this;
    }

    public Builder agentEmailMaybe(String agentEmailMaybe) {
      this.agentEmailMaybe = agentEmailMaybe;
      return this;
    }

    public Builder actionId(String actionId) {
      this.actionId = actionId;
      return this;
    }

    public Builder actionType(ActionType actionType) {
      this.actionType = actionType;
      return this;
    }

    public Builder targetType(TargetType targetType) {
      this.targetType = targetType;
      return this;
    }

    public Builder targetPropertyMaybe(String targetPropertyMaybe) {
      this.targetPropertyMaybe = targetPropertyMaybe;
      return this;
    }

    public Builder targetIdMaybe(Long targetIdMaybe) {
      this.targetIdMaybe = targetIdMaybe;
      return this;
    }

    public Builder previousValueMaybe(String previousValueMaybe) {
      this.previousValueMaybe = previousValueMaybe;
      return this;
    }

    public Builder newValueMaybe(String newValueMaybe) {
      this.newValueMaybe = newValueMaybe;
      return this;
    }

    private void verifyRequiredFields() {
      if (timestamp == null
          || agentType == null
          || actionId == null
          || actionType == null
          || targetType == null) {
        throw new IllegalArgumentException("Missing required arguments.");
      }
    }

    public ActionAuditEvent build() {
      verifyRequiredFields();
      return new ActionAuditEvent(
          this.timestamp,
          this.agentType,
          this.agentIdMaybe,
          this.agentEmailMaybe,
          this.actionId,
          this.actionType,
          this.targetType,
          this.targetPropertyMaybe,
          this.targetIdMaybe,
          this.previousValueMaybe,
          this.newValueMaybe);
    }
  }
}
