package org.pmiops.workbench.audit;

import java.sql.Timestamp;
import java.util.Optional;

/** temporary "immutable" class until i cna get immutables to work in gradle
 *
 */
public class AuditableEvent extends AbstractAuditableEvent {

  private final Timestamp timestamp;
  private final AgentType agentType;
  private final long agentId;
  private Optional<String> agentEmail;
  private final ActionType actionType;
  private final TargetType targetType;
  private final Optional<String> targetPropertyMaybe;
  private Optional<Long> targetIdMaybe;
  private final Optional<String> previousValueMaybe;
  private final Optional<String> newValueMaybe;

  public AuditableEvent(Timestamp timestamp, AgentType agentType, long agentId, Optional<String> agentEmail,
      ActionType actionType, TargetType targetType, Optional<String> targetPropertyMaybe, Optional<Long> targetIdMaybe,
      Optional<String> previousValueMaybe, Optional<String> newValueMaybe) {
    this.timestamp = timestamp;
    this.agentType = agentType;
    this.agentId = agentId;
    this.agentEmail = agentEmail;
    this.actionType = actionType;
    this.targetType = targetType;
    this.targetPropertyMaybe = targetPropertyMaybe;
    this.targetIdMaybe = targetIdMaybe;
    this.previousValueMaybe = previousValueMaybe;
    this.newValueMaybe = newValueMaybe;
  }

  @Override
  public Timestamp timestamp() {
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
  public ActionType actionType() {
    return actionType;
  }

  @Override
  public TargetType targetType() {
    return targetType;
  }

  @Override
  public Optional<Long> targetId() {
    return targetIdMaybe;
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
    return previousValueMaybe;
  }
}
