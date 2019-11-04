package org.pmiops.workbench.actionaudit.adapters

import com.google.common.annotations.VisibleForTesting
import java.time.Clock
import java.util.logging.Level
import java.util.logging.Logger
import javax.inject.Provider
import org.pmiops.workbench.actionaudit.ActionAuditEvent
import org.pmiops.workbench.actionaudit.ActionAuditService
import org.pmiops.workbench.actionaudit.ActionType
import org.pmiops.workbench.actionaudit.AgentType
import org.pmiops.workbench.actionaudit.TargetType
import org.pmiops.workbench.actionaudit.targetproperties.AclTargetProperty
import org.pmiops.workbench.actionaudit.targetproperties.TargetPropertyUtils
import org.pmiops.workbench.actionaudit.targetproperties.WorkspaceTargetProperty
import org.pmiops.workbench.db.model.User
import org.pmiops.workbench.model.Workspace
import org.pmiops.workbench.workspaces.WorkspaceConversionUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@VisibleForTesting
@Service
open class WorkspaceAuditAdapterServiceImpl @Autowired
constructor(
    private val userProvider: Provider<User>,
    private val actionAuditService: ActionAuditService,
    private val clock: Clock
) : WorkspaceAuditAdapterService {

    override fun fireCreateAction(createdWorkspace: Workspace, dbWorkspaceId: Long) {
        try {
            val actionId = ActionAuditEvent.newActionId()
            val userId = userProvider.get().userId
            val userEmail = userProvider.get().email
            val propertyValues = TargetPropertyUtils.getPropertyValuesByName(
                    WorkspaceTargetProperty::values, createdWorkspace)
            val timestamp = clock.millis()
            val events = propertyValues.entries
                    .map { ActionAuditEvent(
                            timestamp = timestamp,
                            actionId = actionId,
                            actionType = ActionType.CREATE,
                            agentType = AgentType.USER,
                            agentId = userId,
                            agentEmailMaybe = userEmail,
                            targetType = TargetType.WORKSPACE,
                            targetIdMaybe = dbWorkspaceId,
                            targetPropertyMaybe = it.key,
                            previousValueMaybe = null,
                            newValueMaybe = it.value)
                    }
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
            val event = ActionAuditEvent(
                    timestamp = timestamp,
                    actionId = actionId,
                    actionType = ActionType.DELETE,
                    agentType = AgentType.USER,
                    agentId = userId,
                    agentEmailMaybe = userEmail,
                    targetType = TargetType.WORKSPACE,
                    targetIdMaybe = dbWorkspace.workspaceId,
                    targetPropertyMaybe = null,
                    previousValueMaybe = null,
                    newValueMaybe = null)

            actionAuditService.send(event)
        } catch (e: RuntimeException) {
            logAndSwallow(e)
        }
    }

    override fun fireDuplicateAction(
        sourceWorkspaceDbModel: org.pmiops.workbench.db.model.Workspace,
        destinationWorkspaceDbModel: org.pmiops.workbench.db.model.Workspace
    ) {
        try {
            // We represent the duplication as a single action with events with different
            // ActionTypes: DUPLICATE_FROM and DUPLICATE_TO. The latter action generates many events
            // (similar to how CREATE is handled) for all the properties. I'm not (currently) filtering
            // out values that are the same.
            val actionId = ActionAuditEvent.newActionId()
            val userId = userProvider.get().userId
            val userEmail = userProvider.get().email
            val timestamp = clock.millis()
            // TODO(jaycarlton): consider grabbing source event properties as well
            val sourceEvent = ActionAuditEvent(
                    timestamp = timestamp,
                    actionId = actionId,
                    actionType = ActionType.DUPLICATE_FROM,
                    agentType = AgentType.USER,
                    agentId = userId,
                    agentEmailMaybe = userEmail,
                    targetType = TargetType.WORKSPACE,
                    targetIdMaybe = sourceWorkspaceDbModel.workspaceId,
                    targetPropertyMaybe = null,
                    previousValueMaybe = null,
                    newValueMaybe = null
            )
            val destinationPropertyValues = TargetPropertyUtils.getPropertyValuesByName(
                    WorkspaceTargetProperty::values,
                    WorkspaceConversionUtils.toApiWorkspace(destinationWorkspaceDbModel))

            val destinationEvents: List<ActionAuditEvent> = destinationPropertyValues.entries
                    .map { ActionAuditEvent(
                            timestamp = timestamp,
                            actionId = actionId,
                            actionType = ActionType.DUPLICATE_TO,
                            agentType = AgentType.USER,
                            agentId = userId,
                            agentEmailMaybe = userEmail,
                            targetType = TargetType.WORKSPACE,
                            targetIdMaybe = destinationWorkspaceDbModel.workspaceId,
                            targetPropertyMaybe = it.key,
                            previousValueMaybe = null,
                            newValueMaybe = it.value) }

            val allEvents = listOf(listOf(sourceEvent), destinationEvents).flatten()
            actionAuditService.send(allEvents)
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

            val workspaceTargetEvent = ActionAuditEvent(
                    timestamp = timestamp,
                    actionId = actionId,
                    actionType = ActionType.COLLABORATE,
                    agentType = AgentType.USER,
                    agentId = sharingUserId,
                    agentEmailMaybe = userEmail,
                    targetType = TargetType.WORKSPACE,
                    targetIdMaybe = sourceWorkspaceId,
                    targetPropertyMaybe = null,
                    previousValueMaybe = null,
                    newValueMaybe = null
            )

            val inviteeEvents = aclStringsByUserId.entries
                    .map { ActionAuditEvent(
                            timestamp = timestamp,
                            actionId = actionId,
                            actionType = ActionType.COLLABORATE,
                            agentType = AgentType.USER,
                            agentId = sharingUserId,
                            agentEmailMaybe = userEmail,
                            targetType = TargetType.USER,
                            targetIdMaybe = it.key,
                            targetPropertyMaybe = AclTargetProperty.ACCESS_LEVEL.name,
                            previousValueMaybe = null,
                            newValueMaybe = it.value
                    ) }
            val allEvents = listOf(listOf(workspaceTargetEvent), inviteeEvents).flatten()
            actionAuditService.send(allEvents)
        } catch (e: RuntimeException) {
            logAndSwallow(e)
        }
    }

    companion object {
        private val logger = Logger.getLogger(WorkspaceAuditAdapterServiceImpl::class.java.name)
    }
}
