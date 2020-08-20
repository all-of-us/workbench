package org.pmiops.workbench.reporting;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.pmiops.workbench.cohortbuilder.util.QueryParameterValues.timestampQpvToInstant;
import static org.pmiops.workbench.cohortbuilder.util.QueryParameterValues.timestampStringToInstant;

import com.google.cloud.bigquery.InsertAllRequest;
import com.google.cloud.bigquery.InsertAllRequest.RowToInsert;
import com.google.cloud.bigquery.InsertAllResponse;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.TableResult;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
import org.pmiops.workbench.reporting.insertion.WorkspaceParameter;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.utils.TestMockFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Test all implementations of ReportingUploadService to save on setup code. If this becomes too
 * complex (e.g. by having multiple public methods on each service), then we could share the setup
 * code and have separate tests.
 */
@RunWith(SpringRunner.class)
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class ReportingUploadServiceTest {
  private static final Instant NOW = Instant.parse("2000-01-01T00:00:00.00Z");
  private static final Instant THEN = Instant.parse("1989-02-17T00:00:00.00Z");

  private ReportingSnapshot reportingSnapshot;

  @MockBean private BigQueryService mockBigQueryService;
  @MockBean private Stopwatch mockStopwatch;

  @Autowired
  @Qualifier("REPORTING_UPLOAD_SERVICE_DML_IMPL")
  private ReportingUploadService reportingUploadServiceDmlImpl;

  @Autowired
  @Qualifier("REPORTING_UPLOAD_SERVICE_STREAMING_IMPL")
  private ReportingUploadService reportingUploadServiceStreamingImpl;

  @Captor private ArgumentCaptor<QueryJobConfiguration> queryJobConfigurationCaptor;
  @Captor private ArgumentCaptor<InsertAllRequest> insertAllRequestCaptor;
  private static final int RESEARCHER_COLUMN_COUNT = 4;
  private static final int WORKSPACE_COLUMN_COUNT = 5;

  @TestConfiguration
  @Import({ReportingUploadServiceDmlImpl.class, ReportingUploadServiceStreamingImpl.class})
  @MockBean(Stopwatch.class)
  public static class config {
    @Bean
    public Clock getClock() {
      return new FakeClock(NOW);
    }

    @Bean
    public WorkbenchConfig workbenchConfig() {
      final WorkbenchConfig workbenchConfig = WorkbenchConfig.createEmptyConfig();
      workbenchConfig.reporting.dataset = "wb_reporting";
      workbenchConfig.reporting.maxRowsPerInsert = 5;
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
                        .workspaceId(201L)
                        .name("Circle K")
                        .creationTime(THEN.toEpochMilli())
                        .fakeSize(4444L)
                        .creatorId(101L),
                    new ReportingWorkspace()
                        .workspaceId(202L)
                        .name("Wyld Stallyns")
                        .creationTime(THEN.toEpochMilli())
                        .fakeSize(4444L)
                        .creatorId(101L),
                    new ReportingWorkspace()
                        .workspaceId(203L)
                        .name("You-us said what we-us are saying right now.")
                        .creationTime(THEN.toEpochMilli())
                        .fakeSize(4444L)
                        .creatorId(202L)));

    final TableResult mockTableResult = mock(TableResult.class);
    doReturn(99L).when(mockTableResult).getTotalRows();

    doReturn(mockTableResult)
        .when(mockBigQueryService)
        .executeQuery(any(QueryJobConfiguration.class), anyLong());

    TestMockFactory.stubStopwatch(mockStopwatch, Duration.ofMillis(250));
  }

  @Test
  public void testUploadSnapshot_dml() {
    reportingUploadServiceDmlImpl.uploadSnapshot(reportingSnapshot);
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

    assertThat(
            (double)
                timestampQpvToInstant(jobs.get(1).getNamedParameters().get("creation_time__0"))
                    .toEpochMilli())
        .isWithin(500.0)
        .of(THEN.toEpochMilli());
  }

  @Test
  public void testUploadSnapshot_dmlBatchInserts() {
    final ReportingSnapshot largeSnapshot =
        new ReportingSnapshot().captureTimestamp(NOW.toEpochMilli());
    // It's certainly possible to make the batch size an environment configuration value and
    // inject it so that we don't need this many rows in the test, but I didn't think that was
    // necessarily a good enoughh reason to add configurable state.
    final List<ReportingResearcher> researchers =
        IntStream.range(0, 21)
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
                .workspaceId(303L)
                .name("Circle K")
                .creationTime(THEN.toEpochMilli())
                .fakeSize(4444L)
                .creatorId(101L)));

    reportingUploadServiceDmlImpl.uploadSnapshot(largeSnapshot);
    verify(mockBigQueryService, times(6))
        .executeQuery(queryJobConfigurationCaptor.capture(), anyLong());

    final List<QueryJobConfiguration> jobs = queryJobConfigurationCaptor.getAllValues();
    assertThat(jobs).hasSize(6);

    assertThat(jobs.get(0).getNamedParameters()).hasSize(RESEARCHER_COLUMN_COUNT * 5 + 1);
    assertThat(jobs.get(4).getNamedParameters()).hasSize(RESEARCHER_COLUMN_COUNT + 1);
    assertThat(jobs.get(5).getNamedParameters()).hasSize(WORKSPACE_COLUMN_COUNT + 1);

    final QueryParameterValue creationTime =
        jobs.get(5).getNamedParameters().get("creation_time__0");
    assertThat(creationTime).isNotNull();
    final Instant instant = QueryParameterValues.timestampQpvToInstant(creationTime);
    assertThat((double) instant.toEpochMilli()).isWithin(500.0).of(THEN.toEpochMilli());
  }

  @Test
  public void testUploadSnapshot_streaming() {
    final InsertAllResponse mockInsertAllResponse = mock(InsertAllResponse.class);
    doReturn(Collections.emptyMap()).when(mockInsertAllResponse).getInsertErrors();

    doReturn(mockInsertAllResponse)
        .when(mockBigQueryService)
        .insertAll(any(InsertAllRequest.class));
    final ReportingJobResult result =
        reportingUploadServiceStreamingImpl.uploadSnapshot(reportingSnapshot);
    verify(mockBigQueryService, times(2)).insertAll(insertAllRequestCaptor.capture());
    final List<InsertAllRequest> requests = insertAllRequestCaptor.getAllValues();

    assertThat(requests).hasSize(2);

    final List<RowToInsert> researcherRows = requests.get(0).getRows();
    assertThat(researcherRows).hasSize(3);
    assertThat(researcherRows.get(0).getId()).hasLength(16);
    assertThat(researcherRows.get(0).getContent()).hasSize(RESEARCHER_COLUMN_COUNT + 1);

    final List<RowToInsert> workspaceRows = requests.get(1).getRows();
    assertThat(workspaceRows).hasSize(3);

    final Map<String, Object> workspaceColumnValues = workspaceRows.get(0).getContent();
    assertThat(workspaceColumnValues.get(WorkspaceParameter.WORKSPACE_ID.getParameterName()))
        .isEqualTo(201L);
    final Instant creationInstant =
        timestampStringToInstant(
            (String)
                workspaceColumnValues.get(WorkspaceParameter.CREATION_TIME.getParameterName()));
    assertThat((double) creationInstant.toEpochMilli()).isWithin(500.0).of(THEN.toEpochMilli());
    assertThat(workspaceColumnValues.get(WorkspaceParameter.CREATOR_ID.getParameterName()))
        .isEqualTo(101L);
  }
}
