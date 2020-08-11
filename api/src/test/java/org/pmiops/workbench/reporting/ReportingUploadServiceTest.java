package org.pmiops.workbench.reporting;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.cloud.bigquery.EmptyTableResult;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.common.collect.ImmutableList;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.cohortbuilder.util.QueryParameterValues;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.model.ReportingResearcher;
import org.pmiops.workbench.model.ReportingSnapshot;
import org.pmiops.workbench.model.ReportingWorkspace;
import org.pmiops.workbench.test.FakeClock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class ReportingUploadServiceTest {

  private static final Instant NOW = Instant.parse("2000-01-01T00:00:00.00Z");
  private static final Instant THEN = Instant.parse("1989-02-17T00:00:00.00Z");

  private ReportingSnapshot reportingSnapshot;

  @MockBean private BigQueryService mockBigQueryService;

  @Autowired private ReportingUploadService reportingUploadService;
  @Captor private ArgumentCaptor<QueryJobConfiguration> queryJobConfigurationCaptor;

  @TestConfiguration
  @Import({ReportingUploadServiceImpl.class})
  public static class config {
    @Bean
    public Clock getClock() {
      return new FakeClock(NOW);
    }

    @Bean
    public WorkbenchConfig workbenchConfig() {
      final WorkbenchConfig workbenchConfig = WorkbenchConfig.createEmptyConfig();
      workbenchConfig.reporting.dataset = "wb_reporting";
      workbenchConfig.server.projectId = "rw-wb-unit-test";
      return workbenchConfig;
    }
  }

  @Before
  public void setup() {
    reportingSnapshot =
        new ReportingSnapshot()
            .captureTimestamp(NOW.toEpochMilli())
            .researchers(
                ImmutableList.of(
                    new ReportingResearcher()
                        .username("bill@aou.biz")
                        .firstName("Bill")
                        .isDisabled(false)
                        .researcherId(101L),
                    new ReportingResearcher()
                        .username("ted@aou.biz")
                        .firstName("Ted")
                        .isDisabled(true)
                        .researcherId(202L),
                    new ReportingResearcher()
                        .username("socrates@aou.biz")
                        .firstName("So-Crates")
                        .isDisabled(false)
                        .researcherId(303L)))
            .workspaces(
                ImmutableList.of(
                    new ReportingWorkspace()
                        .name("Circle K")
                        .creationTime(THEN.toEpochMilli())
                        .fakeSize(4444L)
                        .creatorId(101L),
                    new ReportingWorkspace()
                        .name("Wyld Stallyns")
                        .creationTime(THEN.toEpochMilli())
                        .fakeSize(4444L)
                        .creatorId(101L),
                    new ReportingWorkspace()
                        .name("You-us said what we-us are saying right now.")
                        .creationTime(THEN.toEpochMilli())
                        .fakeSize(4444L)
                        .creatorId(202L)));
    doReturn(new EmptyTableResult())
        .when(mockBigQueryService)
        .executeQuery(any(QueryJobConfiguration.class), anyLong());
  }

  @Test
  public void testUploadSnapshot() {
    reportingUploadService.uploadSnapshot(reportingSnapshot);
    verify(mockBigQueryService, times(2))
        .executeQuery(queryJobConfigurationCaptor.capture(), anyLong());

    final List<QueryJobConfiguration> jobs = queryJobConfigurationCaptor.getAllValues();
    assertThat(jobs).hasSize(2);

    final QueryJobConfiguration job0 = jobs.get(0);
    final String query0 = job0.getQuery();
    assertThat(query0).isNotEmpty();

    final String expandedQuery =
        QueryParameterValues.formatQuery(QueryParameterValues.replaceNamedParameters(job0));

    assertThat(expandedQuery).containsMatch("INSERT\\s+INTO");
  }

  @Test
  public void testUploadSnapshot_batchInserts() {
    final ReportingSnapshot largeSnapshot =
        new ReportingSnapshot().captureTimestamp(NOW.toEpochMilli());
    // It's certainly possible to make the batch size an environment configuration value and
    // inject it so that we don't need this many rows in the test, but I didn't think that was
    // necessarily a good enoughh reason to add configurable state.
    final List<ReportingResearcher> researchers =
        IntStream.range(0, 2001)
            .mapToObj(
                id ->
                    new ReportingResearcher()
                        .username("bill@aou.biz")
                        .firstName("Bill")
                        .isDisabled(false)
                        .researcherId((long) id))
            .collect(ImmutableList.toImmutableList());
    largeSnapshot.setResearchers(researchers);
    largeSnapshot.setWorkspaces(
        ImmutableList.of(
            new ReportingWorkspace()
                .name("Circle K")
                .creationTime(THEN.toEpochMilli())
                .fakeSize(4444L)
                .creatorId(101L)));

    reportingUploadService.uploadSnapshot(largeSnapshot);
    verify(mockBigQueryService, times(6))
        .executeQuery(queryJobConfigurationCaptor.capture(), anyLong());

    final List<QueryJobConfiguration> jobs = queryJobConfigurationCaptor.getAllValues();
    assertThat(jobs).hasSize(6);
    final int researcherColumnCount = 4;
    final int workspaceColumnCount = 5;

    assertThat(jobs.get(0).getNamedParameters()).hasSize(researcherColumnCount * 500 + 1);
    assertThat(jobs.get(4).getNamedParameters()).hasSize(researcherColumnCount * 1 + 1);

    assertThat(jobs.get(4).getNamedParameters()).hasSize(workspaceColumnCount * 1 + 1);
  }
}
