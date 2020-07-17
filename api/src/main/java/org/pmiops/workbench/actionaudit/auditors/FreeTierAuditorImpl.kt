package org.pmiops.workbench.actionaudit.auditors

import org.pmiops.workbench.actionaudit.ActionAuditEvent
import org.pmiops.workbench.actionaudit.ActionAuditService
import org.pmiops.workbench.actionaudit.ActionType
import org.pmiops.workbench.actionaudit.AgentType
import org.pmiops.workbench.actionaudit.TargetType
import org.pmiops.workbench.actionaudit.targetproperties.ProfileTargetProperty
import org.pmiops.workbench.db.model.DbUser
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.time.Clock
import javax.inject.Provider

@Service
class FreeTierAuditorImpl @Autowired
constructor(
    private val userProvider: Provider<DbUser>,
    private val actionAuditService: ActionAuditService,
    private val clock: Clock,
    @Qualifier("ACTION_ID") private val actionIdProvider: Provider<String>
) : FreeTierAuditor {
    override fun fireFreeTierDollarQuotaAction(userId: Long, previousDollarQuota: Double?, newDollarQuota: Double?) {
        actionAuditService.send(ActionAuditEvent(
                timestamp = clock.millis(),
                actionId = actionIdProvider.get(),
                actionType = ActionType.FREE_TIER_DOLLAR_OVERRIDE,
                agentType = AgentType.ADMINISTRATOR,
                agentIdMaybe = userProvider.get().userId,
                agentEmailMaybe = userProvider.get().username,
                targetType = TargetType.USER,
                targetIdMaybe = userId,
                targetPropertyMaybe = ProfileTargetProperty.FREE_TIER_DOLLAR_QUOTA.name,
                previousValueMaybe = previousDollarQuota?.toString(),
                newValueMaybe = newDollarQuota?.toString()))
    }
}
