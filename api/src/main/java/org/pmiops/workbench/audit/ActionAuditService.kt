package org.pmiops.workbench.audit

import java.util.*

interface ActionAuditService {
    fun send(event: ActionAuditEvent)

    fun send(events: Collection<ActionAuditEvent>)

    fun newActionId(): String = UUID.randomUUID().toString()
}
