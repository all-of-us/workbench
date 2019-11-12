package org.pmiops.workbench.actionaudit

import java.util.logging.Logger

interface ActionAuditService {
    fun send(event: ActionAuditEvent)

    fun send(events: Collection<ActionAuditEvent>)

    fun logRuntimeException(logger: Logger, exception: RuntimeException)
}
