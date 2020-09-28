package org.pmiops.workbench.actionaudit

data class ActionAuditEvent constructor(
    val timestamp: Long,
    val agentType: AgentType,
    val agentIdMaybe: Long?,
    val agentEmailMaybe: String?,
    val actionId: String,
    val actionType: ActionType,
    val targetType: TargetType,
    val targetPropertyMaybe: String? = null,
    val targetIdMaybe: Long? = null,
    val previousValueMaybe: String? = null,
    val newValueMaybe: String? = null
) {
    /**
     * Since Java code can't take advantage of the named constructor parameters, we provide
     * a traditional Java-style builder.
     */
    data class Builder internal constructor(
        var timestamp: Long? = null,
        var agentType: AgentType? = null,
        var agentIdMaybe: Long? = null,
        var agentEmailMaybe: String? = null,
        var actionId: String? = null,
        var actionType: ActionType? = null,
        var targetType: TargetType? = null,
        var targetPropertyMaybe: String? = null,
        var targetIdMaybe: Long? = null,
        var previousValueMaybe: String? = null,
        var newValueMaybe: String? = null
    ) {
        fun timestamp(timestamp: Long) = apply { this.timestamp = timestamp }
        fun agentType(agentType: AgentType) = apply { this.agentType = agentType }
        fun agentIdMaybe(agentIdMaybe: Long?) = apply { this.agentIdMaybe = agentIdMaybe }
        fun agentEmailMaybe(agentEmailMaybe: String?) = apply { this.agentEmailMaybe = agentEmailMaybe }
        fun actionId(actionId: String) = apply { this.actionId = actionId }
        fun actionType(actionType: ActionType) = apply { this.actionType = actionType }
        fun targetType(targetType: TargetType) = apply { this.targetType = targetType }
        fun targetPropertyMaybe(targetPropertyMaybe: String?) = apply { this.targetPropertyMaybe = targetPropertyMaybe }
        fun targetIdMaybe(targetIdMaybe: Long?) = apply { this.targetIdMaybe = targetIdMaybe }
        fun previousValueMaybe(previousValueMaybe: String?) = apply { this.previousValueMaybe = previousValueMaybe }
        fun newValueMaybe(newValueMaybe: String?) = apply { this.newValueMaybe = newValueMaybe }

        private fun verifyRequiredFields() {
            if (timestamp == null ||
                    agentType == null ||
                    actionId == null ||
                    actionType == null ||
                    targetType == null) {
                throw IllegalArgumentException("Missing required arguments.")
            }
        }

        fun build(): ActionAuditEvent {
            verifyRequiredFields()
            return ActionAuditEvent(
                    timestamp = this.timestamp!!,
                    agentType = agentType!!,
                    agentIdMaybe = agentIdMaybe,
                    agentEmailMaybe = agentEmailMaybe,
                    actionId = actionId!!,
                    actionType = actionType!!,
                    targetType = targetType!!,
                    targetPropertyMaybe = targetPropertyMaybe,
                    targetIdMaybe = targetIdMaybe,
                    previousValueMaybe = previousValueMaybe,
                    newValueMaybe = newValueMaybe)
        }
    }

    companion object {
        @JvmStatic
        fun builder(): Builder {
            return Builder()
        }
    }
}
