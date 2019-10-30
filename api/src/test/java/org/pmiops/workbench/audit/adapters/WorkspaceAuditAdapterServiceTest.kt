package org.pmiops.workbench.audit.adapters

import com.google.common.truth.Truth.assertThat
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyZeroInteractions

import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableSet
import java.sql.Timestamp
import java.time.Clock
import java.time.Instant
import java.util.Collections
import java.util.Optional
import java.util.stream.Collectors
import javax.inject.Provider
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.pmiops.workbench.audit.ActionAuditEvent
import org.pmiops.workbench.audit.ActionAuditService
import org.pmiops.workbench.audit.ActionType
import org.pmiops.workbench.audit.TargetType
import org.pmiops.workbench.audit.targetproperties.AclTargetProperty
import org.pmiops.workbench.db.model.User
import org.pmiops.workbench.model.DataAccessLevel
import org.pmiops.workbench.model.ResearchPurpose
import org.pmiops.workbench.model.Workspace
import org.pmiops.workbench.model.WorkspaceAccessLevel
import org.pmiops.workbench.workspaces.WorkspaceConversionUtils
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
class WorkspaceAuditAdapterServiceTest {

    private var workspaceAuditAdapterService: WorkspaceAuditAdapterService? = null
    private var workspace1: Workspace? = null
    private var user1: User? = null
    private var dbWorkspace1: org.pmiops.workbench.db.model.Workspace? = null
    private var dbWorkspace2: org.pmiops.workbench.db.model.Workspace? = null

    @Mock
    private val mockUserProvider: Provider<User>? = null
    @Mock
    private val mockClock: Clock? = null
    @Mock
    private val mockActionAuditService: ActionAuditService? = null

    @Captor
    private val eventListCaptor: ArgumentCaptor<Collection<ActionAuditEvent>>? = null
    @Captor
    private val eventCaptor: ArgumentCaptor<ActionAuditEvent>? = null

    @TestConfiguration
    @MockBean(value = [ActionAuditService::class])
    internal class Configuration

    @Before
    fun setUp() {
        user1 = User()
        user1!!.userId = 101L
        user1!!.email = "fflinstone@slate.com"
        user1!!.givenName = "Fred"
        user1!!.familyName = "Flintstone"
        doReturn(user1).`when`<Provider<User>>(mockUserProvider).get()
        workspaceAuditAdapterService = WorkspaceAuditAdapterServiceImpl(mockUserProvider!!, mockActionAuditService!!, mockClock!!)

        val researchPurpose1 = ResearchPurpose()
        researchPurpose1.intendedStudy = "stubbed toes"
        researchPurpose1.additionalNotes = "I really like the cloud."
        val now = System.currentTimeMillis()

        workspace1 = Workspace()
        workspace1!!.name = "Workspace 1"
        workspace1!!.id = "fc-id-1"
        workspace1!!.namespace = "aou-rw-local1-c4be869a"
        workspace1!!.creator = "user@fake-research-aou.org"
        workspace1!!.cdrVersionId = "1"
        workspace1!!.researchPurpose = researchPurpose1
        workspace1!!.creationTime = now
        workspace1!!.lastModifiedTime = now
        workspace1!!.etag = "etag_1"
        workspace1!!.dataAccessLevel = DataAccessLevel.REGISTERED
        workspace1!!.published = false

        dbWorkspace1 = WorkspaceConversionUtils.toDbWorkspace(workspace1!!)
        dbWorkspace1!!.workspaceId = WORKSPACE_1_DB_ID
        dbWorkspace1!!.lastAccessedTime = Timestamp(now)
        dbWorkspace1!!.lastModifiedTime = Timestamp(now)
        dbWorkspace1!!.creationTime = Timestamp(now)

        dbWorkspace2 = org.pmiops.workbench.db.model.Workspace()
        dbWorkspace2!!.workspaceId = 201L
        dbWorkspace2!!.published = false
        dbWorkspace2!!.lastModifiedTime = Timestamp(now)
        dbWorkspace2!!.creationTime = Timestamp(now)
        dbWorkspace2!!.creator = user1

        doReturn(Y2K_EPOCH_MILLIS).`when`(mockClock).millis()
    }

    @Test
    fun testFiresCreateWorkspaceEvents() {
        workspaceAuditAdapterService!!.fireCreateAction(workspace1!!, WORKSPACE_1_DB_ID)
        verify<ActionAuditService>(mockActionAuditService).send(eventListCaptor!!.capture())
        val eventsSent = eventListCaptor.value
        assertThat(eventsSent.size).isEqualTo(6)
        val firstEvent = eventsSent.stream().findFirst()
        assertThat(firstEvent.isPresent).isTrue()
        assertThat(firstEvent.get().actionType()).isEqualTo(ActionType.CREATE)
        assertThat(
                eventsSent.stream()
                        .map<ActionType>(Function<ActionAuditEvent, ActionType> { it.actionType() })
                        .collect<Set<ActionType>, Any>(Collectors.toSet())
                        .size)
                .isEqualTo(1)
    }

    @Test
    fun testFirestNoEventsForNullWorkspace() {
        workspaceAuditAdapterService!!.fireCreateAction(null!!, WORKSPACE_1_DB_ID)
        verify<ActionAuditService>(mockActionAuditService).send(eventListCaptor!!.capture())
        val eventsSent = eventListCaptor.value
        assertThat(eventsSent).isEmpty()
    }

