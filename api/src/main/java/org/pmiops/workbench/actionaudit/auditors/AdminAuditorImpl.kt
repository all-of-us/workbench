package org.pmiops.workbench.actionaudit.auditors

import org.pmiops.workbench.actionaudit.ActionAuditEvent
import org.pmiops.workbench.actionaudit.ActionAuditService
import org.pmiops.workbench.actionaudit.ActionType
import org.pmiops.workbench.actionaudit.AgentType
import org.pmiops.workbench.actionaudit.TargetType
import org.pmiops.workbench.db.model.DbUser
import org.pmiops.workbench.model.AccessReason
import org.pmiops.workbench.model.AdminLockingRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.time.Clock
import java.util.Date
import java.util.logging.Logger
import javax.inject.Provider

@Service
class AdminAuditorImpl
    @Autowired
    constructor(
        private val userProvider: Provider<DbUser>,
        private val actionAuditService: ActionAuditService,
        private val clock: Clock,
        @Qualifier("ACTION_ID") private val actionIdProvider: Provider<String>,
    ) : AdminAuditor {
        override fun fireViewNotebookAction(
            workspaceNamespace: String,
            workspaceName: String,
            notebookFilename: String,
            accessReason: AccessReason,
        ) {
            val dbUser = userProvider.get()
            val actionId = actionIdProvider.get()
            val timestamp = clock.millis()

            val props =
                mapOf(
                    "workspace_namespace" to workspaceNamespace,
                    "workspace_name" to workspaceName,
                    "notebook_name" to notebookFilename,
                    "access_reason" to accessReason.reason,
                )

            val events =
                props.map {
                    ActionAuditEvent(
                        actionId = actionId,
                        actionType = ActionType.VIEW,
                        agentType = AgentType.ADMINISTRATOR,
                        agentEmailMaybe = dbUser.username,
                        agentIdMaybe = dbUser.userId,
                        targetType = TargetType.NOTEBOOK,
                        targetPropertyMaybe = it.key,
                        newValueMaybe = it.value,
                        timestamp = timestamp,
                    )
                }

            actionAuditService.send(events)
        }

        override fun fireLockWorkspaceAction(
            workspaceId: Long,
            adminLockingRequest: AdminLockingRequest,
        ) {
            val dbUser = userProvider.get()
            val actionId = actionIdProvider.get()
            val timestamp = clock.millis()
            val requestTimestamp = Date(adminLockingRequest.requestDateInMillis).toString()

            val props =
                mapOf(
                    "locked" to "true",
                    "reason" to adminLockingRequest.requestReason,
                    "request_date" to requestTimestamp,
                )

            val events =
                props.map {
                    ActionAuditEvent(
                        actionId = actionId,
                        actionType = ActionType.EDIT,
                        agentType = AgentType.ADMINISTRATOR,
                        agentEmailMaybe = dbUser.username,
                        agentIdMaybe = dbUser.userId,
                        targetType = TargetType.WORKSPACE,
                        targetIdMaybe = workspaceId,
                        targetPropertyMaybe = it.key,
                        newValueMaybe = it.value,
                        timestamp = timestamp,
                    )
                }

            actionAuditService.send(events)
        }

        override fun fireUnlockWorkspaceAction(workspaceId: Long) {
            val dbUser = userProvider.get()
            val actionId = actionIdProvider.get()
            val timestamp = clock.millis()

            val event =
                ActionAuditEvent(
                    actionId = actionId,
                    actionType = ActionType.EDIT,
                    agentType = AgentType.ADMINISTRATOR,
                    agentEmailMaybe = dbUser.username,
                    agentIdMaybe = dbUser.userId,
                    targetType = TargetType.WORKSPACE,
                    targetIdMaybe = workspaceId,
                    targetPropertyMaybe = "locked",
                    newValueMaybe = "false",
                    timestamp = timestamp,
                )

            actionAuditService.send(event)
        }

        companion object {
            private val logger = Logger.getLogger(AdminAuditorImpl::class.java.name)
        }
    }
