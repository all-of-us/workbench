package org.pmiops.workbench.actionaudit

class WorkspaceActionAuditEvent(timestamp: Long,
                                agentType: AgentType, agentId: Long,
                                agentEmailMaybe: String?,
                                actionId: String,
                                actionType: ActionType,
                                targetType: TargetType,
                                targetPropertyMaybe: String?,
                                targetIdMaybe: Long?,
                                previousValueMaybe: String?,
                                newValueMaybe: String?) : ActionAuditEvent(timestamp, agentType, agentId, agentEmailMaybe, actionId, actionType, targetType,
        targetPropertyMaybe, targetIdMaybe, previousValueMaybe, newValueMaybe)
