package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.cloudtasks.TaskQueueService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.config.WorkbenchLocationConfigService;
import org.pmiops.workbench.disks.DiskAdminService;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoAuditInfo;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoCloudContext;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoCloudProvider;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoGetRuntimeResponse;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoListRuntimeResponse;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoRuntimeStatus;
import org.pmiops.workbench.leonardo.LeonardoApiClient;
import org.pmiops.workbench.utils.mappers.LeonardoMapper;
import org.pmiops.workbench.utils.mappers.LeonardoMapperImpl;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;

@DataJpaTest
public class OfflineEnvironmentsControllerTest {
  private static final Instant NOW = FakeClockConfiguration.NOW.toInstant();
  private static final Duration RUNTIME_MAX_AGE = Duration.ofDays(14);
  private static final Duration RUNTIME_IDLE_MAX_AGE = Duration.ofDays(7);

  @TestConfiguration
  @MockBean({
    DiskAdminService.class,
    WorkspaceService.class,
  })
  @Import({
    FakeClockConfiguration.class,
    LeonardoMapperImpl.class,
    OfflineEnvironmentsController.class,
    TaskQueueService.class,
    WorkbenchLocationConfigService.class,
  })
  static class Configuration {
    @Bean
    public WorkbenchConfig workbenchConfig() {
      return config;
    }
  }

  @MockBean private LeonardoApiClient mockLeonardoApiClient;

  @Autowired private OfflineEnvironmentsController controller;

  @Autowired private LeonardoMapper leonardoMapper;

  private int runtimeProjectIdIndex = 0;
  private static final WorkbenchConfig config = WorkbenchConfig.createEmptyConfig();

  @BeforeEach
  public void setUp() {
    config.firecloud.notebookRuntimeMaxAgeDays = (int) RUNTIME_MAX_AGE.toDays();
    config.firecloud.notebookRuntimeIdleMaxAgeDays = (int) RUNTIME_IDLE_MAX_AGE.toDays();
    config.billing.accountId = "initial-credits";

    runtimeProjectIdIndex = 0;
  }

  private LeonardoGetRuntimeResponse runtimeWithAge(Duration age) {
    return runtimeWithAgeAndIdle(age, Duration.ZERO);
  }

  private LeonardoGetRuntimeResponse runtimeWithAgeAndIdle(Duration age, Duration idleTime) {
    // There should only be one runtime per project, so increment an index for
    // each runtime created per test.
    return new LeonardoGetRuntimeResponse()
        .runtimeName("all-of-us")
        .cloudContext(
            new LeonardoCloudContext()
                .cloudProvider(LeonardoCloudProvider.GCP)
                .cloudResource(String.format("proj-%d", runtimeProjectIdIndex++)))
        .status(LeonardoRuntimeStatus.RUNNING)
        .auditInfo(
            new LeonardoAuditInfo()
                .createdDate(NOW.minus(age).toString())
                .dateAccessed(NOW.minus(idleTime).toString()));
  }

  private List<LeonardoListRuntimeResponse> toListRuntimeResponseList(
      List<LeonardoGetRuntimeResponse> runtimes) {
    return runtimes.stream().map(leonardoMapper::toListRuntimeResponse).toList();
  }

  private void stubRuntimes(List<LeonardoGetRuntimeResponse> runtimes) {
    when(mockLeonardoApiClient.listRuntimesAsService())
        .thenReturn(toListRuntimeResponseList(runtimes));

    for (LeonardoGetRuntimeResponse runtime : runtimes) {
      String googleProject = leonardoMapper.toGoogleProject(runtime.getCloudContext());
      String runtimeName = runtime.getRuntimeName();

      when(mockLeonardoApiClient.getRuntimeAsService(googleProject, runtimeName))
          .thenReturn(runtime);
    }
  }

  @Test
  public void testDeleteOldRuntimesNoResults() {
    stubRuntimes(Collections.emptyList());
    assertThat(controller.deleteOldRuntimes().getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    verify(mockLeonardoApiClient, never()).deleteRuntimeAsService(any(), any(), anyBoolean());
  }

  @Test
  public void testDeleteOldRuntimesActiveRuntime() {
    stubRuntimes(List.of(runtimeWithAge(Duration.ofHours(10))));
    assertThat(controller.deleteOldRuntimes().getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    verify(mockLeonardoApiClient, never()).deleteRuntimeAsService(any(), any(), anyBoolean());
  }

  @Test
  public void testDeleteOldRuntimesActiveTooOld() {
    stubRuntimes(List.of(runtimeWithAge(RUNTIME_MAX_AGE.plusMinutes(5))));
    assertThat(controller.deleteOldRuntimes().getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    verify(mockLeonardoApiClient).deleteRuntimeAsService(any(), any(), anyBoolean());
  }

  @Test
  public void testDeleteOldRuntimesIdleYoung() {
    // Running for under the IDLE_MAX_AGE, idle for 10 hours
    stubRuntimes(
        List.of(
            runtimeWithAgeAndIdle(RUNTIME_IDLE_MAX_AGE.minusMinutes(10), Duration.ofHours(10))));
    assertThat(controller.deleteOldRuntimes().getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    verify(mockLeonardoApiClient, never()).deleteRuntimeAsService(any(), any(), anyBoolean());
  }

  @Test
  public void testDeleteOldRuntimesIdleOld() {
    // Running for >IDLE_MAX_AGE, idle for 10 hours
    stubRuntimes(
        List.of(runtimeWithAgeAndIdle(RUNTIME_IDLE_MAX_AGE.plusMinutes(15), Duration.ofHours(10))));
    assertThat(controller.deleteOldRuntimes().getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    verify(mockLeonardoApiClient).deleteRuntimeAsService(any(), any(), anyBoolean());
  }

  @Test
  public void testDeleteOldRuntimesBrieflyIdleOld() {
    // Running for >IDLE_MAX_AGE, idle for only 15 minutes
    stubRuntimes(
        List.of(
            runtimeWithAgeAndIdle(RUNTIME_IDLE_MAX_AGE.plusMinutes(15), Duration.ofMinutes(15))));
    assertThat(controller.deleteOldRuntimes().getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    verify(mockLeonardoApiClient, never()).deleteRuntimeAsService(any(), any(), anyBoolean());
  }

  @Test
  public void testDeleteOldRuntimesOtherStatusFiltered() {
    stubRuntimes(
        List.of(
            runtimeWithAge(RUNTIME_MAX_AGE.plusDays(10)).status(LeonardoRuntimeStatus.DELETING)));
    assertThat(controller.deleteOldRuntimes().getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    verify(mockLeonardoApiClient, never()).deleteRuntimeAsService(any(), any(), anyBoolean());
  }
}
