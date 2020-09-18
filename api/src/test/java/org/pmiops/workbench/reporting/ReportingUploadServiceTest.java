package org.pmiops.workbench.reporting;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.pmiops.workbench.cohortbuilder.util.QueryParameterValues.rowToInsertStringToOffsetTimestamp;
import static org.pmiops.workbench.cohortbuilder.util.QueryParameterValues.timestampQpvToOffsetDateTime;
import static org.pmiops.workbench.testconfig.ReportingTestUtils.createDtoUser;
import static org.pmiops.workbench.utils.TimeAssertions.assertTimeApprox;

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
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.cohortbuilder.util.QueryParameterValues;
import org.pmiops.workbench.model.BqDtoUser;
import org.pmiops.workbench.model.BqDtoWorkspace;
import org.pmiops.workbench.model.ReportingSnapshot;
import org.pmiops.workbench.reporting.insertion.UserParameterColumn;
import org.pmiops.workbench.reporting.insertion.WorkspaceParameterColumn;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.testconfig.ReportingTestConfig;
import org.pmiops.workbench.utils.TestMockFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Test all implementations of ReportingUploadService to save on setup code. If this becomes too
 * complex (e.g. by having multiple public methods on each service), then we could share the setup
 * code and have separate tests.
 */
@RunWith(SpringRunner.class)
public class ReportingUploadServiceTest {
  private static final Instant NOW = Instant.parse("2000-01-01T00:00:00.00Z");
  private static final Instant THEN_INSTANT = Instant.parse("1989-02-17T00:00:00.00Z");
  private static final OffsetDateTime THEN = OffsetDateTime.ofInstant(THEN_INSTANT, ZoneOffset.UTC);

  private ReportingSnapshot reportingSnapshot;
  private ReportingSnapshot snapshotWithNulls;
  private ReportingSnapshot emptySnapshot;

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
  private static final int USER_COLUMN_COUNT = UserParameterColumn.values().length;
  private static final int WORKSPACE_COLUMN_COUNT = WorkspaceParameterColumn.values().length;

  @TestConfiguration
  @Import({
    ReportingUploadServiceDmlImpl.class,
    ReportingUploadServiceStreamingImpl.class,
    ReportingTestConfig.class
  })
  @MockBean(Stopwatch.class)
  public static class config {
    @Bean
    public Clock getClock() {
      return new FakeClock(NOW);
    }
  }

  @Before
  public void setup() {
    reportingSnapshot =
        new ReportingSnapshot()
            .captureTimestamp(NOW.toEpochMilli())
            .users(
                ImmutableList.of(
                    createDtoUser(),
                    new BqDtoUser()
                        .username("ted@aou.biz")
                        .givenName("Ted")
                        .disabled(true)
                        .userId(202L),
                    new BqDtoUser()
                        .username("socrates@aou.biz")
                        .givenName("So-Crates")
                        .disabled(false)
                        .userId(303L)))
            .workspaces(
                ImmutableList.of(
                    new BqDtoWorkspace()
                        .workspaceId(201L)
                        .name("Circle K")
                        .creationTime(THEN)
                        .creatorId(101L),
                    new BqDtoWorkspace()
                        .workspaceId(202L)
                        .name("Wyld Stallyns")
                        .creationTime(THEN)
                        .creatorId(101L),
                    new BqDtoWorkspace()
                        .workspaceId(203L)
                        .name("You-us said what we-us are saying right now.")
                        .creationTime(THEN)
                        .creatorId(202L)));

    snapshotWithNulls =
        new ReportingSnapshot()
            .captureTimestamp(NOW.toEpochMilli())
            .users(
                ImmutableList.of(
                    new BqDtoUser()
                        .username(null)
                        .givenName("Nullson")
                        .disabled(false)
                        .userId(101L),
                    new BqDtoUser()
                        .username("america@usa.gov")
                        .givenName(null)
                        .disabled(false)
                        .userId(202L),
                    new BqDtoUser().username(null).givenName(null).disabled(true).userId(303L)))
            .workspaces(
                ImmutableList.of(
                    new BqDtoWorkspace()
                        .workspaceId(201L)
                        .name(null)
                        .creationTime(THEN)
                        .creatorId(101L),
                    new BqDtoWorkspace()
                        .workspaceId(202L)
                        .name("Work Work Work")
                        .creationTime(THEN)
                        .creatorId(101L),
                    new BqDtoWorkspace()
                        .workspaceId(203L)
                        .name(null)
                        .creationTime(THEN)
                        .creatorId(202L)));

    emptySnapshot =
        new ReportingSnapshot()
            .captureTimestamp(NOW.toEpochMilli())
            .users(Collections.emptyList())
            .workspaces(Collections.emptyList());

    final TableResult mockTableResult = mock(TableResult.class);
    doReturn(99L).when(mockTableResult).getTotalRows();

    doReturn(mockTableResult)
        .when(mockBigQueryService)
        .executeQuery(any(QueryJobConfiguration.class), anyLong());

    TestMockFactory.stubStopwatch(mockStopwatch, Duration.ofMillis(250));
  }

