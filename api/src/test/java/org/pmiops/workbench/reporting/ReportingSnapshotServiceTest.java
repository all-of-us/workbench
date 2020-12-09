package org.pmiops.workbench.reporting;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.pmiops.workbench.testconfig.ReportingTestUtils.*;
import static org.pmiops.workbench.utils.TimeAssertions.assertTimeApprox;

import com.google.common.collect.ImmutableList;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.cohorts.CohortService;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.dao.projection.ProjectedReportingCohort;
import org.pmiops.workbench.db.dao.projection.ProjectedReportingUser;
import org.pmiops.workbench.db.jdbc.ReportingNativeQueryService;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.institution.InstitutionService;
import org.pmiops.workbench.model.ReportingDataset;
import org.pmiops.workbench.model.ReportingDatasetCohort;
import org.pmiops.workbench.model.ReportingInstitution;
import org.pmiops.workbench.model.ReportingSnapshot;
import org.pmiops.workbench.model.ReportingUser;
import org.pmiops.workbench.model.ReportingWorkspace;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.testconfig.ReportingTestConfig;
import org.pmiops.workbench.testconfig.ReportingTestUtils;
import org.pmiops.workbench.testconfig.fixtures.ReportingTestFixture;
import org.pmiops.workbench.utils.mappers.CommonMappers;
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
  @MockBean private ReportingNativeQueryService mockReportingNativeQueryService;

  @Autowired private ReportingSnapshotService reportingSnapshotService;

  @Autowired
  private ReportingTestFixture<DbUser, ProjectedReportingUser, ReportingUser> userFixture;

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

  @Test
  public void testGetSnapshot_noEntries() {
    final ReportingSnapshot snapshot = reportingSnapshotService.takeSnapshot();
    assertThat(snapshot.getCaptureTimestamp()).isEqualTo(NOW_EPOCH_MILLI);
    assertThat(snapshot.getCohorts()).isEmpty();
    assertThat(snapshot.getDatasets()).isEmpty();
    assertThat(snapshot.getInstitutions()).isEmpty();
    assertThat(snapshot.getUsers()).isEmpty();
    assertThat(snapshot.getWorkspaces()).isEmpty();
  }

  @Test
  public void testGetSnapshot() {
    mockUsers();
    mockWorkspaces();
    mockCohorts();
    mockDatasets();
    mockDatasetCohorts();
    mockInstitutions();

    final ReportingSnapshot snapshot = reportingSnapshotService.takeSnapshot();
    assertTimeApprox(snapshot.getCaptureTimestamp(), NOW_INSTANT.toEpochMilli());

    assertThat(snapshot.getCohorts()).hasSize(1);
    assertCohortFields(snapshot.getCohorts().get(0));

    assertThat(snapshot.getDatasets()).hasSize(1);
    assertDatasetFields(snapshot.getDatasets().get(0));

    assertThat(snapshot.getDatasetCohorts()).hasSize(2);
    assertThat(snapshot.getDatasetCohorts().get(0).getCohortId()).isEqualTo(101L);

    assertThat(snapshot.getUsers()).hasSize(2);
    final ReportingUser user = snapshot.getUsers().get(0);
    userFixture.assertDTOFieldsMatchConstants(user);

    assertThat(snapshot.getWorkspaces()).hasSize(1);
    final ReportingWorkspace workspace = snapshot.getWorkspaces().get(0);
    ReportingTestUtils.assertDtoWorkspaceFields(workspace);

    assertThat(snapshot.getInstitutions()).hasSize(1);
    assertInstitutionFields(snapshot.getInstitutions().get(0));
  }

  private void mockUsers() {
    final List<ProjectedReportingUser> users =
        ImmutableList.of(userFixture.mockProjection(), userFixture.mockProjection());
    doReturn(users).when(mockUserService).getReportingUsers();
  }

  private void mockWorkspaces() {
    doReturn(ImmutableList.of(createDtoWorkspace()))
        .when(mockReportingNativeQueryService)
        .getReportingWorkspaces();
  }

  private void mockCohorts() {
    final ProjectedReportingCohort mockCohort = mockProjectedReportingCohort();
    doReturn(ImmutableList.of(mockCohort)).when(mockCohortService).getReportingCohorts();
  }

  private void mockDatasets() {
    final ReportingDataset dataset = createReportingDataset();
    doReturn(ImmutableList.of(dataset))
        .when(mockReportingNativeQueryService)
        .getReportingDatasets();
  }

  private void mockDatasetCohorts() {
    final ReportingDatasetCohort reportingDatasetCohort1 =
        new ReportingDatasetCohort().cohortId(101L).datasetId(202L);
    final ReportingDatasetCohort reportingDatasetCohort2 =
        new ReportingDatasetCohort().cohortId(303L).datasetId(404L);
    doReturn(ImmutableList.of(reportingDatasetCohort1, reportingDatasetCohort2))
        .when(mockReportingNativeQueryService)
        .getReportingDatasetCohorts();
  }

  private void mockInstitutions() {
    final ReportingInstitution reportingInstitution = createReportingInstitution();
    doReturn(ImmutableList.of(reportingInstitution))
        .when(mockReportingNativeQueryService)
        .getReportingInstitutions();
  }
}
