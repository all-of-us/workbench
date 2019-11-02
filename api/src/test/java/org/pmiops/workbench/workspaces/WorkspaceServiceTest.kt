package org.pmiops.workbench.workspaces

import org.assertj.core.api.Assertions.assertThat
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy

import java.sql.Timestamp
import java.time.Clock
import java.time.Instant
import java.util.ArrayList
import java.util.EnumSet
import java.util.HashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.stream.Collectors
import javax.inject.Provider
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.pmiops.workbench.cohorts.CohortCloningService
import org.pmiops.workbench.conceptset.ConceptSetService
import org.pmiops.workbench.db.dao.DataSetService
import org.pmiops.workbench.db.dao.UserDao
import org.pmiops.workbench.db.dao.UserRecentWorkspaceDao
import org.pmiops.workbench.db.dao.WorkspaceDao
import org.pmiops.workbench.db.model.User
import org.pmiops.workbench.db.model.UserRecentWorkspace
import org.pmiops.workbench.firecloud.FireCloudService
import org.pmiops.workbench.firecloud.model.Workspace
import org.pmiops.workbench.firecloud.model.WorkspaceACL
import org.pmiops.workbench.firecloud.model.WorkspaceAccessEntry
import org.pmiops.workbench.firecloud.model.WorkspaceResponse
import org.pmiops.workbench.model.WorkspaceAccessLevel
import org.pmiops.workbench.model.WorkspaceActiveStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration::class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class WorkspaceServiceTest {

    @Autowired
    private val workspaceDao: WorkspaceDao? = null
    @Autowired
    private val userDao: UserDao? = null
    @Autowired
    private val userRecentWorkspaceDao: UserRecentWorkspaceDao? = null

    @Mock
    private val mockCohortCloningService: CohortCloningService? = null
    @Mock
    private val mockConceptSetService: ConceptSetService? = null
    @Mock
    private val mockDataSetService: DataSetService? = null
    @Mock
    private val mockUserProvider: Provider<User>? = null
    @Mock
    private val mockFireCloudService: FireCloudService? = null
    @Mock
    private val mockClock: Clock? = null

    private var workspaceService: WorkspaceService? = null

    private val mockWorkspaceResponses = ArrayList<WorkspaceResponse>()
    private val mockWorkspaces = ArrayList<org.pmiops.workbench.db.model.Workspace>()
    private val workspaceIdIncrementer = AtomicLong(1)
    private val NOW = Instant.now()
    private val USER_ID = 1L
    private val DEFAULT_USER_EMAIL = "mock@mock.com"
    private val DEFAULT_WORKSPACE_NAMESPACE = "namespace"

    @TestConfiguration
    internal class Configuration

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        workspaceService = WorkspaceServiceImpl(
                mockClock,
                mockCohortCloningService,
                mockConceptSetService,
                mockDataSetService,
                mockFireCloudService,
                userDao,
                mockUserProvider,
                userRecentWorkspaceDao,
                workspaceDao)

        mockWorkspaceResponses.clear()
        mockWorkspaces.clear()
        addMockedWorkspace(
                workspaceIdIncrementer.getAndIncrement(),
                "reader",
                DEFAULT_WORKSPACE_NAMESPACE,
                WorkspaceAccessLevel.READER,
                WorkspaceActiveStatus.ACTIVE)
        addMockedWorkspace(
                workspaceIdIncrementer.getAndIncrement(),
                "writer",
                DEFAULT_WORKSPACE_NAMESPACE,
                WorkspaceAccessLevel.WRITER,
                WorkspaceActiveStatus.ACTIVE)
        addMockedWorkspace(
                workspaceIdIncrementer.getAndIncrement(),
                "owner",
                DEFAULT_WORKSPACE_NAMESPACE,
                WorkspaceAccessLevel.OWNER,
                WorkspaceActiveStatus.ACTIVE)
        addMockedWorkspace(
                workspaceIdIncrementer.getAndIncrement(),
                "extra",
                DEFAULT_WORKSPACE_NAMESPACE,
                WorkspaceAccessLevel.OWNER,
                WorkspaceActiveStatus.ACTIVE)
        addMockedWorkspace(
                workspaceIdIncrementer.getAndIncrement(),
                "another_extra",
                DEFAULT_WORKSPACE_NAMESPACE,
                WorkspaceAccessLevel.OWNER,
                WorkspaceActiveStatus.ACTIVE)

        doReturn(mockWorkspaceResponses).`when`<FireCloudService>(mockFireCloudService).getWorkspaces(any())
        val mockUser = mock(User::class.java)
        doReturn(mockUser).`when`<Provider<User>>(mockUserProvider).get()
        doReturn(DEFAULT_USER_EMAIL).`when`(mockUser).email
        doReturn(USER_ID).`when`(mockUser).userId
    }

    private fun mockFirecloudWorkspaceResponse(
            workspaceId: String,
            workspaceName: String,
            workspaceNamespace: String,
            accessLevel: WorkspaceAccessLevel): WorkspaceResponse {
        val mockWorkspace = mock(Workspace::class.java)
        doReturn(workspaceNamespace).`when`<Any>(mockWorkspace).getNamespace()
        doReturn(workspaceName).`when`<Any>(mockWorkspace).getName()
        doReturn(workspaceId).`when`<Any>(mockWorkspace).getWorkspaceId()
        val mockWorkspaceResponse = mock(WorkspaceResponse::class.java)
        doReturn(mockWorkspace).`when`<Any>(mockWorkspaceResponse).getWorkspace()
        doReturn(accessLevel.toString()).`when`<Any>(mockWorkspaceResponse).getAccessLevel()
        return mockWorkspaceResponse
    }

    private fun buildDbWorkspace(
            dbId: Long, name: String, namespace: String, activeStatus: WorkspaceActiveStatus): org.pmiops.workbench.db.model.Workspace {
        val workspace = org.pmiops.workbench.db.model.Workspace()
        val nowTimestamp = Timestamp.from(NOW)
        workspace.lastModifiedTime = nowTimestamp
        workspace.creationTime = nowTimestamp
        workspace.name = name
        workspace.workspaceId = dbId
        workspace.workspaceNamespace = namespace
        workspace.workspaceActiveStatusEnum = activeStatus
        workspace.firecloudName = name
        workspace.firecloudUuid = java.lang.Long.toString(dbId)
        return workspace
    }

    private fun addMockedWorkspace(
            workspaceId: Long,
            workspaceName: String,
            workspaceNamespace: String,
            accessLevel: WorkspaceAccessLevel,
            activeStatus: WorkspaceActiveStatus): org.pmiops.workbench.db.model.Workspace {

        val workspaceAccessLevelResponse = spy<Class<WorkspaceACL>>(WorkspaceACL::class.java!!)
        val acl = HashMap<String, WorkspaceAccessEntry>()
        val accessLevelEntry = WorkspaceAccessEntry().accessLevel(accessLevel.toString())
        acl[DEFAULT_USER_EMAIL] = accessLevelEntry
        doReturn(acl).`when`<Any>(workspaceAccessLevelResponse).getAcl()
        workspaceAccessLevelResponse.setAcl(acl)
        val mockWorkspaceResponse = mockFirecloudWorkspaceResponse(
                java.lang.Long.toString(workspaceId), workspaceName, workspaceNamespace, accessLevel)
        doReturn(workspaceAccessLevelResponse)
                .`when`<FireCloudService>(mockFireCloudService)
                .getWorkspaceAcl(workspaceNamespace, workspaceName)
        mockWorkspaceResponses.add(mockWorkspaceResponse)

        val dbWorkspace = workspaceDao!!.save(
                buildDbWorkspace(
                        workspaceId,
                        mockWorkspaceResponse.getWorkspace().getName(),
                        workspaceNamespace,
                        activeStatus))

        mockWorkspaces.add(dbWorkspace)
        return dbWorkspace
    }

    @Test
    fun getWorkspaces() {
        assertThat<List<WorkspaceResponse>>(workspaceService!!.workspaces).hasSize(5)
    }

    @Test
    fun getWorkspaces_skipPending() {
        val currentWorkspacesSize = workspaceService!!.workspaces.size

        addMockedWorkspace(
                workspaceIdIncrementer.getAndIncrement(),
                "inactive",
                DEFAULT_WORKSPACE_NAMESPACE,
                WorkspaceAccessLevel.OWNER,
                WorkspaceActiveStatus.PENDING_DELETION_POST_1PPW_MIGRATION)
        assertThat(workspaceService!!.workspaces.size).isEqualTo(currentWorkspacesSize)
    }

    @Test
    fun getWorkspaces_skipDeleted() {
        val currentWorkspacesSize = workspaceService!!.workspaces.size

        addMockedWorkspace(
                workspaceIdIncrementer.getAndIncrement(),
                "deleted",
                DEFAULT_WORKSPACE_NAMESPACE,
                WorkspaceAccessLevel.OWNER,
                WorkspaceActiveStatus.DELETED)
        assertThat(workspaceService!!.workspaces.size).isEqualTo(currentWorkspacesSize)
    }

    @Test
    fun activeStatus() {
        EnumSet.allOf(WorkspaceActiveStatus::class.java)
                .forEach { status ->
                    assertThat(
                            buildDbWorkspace(
                                    workspaceIdIncrementer.getAndIncrement(),
                                    "1",
                                    DEFAULT_WORKSPACE_NAMESPACE,
                                    status)
                                    .workspaceActiveStatusEnum)
                            .isEqualTo(status)
                }
    }

    @Test
    fun updateRecentWorkspaces() {
        mockWorkspaces.forEach { workspace ->
            // Need a new 'now' each time or else we won't have lastAccessDates that are different
            // from each other
            workspaceService!!.updateRecentWorkspaces(
                    workspace,
                    USER_ID,
                    Timestamp.from(NOW.minusSeconds(mockWorkspaces.size - workspace.workspaceId)))
        }
        val recentWorkspaces = workspaceService!!.recentWorkspaces
        assertThat(recentWorkspaces.size).isEqualTo(WorkspaceServiceImpl.RECENT_WORKSPACE_COUNT)

        val actualIds = recentWorkspaces.stream()
                .map<Long>(Function<UserRecentWorkspace, Long> { it.getWorkspaceId() })
                .collect<List<Long>, Any>(Collectors.toList())
        val expectedIds = mockWorkspaces
                .subList(
                        mockWorkspaces.size - WorkspaceServiceImpl.RECENT_WORKSPACE_COUNT,
                        mockWorkspaces.size)
                .stream()
                .map<Long>(Function<Workspace, Long> { it.getWorkspaceId() })
                .collect<List<Long>, Any>(Collectors.toList())
        assertThat(actualIds).containsAll(expectedIds)
    }

    @Test
    fun updateRecentWorkspaces_multipleUsers() {
        val OTHER_USER_ID = 2L
        workspaceService!!.updateRecentWorkspaces(
                mockWorkspaces[0], OTHER_USER_ID, Timestamp.from(NOW))
        mockWorkspaces.forEach { workspace ->
            // Need a new 'now' each time or else we won't have lastAccessDates that are different
            // from each other
            workspaceService!!.updateRecentWorkspaces(
                    workspace,
                    USER_ID,
                    Timestamp.from(NOW.minusSeconds(mockWorkspaces.size - workspace.workspaceId)))
        }
        val recentWorkspaces = workspaceService!!.recentWorkspaces

        assertThat(recentWorkspaces.size).isEqualTo(4)
        recentWorkspaces.forEach { userRecentWorkspace ->
            assertThat(userRecentWorkspace.id)
                    .isNotEqualTo(userRecentWorkspace.workspaceId)
        }

        val actualIds = recentWorkspaces.stream()
                .map<Long>(Function<UserRecentWorkspace, Long> { it.getWorkspaceId() })
                .collect<List<Long>, Any>(Collectors.toList())
        val expectedIds = mockWorkspaces
                .subList(
                        mockWorkspaces.size - WorkspaceServiceImpl.RECENT_WORKSPACE_COUNT,
                        mockWorkspaces.size)
                .stream()
                .map<Long>(Function<Workspace, Long> { it.getWorkspaceId() })
                .collect<List<Long>, Any>(Collectors.toList())
        assertThat(actualIds).containsAll(expectedIds)

        val mockUser = mock(User::class.java)
        doReturn(mockUser).`when`<Provider<User>>(mockUserProvider).get()
        doReturn(DEFAULT_USER_EMAIL).`when`(mockUser).email
        doReturn(OTHER_USER_ID).`when`(mockUser).userId
        val otherRecentWorkspaces = workspaceService!!.recentWorkspaces
        assertThat(otherRecentWorkspaces.size).isEqualTo(1)
        assertThat(otherRecentWorkspaces[0].workspaceId)
                .isEqualTo(mockWorkspaces[0].workspaceId)
    }

    @Test
    fun updateRecentWorkspaces_flipFlop() {
        workspaceService!!.updateRecentWorkspaces(
                mockWorkspaces[0], USER_ID, Timestamp.from(NOW.minusSeconds(4)))
        workspaceService!!.updateRecentWorkspaces(
                mockWorkspaces[1], USER_ID, Timestamp.from(NOW.minusSeconds(3)))
        workspaceService!!.updateRecentWorkspaces(
                mockWorkspaces[0], USER_ID, Timestamp.from(NOW.minusSeconds(2)))
        workspaceService!!.updateRecentWorkspaces(
                mockWorkspaces[1], USER_ID, Timestamp.from(NOW.minusSeconds(1)))
        workspaceService!!.updateRecentWorkspaces(mockWorkspaces[0], USER_ID, Timestamp.from(NOW))

        val recentWorkspaces = workspaceService!!.recentWorkspaces
        assertThat(recentWorkspaces.size).isEqualTo(2)
        val actualIds = recentWorkspaces.stream()
                .map<Long>(Function<UserRecentWorkspace, Long> { it.getWorkspaceId() })
                .collect<List<Long>, Any>(Collectors.toList())
        assertThat(actualIds).contains(1L, 2L)
    }

    @Test
    fun enforceFirecloudAclsInRecentWorkspaces() {
        val ownedId = workspaceIdIncrementer.getAndIncrement()
        val ownedWorkspace = addMockedWorkspace(
                ownedId,
                "owned",
                "owned_namespace",
                WorkspaceAccessLevel.OWNER,
                WorkspaceActiveStatus.ACTIVE)
        workspaceService!!.updateRecentWorkspaces(ownedWorkspace, USER_ID, Timestamp.from(NOW))

        val sharedWorkspace = addMockedWorkspace(
                workspaceIdIncrementer.getAndIncrement(),
                "shared",
                "shared_namespace",
                WorkspaceAccessLevel.NO_ACCESS,
                WorkspaceActiveStatus.ACTIVE)
        workspaceService!!.updateRecentWorkspaces(sharedWorkspace, USER_ID, Timestamp.from(NOW))

        val recentWorkspaces = workspaceService!!.recentWorkspaces
        assertThat(recentWorkspaces.size).isEqualTo(1)
        assertThat(recentWorkspaces[0].workspaceId).isEqualTo(ownedId)
    }
}
