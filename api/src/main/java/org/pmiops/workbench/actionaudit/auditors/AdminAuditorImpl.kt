package org.pmiops.workbench.actionaudit.auditors

import java.time.Clock
import java.util.logging.Logger
import javax.inject.Provider
import org.pmiops.workbench.actionaudit.ActionAuditEvent
import org.pmiops.workbench.actionaudit.ActionAuditService
import org.pmiops.workbench.actionaudit.ActionType
import org.pmiops.workbench.actionaudit.AgentType
import org.pmiops.workbench.actionaudit.TargetType
import org.pmiops.workbench.db.model.DbUser
import org.pmiops.workbench.model.AccessReason
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

@Service
class AdminAuditorImpl @Autowired
constructor(
    private val userProvider: Provider<DbUser>,
    private val actionAuditService: ActionAuditService,
    private val clock: Clock,
    @Qualifier("ACTION_ID") private val actionIdProvider: Provider<String>
) : AdminAuditor {

    override fun fireViewNotebookAction(workspaceNamespace: String, workspaceName: String, notebookFilename: String, accessReason: AccessReason) {
        val dbUser = userProvider.get()
        val actionId = actionIdProvider.get()
        val timestamp = clock.millis()

        val props = mapOf(
            "workspace_namespace" to workspaceNamespace,
            "workspace_name" to workspaceName,
            "notebook_name" to notebookFilename,
            "access_reason" to accessReason.reason
        )

        val events = props.map {
            ActionAuditEvent(
                    actionId = actionId,
                    actionType = ActionType.VIEW,
                    agentType = AgentType.ADMINISTRATOR,
                    agentEmailMaybe = dbUser.username,
                    agentIdMaybe = dbUser.userId,
                    targetType = TargetType.NOTEBOOK,
                    targetPropertyMaybe = it.key,
                    newValueMaybe = it.value,
                    timestamp = timestamp) }

        actionAuditService.send(events)
    }

    companion object {
        private val logger = Logger.getLogger(AdminAuditorImpl::class.java.name)
    }
}
