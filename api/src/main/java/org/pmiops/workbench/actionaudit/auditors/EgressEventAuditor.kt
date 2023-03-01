package org.pmiops.workbench.actionaudit.auditors

import org.pmiops.workbench.config.WorkbenchConfig
import org.pmiops.workbench.db.model.DbEgressEvent
import org.pmiops.workbench.db.model.DbUser
import org.pmiops.workbench.model.SumologicEgressEvent
import org.pmiops.workbench.model.SumologicEgressEventRequest

/**
 * Auditor service which handles collecting audit logs for high-egress events. These events
 * are calculated by SumoLogic and routed through to the Workbench for logging and to take any
 * automated action in response to the event.
 */
interface EgressEventAuditor {
    /**
     * Decorates a Sumologic-reported high-egress event with Workbench metadata and
     * fires an audit event log in the target workspace.
     */
    fun fireEgressEvent(event: SumologicEgressEvent)

    /**
     * Decorates a Sumologic-reported high-egress event with Workbench metadata and
     * fires an audit event log in the target workspace for the specified user.
     */
    fun fireEgressEventForUser(event: SumologicEgressEvent, user: DbUser)

    /**
     * Decorates a high-egress event with remediation metadata and fires an audit event log in the
     * target workspace.
     */
    fun fireRemediateEgressEvent(
        dbEvent: DbEgressEvent,
        escalation: WorkbenchConfig.EgressAlertRemediationPolicy.Escalation?
    )

    /**
     * Fires an audit log event for an admin update to an egress event.
     */
    fun fireAdminEditEgressEvent(
        previousEvent: DbEgressEvent,
        updatedEvent: DbEgressEvent
    )

    /**
     * Fires an audit event log tracking when a Sumologic-reported high-egress event
     * request could not successfully be parsed.
     */
    fun fireFailedToParseEgressEventRequest(request: SumologicEgressEventRequest)

    /**
     * Fires an audit event log tracking when a Sumologic-reported high-egress event
     * request did not have a valid API key.
     */
    fun fireBadApiKey(apiKey: String, request: SumologicEgressEventRequest)
}
