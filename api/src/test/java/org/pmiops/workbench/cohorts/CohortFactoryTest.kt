package org.pmiops.workbench.cohorts

import org.assertj.core.api.Assertions.assertThat
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock

import java.sql.Timestamp
import java.time.Clock
import java.util.Collections
import org.junit.Before
import org.junit.Test
import org.pmiops.workbench.db.model.Cohort
import org.pmiops.workbench.db.model.CohortReview
import org.pmiops.workbench.db.model.User
import org.pmiops.workbench.db.model.Workspace
import org.pmiops.workbench.model.ReviewStatus

class CohortFactoryTest {

    private var cohortFactory: CohortFactory? = null

    @Before
    fun setUp() {
        cohortFactory = CohortFactoryImpl(Clock.systemUTC())
    }

    @Test
    fun createCohort() {
        val apiCohort = org.pmiops.workbench.model.Cohort()
        apiCohort.setDescription("desc")
        apiCohort.setName("name")
        apiCohort.setType("type")
        apiCohort.setCriteria("criteria")

        val user = mock(User::class.java)

        val workspaceId = 1L

        val dbCohort = cohortFactory!!.createCohort(apiCohort, user, workspaceId)

        assertThat(dbCohort.description).isEqualTo(apiCohort.getDescription())
        assertThat(dbCohort.name).isEqualTo(apiCohort.getName())
        assertThat(dbCohort.type).isEqualTo(apiCohort.getType())
        assertThat(dbCohort.criteria).isEqualTo(apiCohort.getCriteria())
        assertThat<User>(dbCohort.creator).isSameAs(user)
        assertThat(dbCohort.workspaceId).isEqualTo(workspaceId)
    }

    @Test
    fun duplicateCohort() {
        val originalCohort = Cohort()
        originalCohort.description = "desc"
        originalCohort.name = "name"
        originalCohort.type = "type"
        originalCohort.criteria = "criteria"
        originalCohort.workspaceId = 1L
        originalCohort.cohortReviews = setOf(mock(CohortReview::class.java))

        val user = mock(User::class.java)
        val workspace = mock(Workspace::class.java)
        doReturn(1L).`when`(workspace).workspaceId
        val dbCohort = cohortFactory!!.duplicateCohort("new name", user, workspace, originalCohort)

        assertThat(dbCohort.description).isEqualTo(originalCohort.description)
        assertThat(dbCohort.name).isEqualTo("new name")
        assertThat(dbCohort.type).isEqualTo(originalCohort.type)
        assertThat(dbCohort.criteria).isEqualTo(originalCohort.criteria)
        assertThat<User>(dbCohort.creator).isSameAs(user)
        assertThat(dbCohort.workspaceId).isEqualTo(originalCohort.workspaceId)
        assertThat(dbCohort.cohortReviews).isNull()
    }

    @Test
    fun duplicateCohortReview() {
        val now = Timestamp(Clock.systemUTC().millis())

        val originalCohortReview = CohortReview()
        originalCohortReview.cohortId = 1L
        originalCohortReview.cdrVersionId = 2L
        originalCohortReview.matchedParticipantCount = 3L
        originalCohortReview.reviewSize = 4L
        originalCohortReview.reviewedCount = 5L
        originalCohortReview.reviewStatusEnum = ReviewStatus.CREATED

        val cohort = mock(Cohort::class.java)
        doReturn(1L).`when`(cohort).cohortId
        doReturn(now).`when`(cohort).creationTime
        doReturn(now).`when`(cohort).lastModifiedTime
        val newReview = cohortFactory!!.duplicateCohortReview(originalCohortReview, cohort)

        assertThat(newReview.cohortId).isEqualTo(originalCohortReview.cohortId)
        assertThat(newReview.creationTime).isEqualTo(now)
        assertThat(newReview.lastModifiedTime).isEqualTo(now)
        assertThat(newReview.cdrVersionId).isEqualTo(originalCohortReview.cdrVersionId)
        assertThat(newReview.matchedParticipantCount)
                .isEqualTo(originalCohortReview.matchedParticipantCount)
        assertThat(newReview.reviewSize).isEqualTo(originalCohortReview.reviewSize)
        assertThat(newReview.reviewedCount).isEqualTo(originalCohortReview.reviewedCount)
        assertThat(newReview.reviewStatusEnum)
                .isEqualTo(originalCohortReview.reviewStatusEnum)
    }
}
