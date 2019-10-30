package org.pmiops.workbench.audit.adapters

import com.google.common.collect.ImmutableList
import java.time.Clock
import java.util.logging.Level
import java.util.logging.Logger
import javax.inject.Provider
import org.pmiops.workbench.audit.ActionAuditEvent
import org.pmiops.workbench.audit.ActionAuditEventImpl
import org.pmiops.workbench.audit.ActionAuditService
import org.pmiops.workbench.audit.ActionType
import org.pmiops.workbench.audit.AgentType
import org.pmiops.workbench.audit.TargetType
import org.pmiops.workbench.audit.targetproperties.AclTargetProperty
import org.pmiops.workbench.audit.targetproperties.WorkspaceTargetProperty
import org.pmiops.workbench.db.model.User
import org.pmiops.workbench.model.Workspace
import org.pmiops.workbench.workspaces.WorkspaceConversionUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class WorkspaceAuditAdapterServiceImpl @Autowired
constructor(
        private val userProvider: Provider<User>, private val actionAuditService: ActionAuditService, private val clock: Clock) : WorkspaceAuditAdapterService {

    override fun fireCreateAction(createdWorkspace: Workspace, dbWorkspaceId: Long) {
        try {
            val actionId = ActionAuditEvent.newActionId()
            val userId = userProvider.get().userId
            val userEmail = userProvider.get().email
            val propertyValues = WorkspaceTargetProperty.getPropertyValuesByName(createdWorkspace)
            val timestamp = clock.millis()
            // omit the previous value column
            val events = propertyValues.entries.stream()
                    .map { entry -> ActionAuditEvent(
                            actionId = actionId,
                            agentEmailMaybe = userEmail,
                            actionType = ActionType.CREATE,
                            agentType = AgentType.USER,
                            agentId = userId,
                            targetType = TargetType.WORKSPACE,
                            targetPropertyMaybe = entry.key,
                            targetIdMaybe = dbWorkspaceId,
                            previousValueMaybe = null,
                            newValueMaybe = entry.value,
                            timestamp = timestamp)
                    }
                    .collect<ImmutableList<ActionAuditEvent>, Any>(ImmutableList.toImmutableList())
            actionAuditService.send(events)
        } catch (e: RuntimeException) {
            logAndSwallow(e)
        }

    }

    private fun logAndSwallow(e: RuntimeException) {
        logger.log(Level.WARNING, e) { "Exception encountered during audit." }
    }

    // Because the deleteWorkspace() method operates at the DB level, this method
    // has a different signature than for the create action. This makes keeping the properties
    // consistent across actions somewhat challenging.
    override fun fireDeleteAction(dbWorkspace: org.pmiops.workbench.db.model.Workspace) {
        try {
            val actionId = ActionAuditEvent.newActionId()
            val userId = userProvider.get().userId
            val userEmail = userProvider.get().email
            val timestamp = clock.millis()
            val event = ActionAuditEventImpl.Builder()
                    .setActionId(actionId)
                    .setAgentEmail(userEmail)
                    .setActionType(ActionType.DELETE)
                    .setAgentType(AgentType.USER)
                    .setAgentId(userId)
                    .setTargetType(TargetType.WORKSPACE)
                    .setTargetId(dbWorkspace.workspaceId)
                    .setTimestamp(timestamp)
                    .build()
            actionAuditService.send(event)
        } catch (e: RuntimeException) {
            logAndSwallow(e)
        }

    }

    override fun fireDuplicateAction(
            sourceWorkspaceDbModel: org.pmiops.workbench.db.model.Workspace,
            destinationWorkspaceDbModel: org.pmiops.workbench.db.model.Workspace) {
        try {
            // We represent the duplication as a single action with events with different
            // ActionTypes: DUPLICATE_FROM and DUPLICATE_TO. The latter action generates many events
            // (similar to how CREATE is handled) for all the properties. I'm not (currently) filtering
            // out values that are the same.
            val actionId = ActionAuditEvent.newActionId()
            val userId = userProvider.get().userId
            val userEmail = userProvider.get().email
            val timestamp = clock.millis()
            val sourceEvent = ActionAuditEventImpl.builder()
                    .setActionId(actionId)
                    .setAgentEmail(userEmail)
                    .setActionType(ActionType.DUPLICATE_FROM)
                    .setAgentType(AgentType.USER)
                    .setAgentId(userId)
                    .setTargetType(TargetType.WORKSPACE)
                    .setTargetId(sourceWorkspaceDbModel.workspaceId)
                    .setTimestamp(timestamp)
                    .build()
            val propertyValues = WorkspaceTargetProperty.getPropertyValuesByName(
                    WorkspaceConversionUtils.toApiWorkspace(destinationWorkspaceDbModel))

            val destinationEvents = propertyValues.entries.stream()
                    .map { entry ->
                        ActionAuditEventImpl.builder()
                                .setActionId(actionId)
                                .setAgentEmail(userEmail)
                                .setActionType(ActionType.DUPLICATE_TO)
                                .setAgentType(AgentType.USER)
                                .setAgentId(userId)
                                .setTargetType(TargetType.WORKSPACE)
                                .setTargetProperty(entry.key)
                                .setTargetId(destinationWorkspaceDbModel.workspaceId)
                                .setNewValue(entry.value)
                                .setTimestamp(timestamp)
                                .build()
                    }
                    .collect<ImmutableList<ActionAuditEvent>, Any>(ImmutableList.toImmutableList())

            val allEventsBuilder = ImmutableList.Builder<ActionAuditEvent>()
            allEventsBuilder.add(sourceEvent)
            allEventsBuilder.addAll(destinationEvents)
            actionAuditService.send(allEventsBuilder.build())
        } catch (e: RuntimeException) {
            logAndSwallow(e)
        }

    }

    // We fire at least two events for this action, one with a target type of workspace,
    // and one with a target type of user for each invited or uninvited user. We'll include the
    // sharing user just so we have the whole access list at each sharing.
    override fun fireCollaborateAction(sourceWorkspaceId: Long, aclStringsByUserId: Map<Long, String>) {
        try {
            if (aclStringsByUserId.isEmpty()) {
                logger.warning("Attempted to send collaboration event with empty ACL")
                return
            }

            val actionId = ActionAuditEvent.newActionId()
            val sharingUserId = userProvider.get().userId
            val userEmail = userProvider.get().email
            val timestamp = clock.millis()

            val workspaceTargetEvent = ActionAuditEventImpl.builder()
                    .setActionId(actionId)
                    .setActionType(ActionType.COLLABORATE)
                    .setAgentType(AgentType.USER)
                    .setAgentEmail(userEmail)
                    .setAgentId(sharingUserId)
                    .setTimestamp(timestamp)
                    .setTargetType(TargetType.WORKSPACE)
                    .setTargetId(sourceWorkspaceId)
                    .build()

            val inviteeEvents = aclStringsByUserId.entries.stream()
                    .map { entry ->
                        ActionAuditEventImpl.builder()
                                .setActionId(actionId)
                                .setActionType(ActionType.COLLABORATE)
                                .setAgentType(AgentType.USER)
                                .setAgentEmail(userEmail)
                                .setAgentId(sharingUserId)
                                .setTimestamp(timestamp)
                                .setTargetType(TargetType.USER)
                                .setTargetId(entry.key)
                                .setTargetProperty(AclTargetProperty.ACCESS_LEVEL.name)
                                .setNewValue(entry.value)
                                .build()
                    }
                    .collect<ImmutableList<ActionAuditEvent>, Any>(ImmutableList.toImmutableList())

            val allEventsBuilder = ImmutableList.Builder<ActionAuditEvent>()
            allEventsBuilder.add(workspaceTargetEvent)
            allEventsBuilder.addAll(inviteeEvents)

            actionAuditService.send(allEventsBuilder.build())
        } catch (e: RuntimeException) {
            logAndSwallow(e)
        }

    }

    companion object {

        private val logger = Logger.getLogger(WorkspaceAuditAdapterServiceImpl::class.java.name)
    }
}
