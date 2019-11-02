package org.pmiops.workbench.db.model

import org.pmiops.workbench.model.CohortStatus

/**
 * Projection from [ParticipantCohortStatus] that contains just the participant ID and cohort
 * status.
 */
interface ParticipantIdAndCohortStatus {

    val participantKey: Key

    val status: CohortStatus

    interface Key {
        val participantId: Long?
    }
}
