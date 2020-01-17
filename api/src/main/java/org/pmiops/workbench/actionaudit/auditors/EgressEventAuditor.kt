package org.pmiops.workbench.actionaudit.auditors

import org.pmiops.workbench.model.EgressEvent
import org.pmiops.workbench.model.EgressEventRequest

interface EgressEventAuditor {
    // Decorates a Sumologic-reported high-egress event with Workbench metadata and
    // fires an audit event log in the target workspace.
    fun fireEgressEvent(event: EgressEvent)

    // Fires an audit event log tracking when a Sumologic-reported high-egress event
    // request could not successfully be parsed.
    fun fireFailedToParseEgressEventRequest(request: EgressEventRequest)

    // Fires an audit event log tracking when a Sumologic-reported high-egress event
    // request did not have a valid API key.
    fun fireBadApiKey(apiKey: String, request: EgressEventRequest)
}
