package org.pmiops.workbench.db.model

import java.io.Serializable
import java.util.Objects
import javax.persistence.Column
import javax.persistence.Embeddable
import org.apache.commons.lang3.builder.ToStringBuilder

@Embeddable
class ParticipantCohortStatusKey : Serializable {

    @Column(name = "cohort_review_id")
    var cohortReviewId: Long = 0

    @Column(name = "participant_id")
    var participantId: Long = 0

    constructor() {}

    constructor(cohortReviewId: Long, participantId: Long) {
        this.cohortReviewId = cohortReviewId
        this.participantId = participantId
    }

    fun cohortReviewId(cohortReviewId: Long): ParticipantCohortStatusKey {
        this.cohortReviewId = cohortReviewId
        return this
    }

    fun participantId(participantId: Long): ParticipantCohortStatusKey {
        this.participantId = participantId
        return this
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val that = o as ParticipantCohortStatusKey?
        return cohortReviewId == that!!.cohortReviewId && participantId == that.participantId
    }

    override fun hashCode(): Int {
        return Objects.hash(cohortReviewId, participantId)
    }

    override fun toString(): String {
        return ToStringBuilder(this)
                .append("cohortReviewId", cohortReviewId)
                .append("participantId", participantId)
                .toString()
    }
}