    @Test
    fun testFiresDeleteWorkspaceEvent() {
        workspaceAuditAdapterService!!.fireDeleteAction(dbWorkspace1!!)
        verify<ActionAuditService>(mockActionAuditService).send(eventCaptor!!.capture())
        val eventSent = eventCaptor.value
        assertThat(eventSent.actionType()).isEqualTo(ActionType.DELETE)
        assertThat(eventSent.timestamp()).isEqualTo(Y2K_EPOCH_MILLIS)
    }

    @Test
    fun testFiresDuplicateEvent() {
        workspaceAuditAdapterService!!.fireDuplicateAction(dbWorkspace1!!, dbWorkspace2!!)
        verify<ActionAuditService>(mockActionAuditService).send(eventListCaptor!!.capture())
        val eventsSent = eventListCaptor.value
        assertThat(eventsSent).hasSize(2)

        // need same actionId for all events
        assertThat(eventsSent.stream().map<String>(Function<ActionAuditEvent, String> { it.actionId() }).distinct().count()).isEqualTo(1)

        assertThat(
                eventsSent.stream()
                        .map<TargetType>(Function<ActionAuditEvent, TargetType> { it.targetType() })
                        .allMatch { t -> t == TargetType.WORKSPACE })
                .isTrue()

        val expectedActionTypes = ImmutableSet.of(ActionType.DUPLICATE_FROM, ActionType.DUPLICATE_TO)
        val actualActionTypes = eventsSent.stream()
                .map<ActionType>(Function<ActionAuditEvent, ActionType> { it.actionType() })
                .collect<ImmutableSet<ActionType>, Any>(ImmutableSet.toImmutableSet())
        assertThat(actualActionTypes).containsExactlyElementsIn(expectedActionTypes)
    }

    @Test
    fun testFiresCollaborateAction() {
        val aclsByUserId = ImmutableMap.of(
                user1!!.userId,
                WorkspaceAccessLevel.OWNER.toString(),
                REMOVED_USER_ID,
                WorkspaceAccessLevel.NO_ACCESS.toString(),
                ADDED_USER_ID,
                WorkspaceAccessLevel.READER.toString())
        workspaceAuditAdapterService!!.fireCollaborateAction(dbWorkspace1!!.workspaceId, aclsByUserId)
        verify<ActionAuditService>(mockActionAuditService).send(eventListCaptor!!.capture())
        val eventsSent = eventListCaptor.value
        assertThat(eventsSent).hasSize(4)

        val countByTargetType = eventsSent.stream()
                .collect<Map<String, Long>, Any>(Collectors.groupingBy<ActionAuditEvent, String, Any, Long>({ e -> e.targetType().toString() }, Collectors.counting()))

        assertThat(countByTargetType[TargetType.WORKSPACE.toString()]).isEqualTo(1)
        assertThat(countByTargetType[TargetType.USER.toString()]).isEqualTo(3)

        val targetPropertyMaybe = eventsSent.stream()
                .filter { e -> e.targetType() === TargetType.USER }
                .findFirst()
                .flatMap<String>(Function<ActionAuditEvent, Optional<String>> { it.targetProperty() })

        assertThat(targetPropertyMaybe.isPresent).isTrue()
        assertThat(targetPropertyMaybe.get()).isEqualTo(AclTargetProperty.ACCESS_LEVEL.toString())

        // need same actionId for all events
        assertThat(eventsSent.stream().map<String>(Function<ActionAuditEvent, String> { it.actionId() }).distinct().count()).isEqualTo(1)

        val readerEventMaybe = eventsSent.stream()
                .filter { e ->
                    (e.targetType() === TargetType.USER
                            && e.targetId().isPresent
                            && e.targetId().get() == ADDED_USER_ID)
                }
                .findFirst()
        assertThat(readerEventMaybe.isPresent).isTrue()
        assertThat(readerEventMaybe.get().targetProperty().isPresent).isTrue()
        assertThat(readerEventMaybe.get().targetProperty().get())
                .isEqualTo(AclTargetProperty.ACCESS_LEVEL.toString())
        assertThat(readerEventMaybe.get().newValue().get())
                .isEqualTo(WorkspaceAccessLevel.READER.toString())
        assertThat(readerEventMaybe.get().previousValue().isPresent).isFalse()
    }

    @Test
    fun testCollaborateWithEmptyMapDoesNothing() {
        workspaceAuditAdapterService!!.fireCollaborateAction(WORKSPACE_1_DB_ID, emptyMap())
        verifyZeroInteractions(mockActionAuditService)
    }

    @Test
    fun testDoesNotThrowWhenMissingRequiredFields() {
        workspace1!!.researchPurpose = null // programming error
        workspaceAuditAdapterService!!.fireCreateAction(workspace1!!, WORKSPACE_1_DB_ID)
    }

    @Test
    fun testDoesNotThrowWhenUserProviderFails() {
        doReturn(null).`when`<Provider<User>>(mockUserProvider).get()
        workspaceAuditAdapterService!!.fireDeleteAction(dbWorkspace1!!)
    }

    companion object {

        private val WORKSPACE_1_DB_ID = 101L
        private val Y2K_EPOCH_MILLIS = Instant.parse("2000-01-01T00:00:00.00Z").toEpochMilli()
        private val REMOVED_USER_ID = 301L
        private val ADDED_USER_ID = 401L
    }
}
