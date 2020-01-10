package org.pmiops.workbench.actionaudit.auditors

import com.google.common.collect.ImmutableList
import org.pmiops.workbench.actionaudit.ActionAuditEvent
import org.pmiops.workbench.actionaudit.ActionAuditService
import org.pmiops.workbench.actionaudit.ActionType
import org.pmiops.workbench.actionaudit.AgentType
import org.pmiops.workbench.actionaudit.TargetType
import org.pmiops.workbench.api.ClusterController
import org.pmiops.workbench.db.dao.UserDao
import org.pmiops.workbench.db.model.DbUser
import org.pmiops.workbench.model.EgressEvent
import org.pmiops.workbench.model.EgressEventRequest
import org.pmiops.workbench.workspaces.WorkspaceService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.time.Clock
import java.util.logging.Logger
import javax.inject.Provider

@Service
class SumoLogicAuditorImpl @Autowired
constructor(
    private val actionAuditService: ActionAuditService,
    private val workspaceService: WorkspaceService,
    private val userDao: UserDao,
    private val clock: Clock,
    @Qualifier("ACTION_ID") private val actionIdProvider: Provider<String>
) : SumoLogicAuditor {

    /**
     * Converts a DB-internal user ID to the corresponding VM name. Note: for now, we use the same
     * logic used by ClusterController, but if that logic changes, we may want to adapt this code
     * to support searching for VMs with both the older and newer naming scheme.
     */
    private fun dbUserToClusterName(dbUser: DbUser): String {
        return ClusterController.clusterNameForUser(dbUser)
    }

    /**
     * Converts the AoU-chosen cluster name into the actual VM name as reported by Google Cloud's
     * flow logs. Empirically, an "-m" suffix is added to the VM name.
     *
     * Note: this pattern may change if the Leo team switches to using raw VMs instead of Dataproc
     * clusters!
     */
    private fun dbUserToVmName(dbUser: DbUser): String {
        return dbUserToClusterName(dbUser) + "-m"
    }

    override fun fireEgressEvent(event: EgressEvent) {
        // Load the workspace via the GCP project name
        val dbWorkspace = this.workspaceService.getByNamespace(event.projectName)
        if (dbWorkspace == null) {
            fireFailedToFindWorkspace(event)
            return
        }
        // Using service-level creds, load the FireCloud workspace ACLs to find all members
        // of the workspace. Then attempt to find the user who aligns with the named VM.
        val userRoles = workspaceService.getFirecloudUserRoles(dbWorkspace.workspaceNamespace,
                dbWorkspace.firecloudName)
        val vmOwner = userRoles
                .map { userDao.findUserByUsername(it.email) }
                .filter { dbUserToVmName(it).equals(event.vmName) }
                .firstOrNull()

        var agentEmail = ""
        var agentId = 0L
        if (vmOwner != null) {
            agentEmail = vmOwner.username
            agentId = vmOwner.userId
        } else {
            // If the VM name doesn't match a user on the workspace, we'll still log an
            // event in the target workspace, but with nulled-out user info.
            logger.warning(String.format("Could not find a user for VM name %s in project %s", event.vmName, event.projectName))
        }

        val actionId = actionIdProvider.get()
        actionAuditService.sendWithComment(ImmutableList.of(ActionAuditEvent(
                timestamp = clock.millis(),
                actionId = actionId,
                actionType = ActionType.HIGH_EGRESS,
                agentType = AgentType.USER,
                agentId = agentId,
                agentEmailMaybe = agentEmail,
                targetType = TargetType.WORKSPACE,
                targetIdMaybe = dbWorkspace.workspaceId
        )), String.format(
                "Detected %.2fMb egress across %d seconds in workspace %s, from VM %s",
                event.egressMib, event.timeWindowDuration, event.projectName, event.vmName))
    }

    override fun fireFailedToParseEgressEvent(request: EgressEventRequest) {
        fireGenericEventWithComment(String.format(
                "Failed to parse egress event JSON from SumoLogic. Field contents: %s",
                request.eventsJsonArray))
    }

    override fun fireBadApiKeyEgressEvent(apiKey: String, request: EgressEventRequest) {
        fireGenericEventWithComment(String.format(
                "Received bad API key from SumoLogic. Bad key: %s, full request: %s",
                apiKey, request.toString()))
    }

    private fun fireFailedToFindWorkspace(event: EgressEvent) {
        fireGenericEventWithComment(String.format(
                "Failed to find workspace for high-egress event: %s",
                event.toString()))
    }

    private fun fireGenericEventWithComment(comment: String) {
        actionAuditService.sendWithComment(ImmutableList.of(ActionAuditEvent(
                timestamp = clock.millis(),
                actionId = actionIdProvider.get(),
                actionType = ActionType.HIGH_EGRESS,
                agentType = AgentType.SYSTEM,
                agentId = 0,
                agentEmailMaybe = null,
                targetType = TargetType.WORKSPACE,
                targetIdMaybe = null
        )), comment)
    }

    companion object {
        private val logger = Logger.getLogger(SumoLogicAuditorImpl::class.java.name)
    }
}
