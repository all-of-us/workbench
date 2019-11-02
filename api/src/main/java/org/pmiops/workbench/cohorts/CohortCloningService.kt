package org.pmiops.workbench.cohorts

import org.pmiops.workbench.db.dao.CohortAnnotationDefinitionDao
import org.pmiops.workbench.db.dao.CohortDao
import org.pmiops.workbench.db.dao.CohortReviewDao
import org.pmiops.workbench.db.dao.ParticipantCohortAnnotationDao
import org.pmiops.workbench.db.dao.ParticipantCohortStatusDao
import org.pmiops.workbench.db.model.Cohort
import org.pmiops.workbench.db.model.CohortReview
import org.pmiops.workbench.db.model.Workspace
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CohortCloningService {

    // Note: Cannot use an @Autowired constructor with this version of Spring
    // Boot due to https://jira.spring.io/browse/SPR-15600. See RW-256.
    @Autowired
    private val cohortDao: CohortDao? = null
    @Autowired
    private val cohortFactory: CohortFactory? = null
    @Autowired
    private val cohortReviewDao: CohortReviewDao? = null
    @Autowired
    private val participantCohortStatusDao: ParticipantCohortStatusDao? = null
    @Autowired
    private val cohortAnnotationDefinitionDao: CohortAnnotationDefinitionDao? = null
    @Autowired
    private val participantCohortAnnotationDao: ParticipantCohortAnnotationDao? = null

    @Transactional
    fun cloneCohortAndReviews(fromCohort: Cohort, targetWorkspace: Workspace): Cohort {
        val duplicatedCohort = cohortFactory!!.duplicateCohort(
                fromCohort.name, targetWorkspace.creator, targetWorkspace, fromCohort)
        val toCohort = cohortDao!!.save(duplicatedCohort)
        copyCohortAnnotations(fromCohort, toCohort)

        for (fromReview in fromCohort.cohortReviews!!) {
            val duplicatedReview = cohortFactory.duplicateCohortReview(fromReview, toCohort)
            val toReview = cohortReviewDao!!.save(duplicatedReview)
            copyCohortReviewAnnotations(fromCohort, fromReview, toCohort, toReview)
        }

        return toCohort
    }

    private fun copyCohortAnnotations(from: Cohort, to: Cohort) {
        cohortAnnotationDefinitionDao!!.bulkCopyCohortAnnotationDefinitionByCohort(
                from.cohortId, to.cohortId)
        // Important: this must follow the above method
        // {@link CohortAnnotationDefinitionDao#bulkCopyCohortAnnotationDefinitionByCohort(long, long)}
        cohortAnnotationDefinitionDao.bulkCopyCohortAnnotationEnumsByCohort(
                from.cohortId, to.cohortId)
    }

    private fun copyCohortReviewAnnotations(
            fromCohort: Cohort, fromReview: CohortReview, toCohort: Cohort, toReview: CohortReview) {
        participantCohortStatusDao!!.bulkCopyByCohortReview(
                fromReview.cohortReviewId, toReview.cohortReviewId)
        participantCohortAnnotationDao!!.bulkCopyEnumAnnotationsByCohortReviewAndCohort(
                fromCohort.cohortId, toCohort.cohortId, toReview.cohortReviewId)
        participantCohortAnnotationDao.bulkCopyNonEnumAnnotationsByCohortReviewAndCohort(
                fromCohort.cohortId, toCohort.cohortId, toReview.cohortReviewId)
    }
}
