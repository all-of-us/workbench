package org.pmiops.workbench.db.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull

import java.sql.Timestamp
import java.time.Instant
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.pmiops.workbench.db.dao.CohortDao
import org.pmiops.workbench.db.dao.ConceptSetDao
import org.pmiops.workbench.db.dao.UserDao
import org.pmiops.workbench.db.dao.UserRecentResourceDao
import org.pmiops.workbench.db.dao.UserRecentResourceServiceImpl
import org.pmiops.workbench.db.dao.WorkspaceDao
import org.pmiops.workbench.db.model.Cohort
import org.pmiops.workbench.db.model.ConceptSet
import org.pmiops.workbench.db.model.User
import org.pmiops.workbench.db.model.UserRecentResource
import org.pmiops.workbench.db.model.Workspace
import org.pmiops.workbench.test.FakeClock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration::class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class UserRecentResourceServiceTest {

    internal var userRecentResourceService: UserRecentResourceServiceImpl

    @Autowired
    internal var userDao: UserDao? = null
    @Autowired
    internal var workspaceDao: WorkspaceDao? = null
    @Autowired
    internal var cohortDao: CohortDao? = null
    @Autowired
    internal var notebookCohortCacheDao: UserRecentResourceDao? = null
    @Autowired
    internal var conceptSetDao: ConceptSetDao? = null

    private val newUser = User()
    private val newWorkspace = Workspace()
    private var cohortId: Long? = null
    private var conceptSetId: Long? = null
    private val workspaceId = 1L
    private val userId = 1L
    private var clock: FakeClock? = null

    @Before
    fun setUp() {
        newUser.userId = userId
        userDao!!.save(newUser)
        newWorkspace.workspaceId = workspaceId
        workspaceDao!!.save(newWorkspace)
        val cohort = Cohort()
        cohort.workspaceId = workspaceId
        cohortId = cohortDao!!.save(cohort).cohortId
        val conceptSet = ConceptSet()
        conceptSet.workspaceId = workspaceId
        conceptSetId = conceptSetDao!!.save(conceptSet).conceptSetId
        userRecentResourceService = UserRecentResourceServiceImpl()
        userRecentResourceService.dao = notebookCohortCacheDao
        userRecentResourceService.setCohortDao(cohortDao)
        userRecentResourceService.setConceptSetDao(conceptSetDao)
        clock = FakeClock(NOW)
    }

    @Test
    fun testInsertCohortEntry() {
        userRecentResourceService.updateCohortEntry(
                workspaceId, userId, cohortId!!, Timestamp(clock!!.millis()))
        var rowsCount = userRecentResourceService.dao.count()
        assertEquals(rowsCount, 1)
        val cohort = Cohort()
        cohort.workspaceId = workspaceId
        cohortId = cohortDao!!.save(cohort).cohortId
        userRecentResourceService.updateCohortEntry(
                workspaceId, userId, cohortId!!, Timestamp(clock!!.millis()))
        rowsCount = userRecentResourceService.dao.count()
        assertEquals(rowsCount, 2)
    }

    @Test
    fun testInsertConceptSetEntry() {
        userRecentResourceService.updateConceptSetEntry(
                workspaceId, userId, conceptSetId!!, Timestamp(clock!!.millis()))
        var rowsCount = userRecentResourceService.dao.count()
        assertEquals(rowsCount, 1)
        val conceptSet = ConceptSet()
        conceptSet.workspaceId = workspaceId
        conceptSetId = conceptSetDao!!.save(conceptSet).conceptSetId
        userRecentResourceService.updateConceptSetEntry(
                workspaceId, userId, conceptSetId!!, Timestamp(clock!!.millis()))
        rowsCount = userRecentResourceService.dao.count()
        assertEquals(rowsCount, 2)
    }

    @Test
    fun testInsertNotebookEntry() {
        userRecentResourceService.updateNotebookEntry(
                workspaceId,
                userId,
                "gs://someDirectory/notebooks/notebook1",
                Timestamp(clock!!.millis()))
        var rowsCount = userRecentResourceService.dao.count()
        assertEquals(rowsCount, 1)
        userRecentResourceService.updateNotebookEntry(
                workspaceId,
                userId,
                "gs://someDirectory/notebooks/notebook2",
                Timestamp(clock!!.millis()))
        rowsCount = userRecentResourceService.dao.count()
        assertEquals(rowsCount, 2)
    }

    @Test
    fun testUpdateCohortAccessTime() {
        userRecentResourceService.updateCohortEntry(
                workspaceId, userId, cohortId!!, Timestamp(clock!!.millis()))
        var rowsCount = userRecentResourceService.dao.count()
        assertEquals(rowsCount, 1)
        clock!!.increment(20000)
        userRecentResourceService.updateCohortEntry(
                workspaceId, userId, cohortId!!, Timestamp(clock!!.millis()))
        rowsCount = userRecentResourceService.dao.count()
        assertEquals(rowsCount, 1)
    }

    @Test
    fun testUpdateConceptSetAccessTime() {
        userRecentResourceService.updateConceptSetEntry(
                workspaceId, userId, conceptSetId!!, Timestamp(clock!!.millis()))
        var rowsCount = userRecentResourceService.dao.count()
        assertEquals(rowsCount, 1)
        clock!!.increment(20000)
        userRecentResourceService.updateConceptSetEntry(
                workspaceId, userId, conceptSetId!!, Timestamp(clock!!.millis()))
        rowsCount = userRecentResourceService.dao.count()
        assertEquals(rowsCount, 1)
    }

    @Test
    fun testUpdateNotebookAccessTime() {

        userRecentResourceService.updateNotebookEntry(
                workspaceId,
                userId,
                "gs://someDirectory/notebooks/notebook1",
                Timestamp(clock!!.millis()))
        var rowsCount = userRecentResourceService.dao.count()
        assertEquals(rowsCount, 1)
        clock!!.increment(200)
        userRecentResourceService.updateNotebookEntry(
                workspaceId,
                userId,
                "gs://someDirectory/notebooks/notebook1",
                Timestamp(clock!!.millis()))
        rowsCount = userRecentResourceService.dao.count()
        assertEquals(rowsCount, 1)
    }

    @Test
    fun testUserLimit() {
        val newWorkspace = Workspace()
        newWorkspace.workspaceId = 2L
        workspaceDao!!.save(newWorkspace)
        userRecentResourceService.updateNotebookEntry(
                workspaceId,
                userId,
                "gs://someDirectory1/notebooks/notebook",
                Timestamp(clock!!.millis()))
        clock!!.increment(2000)
        userRecentResourceService.updateNotebookEntry(
                2L, userId, "notebooks", Timestamp(clock!!.millis()))
        userRecentResourceService.updateCohortEntry(
                workspaceId, userId, cohortId!!, Timestamp(clock!!.millis()))
        var count = userRecentResourceService.userEntryCount - 3
        while (count-- >= 0) {
            clock!!.increment(2000)
            userRecentResourceService.updateNotebookEntry(
                    workspaceId,
                    userId,
                    "gs://someDirectory1/notebooks/notebook$count",
                    Timestamp(clock!!.millis()))
        }

        clock!!.increment(2000)
        var rowsCount = userRecentResourceService.dao.count()
        assertEquals(rowsCount, userRecentResourceService.userEntryCount.toLong())

        userRecentResourceService.updateNotebookEntry(
                workspaceId,
                userId,
                "gs://someDirectory/notebooks/notebookExtra",
                Timestamp(clock!!.millis()))
        rowsCount = userRecentResourceService.dao.count()
        assertEquals(rowsCount, userRecentResourceService.userEntryCount.toLong())
        val cache = userRecentResourceService
                .dao
                .findByUserIdAndWorkspaceIdAndNotebookName(
                        workspaceId, userId, "gs://someDirectory1/notebooks/notebook")
        assertNull(cache)
    }

    //  We do test notebook deletion because it is a path reference
    //  We do not test cohort or concept deletion because these are fk refs with
    //  on delete cascade rule in place (no need to test db functionality)
    @Test
    fun testDeleteNotebookEntry() {
        userRecentResourceService.updateNotebookEntry(
                workspaceId,
                userId,
                "gs://someDirectory1/notebooks/notebook1",
                Timestamp(clock!!.millis()))
        var rowsCount = userRecentResourceService.dao.count()
        assertEquals(rowsCount, 1)
        userRecentResourceService.deleteNotebookEntry(
                workspaceId, userId, "gs://someDirectory1/notebooks/notebook1")
        rowsCount = userRecentResourceService.dao.count()
        assertEquals(rowsCount, 0)
    }

    @Test
    fun testDeleteNonExistentNotebookEntry() {
        var rowsCount = userRecentResourceService.dao.count()
        assertEquals(rowsCount, 0)
        userRecentResourceService.deleteNotebookEntry(
                workspaceId, userId, "gs://someDirectory1/notebooks/notebook")
        rowsCount = userRecentResourceService.dao.count()
        assertEquals(rowsCount, 0)
    }

    @Test
    fun testFindAllResources() {
        userRecentResourceService.updateNotebookEntry(
                workspaceId,
                userId,
                "gs://someDirectory1/notebooks/notebook1",
                Timestamp(clock!!.millis() - 10000))
        userRecentResourceService.updateNotebookEntry(
                workspaceId,
                userId,
                "gs://someDirectory1/notebooks/notebook2",
                Timestamp(clock!!.millis() + 10000))
        userRecentResourceService.updateCohortEntry(
                workspaceId, userId, cohortId!!, Timestamp(clock!!.millis()))
        newUser.userId = 78L
        userDao!!.save(newUser)
        userRecentResourceService.updateCohortEntry(
                workspaceId, 78L, cohortId!!, Timestamp(clock!!.millis()))
        val resources = userRecentResourceService.findAllResourcesByUser(userId)
        assertEquals(resources.size.toLong(), 3)
        assertEquals(resources[0].notebookName, "gs://someDirectory1/notebooks/notebook2")
        assertEquals(resources[1].cohort!!.cohortId, cohortId!!.toLong())
        assertEquals(resources[2].notebookName, "gs://someDirectory1/notebooks/notebook1")
    }

    companion object {
        private val NOW = Instant.now()
    }
}
