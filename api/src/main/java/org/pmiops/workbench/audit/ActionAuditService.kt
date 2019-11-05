package org.pmiops.workbench.audit

interface ActionAuditService {
    fun send(event: ActionAuditEvent)

    fun send(events: Collection<ActionAuditEvent>)
}
