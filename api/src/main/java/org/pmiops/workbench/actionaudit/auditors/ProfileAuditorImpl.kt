package org.pmiops.workbench.actionaudit.auditors

import java.time.Clock
import javax.inject.Provider
import org.pmiops.workbench.actionaudit.ActionAuditEvent
import org.pmiops.workbench.actionaudit.ActionAuditService
import org.pmiops.workbench.actionaudit.ActionType
import org.pmiops.workbench.actionaudit.Agent
import org.pmiops.workbench.actionaudit.AgentType
import org.pmiops.workbench.actionaudit.TargetType
import org.pmiops.workbench.actionaudit.targetproperties.ProfileTargetProperty
import org.pmiops.workbench.actionaudit.targetproperties.TargetPropertyExtractor
import org.pmiops.workbench.db.model.DbUser
import org.pmiops.workbench.model.Profile
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

@Service
class ProfileAuditorImpl @Autowired
constructor(
    private val actionAuditService: ActionAuditService,
    private val clock: Clock,
    @Qualifier("ACTION_ID") private val actionIdProvider: Provider<String>
) : ProfileAuditor {

    override fun fireCreateAction(createdProfile: Profile) {
        val propertiesByName: Map<String, String?> = TargetPropertyExtractor
                .getPropertyValuesByName(ProfileTargetProperty.values(), createdProfile)
        val actionId = actionIdProvider.get()
        // We can't rely on the UserProvider here since the user didn't exist when it
        // got loaded or whatever.
        val createEvents = propertiesByName.entries
                .map {
                    ActionAuditEvent(
                            timestamp = clock.millis(),
                            actionId = actionId,
                            actionType = ActionType.CREATE,
                            agentType = AgentType.USER,
                            agentIdMaybe = createdProfile.userId,
                            agentEmailMaybe = createdProfile.contactEmail,
                            targetType = TargetType.PROFILE,
                            targetIdMaybe = createdProfile.userId,
                            targetPropertyMaybe = it.key,
                            previousValueMaybe = null,
                            newValueMaybe = it.value)
                }
        actionAuditService.send(createEvents)
    }

    override fun fireUpdateAction(previousProfile: Profile, updatedProfile: Profile, agent: Agent) {
        // determine changed fields
        val changesByProperty = TargetPropertyExtractor.getChangedValuesByName(
                ProfileTargetProperty.values(),
                previousProfile,
                updatedProfile)
        val events = changesByProperty.entries
                .map {
                    ActionAuditEvent(
                            timestamp = clock.millis(),
                            actionId = actionIdProvider.get(),
                            actionType = ActionType.EDIT,
                            agentType = agent.agentType,
                            agentIdMaybe = agent.idMaybe,
                            agentEmailMaybe = agent.emailMaybe,
                            targetType = TargetType.PROFILE,
                            targetIdMaybe = previousProfile.userId,
                            targetPropertyMaybe = it.key,
                            previousValueMaybe = it.value.previousValue,
                            newValueMaybe = it.value.newValue)
                }
        actionAuditService.send(events)
    }

    // Each user is assumed to have only one profile, but we can't rely on
    // the userProvider if the user is deleted before the profile.
    override fun fireDeleteAction(userId: Long, userEmail: String) {
        val deleteProfileEvent = ActionAuditEvent(
                timestamp = clock.millis(),
                actionId = actionIdProvider.get(),
                actionType = ActionType.DELETE,
                agentType = AgentType.USER,
                agentIdMaybe = userId,
                agentEmailMaybe = userEmail,
                targetType = TargetType.PROFILE,
                targetIdMaybe = userId,
                targetPropertyMaybe = null,
                previousValueMaybe = null,
                newValueMaybe = null)
        actionAuditService.send(deleteProfileEvent)
    }

    override fun fireLoginAction(dbUser: DbUser) {
        actionAuditService.send(ActionAuditEvent(
                timestamp = clock.millis(),
                actionId = actionIdProvider.get(),
                actionType = ActionType.LOGIN,
                agentType = AgentType.USER,
                agentIdMaybe = dbUser.userId,
                agentEmailMaybe = dbUser.username,
                targetType = TargetType.WORKBENCH,
                targetIdMaybe = null,
                targetPropertyMaybe = null,
                previousValueMaybe = null,
                newValueMaybe = null))
    }
}
