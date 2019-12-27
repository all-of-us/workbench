package org.pmiops.workbench.actionaudit.auditors

import org.pmiops.workbench.model.EgressEvent
import org.pmiops.workbench.model.EgressEventRequest

interface SumoLogicAuditor {
    // Decorates a Sumologic-reported egress event with Workbench metadata and
    // fires an audit event log in the target workspace.
    fun fireEgressEvent(event: EgressEvent)

    // Fires an audit event log tracking when a Sumologic-reported egress event
    // request could not successfully be parsed.
    fun fireFailedToParseEgressEvent(request: EgressEventRequest)
}
