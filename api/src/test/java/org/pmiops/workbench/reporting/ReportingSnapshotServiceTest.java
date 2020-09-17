package org.pmiops.workbench.reporting;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.pmiops.workbench.testconfig.ReportingTestUtils.*;
import static org.pmiops.workbench.utils.TimeAssertions.assertTimeApprox;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.dao.projection.PrjUser;
import org.pmiops.workbench.model.BqDtoUser;
import org.pmiops.workbench.model.BqDtoWorkspace;
import org.pmiops.workbench.model.ReportingSnapshot;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.testconfig.ReportingTestConfig;
import org.pmiops.workbench.utils.TestMockFactory;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class ReportingSnapshotServiceTest {
  private static final long NOW_EPOCH_MILLI = 1594404482000L;
  private static final Instant NOW_INSTANT = Instant.ofEpochMilli(NOW_EPOCH_MILLI);

  @MockBean private UserService mockUserService;
  @MockBean private Stopwatch mockStopwatch;
  @MockBean private WorkspaceService mockWorkspaceService;
  @Autowired private ReportingSnapshotService reportingSnapshotService;

  @TestConfiguration
  @Import({
    CommonMappers.class,
    ReportingMapperImpl.class,
    ReportingTestConfig.class,
    ReportingSnapshotServiceImpl.class
  })
  @MockBean({BigQueryService.class})
  public static class config {
    @Bean
    public Clock getClock() {
      return new FakeClock(NOW_INSTANT);
    }
  }

  @Before
  public void setup() {
    TestMockFactory.stubStopwatch(mockStopwatch, Duration.ofMillis(100));
  }

  @Test
  public void testGetSnapshot_noEntries() {
    final ReportingSnapshot snapshot = reportingSnapshotService.takeSnapshot();
    assertThat(snapshot.getCaptureTimestamp()).isEqualTo(NOW_EPOCH_MILLI);
    assertThat(snapshot.getUsers()).isEmpty();
    assertThat(snapshot.getWorkspaces()).isEmpty();
  }

  @Test
  public void testGetSnapshot_someEntries() {
    mockUsers();
    mockWorkspaces();

    final ReportingSnapshot snapshot = reportingSnapshotService.takeSnapshot();
    assertTimeApprox(snapshot.getCaptureTimestamp(), NOW_INSTANT.toEpochMilli());

    assertThat(snapshot.getUsers()).hasSize(2);
    final BqDtoUser user = snapshot.getUsers().get(0);
    assertUserFields(user);

    assertThat(snapshot.getWorkspaces()).hasSize(1);
    final BqDtoWorkspace workspace = snapshot.getWorkspaces().get(0);
    assertWorkspaceFields(workspace);
  }

  private void mockUsers() {
    final List<PrjUser> users = ImmutableList.of(mockUserProjection(), mockUserProjection());
    doReturn(users).when(mockUserService).getRepotingUsers();
  }

  private void mockWorkspaces() {
    doReturn(ImmutableList.of(mockWorkspace())).when(mockWorkspaceService).getReportingWorkspaces();
  }
}
