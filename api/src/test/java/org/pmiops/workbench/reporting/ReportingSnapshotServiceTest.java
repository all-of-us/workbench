package org.pmiops.workbench.reporting;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.pmiops.workbench.testconfig.ReportingTestUtils.assertInstitutionFields;
import static org.pmiops.workbench.testconfig.ReportingTestUtils.createDtoWorkspaceFreeTierUsage;
import static org.pmiops.workbench.testconfig.ReportingTestUtils.createReportingInstitution;
import static org.pmiops.workbench.utils.TimeAssertions.assertTimeApprox;

import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.db.jdbc.ReportingQueryService;
import org.pmiops.workbench.model.ReportingInstitution;
import org.pmiops.workbench.model.ReportingSnapshot;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.testconfig.ReportingTestConfig;
import org.pmiops.workbench.testconfig.ReportingTestUtils;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig
public class ReportingSnapshotServiceTest {
  private static final long NOW_EPOCH_MILLI = FakeClockConfiguration.NOW_TIME;
  private static final Instant NOW_INSTANT = Instant.ofEpochMilli(NOW_EPOCH_MILLI);

  @MockBean private ReportingQueryService mockReportingQueryService;

  @Autowired private ReportingSnapshotService reportingSnapshotService;

  @TestConfiguration
  @Import({
    FakeClockConfiguration.class,
    CommonMappers.class,
    ReportingTestConfig.class,
    ReportingTestUtils.class,
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
    assertThat(snapshot.getInstitutions()).isEmpty();
  }

  @Test
  public void testGetSnapshot() {
    mockWorkspaceFreeTierUsage();
    mockInstitutions();

    final ReportingSnapshot snapshot = reportingSnapshotService.takeSnapshot();
    assertTimeApprox(snapshot.getCaptureTimestamp(), NOW_INSTANT.toEpochMilli());

    ReportingTestUtils.assertDtoWorkspaceFreeTierUsageFields(
        snapshot.getWorkspaceFreeTierUsage().get(0));

    assertThat(snapshot.getInstitutions()).hasSize(1);
    assertInstitutionFields(snapshot.getInstitutions().get(0));
  }

  private void mockWorkspaceFreeTierUsage() {
    doReturn(Collections.singletonList(createDtoWorkspaceFreeTierUsage()))
        .when(mockReportingQueryService)
        .getWorkspaceFreeTierUsage();
  }

  private void mockInstitutions() {
    final ReportingInstitution reportingInstitution = createReportingInstitution();
    doReturn(Collections.singletonList(reportingInstitution))
        .when(mockReportingQueryService)
        .getInstitutions();
  }
}
