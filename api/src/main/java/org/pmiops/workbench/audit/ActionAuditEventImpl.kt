package org.pmiops.workbench.audit

import java.util.Optional
import org.elasticsearch.common.Strings

// This value class presents an Immutables-style builder
// and access pattern. All consturction must happen via the
// builder, and any missing required fields will generate an
// IllegalArgumentException. There are no public mutators on the fields.
class ActionAuditEventImpl private constructor(
        private val timestamp: Long,
        private val agentType: AgentType,
        private val agentId: Long,
        private val agentEmailMaybe: Optional<String>,
        private val actionId: String,
        private val actionType: ActionType,
        private val targetType: TargetType,
        private val targetPropertyMaybe: Optional<String>,
        private val targetIdMaybe: Optional<Long>,
        private val previousValueMaybe: Optional<String>,
        private val newValueMaybe: Optional<String>) : ActionAuditEvent {

    override fun timestamp(): Long {
        return timestamp
    }

    override fun agentType(): AgentType {
        return agentType
    }

    override fun agentId(): Long {
        return agentId
    }

    override fun agentEmail(): Optional<String> {
        return agentEmailMaybe
    }

    override fun actionId(): String {
        return actionId
    }

    override fun actionType(): ActionType {
        return actionType
    }

    override fun targetType(): TargetType {
        return targetType
    }

    override fun targetId(): Optional<Long> {
        return targetIdMaybe // check if this is null somehow
    }

    override fun targetProperty(): Optional<String> {
        return targetPropertyMaybe
    }

    override fun previousValue(): Optional<String> {
        return previousValueMaybe
    }

    override fun newValue(): Optional<String> {
        return newValueMaybe
    }

    // This builder handles null inputs for optional fields, simplifying code at the call site.
    // For example setAgentEmail() can be called safely with null (or just not called it's not known).
    class Builder {

        private var timestamp: Long = 0
        private var agentType: AgentType? = null
        private var agentId: Long = 0
        private var agentEmail: String? = null
        private var actionId: String? = null
        private var actionType: ActionType? = null
        private var targetType: TargetType? = null
        private var targetProperty: String? = null
        private var targetId: Long? = null
        private var previousValue: String? = null
        private var newValue: String? = null

        fun setTimestamp(timestamp: Long): Builder {
            this.timestamp = timestamp
            return this
        }

        fun setAgentType(agentType: AgentType): Builder {
            this.agentType = agentType
            return this
        }

        fun setAgentId(agentId: Long): Builder {
            this.agentId = agentId
            return this
        }

        fun setAgentEmail(agentEmail: String): Builder {
            this.agentEmail = agentEmail
            return this
        }

        fun setActionId(actionId: String): Builder {
            this.actionId = actionId
            return this
        }

        fun setActionType(actionType: ActionType): Builder {
            this.actionType = actionType
            return this
        }

        fun setTargetType(targetType: TargetType): Builder {
            this.targetType = targetType
            return this
        }

        fun setTargetProperty(targetProperty: String): Builder {
            this.targetProperty = targetProperty
            return this
        }

        fun setTargetId(targetId: Long?): Builder {
            this.targetId = targetId
            return this
        }

        fun setPreviousValue(previousValue: String): Builder {
            this.previousValue = previousValue
            return this
        }

        fun setNewValue(newValue: String): Builder {
            this.newValue = newValue
            return this
        }

        @Throws(IllegalArgumentException::class)
        private fun validateRequiredFields() {
            require(timestamp > 0L) { "Missing required field timestamp" }
            requireNotNull(agentType) { "Missing required field agentType" }
        }

        fun build(): ActionAuditEventImpl {
            validateRequiredFields()
            return ActionAuditEventImpl(
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
                    Optional.ofNullable(newValue))
        }
    }

    companion object {

        fun builder(): Builder {
            return Builder()
        }
    }
}
