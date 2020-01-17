package org.pmiops.workbench.actionaudit.auditors

import org.pmiops.workbench.actionaudit.ActionAuditEvent
import org.pmiops.workbench.actionaudit.ActionAuditService
import org.pmiops.workbench.actionaudit.ActionType
import org.pmiops.workbench.actionaudit.AgentType
import org.pmiops.workbench.actionaudit.TargetType
import org.pmiops.workbench.actionaudit.targetproperties.EgressEventCommentTargetProperty
import org.pmiops.workbench.actionaudit.targetproperties.EgressEventTargetProperty
import org.pmiops.workbench.actionaudit.targetproperties.TargetPropertyExtractor
import org.pmiops.workbench.api.ClusterController
import org.pmiops.workbench.db.dao.UserDao
import org.pmiops.workbench.db.model.DbUser
import org.pmiops.workbench.exceptions.BadRequestException
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
class EgressEventAuditorImpl @Autowired
constructor(
    private val actionAuditService: ActionAuditService,
    private val workspaceService: WorkspaceService,
    private val userDao: UserDao,
    private val clock: Clock,
    @Qualifier("ACTION_ID") private val actionIdProvider: Provider<String>
) : EgressEventAuditor {

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
     * flow logs. Empirically, an "-m" suffix is added to the VM name, due to Leo team's use
     * of Dataproc (see https://cloud.google.com/dataproc/docs/concepts/configuring-clusters/metadata).
     *
     * Note: this pattern may change if the Leo team switches to using raw VMs instead of Dataproc
     * clusters!
     */
    private fun dbUserToVmName(dbUser: DbUser): String {
        return dbUserToClusterName(dbUser) + DATAPROC_MASTER_NODE_SUFFIX
    }

    override fun fireEgressEvent(event: EgressEvent) {
        // Load the workspace via the GCP project name
        val dbWorkspaceMaybe = workspaceService.getByNamespace(event.projectName)
        if (!dbWorkspaceMaybe.isPresent()) {
            // Failing to find a workspace is odd enough that we want to return a non-success
            // response at the API level. But it's also worth storing a permanent record of the
            // high-egress event by logging it without a workspace target.
            fireFailedToFindWorkspace(event)
            throw BadRequestException(String.format(
                    "The workspace '%s' referred to by the given event is no longer active or never existed.",
                    event.projectName))
        }
        val dbWorkspace = dbWorkspaceMaybe.get()

        // Using service-level creds, load the FireCloud workspace ACLs to find all members
        // of the workspace. Then attempt to find the user who aligns with the named VM.
        val userRoles = workspaceService.getFirecloudUserRoles(dbWorkspace.workspaceNamespace,
                dbWorkspace.firecloudName)
        val vmOwner = userRoles
                .map { userDao.findUserByUsername(it.email) }
                .filter { dbUserToVmName(it).equals(event.vmName) }
                .firstOrNull()

        var agentEmail: String? = null
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
        fireEventSet(baseEvent = ActionAuditEvent(
                timestamp = clock.millis(),
                actionId = actionId,
                actionType = ActionType.EGRESS_EVENT,
                agentType = AgentType.USER,
                agentId = agentId,
                agentEmailMaybe = agentEmail,
                targetType = TargetType.WORKSPACE,
                targetIdMaybe = dbWorkspace.workspaceId
        ), egressEvent = event)
    }

    override fun fireFailedToParseEgressEventRequest(request: EgressEventRequest) {
        fireEventSet(baseEvent = getGenericBaseEvent(), comment = String.format(
                "Failed to parse egress event JSON from SumoLogic. Field contents: %s",
                request.eventsJsonArray))
    }

    override fun fireBadApiKey(apiKey: String, request: EgressEventRequest) {
        fireEventSet(baseEvent = getGenericBaseEvent(), comment = String.format(
                "Received bad API key from SumoLogic. Bad key: %s, full request: %s",
                apiKey, request.toString()))
    }

    private fun fireFailedToFindWorkspace(event: EgressEvent) {
        fireEventSet(baseEvent = getGenericBaseEvent(), egressEvent = event, comment = String.format(
                "Failed to find workspace for high-egress event: %s",
                event.toString()))
    }

    /**
     * Creates and fires a set of events derived from the given base event, incorporating audit rows
     * to record properties of the egress event and a human-readable comment. Either the egress event
     * or the comment may be null, in which case those row(s) won't be generated.
     */
    private fun fireEventSet(baseEvent: ActionAuditEvent, egressEvent: EgressEvent? = null, comment: String? = null) {
        var events = ArrayList<ActionAuditEvent>()
        if (egressEvent != null) {
            val propertyValues = TargetPropertyExtractor.getPropertyValuesByName(
                    EgressEventTargetProperty.values(), egressEvent)
            events.addAll(propertyValues.map {
                baseEvent.copy(
                        targetPropertyMaybe = it.key,
                        newValueMaybe = it.value)
            })
        }
        if (comment != null) {
            events.add(baseEvent.copy(
                    targetPropertyMaybe = EgressEventCommentTargetProperty.COMMENT.propertyName,
                    newValueMaybe = comment
            ))
        }
        actionAuditService.send(events)
    }

    /**
     * Returns an ActionAuditEvent suitable for logging system-level error messages, e.g. when
     * an inbound high-egress event refers to an inactive workspace or when the request JSON
     * could not be successfully parsed.
     */
    private fun getGenericBaseEvent(): ActionAuditEvent {
        return ActionAuditEvent(
                timestamp = clock.millis(),
                actionId = actionIdProvider.get(),
                actionType = ActionType.EGRESS_EVENT,
                agentType = AgentType.SYSTEM,
                agentId = 0,
                agentEmailMaybe = null,
                targetType = TargetType.WORKSPACE,
                targetIdMaybe = null
        )
    }

    companion object {
        //
        private val DATAPROC_MASTER_NODE_SUFFIX = "-m"
        private val logger = Logger.getLogger(EgressEventAuditorImpl::class.java.name)
    }
}
