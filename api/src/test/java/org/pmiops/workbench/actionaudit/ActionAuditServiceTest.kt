package org.pmiops.workbench.actionaudit

import com.google.common.truth.Truth.assertThat

import com.google.cloud.logging.LogEntry
import com.google.cloud.logging.Logging
import com.google.cloud.logging.Payload.JsonPayload
import com.google.cloud.logging.Payload.Type
import com.google.common.collect.ImmutableList
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import java.util.Arrays
import javax.inject.Provider
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.pmiops.workbench.config.WorkbenchConfig
import org.pmiops.workbench.config.WorkbenchConfig.ActionAuditConfig
import org.pmiops.workbench.config.WorkbenchConfig.ServerConfig
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
class ActionAuditServiceTest {
    private val mockLogging = mock<Logging>()

    private val mockConfigProvider = mock<Provider<WorkbenchConfig>>()

    private var event1: ActionAuditEvent? = null
    private var event2: ActionAuditEvent? = null
    private var actionAuditService: ActionAuditService? = null

    @Before
    fun setUp() {
        val actionAuditConfig = ActionAuditConfig()
        actionAuditConfig.logName = "log_path_1"
        val serverConfig = ServerConfig()
        serverConfig.projectId = "gcp-project-id"

        val workbenchConfig = WorkbenchConfig()
        workbenchConfig.actionAudit = actionAuditConfig
        workbenchConfig.server = serverConfig
        whenever(mockConfigProvider.get()).thenReturn(workbenchConfig)

        actionAuditService = ActionAuditServiceImpl(mockConfigProvider, mockLogging)

        // ordinarily events sharing an action would have more things in common than this,
        // but the schema doesn't require it
        event1 = ActionAuditEvent(
                agentEmailMaybe = "a@b.co",
                targetType = TargetType.DATASET,
                targetIdMaybe = 1L,
                agentType = AgentType.USER,
                agentId = AGENT_ID_1,
                actionId = ACTION_ID,
                actionType = ActionType.EDIT,
                targetPropertyMaybe = "foot",
                previousValueMaybe = "bare",
                newValueMaybe = "shod",
                timestamp = System.currentTimeMillis()
        )
        event2 = ActionAuditEvent(
                agentEmailMaybe = "f@b.co",
                targetType = TargetType.DATASET,
                targetIdMaybe = 2L,
                agentType = AgentType.USER,
                agentId = AGENT_ID_2,
                actionId = ACTION_ID,
                actionType = ActionType.EDIT,
                targetPropertyMaybe = "height",
                previousValueMaybe = "yay high",
                newValueMaybe = "about that tall",
                timestamp = System.currentTimeMillis()
        )
    }

    @Test
    fun testSendsSingleEvent() {
        actionAuditService!!.send(event1!!)
        argumentCaptor<List<LogEntry>>().apply {
            verify(mockLogging).write(capture())
            val entryList: List<LogEntry> = firstValue
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
    }

    @Test
    fun testSendsExpectedColumnNames() {
        actionAuditService!!.send(event1!!)
        argumentCaptor<List<LogEntry>>().apply {
            verify(mockLogging).write(capture())
            val entryList = firstValue
            assertThat(entryList.size).isEqualTo(1)
            val entry = entryList[0]
            val jsonPayload = entry.getPayload<JsonPayload>()

            for (key in jsonPayload.dataAsMap.keys) {
                assertThat(Arrays.stream(AuditColumn.values()).anyMatch { col -> col.toString() == key })
                        .isTrue()
            }
        }
    }

    @Test
    fun testSendsMultipleEventsAsSingleAction() {
        actionAuditService!!.send(ImmutableList.of(event1!!, event2!!))
        argumentCaptor<List<LogEntry>>().apply {
            verify(mockLogging).write(capture())
            val entryList = firstValue
            assertThat(entryList.size).isEqualTo(2)

            val payloads = entryList
                    .map { it.getPayload<JsonPayload>() }

            assertThat(
                    payloads
                            .map { it.dataAsMap }
                            .map { it[AuditColumn.ACTION_ID.name] }
                            .distinct()
                            .count())
                    .isEqualTo(1)
        }
    }

    @Test
    fun testSendWithEmptyCollectionDoesNotCallCloudLoggingApi() {
        actionAuditService!!.send(emptySet())
        argumentCaptor<List<LogEntry>>().apply {
            verify(mockLogging, never()).write(any())
        }
    }

    companion object {
        private const val AGENT_ID_1 = 101L
        private const val AGENT_ID_2 = 102L
        private const val ACTION_ID = "b52a36f6-3e88-4a30-a57f-ae884838bfbf"
    }
}
