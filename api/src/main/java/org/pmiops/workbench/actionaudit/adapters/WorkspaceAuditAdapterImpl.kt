package org.pmiops.workbench.actionaudit.adapters

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
import org.pmiops.workbench.workspaces.WorkspaceConversionUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

@Service
class WorkspaceAuditAdapterImpl @Autowired
constructor(
    private val userProvider: Provider<DbUser>,
    private val actionAuditService: ActionAuditService,
    private val clock: Clock,
    @Qualifier("ACTION_ID") private val actionIdProvider: Provider<String>
) : WorkspaceAuditAdapter {

    override fun fireCreateAction(createdWorkspace: Workspace, dbWorkspaceId: Long) {
        try {
            val info = getAuditEventInfo()
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
        } catch (e: RuntimeException) {
            logAndSwallow(e)
        }
    }

    override fun fireEditAction(
            previousWorkspace: Workspace,
            editedWorkspace: Workspace,
            workspaceId: Long) {
        try {
            val info = getAuditEventInfo()
            val propertyToChangedValue = TargetPropertyExtractor.getChangedValuesByName(
                    WorkspaceTargetProperty.values(),
                    previousWorkspace,
                    editedWorkspace)

            propertyToChangedValue.entries
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
                    )}
        } catch (e: RuntimeException) {
            logAndSwallow(e)
        }
    }

    private fun logAndSwallow(e: RuntimeException) {
        actionAuditService.logRuntimeException(logger, e)
    }

    // Because the deleteWorkspace() method operates at the DB level, this method
    // has a different signature than for the create action. This makes keeping the properties
    // consistent across actions somewhat challenging.
    override fun fireDeleteAction(dbWorkspace: DbWorkspace) {
        try {
            val info = getAuditEventInfo()
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
        } catch (e: RuntimeException) {
            logAndSwallow(e)
        }
    }

    override fun fireDuplicateAction(
        sourceWorkspaceDbModel: DbWorkspace,
        destinationWorkspaceDbModel: DbWorkspace
    ) {
        try {
            // We represent the duplication as a single action with events with different
            // ActionTypes: DUPLICATE_FROM and DUPLICATE_TO. The latter action generates many events
            // (similar to how CREATE is handled) for all the properties. I'm not (currently) filtering
            // out values that are the same.
            // TODO(jaycarlton): consider grabbing source event properties as well
            val info = getAuditEventInfo()
            val sourceEvent = ActionAuditEvent(
                    actionId = info.actionId,
                    agentEmailMaybe = info.userEmail,
                    actionType = ActionType.DUPLICATE_FROM,
                    agentType = AgentType.USER,
                    agentId = info.userId,
                    targetType = TargetType.WORKSPACE,
                    targetIdMaybe = sourceWorkspaceDbModel.workspaceId,
                    timestamp = info.timestamp
            )
            val destinationPropertyValues = TargetPropertyExtractor.getPropertyValuesByName(
                    WorkspaceTargetProperty.values(),
                    WorkspaceConversionUtils.toApiWorkspace(destinationWorkspaceDbModel))

            val destinationEvents: List<ActionAuditEvent> = destinationPropertyValues.entries
                    .map { ActionAuditEvent(
                            actionId = info.actionId,
                            agentEmailMaybe = info.userEmail,
                            actionType = ActionType.DUPLICATE_TO,
                            agentType = AgentType.USER,
                            agentId = info.userId,
                            targetType = TargetType.WORKSPACE,
                            targetPropertyMaybe = it.key,
                            targetIdMaybe = destinationWorkspaceDbModel.workspaceId,
                            newValueMaybe = it.value,
                            timestamp = info.timestamp
                            ) }

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

            val info = getAuditEventInfo()
            val workspaceTargetEvent = ActionAuditEvent(
                    actionId = info.actionId,
                    actionType = ActionType.COLLABORATE,
                    agentType = AgentType.USER,
                    agentEmailMaybe = info.userEmail,
                    agentId = info.userId,
                    timestamp = info.timestamp,
                    targetType = TargetType.WORKSPACE,
                    targetIdMaybe = sourceWorkspaceId
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
        } catch (e: RuntimeException) {
            logAndSwallow(e)
        }
    }

    private fun getAuditEventInfo(): CommonAuditEventInfo {
        return CommonAuditEventInfo(actionId = actionIdProvider.get(),
                userId = userProvider.get().userId,
                userEmail = userProvider.get().email,
                timestamp = clock.millis())
    }

    companion object {
        private val logger = Logger.getLogger(WorkspaceAuditAdapterImpl::class.java.name)
    }
}
