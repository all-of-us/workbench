package org.pmiops.workbench.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.mockito.ArgumentMatchers.anyList
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

import com.google.cloud.storage.BlobId
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSet
import java.sql.Timestamp
import java.time.Instant
import java.util.Arrays
import java.util.Collections
import javax.inject.Provider
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.pmiops.workbench.db.dao.UserRecentResourceService
import org.pmiops.workbench.db.model.Cohort
import org.pmiops.workbench.db.model.User
import org.pmiops.workbench.db.model.UserRecentResource
import org.pmiops.workbench.db.model.Workspace
import org.pmiops.workbench.firecloud.FireCloudService
import org.pmiops.workbench.firecloud.model.WorkspaceResponse
import org.pmiops.workbench.google.CloudStorageService
import org.pmiops.workbench.model.RecentResource
import org.pmiops.workbench.model.RecentResourceRequest
import org.pmiops.workbench.model.RecentResourceResponse
import org.pmiops.workbench.test.FakeClock
import org.pmiops.workbench.workspaces.WorkspaceService
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration::class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserMetricsControllerTest {

    @Mock
    private val cloudStorageService: CloudStorageService? = null
    @Mock
    private val userRecentResourceService: UserRecentResourceService? = null
    @Mock
    private val userProvider: Provider<User>? = null
    @Mock
    private val fireCloudService: FireCloudService? = null
    @Mock
    private val workspaceService: WorkspaceService? = null

    private var userMetricsController: UserMetricsController? = null
    private val clock = FakeClock(NOW)

    private var user: User? = null
    private var resource1: UserRecentResource? = null
    private var resource2: UserRecentResource? = null
    private var workspace2: Workspace? = null

    @Before
    fun setUp() {
        user = User()
        user!!.userId = 123L

        val cohort = Cohort()
        cohort.name = "Cohort Name"
        cohort.cohortId = 1L
        cohort.description = "Cohort description"
        cohort.lastModifiedTime = Timestamp(clock.millis())
        cohort.creationTime = Timestamp(clock.millis())

        val workspace1 = Workspace()
        workspace1.workspaceId = 1L
        workspace1.workspaceNamespace = "workspaceNamespace1"
        workspace1.firecloudName = "firecloudname1"

        workspace2 = Workspace()
        workspace2!!.workspaceId = 2L
        workspace2!!.workspaceNamespace = "workspaceNamespace2"
        workspace2!!.firecloudName = "firecloudName2"

        resource1 = UserRecentResource()
        resource1!!.notebookName = "gs://bucketFile/notebooks/notebook1.ipynb"
        resource1!!.cohort = null
        resource1!!.lastAccessDate = Timestamp(clock.millis())
        resource1!!.setUserId(user!!.userId)
        resource1!!.workspaceId = workspace1.workspaceId

        resource2 = UserRecentResource()
        resource2!!.notebookName = null
        resource2!!.cohort = cohort
        resource2!!.lastAccessDate = Timestamp(clock.millis() - 10000)
        resource2!!.setUserId(user!!.userId)
        resource2!!.workspaceId = workspace2!!.workspaceId

        val resource3 = UserRecentResource()
        resource3.notebookName = "gs://bucketFile/notebooks/notebook2.ipynb"
        resource3.cohort = null
        resource3.lastAccessDate = Timestamp(clock.millis() - 10000)
        resource3.setUserId(user!!.userId)
        resource3.workspaceId = workspace2!!.workspaceId

        val fcWorkspace = org.pmiops.workbench.firecloud.model.Workspace()
        fcWorkspace.setNamespace(workspace1.firecloudName)

        val fcWorkspace2 = org.pmiops.workbench.firecloud.model.Workspace()
        fcWorkspace.setNamespace(workspace2!!.firecloudName)

        val workspaceResponse = WorkspaceResponse()
        workspaceResponse.setAccessLevel("OWNER")
        workspaceResponse.setWorkspace(fcWorkspace)

        val workspaceResponse2 = WorkspaceResponse()
        workspaceResponse2.setAccessLevel("READER")
        workspaceResponse2.setWorkspace(fcWorkspace2)

        `when`(userProvider!!.get()).thenReturn(user)

        `when`(userRecentResourceService!!.findAllResourcesByUser(user!!.userId))
                .thenReturn(Arrays.asList<UserRecentResource>(resource1, resource2, resource3))

        `when`(workspaceService!!.findByWorkspaceId(workspace1.workspaceId)).thenReturn(workspace1)

        `when`(workspaceService.findByWorkspaceId(workspace2!!.workspaceId)).thenReturn(workspace2)

        `when`(workspaceService.getRequired(
                workspace2!!.workspaceNamespace, workspace2!!.firecloudName))
                .thenReturn(workspace2)

        `when`<Any>(fireCloudService!!.getWorkspace(
                workspace1.workspaceNamespace, workspace1.firecloudName))
                .thenReturn(workspaceResponse)

        `when`<Any>(fireCloudService.getWorkspace(
                workspace2!!.workspaceNamespace, workspace2!!.firecloudName))
                .thenReturn(workspaceResponse2)

        `when`(cloudStorageService!!.blobsExist(anyList()))
                .then { i ->
                    val ids = i.getArgument<List<BlobId>>(0)
                    if (ids.contains(null)) {
                        throw NullPointerException()
                    }
                    ImmutableSet.copyOf(ids)
                }

        userMetricsController = UserMetricsController(
                userProvider,
                userRecentResourceService,
                workspaceService,
                fireCloudService,
                cloudStorageService,
                clock)
        userMetricsController!!.setDistinctWorkspaceLimit(5)
    }

    @Test
    fun testGetUserRecentResourceFromRawBucket() {
        resource1!!.notebookName = "gs://bucketFile/notebook.ipynb"
        `when`(userRecentResourceService!!.findAllResourcesByUser(user!!.userId))
                .thenReturn(listOf<UserRecentResource>(resource1))

        val recentResources = userMetricsController!!.userRecentResources.body
        assertNotNull(recentResources)
        assertEquals(recentResources.get(0).getNotebook().getPath(), "gs://bucketFile/")
        assertEquals(recentResources.get(0).getNotebook().getName(), "notebook.ipynb")
    }

    @Test
    fun testGetUserRecentResourceWithDuplicatedNameInPath() {
        resource1!!.notebookName = "gs://bucketFile/nb.ipynb/intermediate/nb.ipynb"
        `when`(userRecentResourceService!!.findAllResourcesByUser(user!!.userId))
                .thenReturn(listOf<UserRecentResource>(resource1))

        val recentResources = userMetricsController!!.userRecentResources.body
        assertNotNull(recentResources)
        assertEquals(
                recentResources.get(0).getNotebook().getPath(), "gs://bucketFile/nb.ipynb/intermediate/")
        assertEquals(recentResources.get(0).getNotebook().getName(), "nb.ipynb")
    }

    @Test
    fun testGetUserRecentResourceWithSpacesInPath() {
        resource1!!.notebookName = "gs://bucketFile/note books/My favorite notebook.ipynb"
        `when`(userRecentResourceService!!.findAllResourcesByUser(user!!.userId))
                .thenReturn(listOf<UserRecentResource>(resource1))

        val recentResources = userMetricsController!!.userRecentResources.body
        assertNotNull(recentResources)
        assertEquals(recentResources.get(0).getNotebook().getPath(), "gs://bucketFile/note books/")
        assertEquals(recentResources.get(0).getNotebook().getName(), "My favorite notebook.ipynb")
    }

    @Test
    fun testGetUserRecentResourceInvalidURINotebookPath() {
        resource1!!.notebookName = "my local notebook directory: notebook.ipynb"
        `when`(userRecentResourceService!!.findAllResourcesByUser(user!!.userId))
                .thenReturn(listOf<UserRecentResource>(resource1))

        val recentResources = userMetricsController!!.userRecentResources.body
        assertNotNull(recentResources)
        assertEquals(recentResources.size(), 0)
    }

    @Test
    fun testGetUserRecentResourceNotebookPathEndsWithSlash() {
        resource1!!.notebookName = "gs://bucketFile/notebooks/notebook.ipynb/"
        `when`(userRecentResourceService!!.findAllResourcesByUser(user!!.userId))
                .thenReturn(listOf<UserRecentResource>(resource1))

        val recentResources = userMetricsController!!.userRecentResources.body
        assertNotNull(recentResources)
        assertNotNull(recentResources.get(0).getNotebook())
        assertEquals(
                recentResources.get(0).getNotebook().getPath(),
                "gs://bucketFile/notebooks/notebook.ipynb/")
        assertEquals(recentResources.get(0).getNotebook().getName(), "")
    }

    @Test
    fun testGetUserRecentResourceNonexistentNotebook() {
        resource1!!.notebookName = "gs://bkt/notebooks/notebook.ipynb"
        resource2!!.cohort = null
        resource2!!.notebookName = "gs://bkt/notebooks/not-found.ipynb"
        `when`(userRecentResourceService!!.findAllResourcesByUser(user!!.userId))
                .thenReturn(ImmutableList.of(resource1!!, resource2!!))
        `when`(cloudStorageService!!.blobsExist(anyList()))
                .thenReturn(ImmutableSet.of(BlobId.of("bkt", "notebooks/notebook.ipynb")))

        val recentResources = userMetricsController!!.userRecentResources.body
        assertNotNull(recentResources)
        assertEquals(recentResources.size(), 1)
        assertNotNull(recentResources.get(0).getNotebook())
        assertEquals(recentResources.get(0).getNotebook().getName(), "notebook.ipynb")
    }

    @Test
    fun testGetUserRecentResource() {
        val recentResources = userMetricsController!!.userRecentResources.body
        assertNotNull(recentResources)
        assertEquals(3, recentResources.size())
        assertNull(recentResources.get(0).getCohort())
        assertEquals(recentResources.get(0).getNotebook().getPath(), "gs://bucketFile/notebooks/")

        assertEquals(recentResources.get(0).getNotebook().getName(), "notebook1.ipynb")
        assertNotNull(recentResources.get(1).getCohort())
        assertEquals(recentResources.get(1).getCohort().getName(), "Cohort Name")
    }

    @Test
    fun testWorkspaceLimit() {
        userMetricsController!!.setDistinctWorkspaceLimit(1)
        val recentResources = userMetricsController!!.userRecentResources.body

        assertNotNull(recentResources)
        assertEquals(1, recentResources.size())
        assertNull(recentResources.get(0).getCohort())
        assertEquals(recentResources.get(0).getNotebook().getPath(), "gs://bucketFile/notebooks/")
    }

    @Test
    fun testDeleteResource() {
        val request = RecentResourceRequest()
        request.setNotebookName(resource1!!.notebookName)
        userMetricsController!!.deleteRecentResource(
                workspace2!!.workspaceNamespace, workspace2!!.firecloudName, request)
        verify<UserRecentResourceService>(userRecentResourceService)
                .deleteNotebookEntry(
                        workspace2!!.workspaceId, user!!.userId, resource1!!.notebookName)
    }

    @Test
    fun testUpdateRecentResource() {
        val now = Timestamp(clock.instant()!!.toEpochMilli())
        val mockUserRecentResource = UserRecentResource()
        mockUserRecentResource.cohort = null
        mockUserRecentResource.workspaceId = workspace2!!.workspaceId
        mockUserRecentResource.setUserId(user!!.userId)
        mockUserRecentResource.notebookName = "gs://newBucket/notebooks/notebook.ipynb"
        mockUserRecentResource.lastAccessDate = now
        `when`(userRecentResourceService!!.updateNotebookEntry(
                workspace2!!.workspaceId,
                user!!.userId,
                "gs://newBucket/notebooks/notebook.ipynb",
                now))
                .thenReturn(mockUserRecentResource)

        val request = RecentResourceRequest()
        request.setNotebookName("gs://newBucket/notebooks/notebook.ipynb")

        val addedEntry = userMetricsController!!
                .updateRecentResource(
                        workspace2!!.workspaceNamespace, workspace2!!.firecloudName, request)
                .body

        assertNotNull(addedEntry)
        assertEquals(addedEntry.getWorkspaceId() as Long, workspace2!!.workspaceId)
        assertNull(addedEntry.getCohort())
        assertNotNull(addedEntry.getNotebook())
        assertEquals(addedEntry.getNotebook().getName(), "notebook.ipynb")
        assertEquals(addedEntry.getNotebook().getPath(), "gs://newBucket/notebooks/")
    }

    companion object {
        private val NOW = Instant.now()
    }
}
