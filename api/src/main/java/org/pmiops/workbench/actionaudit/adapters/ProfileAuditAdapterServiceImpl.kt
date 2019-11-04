package org.pmiops.workbench.actionaudit.adapters

import java.time.Clock
import java.util.logging.Level
import java.util.logging.Logger
import javax.inject.Provider
import org.pmiops.workbench.actionaudit.ActionAuditEvent
import org.pmiops.workbench.actionaudit.ActionAuditService
import org.pmiops.workbench.actionaudit.ActionType
import org.pmiops.workbench.actionaudit.AgentType
import org.pmiops.workbench.actionaudit.TargetType
import org.pmiops.workbench.db.model.User
import org.pmiops.workbench.model.Profile
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ProfileAuditAdapterServiceImpl @Autowired
constructor(
    private val userProvider: Provider<User>,
    private val actionAuditService: ActionAuditService,
    private val clock: Clock
) : ProfileAuditAdapterService {

    override fun fireCreateAction(createdProfile: Profile) {
        try {
            val createProfileEvent = ActionAuditEvent(
                    timestamp = clock.millis(),
                    actionId = ActionAuditEvent.newActionId(),
                    actionType = ActionType.CREATE,
                    agentType = AgentType.USER,
                    agentId = userProvider.get().userId,
                    agentEmailMaybe = userProvider.get().email,
                    targetType = TargetType.PROFILE,
                    targetIdMaybe = userProvider.get().userId,
                    targetPropertyMaybe = null,
                    previousValueMaybe = null,
                    newValueMaybe = null
            )
            actionAuditService.send(createProfileEvent)
        } catch (e: RuntimeException) {
            logAndSwallow(e)
        }
    }

    override fun fireUpdateAction(previousProfile: Profile, updatedProfile: Profile) {}

    // Each user is assumed to have only one profile, but we can't rely on
    // the userProvider if the user is deleted before the profile.
    override fun fireDeleteAction(userId: Long, userEmail: String) {
        try {
            val deleteProfileEvent = ActionAuditEvent(
                    timestamp = clock.millis(),
                    actionId = ActionAuditEvent.newActionId(),
                    actionType = ActionType.DELETE,
                    agentType = AgentType.USER,
                    agentId = userId,
                    agentEmailMaybe = userEmail,
                    targetType = TargetType.PROFILE,
                    targetIdMaybe = userId,
                    targetPropertyMaybe = null,
                    previousValueMaybe = null,
                    newValueMaybe = null
                    )
            actionAuditService.send(deleteProfileEvent)
        } catch (e: RuntimeException) {
            logAndSwallow(e)
        }
    }

    private fun logAndSwallow(e: RuntimeException) {
        logger.log(Level.WARNING, e) { "Exception encountered during audit." }
    }

    companion object {

        private val logger = Logger.getLogger(ProfileAuditAdapterServiceImpl::class.java.name)
    }
}
