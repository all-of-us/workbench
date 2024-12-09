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
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.exfiltration.EgressEventService;
import org.pmiops.workbench.model.VwbEgressEventRequest;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Import(FakeClockConfiguration.class)
public class VwbEgressAdminControllerTest {
  @Mock private Provider<DbUser> mockUserProvider;
  @Mock private Provider<WorkbenchConfig> mockConfigProvider;
  @Mock private EgressEventService mockEgressEventService;

  private VwbEgressAdminController vwbEgressAdminController;

  private WorkbenchConfig workbenchConfig = new WorkbenchConfig();
  private VwbEgressEventRequest vwbEvent;

  @BeforeEach
  public void setUp() {
    this.vwbEgressAdminController =
        new VwbEgressAdminController(mockEgressEventService, mockConfigProvider, mockUserProvider);
    workbenchConfig.vwb = new WorkbenchConfig.VwbConfig();
    workbenchConfig.vwb.exfilManagerServiceAccount = "authorized-user@example.com";

    vwbEvent =
        new VwbEgressEventRequest()
            .userEmail("testuser@example.com")
            .workspaceId("testWorkspaceId")
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
    DbUser dbUser = new DbUser();
    dbUser.setUsername("test-user@example.com");

    when(mockConfigProvider.get()).thenReturn(workbenchConfig);
    when(mockUserProvider.get()).thenReturn(dbUser);

    ResponseEntity<Void> response = vwbEgressAdminController.createVwbEgressEvent(vwbEvent);

    verify(mockEgressEventService).handleVwbEvent(vwbEvent);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  public void testCreateVwbEgressEvent_unauthorizedUser() {
    DbUser dbUser = new DbUser();
    dbUser.setUsername("unauthorized-user@example.com");

    when(mockConfigProvider.get()).thenReturn(workbenchConfig);
    when(mockUserProvider.get()).thenReturn(dbUser);

    assertThrows(
        ForbiddenException.class,
        () -> {
          vwbEgressAdminController.createVwbEgressEvent(vwbEvent);
        });
  }
}
