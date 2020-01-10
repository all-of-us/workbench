package org.pmiops.workbench.actionaudit

import java.util.logging.Logger

interface ActionAuditService {
    fun send(event: ActionAuditEvent) {
        send(setOf(event))
    }

    fun send(events: Collection<ActionAuditEvent>)

    /**
     * Sends a collection of events plus an event with ActionType.COMMENT and the given comment
     * string as the newValueMaybe. The COMMENT action derives its other properties from the
     * first event from the original collection.
     */
    fun sendWithComment(events: Collection<ActionAuditEvent>, comment: String)

    fun logRuntimeException(logger: Logger, exception: RuntimeException)
}
