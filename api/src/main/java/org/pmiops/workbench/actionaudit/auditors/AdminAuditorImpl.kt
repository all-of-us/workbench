package org.pmiops.workbench.actionaudit.auditors

import org.pmiops.workbench.actionaudit.ActionAuditEvent
import org.pmiops.workbench.actionaudit.ActionAuditService
import org.pmiops.workbench.actionaudit.ActionType
import org.pmiops.workbench.actionaudit.AgentType
import org.pmiops.workbench.actionaudit.TargetType
import org.pmiops.workbench.db.model.DbUser
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.time.Clock
import java.util.logging.Logger
import javax.inject.Provider

@Service
class AdminAuditorImpl @Autowired
constructor(
    private val userProvider: Provider<DbUser>,
    private val actionAuditService: ActionAuditService,
    private val clock: Clock,
    @Qualifier("ACTION_ID") private val actionIdProvider: Provider<String>
) : AdminAuditor {

    override fun fireViewNotebookAction(workspaceNamespace: String, workspaceName: String, notebookFilename: String, accessReason: String) {
        val dbUser = userProvider.get()
        val actionId = actionIdProvider.get()

        val props = mapOf(
            "Workspace Namespace" to workspaceNamespace,
            "Workspace Name" to workspaceName,
            "Notebook Name" to notebookFilename,
            "Access Reason" to accessReason
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
                    timestamp = clock.millis()) }

        actionAuditService.send(events)
    }

    companion object {
        private val logger = Logger.getLogger(AdminAuditorImpl::class.java.name)
    }
}
