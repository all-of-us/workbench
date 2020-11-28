package org.pmiops.workbench.reporting;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.pmiops.workbench.testconfig.ReportingTestUtils.*;
import static org.pmiops.workbench.utils.TimeAssertions.assertTimeApprox;

import com.google.common.collect.ImmutableList;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.cohorts.CohortService;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.dao.projection.ProjectedReportingCohort;
import org.pmiops.workbench.db.dao.projection.ProjectedReportingInstitution;
import org.pmiops.workbench.db.dao.projection.ProjectedReportingUser;
import org.pmiops.workbench.institution.InstitutionService;
import org.pmiops.workbench.model.ReportingSnapshot;
import org.pmiops.workbench.model.ReportingUser;
import org.pmiops.workbench.model.ReportingWorkspace;
import org.pmiops.workbench.reporting.snapshot.ReportingSnapshotService;
import org.pmiops.workbench.reporting.snapshot.ReportingSnapshotServiceImpl;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.testconfig.ReportingTestConfig;
import org.pmiops.workbench.testconfig.ReportingTestUtils;
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

  @MockBean private CohortService mockCohortService;
  @MockBean private InstitutionService mockInstitutionService;
  @MockBean private UserService mockUserService;
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
  public static class Configuration {
    @Bean
    public Clock getClock() {
      return new FakeClock(NOW_INSTANT);
    }
  }

  @Before
  public void setup() {}

  @Test
  public void testGetSnapshot_noEntries() {
    final ReportingSnapshot snapshot = reportingSnapshotService.takeSnapshot();
    assertThat(snapshot.getCaptureTimestamp()).isEqualTo(NOW_EPOCH_MILLI);
    assertThat(snapshot.getCohorts()).isEmpty();
    assertThat(snapshot.getInstitutions()).isEmpty();
    assertThat(snapshot.getUsers()).isEmpty();
    assertThat(snapshot.getWorkspaces()).isEmpty();
  }

  @Test
  public void testGetSnapshot_someEntries() {
    mockUsers();
    mockWorkspaces();
    mockCohorts();
    mockInstitutions();

    final ReportingSnapshot snapshot = reportingSnapshotService.takeSnapshot();
    assertTimeApprox(snapshot.getCaptureTimestamp(), NOW_INSTANT.toEpochMilli());

    assertThat(snapshot.getCohorts()).hasSize(1);
    assertCohortFields(snapshot.getCohorts().get(0));

    assertThat(snapshot.getUsers()).hasSize(2);
    final ReportingUser user = snapshot.getUsers().get(0);
    assertDtoUserFields(user);

    assertThat(snapshot.getWorkspaces()).hasSize(1);
    final ReportingWorkspace workspace = snapshot.getWorkspaces().get(0);
    ReportingTestUtils.assertDtoWorkspaceFields(workspace);

    assertThat(snapshot.getInstitutions()).hasSize(1);
    assertInstitutionFields(snapshot.getInstitutions().get(0));
  }

  private void mockUsers() {
    final List<ProjectedReportingUser> users =
        ImmutableList.of(mockProjectedUser(), mockProjectedUser());
    doReturn(users).when(mockUserService).getReportingUsers();
  }

  private void mockWorkspaces() {
    doReturn(ImmutableList.of(mockProjectedWorkspace()))
        .when(mockWorkspaceService)
        .getReportingWorkspaces();
  }

  private void mockCohorts() {
    final ProjectedReportingCohort mockCohort = mockProjectedReportingCohort();
    doReturn(ImmutableList.of(mockCohort)).when(mockCohortService).getReportingCohorts();
  }

  private void mockInstitutions() {
    final ProjectedReportingInstitution mockInstitution =
        ReportingTestUtils.mockProjectedReportingInstitution();
    doReturn(ImmutableList.of(mockInstitution))
        .when(mockInstitutionService)
        .getReportingInstitutions();
  }
}
