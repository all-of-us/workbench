package org.pmiops.workbench.api

import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.mockito.Mockito.`when`

import java.util.ArrayList
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.pmiops.workbench.db.dao.CohortAnnotationDefinitionDao
import org.pmiops.workbench.db.dao.CohortDao
import org.pmiops.workbench.db.dao.WorkspaceDao
import org.pmiops.workbench.db.model.Cohort
import org.pmiops.workbench.db.model.CohortAnnotationDefinition
import org.pmiops.workbench.db.model.Workspace
import org.pmiops.workbench.exceptions.ConflictException
import org.pmiops.workbench.exceptions.NotFoundException
import org.pmiops.workbench.model.AnnotationType
import org.pmiops.workbench.model.EmptyResponse
import org.pmiops.workbench.model.WorkspaceAccessLevel
import org.pmiops.workbench.workspaces.WorkspaceService
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
class CohortAnnotationDefinitionControllerTest {
    private var workspace: Workspace? = null
    private var cohort: Cohort? = null
    private var dbCohortAnnotationDefinition: CohortAnnotationDefinition? = null
    @Autowired
    internal var cohortAnnotationDefinitionDao: CohortAnnotationDefinitionDao? = null
    @Autowired
    internal var cohortDao: CohortDao? = null
    @Autowired
    internal var workspaceDao: WorkspaceDao? = null
    @Mock
    internal var workspaceService: WorkspaceService? = null
    internal var cohortAnnotationDefinitionController: CohortAnnotationDefinitionController

    @Before
    fun setUp() {
        cohortAnnotationDefinitionController = CohortAnnotationDefinitionController(
                cohortAnnotationDefinitionDao, cohortDao, workspaceService)

        workspace = Workspace()
        workspace!!.workspaceNamespace = NAMESPACE
        workspace!!.firecloudName = NAME
        workspaceDao!!.save<Workspace>(workspace)

        cohort = Cohort()
        cohort!!.workspaceId = workspace!!.workspaceId
        cohortDao!!.save<Cohort>(cohort)

        dbCohortAnnotationDefinition = CohortAnnotationDefinition()
                .cohortId(cohort!!.cohortId)
                .annotationTypeEnum(AnnotationType.STRING)
                .columnName(EXISTING_COLUMN_NAME)
                .version(0)
        cohortAnnotationDefinitionDao!!.save<CohortAnnotationDefinition>(dbCohortAnnotationDefinition)
    }

    @Test
    @Throws(Exception::class)
    fun createCohortAnnotationDefinition_BadCohortId() {
        setupWorkspaceServiceMock()

        try {
            cohortAnnotationDefinitionController.createCohortAnnotationDefinition(
                    NAMESPACE, NAME, 0L, org.pmiops.workbench.model.CohortAnnotationDefinition())
            fail("Should have thrown a NotFoundException!")
        } catch (e: NotFoundException) {
            assertEquals("Not Found: No Cohort exists for cohortId: " + 0L, e.message)
        }

    }

    @Test
    @Throws(Exception::class)
    fun createCohortAnnotationDefinition_BadWorkspace() {
        setupBadWorkspaceServiceMock()

        try {
            cohortAnnotationDefinitionController.createCohortAnnotationDefinition(
                    NAMESPACE,
                    NAME,
                    cohort!!.cohortId,
                    org.pmiops.workbench.model.CohortAnnotationDefinition())
            fail("Should have thrown a NotFoundException!")
        } catch (e: NotFoundException) {
            assertEquals(
                    "Not Found: No workspace matching workspaceNamespace: "
                            + NAMESPACE
                            + ", workspaceId: "
                            + NAME,
                    e.message)
        }

    }

    @Test
    @Throws(Exception::class)
    fun createCohortAnnotationDefinition_NameConflict() {
        setupWorkspaceServiceMock()

        val request = org.pmiops.workbench.model.CohortAnnotationDefinition()
                .cohortAnnotationDefinitionId(
                        dbCohortAnnotationDefinition!!.cohortAnnotationDefinitionId)
                .cohortId(cohort!!.cohortId)
                .columnName(dbCohortAnnotationDefinition!!.columnName)
                .annotationType(AnnotationType.STRING)
                .enumValues(ArrayList<E>())
                .etag(Etags.fromVersion(0))

        try {
            cohortAnnotationDefinitionController.createCohortAnnotationDefinition(
                    NAMESPACE, NAME, cohort!!.cohortId, request)
            fail("Should have thrown a ConflictException!")
        } catch (e: ConflictException) {
            assertEquals(
                    "Conflict: Cohort Annotation Definition name exists for: " + dbCohortAnnotationDefinition!!.columnName!!,
                    e.message)
        }

    }

