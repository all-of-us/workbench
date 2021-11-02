package org.pmiops.workbench.api;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.SpringTest;
import org.pmiops.workbench.actionaudit.auditors.EgressEventAuditor;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.UnauthorizedException;
import org.pmiops.workbench.exfiltration.EgressEventService;
import org.pmiops.workbench.google.CloudStorageClient;
import org.pmiops.workbench.model.EgressEvent;
import org.pmiops.workbench.model.SumologicEgressEvent;
import org.pmiops.workbench.model.SumologicEgressEventRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;

public class SumoLogicControllerTest extends SpringTest {

  private static final String API_KEY = "12345";

  @MockBean private EgressEventAuditor mockEgressEventAuditor;
  @MockBean private CloudStorageClient mockCloudStorageClient;
  @MockBean private EgressEventService mockEgressEventService;
  private static WorkbenchConfig config;

  @Autowired private SumoLogicController sumoLogicController;

  private SumologicEgressEventRequest request;
  private SumologicEgressEvent event;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @TestConfiguration
  @Import({SumoLogicController.class})
  static class Configuration {
    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public WorkbenchConfig workbenchConfig() {
      return config;
    }
  }

  @BeforeEach
  public void setUp() throws JsonProcessingException {
    config = WorkbenchConfig.createEmptyConfig();

    event =
        new SumologicEgressEvent()
            .projectName("aou-rw-test-c7dec260")
            .egressMibThreshold(100.0)
            .egressMib(123.0)
            .environment(SumologicEgressEvent.EnvironmentEnum.TEST)
            .timeWindowDuration(Duration.ofSeconds(300).getSeconds())
            .timeWindowStart(Instant.now().toEpochMilli());

    request =
        new SumologicEgressEventRequest()
            .eventsJsonArray(objectMapper.writeValueAsString(Collections.singletonList(event)));

    when(mockCloudStorageClient.getCredentialsBucketString(
            SumoLogicController.SUMOLOGIC_KEY_FILENAME))
        .thenReturn(API_KEY);
  }

  @Test
  public void testLogsApiKeyFailure() {
    assertThrows(
        UnauthorizedException.class, () -> sumoLogicController.logEgressEvent("bad-key", request));
    verify(mockEgressEventAuditor).fireBadApiKey("bad-key", request);
  }

  @Test
  public void testLogsRequestParsingFailure() {
    request.setEventsJsonArray("bad-json");
    assertThrows(
        BadRequestException.class, () -> sumoLogicController.logEgressEvent(API_KEY, request));
    verify(mockEgressEventAuditor).fireFailedToParseEgressEventRequest(request);
  }

  @Test
  public void testLogsSingleEvent() {
    sumoLogicController.logEgressEvent(API_KEY, request);
    verify(mockEgressEventService).handleEvent(event);
  }

  @Test
  public void testLogsMultipleEvents() throws JsonProcessingException {
    EgressEvent event2 = new EgressEvent();
    request.setEventsJsonArray(objectMapper.writeValueAsString(Arrays.asList(event, event2)));
    sumoLogicController.logEgressEvent(API_KEY, request);
    verify(mockEgressEventService, times(2)).handleEvent(any());
  }

  @Test
  public void testLogsRequestParsingSuccess_ignoreUnknownField() throws Exception {
    String test = "[{\"project_name\":\"aou-rw-test-c7dec260\", \"random_stuff\":\"foo\"}]";
    request.setEventsJsonArray(test);
    sumoLogicController.logEgressEvent(API_KEY, request);
    verify(mockEgressEventService).handleEvent(any());
  }
}
