package org.pmiops.workbench.audit

import com.google.appengine.repackaged.com.google.common.collect.ImmutableMap
import com.google.cloud.MonitoredResource
import com.google.cloud.logging.Payload
import java.util.UUID.randomUUID

class ActionAuditEvent(
        val timestamp: Long,
        val agentType: AgentType,
        val agentId: Long,
        val agentEmailMaybe: String?,
        val actionId: String,
        val actionType: ActionType,
        val targetType: TargetType,
        val targetPropertyMaybe: String? = null,
        val targetIdMaybe: Long? = null,
        val previousValueMaybe: String? = null,
        val newValueMaybe: String? = null) {

    val jsonPayload: Payload.JsonPayload
        get() {
            val resultBuilder = ImmutableMap.builder<String, Any>()
            resultBuilder.put(AuditColumn.TIMESTAMP.name, timestamp)
            resultBuilder.put(AuditColumn.ACTION_ID.name, actionId)
            resultBuilder.put(AuditColumn.ACTION_TYPE.name, actionType)
            resultBuilder.put(AuditColumn.AGENT_TYPE.name, agentType)
            resultBuilder.put(AuditColumn.AGENT_ID.name, agentId)
            agentEmailMaybe ?: resultBuilder.put(AuditColumn.AGENT_EMAIL.name, agentEmailMaybe!!)
            resultBuilder.put(AuditColumn.TARGET_TYPE.name, targetType)
            targetIdMaybe ?: resultBuilder.put(AuditColumn.TARGET_ID.name, targetIdMaybe!!)
            resultBuilder.put(AuditColumn.AGENT_ID.name, agentId)
            targetPropertyMaybe ?: resultBuilder.put(AuditColumn.TARGET_PROPERTY.name, targetPropertyMaybe!!)
            previousValueMaybe ?: resultBuilder.put(AuditColumn.PREV_VALUE.name, previousValueMaybe!!)
            newValueMaybe ?: resultBuilder.put(AuditColumn.NEW_VALUE.name, newValueMaybe!!)
            return Payload.JsonPayload.of(resultBuilder.build())
        }

    companion object {
        private val MONITORED_RESOURCE = MonitoredResource.newBuilder("global").build()
        fun newActionId(): String = randomUUID().toString()
    }
}
