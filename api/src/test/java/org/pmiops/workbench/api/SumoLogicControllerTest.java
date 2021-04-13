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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.actionaudit.auditors.EgressEventAuditor;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.UnauthorizedException;
import org.pmiops.workbench.google.CloudStorageClient;
import org.pmiops.workbench.model.EgressEvent;
import org.pmiops.workbench.model.EgressEventRequest;
import org.pmiops.workbench.opsgenie.EgressEventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class SumoLogicControllerTest {

  private static final String API_KEY = "12345";

  @MockBean private EgressEventAuditor mockEgressEventAuditor;
  @MockBean private CloudStorageClient mockCloudStorageClient;
  @MockBean private EgressEventService mockEgressEventService;
  private static WorkbenchConfig config;

  @Autowired private SumoLogicController sumoLogicController;

  private EgressEventRequest request;
  private EgressEvent event;

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

  @Before
  public void setUp() throws JsonProcessingException {
    config = WorkbenchConfig.createEmptyConfig();

    event = new EgressEvent();
    event.setProjectName("aou-rw-test-c7dec260");
    event.setEgressMibThreshold(100.0);
    event.setEgressMib(123.0);
    event.setEnvironment(EgressEvent.EnvironmentEnum.TEST);
    event.setTimeWindowDuration(Duration.ofSeconds(300).getSeconds());
    event.setTimeWindowStart(Instant.now().toEpochMilli());

    System.out.println("~~~~~~~~~~~");
    System.out.println(objectMapper.writeValueAsString(Collections.singletonList(event)));

    request = new EgressEventRequest();
    request.setEventsJsonArray(objectMapper.writeValueAsString(Collections.singletonList(event)));

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
