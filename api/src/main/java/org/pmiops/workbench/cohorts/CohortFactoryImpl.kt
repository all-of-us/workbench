package org.pmiops.workbench.cohorts

import java.sql.Timestamp
import java.time.Clock
import org.pmiops.workbench.db.model.Cohort
import org.pmiops.workbench.db.model.CohortReview
import org.pmiops.workbench.db.model.User
import org.pmiops.workbench.db.model.Workspace
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class CohortFactoryImpl @Autowired
constructor(private val clock: Clock) : CohortFactory {

    override fun createCohort(
            apiCohort: org.pmiops.workbench.model.Cohort, creator: User, workspaceId: Long): Cohort {
        return createCohort(
                apiCohort.getDescription(),
                apiCohort.getName(),
                apiCohort.getType(),
                apiCohort.getCriteria(),
                creator,
                workspaceId)
    }

    override fun duplicateCohort(
            newName: String, creator: User, workspace: Workspace, original: Cohort): Cohort {
        return createCohort(
                original.description,
                newName,
                original.type,
                original.criteria,
                creator,
                workspace.workspaceId)
    }

    override fun duplicateCohortReview(original: CohortReview, targetCohort: Cohort): CohortReview {
        val newCohortReview = CohortReview()

        newCohortReview.cohortId = targetCohort.cohortId
        newCohortReview.creationTime(targetCohort.creationTime)
        newCohortReview.lastModifiedTime = targetCohort.lastModifiedTime
        newCohortReview.cdrVersionId = original.cdrVersionId
        newCohortReview.matchedParticipantCount = original.matchedParticipantCount
        newCohortReview.reviewSize = original.reviewSize
        newCohortReview.reviewedCount = original.reviewedCount
        newCohortReview.reviewStatusEnum = original.reviewStatusEnum
        newCohortReview.cohortName = original.cohortName
        newCohortReview.cohortDefinition = original.cohortDefinition
        newCohortReview.description = original.description
        newCohortReview.creator = original.creator

        return newCohortReview
    }

    private fun createCohort(
            desc: String?, name: String, type: String?, criteria: String?, creator: User, workspaceId: Long): Cohort {
        val now = Timestamp(clock.instant().toEpochMilli())
        val cohort = Cohort()

        cohort.description = desc
        cohort.name = name
        cohort.type = type
        cohort.criteria = criteria
        cohort.creationTime = now
        cohort.lastModifiedTime = now
        cohort.version = 1
        cohort.creator = creator
        cohort.workspaceId = workspaceId

        return cohort
    }
}
