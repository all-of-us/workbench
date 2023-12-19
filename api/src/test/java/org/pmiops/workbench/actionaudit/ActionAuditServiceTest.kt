package org.pmiops.workbench.actionaudit

import com.google.cloud.logging.LogEntry
import com.google.cloud.logging.Logging
import com.google.cloud.logging.Payload.JsonPayload
import com.google.cloud.logging.Payload.Type
import com.google.common.collect.ImmutableList
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.pmiops.workbench.config.WorkbenchConfig
import org.pmiops.workbench.config.WorkbenchConfig.ActionAuditConfig
import org.pmiops.workbench.config.WorkbenchConfig.ServerConfig
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.util.Arrays
import javax.inject.Provider

@ExtendWith(SpringExtension::class)
class ActionAuditServiceTest {
    private val mockLogging = mock<Logging>()

    private val mockConfigProvider = mock<Provider<WorkbenchConfig>>()

    private var actionAuditService: ActionAuditService? = null

    @BeforeEach
    fun setUp() {
        val actionAuditConfig =
            ActionAuditConfig()
                .apply { logName = "log_path_1" }

        val serverConfig =
            ServerConfig()
                .apply { projectId = "gcp-project-id" }

        val workbenchConfig =
            WorkbenchConfig()
                .apply { actionAudit = actionAuditConfig }
                .apply { server = serverConfig }
        whenever(mockConfigProvider.get()).thenReturn(workbenchConfig)

        actionAuditService = ActionAuditServiceImpl(mockConfigProvider, mockLogging)
    }

    @Test
    fun testSendsSingleEvent() {
        actionAuditService!!.send(EVENT_1)
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
        actionAuditService!!.send(EVENT_1)
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
        actionAuditService!!.send(ImmutableList.of(EVENT_1, EVENT_2))
        argumentCaptor<List<LogEntry>>().apply {
            verify(mockLogging).write(capture())
            val entryList = firstValue
            assertThat(entryList.size).isEqualTo(2)

            val payloads =
                entryList
                    .map { it.getPayload<JsonPayload>() }

            assertThat(
                payloads
                    .map { it.dataAsMap }
                    .map { it[AuditColumn.ACTION_ID.name] }
                    .distinct()
                    .count(),
            )
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

        private val EVENT_1 =
            ActionAuditEvent(
                agentEmailMaybe = "a@b.co",
                targetType = TargetType.DATASET,
                targetIdMaybe = 1L,
                agentType = AgentType.USER,
                agentIdMaybe = AGENT_ID_1,
                actionId = ACTION_ID,
                actionType = ActionType.EDIT,
                targetPropertyMaybe = "foot",
                previousValueMaybe = "bare",
                newValueMaybe = "shod",
                timestamp = System.currentTimeMillis(),
            )

        private val EVENT_2 =
            ActionAuditEvent(
                agentEmailMaybe = "f@b.co",
                targetType = TargetType.DATASET,
                targetIdMaybe = 2L,
                agentType = AgentType.USER,
                agentIdMaybe = AGENT_ID_2,
                actionId = ACTION_ID,
                actionType = ActionType.EDIT,
                targetPropertyMaybe = "height",
                previousValueMaybe = "yay high",
                newValueMaybe = "about that tall",
                timestamp = System.currentTimeMillis(),
            )
    }
}
