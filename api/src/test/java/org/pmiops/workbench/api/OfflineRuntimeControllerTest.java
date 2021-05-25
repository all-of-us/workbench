package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.leonardo.api.RuntimesApi;
import org.pmiops.workbench.leonardo.model.LeonardoAuditInfo;
import org.pmiops.workbench.leonardo.model.LeonardoGetRuntimeResponse;
import org.pmiops.workbench.leonardo.model.LeonardoListRuntimeResponse;
import org.pmiops.workbench.leonardo.model.LeonardoRuntimeStatus;
import org.pmiops.workbench.notebooks.NotebooksConfig;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.utils.mappers.LeonardoMapper;
import org.pmiops.workbench.utils.mappers.LeonardoMapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringRunner;

@ExtendWith(SpringExtension.class)
public class OfflineRuntimeControllerTest {
  private static final Instant NOW = Instant.parse("1988-12-26T00:00:00Z");
  private static final FakeClock CLOCK = new FakeClock(NOW, ZoneId.systemDefault());
  private static final Duration MAX_AGE = Duration.ofDays(14);
  private static final Duration IDLE_MAX_AGE = Duration.ofDays(7);

  @TestConfiguration
  @Import({OfflineRuntimeController.class, LeonardoMapperImpl.class})
  static class Configuration {

    @Bean
    Clock clock() {
      return CLOCK;
    }

    @Bean
    public WorkbenchConfig workbenchConfig() {
      WorkbenchConfig config = WorkbenchConfig.createEmptyConfig();
      config.firecloud.notebookRuntimeMaxAgeDays = (int) MAX_AGE.toDays();
      config.firecloud.notebookRuntimeIdleMaxAgeDays = (int) IDLE_MAX_AGE.toDays();
      return config;
    }
  }

  @Qualifier(NotebooksConfig.SERVICE_RUNTIMES_API)
  @MockBean
  private RuntimesApi mockRuntimesApi;

  @Autowired private LeonardoMapper leonardoMapper;
  @Autowired private OfflineRuntimeController controller;

  private int projectIdIndex = 0;

  @BeforeEach
  public void setUp() {
    CLOCK.setInstant(NOW);
    projectIdIndex = 0;
  }

  private LeonardoGetRuntimeResponse runtimeWithAge(Duration age) {
    return runtimeWithAgeAndIdle(age, Duration.ZERO);
  }

  private LeonardoGetRuntimeResponse runtimeWithAgeAndIdle(Duration age, Duration idleTime) {
    // There should only be one runtime per project, so increment an index for
    // each runtime created per test.
    return new LeonardoGetRuntimeResponse()
        .runtimeName("all-of-us")
        .googleProject(String.format("proj-%d", projectIdIndex++))
        .status(LeonardoRuntimeStatus.RUNNING)
        .auditInfo(
            new LeonardoAuditInfo()
                .createdDate(NOW.minus(age).toString())
                .dateAccessed(NOW.minus(idleTime).toString()));
  }

  private List<LeonardoListRuntimeResponse> toListRuntimeResponseList(
      List<LeonardoGetRuntimeResponse> runtimes) {
    return runtimes.stream()
        .map(leonardoMapper::toListRuntimeResponse)
        .collect(Collectors.toList());
  }

  private void stubRuntimes(List<LeonardoGetRuntimeResponse> runtimes) throws Exception {
    when(mockRuntimesApi.listRuntimes(any(), any()))
        .thenReturn(toListRuntimeResponseList(runtimes));

    for (LeonardoGetRuntimeResponse runtime : runtimes) {
      when(mockRuntimesApi.getRuntime(runtime.getGoogleProject(), runtime.getRuntimeName()))
          .thenReturn(runtime);
    }
  }

  @Test
  public void testCheckRuntimesNoResults() throws Exception {
    stubRuntimes(ImmutableList.of());
    assertThat(controller.checkRuntimes().getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    verify(mockRuntimesApi, never()).deleteRuntime(any(), any(), any());
  }

  @Test
  public void testCheckRuntimesActiveRuntime() throws Exception {
    stubRuntimes(ImmutableList.of(runtimeWithAge(Duration.ofHours(10))));
    assertThat(controller.checkRuntimes().getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    verify(mockRuntimesApi, never()).deleteRuntime(any(), any(), any());
  }

  @Test
  public void testCheckRuntimesActiveTooOld() throws Exception {
    stubRuntimes(ImmutableList.of(runtimeWithAge(MAX_AGE.plusMinutes(5))));
    assertThat(controller.checkRuntimes().getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    verify(mockRuntimesApi).deleteRuntime(any(), any(), any());
  }

  @Test
  public void testCheckRuntimesIdleYoung() throws Exception {
    // Running for under the IDLE_MAX_AGE, idle for 10 hours
    stubRuntimes(
        ImmutableList.of(
            runtimeWithAgeAndIdle(IDLE_MAX_AGE.minusMinutes(10), Duration.ofHours(10))));
    assertThat(controller.checkRuntimes().getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    verify(mockRuntimesApi, never()).deleteRuntime(any(), any(), any());
  }

  @Test
  public void testCheckRuntimesIdleOld() throws Exception {
    // Running for >IDLE_MAX_AGE, idle for 10 hours
    stubRuntimes(
        ImmutableList.of(
            runtimeWithAgeAndIdle(IDLE_MAX_AGE.plusMinutes(15), Duration.ofHours(10))));
    assertThat(controller.checkRuntimes().getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    verify(mockRuntimesApi).deleteRuntime(any(), any(), any());
  }

  @Test
  public void testCheckRuntimesBrieflyIdleOld() throws Exception {
    // Running for >IDLE_MAX_AGE, idle for only 15 minutes
    stubRuntimes(
        ImmutableList.of(
            runtimeWithAgeAndIdle(IDLE_MAX_AGE.plusMinutes(15), Duration.ofMinutes(15))));
    assertThat(controller.checkRuntimes().getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    verify(mockRuntimesApi, never()).deleteRuntime(any(), any(), any());
  }

  @Test
  public void testCheckRuntimesOtherStatusFiltered() throws Exception {
    stubRuntimes(
        ImmutableList.of(
            runtimeWithAge(MAX_AGE.plusDays(10)).status(LeonardoRuntimeStatus.DELETING)));
    assertThat(controller.checkRuntimes().getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    verify(mockRuntimesApi, never()).deleteRuntime(any(), any(), any());
  }
}
