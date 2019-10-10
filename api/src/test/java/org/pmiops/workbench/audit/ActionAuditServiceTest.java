package org.pmiops.workbench.audit;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.verify;

import com.google.cloud.logging.LogEntry;
import com.google.cloud.logging.Logging;
import com.google.cloud.logging.Payload;
import com.google.cloud.logging.Payload.JsonPayload;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class ActionAuditServiceTest {

  @Mock private Logging mockLogging;
  @Captor
  private ArgumentCaptor<List<LogEntry>> logEntryListCaptor;

  private ActionAuditEvent event1;
  private ActionAuditService actionAuditService;

  @Before
  public void setUp() {
    actionAuditService = new ActionAuditServiceImpl(mockLogging);

    event1 = new ActionAuditEvent.Builder()
        .setActionId("foo")
        .setAgentEmail("a@b.co")
        .setTargetType(TargetType.DATASET)
        .setTargetId(1L)
        .setAgentType(AgentType.USER)
        .setAgentId(2L)
        .setActionId(ActionAuditService.newActionId())
        .setTargetProperty("foot")
        .setPreviousValue("bare")
        .setNewValue("shod")
        .setTimestamp(System.currentTimeMillis())
        .build();
  }

  @Test
  public void testSendsSingleEvent() {
    actionAuditService.send(event1);
    verify(mockLogging).write(logEntryListCaptor.capture());
    List<LogEntry> entryList = logEntryListCaptor.getValue();
    assertThat(entryList.size()).isEqualTo(1);
    LogEntry entry = entryList.get(0);
    assertThat(entry.getPayload().getType()).isEqualTo(Payload.Type.JSON);
    JsonPayload jsonPayload = entry.getPayload();
    assertThat(jsonPayload.getDataAsMap().size()).isEqualTo(11);

    Map<String, Object> payloadMap = jsonPayload.getDataAsMap();
    assertThat(payloadMap.get(AuditColumn.NEW_VALUE.name()))
        .isEqualTo("shod");
    assertThat(payloadMap.get(AuditColumn.AGENT_ID.name()))
        .isEqualTo(2.0);
  }

  @Test
  public void testNullPayloadDoesNotThrow() {
    actionAuditService.send((Collection<ActionAuditEvent>) null);
  }
}
