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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.actionaudit.auditors.EgressEventAuditor;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.UnauthorizedException;
import org.pmiops.workbench.google.CloudStorageService;
import org.pmiops.workbench.model.EgressEvent;
import org.pmiops.workbench.model.EgressEventRequest;
import org.pmiops.workbench.opsgenie.OpsGenieService;
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
  @MockBean private CloudStorageService mockCloudStorageService;
  @MockBean private OpsGenieService mockOpsGenieService;
  private static WorkbenchConfig config;

  @Autowired private SumoLogicController sumoLogicController;

  private EgressEventRequest request;
  private EgressEvent event;

  private ObjectMapper mapper = new ObjectMapper();

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
    config.featureFlags.enableSumoLogicEventHandling = true;

    event = new EgressEvent();
    event.setProjectName("aou-rw-test-c7dec260");
    event.setEgressMibThreshold(100.0);
    event.setEgressMib(123.0);
    event.setEnvironment(EgressEvent.EnvironmentEnum.TEST);
    event.setTimeWindowDuration(Duration.ofSeconds(300).getSeconds());
    event.setTimeWindowStart(Instant.now().toEpochMilli());

    request = new EgressEventRequest();
    request.setEventsJsonArray(mapper.writeValueAsString(Arrays.asList(event)));

    when(mockCloudStorageService.getCredentialsBucketString(
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
    verify(mockEgressEventAuditor).fireEgressEvent(event);
    verify(mockOpsGenieService).createEgressEventAlert(event);
  }

  @Test
  public void testLogsMultipleEvents() throws JsonProcessingException {
    EgressEvent event2 = new EgressEvent();
    request.setEventsJsonArray(mapper.writeValueAsString(Arrays.asList(event, event2)));
    sumoLogicController.logEgressEvent(API_KEY, request);
    verify(mockEgressEventAuditor, times(2)).fireEgressEvent(any());
    verify(mockOpsGenieService, times(2)).createEgressEventAlert(any());
  }
}