  @Test
  public void testUploadSnapshot_dml() {
    testUploadSnapshot_dml(reportingSnapshot);
  }

  @Test
  public void testUploadSnapshot_dml_with_nulls() {
    testUploadSnapshot_dml(snapshotWithNulls);
  }

  @Test
  public void testUploadSnapshot_dml_empty() {
    reportingUploadServiceDmlImpl.uploadSnapshot(emptySnapshot);
    verify(mockBigQueryService, never()).executeQuery(any(), anyLong());
  }

  private void testUploadSnapshot_dml(ReportingSnapshot snapshot) {
    reportingUploadServiceDmlImpl.uploadSnapshot(snapshot);
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

    final OffsetDateTime convertedOdt =
        timestampQpvToOffsetDateTime(jobs.get(1).getNamedParameters().get("creation_time__0"))
            .get();
    assertTimeApprox(convertedOdt, THEN);
  }

  @Test
  public void testUploadSnapshot_dmlBatchInserts() {
    final ReportingSnapshot largeSnapshot =
        new ReportingSnapshot().captureTimestamp(NOW.toEpochMilli());
    // It's certainly possible to make the batch size an environment configuration value and
    // inject it so that we don't need this many rows in the test, but I didn't think that was
    // necessarily a good enoughh reason to add configurable state.
    final List<BqDtoUser> users =
        IntStream.range(0, 21)
            .mapToObj(
                id ->
                    new BqDtoUser()
                        .username("bill@aou.biz")
                        .givenName("Bill")
                        .disabled(false)
                        .userId((long) id))
            .collect(ImmutableList.toImmutableList());
    largeSnapshot.setUsers(users);
    largeSnapshot.setWorkspaces(
        ImmutableList.of(
            new BqDtoWorkspace()
                .workspaceId(303L)
                .name("Circle K")
                .creationTime(THEN)
                .creatorId(101L)));

    reportingUploadServiceDmlImpl.uploadSnapshot(largeSnapshot);
    verify(mockBigQueryService, times(6))
        .executeQuery(queryJobConfigurationCaptor.capture(), anyLong());

    final List<QueryJobConfiguration> jobs = queryJobConfigurationCaptor.getAllValues();
    assertThat(jobs).hasSize(6);

    // Since null values are omitted, map sizes will vary
    assertThat(jobs.get(0).getNamedParameters()).isNotEmpty();
    assertThat(jobs.get(4).getNamedParameters()).isNotEmpty();

    final QueryParameterValue creationTime =
        jobs.get(5).getNamedParameters().get("creation_time__0");
    assertThat(creationTime).isNotNull();
    final Optional<OffsetDateTime> creationOdt =
        QueryParameterValues.timestampQpvToOffsetDateTime(creationTime);
    assertThat(creationOdt).isPresent();
    assertTimeApprox(creationOdt.get(), THEN);
  }

  @Test
  public void testUploadSnapshot_streaming() {
    testUploadSnapshot_streaming(reportingSnapshot);
  }

  @Test
  public void testUploadSnapshot_streaming_with_nulls() {
    testUploadSnapshot_streaming(snapshotWithNulls);
  }

  @Test
  public void testUploadSnapshot_streaming_empty() {
    reportingUploadServiceStreamingImpl.uploadSnapshot(emptySnapshot);
    verify(mockBigQueryService, never()).insertAll(any());
  }

  private void testUploadSnapshot_streaming(ReportingSnapshot snapshot) {
    final InsertAllResponse mockInsertAllResponse = mock(InsertAllResponse.class);
    doReturn(Collections.emptyMap()).when(mockInsertAllResponse).getInsertErrors();

    doReturn(mockInsertAllResponse)
        .when(mockBigQueryService)
        .insertAll(any(InsertAllRequest.class));
    final ReportingJobResult result = reportingUploadServiceStreamingImpl.uploadSnapshot(snapshot);
    verify(mockBigQueryService, times(2)).insertAll(insertAllRequestCaptor.capture());
    final List<InsertAllRequest> requests = insertAllRequestCaptor.getAllValues();

    assertThat(requests).hasSize(2);

    final List<RowToInsert> userRows = requests.get(0).getRows();
    assertThat(userRows).hasSize(3);
    assertThat(userRows.get(0).getId()).hasLength(16);

    final List<RowToInsert> workspaceRows = requests.get(1).getRows();
    assertThat(workspaceRows).hasSize(3);

    final Map<String, Object> workspaceColumnValues = workspaceRows.get(0).getContent();
    assertThat(workspaceColumnValues.get(WorkspaceParameterColumn.WORKSPACE_ID.getParameterName()))
        .isEqualTo(201L);
    assertTimeApprox(
        rowToInsertStringToOffsetTimestamp(
                (String)
                    workspaceColumnValues.get(
                        WorkspaceParameterColumn.CREATION_TIME.getParameterName()))
            .get(),
        THEN);
    assertThat(workspaceColumnValues.get(WorkspaceParameterColumn.CREATOR_ID.getParameterName()))
        .isEqualTo(101L);
  }
}
