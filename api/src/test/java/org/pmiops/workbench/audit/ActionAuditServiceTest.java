package org.pmiops.workbench.audit;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import com.google.cloud.MonitoredResource;
import com.google.cloud.logging.LogEntry;
import com.google.cloud.logging.Logging;
import com.google.cloud.logging.Payload;
import com.google.cloud.logging.Payload.JsonPayload;
import com.google.cloud.logging.Payload.Type;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.inject.Provider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.config.WorkbenchConfig.ActionAuditConfig;
import org.pmiops.workbench.config.WorkbenchConfig.ServerConfig;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class ActionAuditServiceTest {

  private static final long AGENT_ID_1 = 101L;
  private static final long AGENT_ID_2 = 102L;
  @Mock private Logging mockLogging;
  @Mock private Provider<WorkbenchConfig> mockConfigProvider;
  @Captor private ArgumentCaptor<List<LogEntry>> logEntryListCaptor;

  private ActionAuditEventImpl event1;
  private ActionAuditEventImpl event2;
  private ActionAuditService actionAuditService;

  @Before
  public void setUp() {

    final WorkbenchConfig.ActionAuditConfig actionAuditConfig = new ActionAuditConfig();
    actionAuditConfig.logName = "log_path_1";
    final WorkbenchConfig.ServerConfig serverConfig = new ServerConfig();
    serverConfig.projectId = "gcp-project-id";

    final WorkbenchConfig workbenchConfig = new WorkbenchConfig();
    workbenchConfig.actionAudit = actionAuditConfig;
    workbenchConfig.server = serverConfig;
    doReturn(workbenchConfig).when(mockConfigProvider).get();

    actionAuditService =
        new ActionAuditServiceImpl(mockConfigProvider, mockLogging);
    final String actionId = ActionAuditService.newActionId();

    // ordinarily events sharing an action would have more things in common than this,
    // but the schema doesn't require it
    event1 =
        new ActionAuditEventImpl.Builder()
            .setAgentEmail("a@b.co")
            .setTargetType(TargetType.DATASET)
            .setTargetId(1L)
            .setAgentType(AgentType.USER)
            .setAgentId(AGENT_ID_1)
            .setActionId(actionId)
            .setTargetProperty("foot")
            .setPreviousValue("bare")
            .setNewValue("shod")
            .setTimestamp(System.currentTimeMillis())
            .build();

    event2 =
        new ActionAuditEventImpl.Builder()
            .setAgentEmail("f@b.co")
            .setTargetType(TargetType.DATASET)
            .setTargetId(2L)
            .setAgentType(AgentType.USER)
            .setAgentId(AGENT_ID_2)
            .setActionId(actionId)
            .setTargetProperty("height")
            .setPreviousValue("yay high")
            .setNewValue("about that tall")
            .setTimestamp(System.currentTimeMillis())
            .build();
  }

  @Test
  public void testSendsSingleEvent() {
    actionAuditService.send(event1);
    verify(mockLogging).write(logEntryListCaptor.capture());

    final List<LogEntry> entryList = logEntryListCaptor.getValue();
    assertThat(entryList.size()).isEqualTo(1);

    final LogEntry entry = entryList.get(0);
    assertThat(entry.getPayload().getType()).isEqualTo(Payload.Type.JSON);

    final JsonPayload jsonPayload = entry.getPayload();
    assertThat(jsonPayload.getDataAsMap().size()).isEqualTo(11);

    Map<String, Object> payloadMap = jsonPayload.getDataAsMap();
    assertThat(payloadMap.get(AuditColumn.NEW_VALUE.name())).isEqualTo("shod");
    // Logging passes numeric json fields as doubles when building a JsonPayload
    assertThat(payloadMap.get(AuditColumn.AGENT_ID.name())).isEqualTo((double) AGENT_ID_1);
  }

  @Test
  public void testSendsMultipleEvents() {
    actionAuditService.send(ImmutableList.of(event1, event2));
    verify(mockLogging).write(logEntryListCaptor.capture());
    List<LogEntry> entryList = logEntryListCaptor.getValue();
    assertThat(entryList.size()).isEqualTo(2);

    final ImmutableList<JsonPayload> payloads =
        entryList.stream()
            .map(LogEntry::getPayload)
            .filter(p -> ((Payload) p).getType() == Type.JSON)
            .map(p -> (JsonPayload) p)
            .collect(ImmutableList.toImmutableList());

    assertThat(
            payloads.stream()
                .map(JsonPayload::getDataAsMap)
                .map(entry -> entry.get(AuditColumn.ACTION_ID.name()))
                .distinct()
                .count())
        .isEqualTo(1);
  }

  @Test
  public void testNullPayloadDoesNotThrow() {
    actionAuditService.send((Collection<ActionAuditEvent>) null);
  }
}
