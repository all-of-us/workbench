package org.pmiops.workbench.actionaudit

data class ActionAuditEvent(
    val timestamp: Long,
    val actionId: String,
    val actionType: ActionType,
    val agentType: AgentType,
    val agentId: Long,
    val agentEmailMaybe: String?,
    val targetType: TargetType,
    val targetIdMaybe: Long?,
    val targetPropertyMaybe: String?,
    val previousValueMaybe: String?,
    val newValueMaybe: String?
)
