package org.pmiops.workbench.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.pmiops.workbench.actionaudit.auditors.SumoLogicAuditor;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.UnauthorizedException;
import org.pmiops.workbench.google.CloudStorageService;
import org.pmiops.workbench.model.EgressEvent;
import org.pmiops.workbench.model.EgressEventRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class SumoLogicControllerTest {

  private static final String API_KEY = "12345";

  @MockBean private SumoLogicAuditor sumoLogicAuditor;
  @MockBean private CloudStorageService cloudStorageService;

  @Autowired private SumoLogicController sumoLogicController;

  @Rule public final ExpectedException exception = ExpectedException.none();

  private EgressEventRequest request;
  private EgressEvent event;

  private ObjectMapper mapper = new ObjectMapper();

  @TestConfiguration
  @Import({SumoLogicController.class})
  static class Configuration {}

  @Before
  public void setUp() throws JsonProcessingException {
    event = new EgressEvent();
    event.setProjectName("aou-rw-test-c7dec260");
    event.setEgressMibThreshold(100.0);
    event.setEgressMib(123.0);
    event.setEnvironment(EgressEvent.EnvironmentEnum.TEST);
    event.setTimeWindowDuration(300L);
    event.setTimeWindowStart(Instant.now().toEpochMilli());

    request = new EgressEventRequest();
    request.setEventsJsonArray(mapper.writeValueAsString(Arrays.asList(event)));

    when(cloudStorageService.getCredentialsBucketString(SumoLogicController.SUMOLOGIC_KEY_FILENAME))
        .thenReturn(API_KEY);
  }

  @Test
  public void testLogsApiKeyFailure() {
    exception.expect(UnauthorizedException.class);
    sumoLogicController.logEgressEvent("bad-key", request);
    verify(sumoLogicAuditor).fireBadApiKeyEgressEvent("bad-key", request);
  }

  @Test
  public void testLogsRequestParsingFailure() {
    exception.expect(BadRequestException.class);
    request.setEventsJsonArray("bad-json");
    sumoLogicController.logEgressEvent(API_KEY, request);
    verify(sumoLogicAuditor).fireFailedToParseEgressEvent(request);
  }

  @Test
  public void testLogsSingleEvent() {
    sumoLogicController.logEgressEvent(API_KEY, request);
    verify(sumoLogicAuditor).fireEgressEvent(event);
  }

  @Test
  public void testLogsMultipleEvents() throws JsonProcessingException {
    EgressEvent event2 = new EgressEvent();
    request.setEventsJsonArray(mapper.writeValueAsString(Arrays.asList(event, event2)));
    sumoLogicController.logEgressEvent(API_KEY, request);
    verify(sumoLogicAuditor, times(2)).fireEgressEvent(any());
  }
}