    @Test
    @Throws(Exception::class)
    fun createCohortAnnotationDefinition() {
        setupWorkspaceServiceMock()

        val request = org.pmiops.workbench.model.CohortAnnotationDefinition()
                .cohortId(cohort!!.cohortId)
                .columnName(NEW_COLUMN_NAME)
                .annotationType(AnnotationType.STRING)
                .enumValues(ArrayList<E>())
                .etag(Etags.fromVersion(0))

        val response = cohortAnnotationDefinitionController
                .createCohortAnnotationDefinition(NAMESPACE, NAME, cohort!!.cohortId, request)
                .body
        val expectedResponse = org.pmiops.workbench.model.CohortAnnotationDefinition()
                .cohortAnnotationDefinitionId(response.getCohortAnnotationDefinitionId())
                .cohortId(cohort!!.cohortId)
                .columnName(NEW_COLUMN_NAME)
                .annotationType(AnnotationType.STRING)
                .enumValues(ArrayList<E>())
                .etag(Etags.fromVersion(0))
        assertEquals(expectedResponse, response)
    }

    @Test
    @Throws(Exception::class)
    fun updateCohortAnnotationDefinition_BadCohortId() {
        setupWorkspaceServiceMock()

        val request = org.pmiops.workbench.model.CohortAnnotationDefinition().columnName("ignore")

        try {
            cohortAnnotationDefinitionController.updateCohortAnnotationDefinition(
                    NAMESPACE,
                    NAME,
                    99L,
                    dbCohortAnnotationDefinition!!.cohortAnnotationDefinitionId,
                    request)
            fail("Should have thrown a NotFoundException!")
        } catch (e: NotFoundException) {
            assertEquals("Not Found: No Cohort exists for cohortId: " + 99L, e.message)
        }

    }

    @Test
    @Throws(Exception::class)
    fun updateCohortAnnotationDefinition_BadWorkspace() {
        setupBadWorkspaceServiceMock()

        val request = org.pmiops.workbench.model.CohortAnnotationDefinition().columnName("ignore")

        try {
            cohortAnnotationDefinitionController.updateCohortAnnotationDefinition(
                    NAMESPACE,
                    NAME,
                    cohort!!.cohortId,
                    dbCohortAnnotationDefinition!!.cohortAnnotationDefinitionId,
                    request)
            fail("Should have thrown a NotFoundException!")
        } catch (e: NotFoundException) {
            assertEquals(
                    "Not Found: No workspace matching workspaceNamespace: "
                            + NAMESPACE
                            + ", workspaceId: "
                            + NAME,
                    e.message)
        }

    }

    @Test
    @Throws(Exception::class)
    fun updateCohortAnnotationDefinition_BadAnnotationDefinitionId() {
        setupWorkspaceServiceMock()

        val request = org.pmiops.workbench.model.CohortAnnotationDefinition().columnName("ignore")

        try {
            cohortAnnotationDefinitionController.updateCohortAnnotationDefinition(
                    NAMESPACE, NAME, cohort!!.cohortId, 99L, request)
            fail("Should have thrown a NotFoundException!")
        } catch (e: NotFoundException) {
            assertEquals(
                    "Not Found: No Cohort Annotation Definition exists for annotationDefinitionId: " + 99L,
                    e.message)
        }

    }

    @Test
    @Throws(Exception::class)
    fun updateCohortAnnotationDefinition_NameConflict() {
        setupWorkspaceServiceMock()

        val request = org.pmiops.workbench.model.CohortAnnotationDefinition()
                .columnName(EXISTING_COLUMN_NAME)
                .etag(Etags.fromVersion(0))

        try {
            cohortAnnotationDefinitionController.updateCohortAnnotationDefinition(
                    NAMESPACE,
                    NAME,
                    cohort!!.cohortId,
                    dbCohortAnnotationDefinition!!.cohortAnnotationDefinitionId,
                    request)
            fail("Should have thrown a ConflictException!")
        } catch (e: ConflictException) {
            assertEquals(
                    "Conflict: Cohort Annotation Definition name exists for: $EXISTING_COLUMN_NAME",
                    e.message)
        }

    }

