package org.pmiops.workbench.actionaudit;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.logging.LogEntry;
import com.google.cloud.logging.Logging;
import com.google.cloud.logging.Payload.JsonPayload;
import com.google.cloud.logging.Payload.Type;
import com.google.common.collect.ImmutableList;
import java.util.List;
import javax.inject.Provider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.config.WorkbenchConfig.ActionAuditConfig;
import org.pmiops.workbench.config.WorkbenchConfig.ServerConfig;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith({SpringExtension.class, MockitoExtension.class})
public class ActionAuditServiceTest {

  @Mock private Logging mockLogging;

  @Mock private Provider<WorkbenchConfig> mockConfigProvider;

  private ActionAuditService actionAuditService;

  private void stubWorkbenchConfig() {
    ActionAuditConfig actionAuditConfig = new ActionAuditConfig();
    actionAuditConfig.logName = "log_path_1";

    ServerConfig serverConfig = new ServerConfig();
    serverConfig.projectId = "gcp-project-id";

    WorkbenchConfig workbenchConfig = new WorkbenchConfig();
    workbenchConfig.actionAudit = actionAuditConfig;
    workbenchConfig.server = serverConfig;

    when(mockConfigProvider.get()).thenReturn(workbenchConfig);
  }

  @BeforeEach
  public void setUp() {
    actionAuditService = new ActionAuditServiceImpl(mockConfigProvider, mockLogging);
  }

  @Test
  public void testSendsSingleEvent() {
    stubWorkbenchConfig();
    actionAuditService.send(EVENT_1);
    ArgumentCaptor<List<LogEntry>> captor = ArgumentCaptor.forClass(List.class);
    verify(mockLogging).write(captor.capture());
    List<LogEntry> entryList = captor.getValue();

    assertThat(entryList.size()).isEqualTo(1);

    JsonPayload jsonPayload = entryList.get(0).getPayload();

    assertThat(jsonPayload.getType()).isEqualTo(Type.JSON);
    var payloadMap = jsonPayload.getDataAsMap();
    assertThat(payloadMap.size()).isEqualTo(11);

    assertThat(payloadMap.get(AuditColumn.NEW_VALUE.name())).isEqualTo("shod");

    // Logging passes numeric json fields as doubles when building a JsonPayload
    assertThat(payloadMap.get(AuditColumn.AGENT_ID.name())).isEqualTo((double) AGENT_ID_1);
  }

  @Test
  public void testSendsExpectedColumnNames() {
    stubWorkbenchConfig();
    actionAuditService.send(EVENT_1);
    verify(mockLogging).write(any());
  }

  @Test
  public void testSendsMultipleEventsAsSingleAction() {
    stubWorkbenchConfig();
    actionAuditService.send(ImmutableList.of(EVENT_1, EVENT_2));
    verify(mockLogging).write(any());
  }

  @Test
  public void testSendWithEmptyCollectionDoesNotCallCloudLoggingApi() {
    actionAuditService.send(ImmutableList.of());
    verify(mockLogging, never()).write(any());
  }

  private static final long AGENT_ID_1 = 101L;
  private static final long AGENT_ID_2 = 102L;
  private static final String ACTION_ID = "b52a36f6-3e88-4a30-a57f-ae884838bfbf";

  private static final ActionAuditEvent EVENT_1 =
      new ActionAuditEvent(
          System.currentTimeMillis(),
          AgentType.USER,
          AGENT_ID_1,
          "a@b.co",
          ACTION_ID,
          ActionType.EDIT,
          TargetType.DATASET,
          "foot",
          1L,
          "bare",
          "shod");

  private static final ActionAuditEvent EVENT_2 =
      new ActionAuditEvent(
          System.currentTimeMillis(),
          AgentType.USER,
          AGENT_ID_2,
          "f@b.co",
          ACTION_ID,
          ActionType.EDIT,
          TargetType.DATASET,
          "height",
          2L,
          "yay high",
          "about that tall");
}