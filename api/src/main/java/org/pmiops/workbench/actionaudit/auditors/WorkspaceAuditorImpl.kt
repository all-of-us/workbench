package org.pmiops.workbench.actionaudit.auditors

import java.time.Clock
import java.util.logging.Logger
import javax.inject.Provider
import org.pmiops.workbench.actionaudit.ActionAuditEvent
import org.pmiops.workbench.actionaudit.ActionAuditService
import org.pmiops.workbench.actionaudit.ActionType
import org.pmiops.workbench.actionaudit.AgentType
import org.pmiops.workbench.actionaudit.TargetType
import org.pmiops.workbench.actionaudit.targetproperties.AclTargetProperty
import org.pmiops.workbench.actionaudit.targetproperties.TargetPropertyExtractor
import org.pmiops.workbench.actionaudit.targetproperties.WorkspaceTargetProperty
import org.pmiops.workbench.db.model.DbUser
import org.pmiops.workbench.db.model.DbWorkspace
import org.pmiops.workbench.model.Workspace
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.util.logging.Level

@Service
class WorkspaceAuditorImpl @Autowired
constructor(
    private val userProvider: Provider<DbUser>,
    private val actionAuditService: ActionAuditService,
    private val clock: Clock,
    @Qualifier("ACTION_ID") private val actionIdProvider: Provider<String>
) : WorkspaceAuditor {

    override fun fireCreateAction(createdWorkspace: Workspace, dbWorkspaceId: Long) {
        val info = getAuditEventInfo() ?: return
        val propertyValues = TargetPropertyExtractor.getPropertyValuesByName(
                WorkspaceTargetProperty.values(), createdWorkspace)
        val events = propertyValues.entries
                .map { ActionAuditEvent(
                        actionId = info.actionId,
                        agentEmailMaybe = info.userEmail,
                        actionType = ActionType.CREATE,
                        agentType = AgentType.USER,
                        agentId = info.userId,
                        targetType = TargetType.WORKSPACE,
                        targetPropertyMaybe = it.key,
                        targetIdMaybe = dbWorkspaceId,
                        previousValueMaybe = null,
                        newValueMaybe = it.value,
                        timestamp = info.timestamp)
                }
        actionAuditService.send(events)
    }

    override fun fireEditAction(
        previousWorkspace: Workspace?,
        editedWorkspace: Workspace?,
        workspaceId: Long
    ) {
        if (previousWorkspace == null || editedWorkspace == null) {
            logger.log(Level.WARNING, "Null workspace passed to fireEditAction.")
            return
        }
        val info = getAuditEventInfo() ?: return
        val propertyToChangedValue = TargetPropertyExtractor.getChangedValuesByName(
                WorkspaceTargetProperty.values(),
                previousWorkspace,
                editedWorkspace)

        val actionAuditEvents = propertyToChangedValue.entries
                .map { ActionAuditEvent(
                        actionId = info.actionId,
                        agentEmailMaybe = info.userEmail,
                        actionType = ActionType.EDIT,
                        agentType = AgentType.USER,
                        agentId = info.userId,
                        targetType = TargetType.WORKSPACE,
                        targetPropertyMaybe = it.key,
                        targetIdMaybe = workspaceId,
                        previousValueMaybe = it.value.previousValue,
                        newValueMaybe = it.value.newValue,
                        timestamp = info.timestamp
                ) }
        actionAuditService.send(actionAuditEvents)
    }

    // Because the deleteWorkspace() method operates at the DB level, this method
    // has a different signature than for the create action. This makes keeping the properties
    // consistent across actions somewhat challenging.
    override fun fireDeleteAction(dbWorkspace: DbWorkspace) {
        val info = getAuditEventInfo() ?: return
        val event = ActionAuditEvent(
                actionId = info.actionId,
                agentEmailMaybe = info.userEmail,
                actionType = ActionType.DELETE,
                agentType = AgentType.USER,
                agentId = info.userId,
                targetType = TargetType.WORKSPACE,
                targetIdMaybe = dbWorkspace.workspaceId,
                timestamp = info.timestamp)

        actionAuditService.send(event)
    }

    override fun fireDuplicateAction(
        sourceWorkspaceId: Long,
        destinationWorkspaceId: Long,
        destinationWorkspace: Workspace
    ) {
        // We represent the duplication as a single action with events with different
        // ActionTypes: DUPLICATE_FROM and DUPLICATE_TO. The latter action generates many events
        // (similar to how CREATE is handled) for all the properties. I'm not (currently) filtering
        // out values that are the same.
        // TODO(jaycarlton): consider grabbing source event properties as well
        val info = getAuditEventInfo() ?: return
        val sourceEvent = ActionAuditEvent(
                actionId = info.actionId,
                agentEmailMaybe = info.userEmail,
                actionType = ActionType.DUPLICATE_FROM,
                agentType = AgentType.USER,
                agentId = info.userId,
                targetType = TargetType.WORKSPACE,
                targetIdMaybe = sourceWorkspaceId,
                timestamp = info.timestamp
        )
        val destinationPropertyValues = TargetPropertyExtractor.getPropertyValuesByName(
                WorkspaceTargetProperty.values(), destinationWorkspace)

        val destinationEvents: List<ActionAuditEvent> = destinationPropertyValues.entries
                .map { ActionAuditEvent(
                        actionId = info.actionId,
                        agentEmailMaybe = info.userEmail,
                        actionType = ActionType.DUPLICATE_TO,
                        agentType = AgentType.USER,
                        agentId = info.userId,
                        targetType = TargetType.WORKSPACE,
                        targetPropertyMaybe = it.key,
                        targetIdMaybe = destinationWorkspaceId,
                        newValueMaybe = it.value,
                        timestamp = info.timestamp
                        ) }

        val allEvents = listOf(listOf(sourceEvent), destinationEvents).flatten()
        actionAuditService.send(allEvents)
    }

    // We fire at least two events for this action, one with a target type of workspace,
    // and one with a target type of user for each invited or uninvited user. We'll include the
    // sharing user just so we have the whole access list at each sharing.
    override fun fireCollaborateAction(sourceWorkspaceId: Long, aclStringsByUserId: Map<Long, String>) {
        if (aclStringsByUserId.isEmpty()) {
            logger.warning("Attempted to send collaboration event with empty ACL")
            return
        }
        val info = getAuditEventInfo() ?: return
        val workspaceTargetEvent = ActionAuditEvent(
                actionId = info.actionId,
                actionType = ActionType.COLLABORATE,
                agentType = AgentType.USER,
                agentEmailMaybe = info.userEmail,
                agentId = info.userId,
                timestamp = info.timestamp,
                targetType = TargetType.WORKSPACE,
                targetIdMaybe = sourceWorkspaceId,
                targetPropertyMaybe = null,
                previousValueMaybe = null,
                newValueMaybe = null
        )

        val inviteeEvents = aclStringsByUserId.entries
                .map { ActionAuditEvent(
                        actionId = info.actionId,
                        actionType = ActionType.COLLABORATE,
                        agentType = AgentType.USER,
                        agentEmailMaybe = info.userEmail,
                        agentId = info.userId,
                        timestamp = info.timestamp,
                        targetType = TargetType.USER,
                        targetIdMaybe = it.key,
                        targetPropertyMaybe = AclTargetProperty.ACCESS_LEVEL.name,
                        newValueMaybe = it.value
                ) }
        val allEvents = listOf(listOf(workspaceTargetEvent), inviteeEvents).flatten()
        actionAuditService.send(allEvents)
    }

    private fun getAuditEventInfo(): CommonAuditEventInfo? {
        val dbUser = userProvider.get()
        return if (dbUser == null) {
            logger.info("No user available for this action.")
            null
        } else {
            CommonAuditEventInfo(
                    actionId = actionIdProvider.get(),
                    userId = dbUser.userId,
                    userEmail = dbUser.userName,
                    timestamp = clock.millis())
        }
    }

    companion object {
        private val logger = Logger.getLogger(WorkspaceAuditorImpl::class.java.name)
    }
}