    @Test
    @Throws(Exception::class)
    fun updateCohortAnnotationDefinition() {
        setupWorkspaceServiceMock()

        val request = org.pmiops.workbench.model.CohortAnnotationDefinition()
                .cohortAnnotationDefinitionId(
                        dbCohortAnnotationDefinition!!.cohortAnnotationDefinitionId)
                .columnName(NEW_COLUMN_NAME)
                .etag(Etags.fromVersion(0))
                .cohortId(cohort!!.cohortId)

        val expectedResponse = org.pmiops.workbench.model.CohortAnnotationDefinition()
                .cohortAnnotationDefinitionId(
                        dbCohortAnnotationDefinition!!.cohortAnnotationDefinitionId)
                .cohortId(cohort!!.cohortId)
                .columnName(NEW_COLUMN_NAME)
                .annotationType(AnnotationType.STRING)
                .enumValues(ArrayList<E>())
                .etag(Etags.fromVersion(0))

        val responseDefinition = cohortAnnotationDefinitionController
                .updateCohortAnnotationDefinition(
                        NAMESPACE,
                        NAME,
                        cohort!!.cohortId,
                        dbCohortAnnotationDefinition!!.cohortAnnotationDefinitionId,
                        request)
                .body

        assertEquals(expectedResponse, responseDefinition)
    }

    @Test
    @Throws(Exception::class)
    fun deleteCohortAnnotationDefinition_BadCohortId() {
        setupWorkspaceServiceMock()

        try {
            cohortAnnotationDefinitionController.deleteCohortAnnotationDefinition(
                    NAMESPACE, NAME, 99L, dbCohortAnnotationDefinition!!.cohortAnnotationDefinitionId)
            fail("Should have thrown a NotFoundException!")
        } catch (e: NotFoundException) {
            assertEquals("Not Found: No Cohort exists for cohortId: " + 99L, e.message)
        }

    }

    @Test
    @Throws(Exception::class)
    fun deleteCohortAnnotationDefinition_BadWorkspace() {
        setupBadWorkspaceServiceMock()

        try {
            cohortAnnotationDefinitionController.deleteCohortAnnotationDefinition(
                    NAMESPACE,
                    NAME,
                    cohort!!.cohortId,
                    dbCohortAnnotationDefinition!!.cohortAnnotationDefinitionId)
            fail("Should have thrown a NotFoundException!")
        } catch (e: NotFoundException) {
            assertEquals(
                    "Not Found: No workspace matching workspaceNamespace: "
                            + NAMESPACE
                            + ", workspaceId: "
                            + NAME,
                    e.message)
        }

    }

    @Test
    @Throws(Exception::class)
    fun deleteCohortAnnotationDefinition_BadAnnotationDefinitionId() {
        setupWorkspaceServiceMock()

        try {
            cohortAnnotationDefinitionController.deleteCohortAnnotationDefinition(
                    NAMESPACE, NAME, cohort!!.cohortId, 99L)
            fail("Should have thrown a NotFoundException!")
        } catch (e: NotFoundException) {
            assertEquals(
                    "Not Found: No Cohort Annotation Definition exists for annotationDefinitionId: " + 99L,
                    e.message)
        }

    }

    @Test
    @Throws(Exception::class)
    fun deleteCohortAnnotationDefinition() {
        setupWorkspaceServiceMock()

        val response = cohortAnnotationDefinitionController
                .deleteCohortAnnotationDefinition(
                        NAMESPACE,
                        NAME,
                        cohort!!.cohortId,
                        dbCohortAnnotationDefinition!!.cohortAnnotationDefinitionId)
                .body

        assertEquals(EmptyResponse(), response)
    }

    @Test
    @Throws(Exception::class)
    fun getCohortAnnotationDefinition_NotFoundCohort() {
        setupWorkspaceServiceMock()

        try {
            cohortAnnotationDefinitionController.getCohortAnnotationDefinition(
                    NAMESPACE, NAME, 99L, dbCohortAnnotationDefinition!!.cohortAnnotationDefinitionId)
            fail("Should have thrown a NotFoundException!")
        } catch (e: NotFoundException) {
            assertEquals("Not Found: No Cohort exists for cohortId: " + 99L, e.message)
        }

    }

    @Test
    @Throws(Exception::class)
    fun getCohortAnnotationDefinition_NotFoundWorkspace() {
        setupBadWorkspaceServiceMock()

        try {
            cohortAnnotationDefinitionController.getCohortAnnotationDefinition(
                    NAMESPACE,
                    NAME,
                    cohort!!.cohortId,
                    dbCohortAnnotationDefinition!!.cohortAnnotationDefinitionId)
            fail("Should have thrown a NotFoundException!")
        } catch (e: NotFoundException) {
            assertEquals(
                    "Not Found: No workspace matching workspaceNamespace: "
                            + NAMESPACE
                            + ", workspaceId: "
                            + NAME,
                    e.message)
        }

    }

    @Test
    @Throws(Exception::class)
    fun getCohortAnnotationDefinition_NotFoundAnnotationDefinition() {
        setupWorkspaceServiceMock()

        try {
            cohortAnnotationDefinitionController.getCohortAnnotationDefinition(
                    NAMESPACE, NAME, cohort!!.cohortId, 99L)
            fail("Should have thrown a NotFoundException!")
        } catch (e: NotFoundException) {
            assertEquals(
                    "Not Found: No Cohort Annotation Definition exists for annotationDefinitionId: " + 99L,
                    e.message)
        }

    }

