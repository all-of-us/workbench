package org.pmiops.workbench.actionaudit

import com.google.common.truth.Truth.assertThat
import org.mockito.ArgumentMatchers.anyList
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.never
import org.mockito.Mockito.verify

import com.google.cloud.logging.LogEntry
import com.google.cloud.logging.Logging
import com.google.cloud.logging.Payload.JsonPayload
import com.google.cloud.logging.Payload.Type
import com.google.common.collect.ImmutableList
import java.util.Arrays
import javax.inject.Provider
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.pmiops.workbench.config.WorkbenchConfig
import org.pmiops.workbench.config.WorkbenchConfig.ActionAuditConfig
import org.pmiops.workbench.config.WorkbenchConfig.ServerConfig
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
class ActionAuditServiceTest {
    @Mock
    private val mockLogging: Logging? = null
    @Mock
    private val mockConfigProvider: Provider<WorkbenchConfig>? = null
    @Captor
    private val logEntryListCaptor: ArgumentCaptor<List<LogEntry>>? = null

    private var event1: ActionAuditEvent? = null
    private var event2: ActionAuditEvent? = null
    private var actionAuditService: ActionAuditService? = null
    private val ACTION_ID = "b52a36f6-3e88-4a30-a57f-ae884838bfbf"

    @Before
    fun setUp() {
        val actionAuditConfig = ActionAuditConfig()
        actionAuditConfig.logName = "log_path_1"
        val serverConfig = ServerConfig()
        serverConfig.projectId = "gcp-project-id"

        val workbenchConfig = WorkbenchConfig()
        workbenchConfig.actionAudit = actionAuditConfig
        workbenchConfig.server = serverConfig
        doReturn(workbenchConfig).`when`<Provider<WorkbenchConfig>>(mockConfigProvider).get()

        actionAuditService = ActionAuditServiceImpl(mockConfigProvider!!, mockLogging!!)

        // ordinarily events sharing an action would have more things in common than this,
        // but the schema doesn't require it
        event1 = ActionAuditEvent(
                timestamp = System.currentTimeMillis(),
                actionId = ACTION_ID,
                actionType = ActionType.EDIT,
                agentType = AgentType.USER,
                agentId = AGENT_ID_1,
                agentEmailMaybe = "a@b.co",
                targetType = TargetType.DATASET,
                targetIdMaybe = 1L,
                targetPropertyMaybe = "foot",
                previousValueMaybe = "bare",
                newValueMaybe = "shod"
        )

        event2 = ActionAuditEvent(
                timestamp = System.currentTimeMillis(),
                actionId = ACTION_ID,
                actionType = ActionType.EDIT,
                agentType = AgentType.USER,
                agentId = AGENT_ID_2,
                agentEmailMaybe = "f@b.co",
                targetType = TargetType.DATASET,
                targetIdMaybe = 2L,
                targetPropertyMaybe = "height",
                previousValueMaybe = "yay high",
                newValueMaybe = "about that tall"
        )
    }

    @Test
    fun testSendsSingleEvent() {
        actionAuditService!!.send(event1!!)
        verify<Logging>(mockLogging).write(logEntryListCaptor!!.capture())

        val entryList: List<LogEntry> = logEntryListCaptor.value
        assertThat(entryList.size).isEqualTo(1)

        val entry: LogEntry = entryList[0]
        val jsonPayload = entry.getPayload<JsonPayload>()

        assertThat(jsonPayload.type).isEqualTo(Type.JSON)
        assertThat(jsonPayload.dataAsMap.size).isEqualTo(11)

        val payloadMap = jsonPayload.dataAsMap
        assertThat(payloadMap[AuditColumn.NEW_VALUE.name]).isEqualTo("shod")

        // Logging passes numeric json fields as doubles when building a JsonPayload
        assertThat(payloadMap[AuditColumn.AGENT_ID.name]).isEqualTo(AGENT_ID_1.toDouble())
    }

    @Test
    fun testSendsExpectedColumnNames() {
        actionAuditService!!.send(event1!!)
        verify<Logging>(mockLogging).write(logEntryListCaptor!!.capture())
        val entryList = logEntryListCaptor.value
        assertThat(entryList.size).isEqualTo(1)
        val entry = entryList[0]
        val jsonPayload = entry.getPayload<JsonPayload>()

        for (key in jsonPayload.dataAsMap.keys) {
            assertThat(Arrays.stream(AuditColumn.values()).anyMatch { col -> col.toString() == key })
                    .isTrue()
        }
    }

    @Test
    fun  testSendsMultipleEventsAsSingleAction() {
        actionAuditService!!.send(ImmutableList.of(event1!!, event2!!))
        verify<Logging>(mockLogging).write(logEntryListCaptor!!.capture())
        val entryList = logEntryListCaptor.value
        assertThat(entryList.size).isEqualTo(2)

        val payloads = entryList
                .map { it.getPayload<JsonPayload>() }

        assertThat(
                payloads
                        .map { it.getDataAsMap() }
                        .map { it[AuditColumn.ACTION_ID.name] }
                        .distinct()
                        .count())
                .isEqualTo(1)
    }

    @Test
    fun testSendWithEmptyCollectionDoesNotCallCloudLoggingApi() {
        actionAuditService!!.send(emptySet())
        verify<Logging>(mockLogging, never()).write(anyList())
    }

    companion object {

        private const val AGENT_ID_1 = 101L
        private const val AGENT_ID_2 = 102L
    }
}
