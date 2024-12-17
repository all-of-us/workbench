package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.inject.Provider;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.exceptions.UnauthorizedException;
import org.pmiops.workbench.exfiltration.EgressEventService;
import org.pmiops.workbench.model.VwbEgressEventRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig
public class VwbEgressAdminControllerTest {
  @Mock private Provider<DbUser> mockUserProvider;
  @Mock private Provider<WorkbenchConfig> mockConfigProvider;
  @Mock private EgressEventService mockEgressEventService;

  private VwbEgressAdminController vwbEgressAdminController;

  private WorkbenchConfig workbenchConfig = new WorkbenchConfig();
  private VwbEgressEventRequest vwbEvent;
  private DbUser authorizedUser;

  @BeforeEach
  public void setUp() {
    this.vwbEgressAdminController =
        new VwbEgressAdminController(mockEgressEventService, mockConfigProvider, mockUserProvider);
    authorizedUser = new DbUser();
    authorizedUser.setUsername("test-user@example.com");
    workbenchConfig.featureFlags = new WorkbenchConfig.FeatureFlagsConfig();
    workbenchConfig.featureFlags.enableVWBEgressMonitor = true;

    workbenchConfig.vwb = new WorkbenchConfig.VwbConfig();
    workbenchConfig.vwb.exfilManagerServiceAccount = authorizedUser.getUsername();
    when(mockConfigProvider.get()).thenReturn(workbenchConfig);

    vwbEvent =
        new VwbEgressEventRequest()
            .userEmail("testuser@example.com")
            .vwbWorkspaceId("testWorkspaceId")
            .vmName("testVmName")
            .incidentCount(1L)
            .egressMib(500.0)
            .egressMibThreshold(100.0)
            .gcpProjectId("test-gcp-project")
            .timeWindowDuration(600L)
            .timeWindowStart(Instant.now().toEpochMilli());
  }

  @Test
  public void testCreateVwbEgressEvent() {
    when(mockUserProvider.get()).thenReturn(authorizedUser);

    ResponseEntity<Void> response = vwbEgressAdminController.createVwbEgressEvent(vwbEvent);

    verify(mockEgressEventService).handleVwbEvent(vwbEvent);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  public void testCreateVwbEgressEvent_unauthorizedUser() {
    DbUser dbUser = new DbUser();
    dbUser.setUsername("unauthorized-user@example.com");

    when(mockUserProvider.get()).thenReturn(dbUser);

    assertThrows(
        UnauthorizedException.class,
        () -> {
          vwbEgressAdminController.createVwbEgressEvent(vwbEvent);
        });
  }

  @Test
  public void testCreateVwbEgressEvent_flagNotEnabled() {
    workbenchConfig.featureFlags = new WorkbenchConfig.FeatureFlagsConfig();
    workbenchConfig.featureFlags.enableVWBEgressMonitor = false;
    when(mockUserProvider.get()).thenReturn(authorizedUser);

    assertThrows(
        ForbiddenException.class,
        () -> {
          vwbEgressAdminController.createVwbEgressEvent(vwbEvent);
        });
  }
}
