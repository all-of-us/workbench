package org.pmiops.workbench.db.dao

import org.pmiops.workbench.cohortreview.util.PageRequest
import org.pmiops.workbench.db.model.ParticipantCohortStatus

/**
 * This implementation manually creates batched sql statements. For unknown reasons Spring JPA nor
 * JDBC batching works in appengine running in the cloud. It's old school but it solves our batching
 * issue and can handle inserts up to 10,000 participants.
 */
interface ParticipantCohortStatusDaoCustom {

    fun saveParticipantCohortStatusesCustom(participantCohortStatuses: List<ParticipantCohortStatus>)

    fun findAll(cohortReviewId: Long?, pageRequest: PageRequest): List<ParticipantCohortStatus>

    fun findCount(cohortReviewId: Long?, pageRequest: PageRequest): Long?
}
