package org.pmiops.workbench.db.dao

import org.junit.Assert.assertEquals

import java.sql.Timestamp
import java.util.Calendar
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.pmiops.workbench.db.model.Cohort
import org.pmiops.workbench.db.model.CohortReview
import org.pmiops.workbench.db.model.StorageEnums
import org.pmiops.workbench.db.model.Workspace
import org.pmiops.workbench.model.WorkspaceActiveStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.transaction.annotation.Transactional

@RunWith(SpringRunner::class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
class CohortReviewDaoTest {
    @Autowired
    internal var workspaceDao: WorkspaceDao? = null
    @Autowired
    internal var cohortDao: CohortDao? = null
    @Autowired
    internal var cohortReviewDao: CohortReviewDao? = null
    private var cohortReview: CohortReview? = null
    private var cohortId: Long = 0

    @Before
    @Throws(Exception::class)
    fun setUp() {
        val cohort = Cohort()
        val workspace = Workspace()
        workspace.workspaceNamespace = "namespace"
        workspace.firecloudName = "firecloudName"
        workspace.workspaceActiveStatusEnum = WorkspaceActiveStatus.ACTIVE
        cohort.workspaceId = workspaceDao!!.save(workspace).workspaceId
        cohortId = cohortDao!!.save(cohort).cohortId
        cohortReview = cohortReviewDao!!.save(createCohortReview())
    }

    @Test
    @Throws(Exception::class)
    fun save() {
        assertEquals(cohortReview, cohortReviewDao!!.findOne(cohortReview!!.cohortReviewId))
    }

    @Test
    @Throws(Exception::class)
    fun update() {
        cohortReview = cohortReviewDao!!.findOne(cohortReview!!.cohortReviewId)
        cohortReview!!.reviewedCount = 3
        cohortReviewDao!!.saveAndFlush<CohortReview>(cohortReview)
        assertEquals(cohortReview, cohortReviewDao!!.findOne(cohortReview!!.cohortReviewId))
    }

    @Test
    @Throws(Exception::class)
    fun findCohortReviewByCohortIdAndCdrVersionId() {
        assertEquals(
                cohortReview,
                cohortReviewDao!!.findCohortReviewByCohortIdAndCdrVersionId(
                        cohortReview!!.cohortId, cohortReview!!.cdrVersionId))
    }

    @Test
    @Throws(Exception::class)
    fun findByFirecloudNameAndActiveStatus() {
        assertEquals(
                cohortReview,
                cohortReviewDao!!
                        .findByFirecloudNameAndActiveStatus(
                                "namespace",
                                "firecloudName",
                                StorageEnums.workspaceActiveStatusToStorage(WorkspaceActiveStatus.ACTIVE)!!)[0])
    }

    private fun createCohortReview(): CohortReview {
        return CohortReview()
                .cohortId(cohortId)
                .cdrVersionId(CDR_VERSION_ID)
                .creationTime(Timestamp(Calendar.getInstance().timeInMillis))
                .lastModifiedTime(Timestamp(Calendar.getInstance().timeInMillis))
                .matchedParticipantCount(100)
                .reviewedCount(10)
                .cohortDefinition("{'name':'test'}")
                .cohortName("test")
    }

    companion object {

        private val CDR_VERSION_ID: Long = 1
    }
}
