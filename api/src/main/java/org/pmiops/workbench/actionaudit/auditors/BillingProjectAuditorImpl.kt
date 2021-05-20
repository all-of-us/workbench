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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

@Service
class BillingProjectAuditorImpl @Autowired
constructor(
    private val userProvider: Provider<DbUser>,
    private val actionAuditService: ActionAuditService,
    private val clock: Clock,
    @Qualifier("ACTION_ID") private val actionIdProvider: Provider<String>
) : BillingProjectAuditor {
    override fun fireDeleteAction(billingProjectName: String) {
        val event = ActionAuditEvent(
                actionId = actionIdProvider.get(),
                actionType = ActionType.DELETE,
                agentType = AgentType.SYSTEM,
                agentIdMaybe = null,
                agentEmailMaybe = null,
                targetType = TargetType.BILLING_PROJECT,
                newValueMaybe = billingProjectName,
                timestamp = clock.millis())

        actionAuditService.send(event)
    }

    companion object {
        private val logger = Logger.getLogger(BillingProjectAuditorImpl::class.java.name)
    }
}