    @Test
    @Throws(Exception::class)
    fun getCohortAnnotationDefinition() {
        setupWorkspaceServiceMock()

        val responseDefinition = cohortAnnotationDefinitionController
                .getCohortAnnotationDefinition(
                        NAMESPACE,
                        NAME,
                        cohort!!.cohortId,
                        dbCohortAnnotationDefinition!!.cohortAnnotationDefinitionId)
                .body

        val expectedResponse = org.pmiops.workbench.model.CohortAnnotationDefinition()
                .cohortAnnotationDefinitionId(
                        dbCohortAnnotationDefinition!!.cohortAnnotationDefinitionId)
                .cohortId(cohort!!.cohortId)
                .annotationType(AnnotationType.STRING)
                .columnName(dbCohortAnnotationDefinition!!.columnName)
                .enumValues(ArrayList<E>())
                .etag(Etags.fromVersion(0))

        assertEquals(expectedResponse, responseDefinition)
    }

    @Test
    @Throws(Exception::class)
    fun getCohortAnnotationDefinitions_NotFoundCohort() {
        setupWorkspaceServiceMock()

        try {
            cohortAnnotationDefinitionController.getCohortAnnotationDefinitions(NAMESPACE, NAME, 99L)
            fail("Should have thrown a NotFoundException!")
        } catch (e: NotFoundException) {
            assertEquals("Not Found: No Cohort exists for cohortId: " + 99L, e.message)
        }

    }

    @Test
    @Throws(Exception::class)
    fun getCohortAnnotationDefinitions_NotFoundWorkspace() {
        setupBadWorkspaceServiceMock()

        try {
            cohortAnnotationDefinitionController.getCohortAnnotationDefinitions(
                    NAMESPACE, NAME, cohort!!.cohortId)
            fail("Should have thrown a NotFoundException!")
        } catch (e: NotFoundException) {
            assertEquals(
                    "Not Found: No workspace matching workspaceNamespace: "
                            + NAMESPACE
                            + ", workspaceId: "
                            + NAME,
                    e.message)
        }

    }

    @Test
    @Throws(Exception::class)
    fun getCohortAnnotationDefinitions() {
        setupWorkspaceServiceMock()

        val responseDefinition = cohortAnnotationDefinitionController
                .getCohortAnnotationDefinitions(NAMESPACE, NAME, cohort!!.cohortId)
                .body

        val expectedResponse = org.pmiops.workbench.model.CohortAnnotationDefinition()
                .cohortAnnotationDefinitionId(
                        dbCohortAnnotationDefinition!!.cohortAnnotationDefinitionId)
                .cohortId(cohort!!.cohortId)
                .annotationType(AnnotationType.STRING)
                .columnName(dbCohortAnnotationDefinition!!.columnName)
                .enumValues(ArrayList<E>())
                .etag(Etags.fromVersion(0))

        assertEquals(1, responseDefinition.getItems().size())
        assertEquals(expectedResponse, responseDefinition.getItems().get(0))
    }

    private fun setupWorkspaceServiceMock() {
        val mockWorkspace = Workspace()
        mockWorkspace.workspaceNamespace = NAMESPACE
        mockWorkspace.firecloudName = NAME
        mockWorkspace.workspaceId = workspace!!.workspaceId

        `when`<Any>(workspaceService!!.enforceWorkspaceAccessLevel(NAMESPACE, NAME, WorkspaceAccessLevel.WRITER))
                .thenReturn(WorkspaceAccessLevel.OWNER)
        `when`(workspaceService!!.getRequired(NAMESPACE, NAME)).thenReturn(mockWorkspace)
    }

    private fun setupBadWorkspaceServiceMock() {
        val mockWorkspace = Workspace()
        mockWorkspace.workspaceNamespace = NAMESPACE
        mockWorkspace.firecloudName = NAME
        mockWorkspace.workspaceId = 0L

        `when`<Any>(workspaceService!!.enforceWorkspaceAccessLevel(NAMESPACE, NAME, WorkspaceAccessLevel.WRITER))
                .thenReturn(WorkspaceAccessLevel.OWNER)
        `when`(workspaceService!!.getRequired(NAMESPACE, NAME)).thenReturn(mockWorkspace)
    }

    companion object {

        private val NAMESPACE = "aou-test"
        private val NAME = "test"
        private val EXISTING_COLUMN_NAME = "testing"
        private val NEW_COLUMN_NAME = "new_column"
    }
}
