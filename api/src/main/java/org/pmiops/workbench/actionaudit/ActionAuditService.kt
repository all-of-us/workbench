package org.pmiops.workbench.actionaudit

interface ActionAuditService {
    fun send(event: ActionAuditEvent)

    fun send(events: Collection<ActionAuditEvent>)
}
