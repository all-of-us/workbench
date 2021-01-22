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
import static org.pmiops.workbench.testconfig.ReportingTestUtils.INSTITUTION__SHORT_NAME;
import static org.pmiops.workbench.testconfig.ReportingTestUtils.countPopulatedTables;
import static org.pmiops.workbench.testconfig.ReportingTestUtils.createEmptySnapshot;
import static org.pmiops.workbench.testconfig.ReportingTestUtils.createReportingCohort;
import static org.pmiops.workbench.testconfig.ReportingTestUtils.createReportingDataset;
import static org.pmiops.workbench.testconfig.ReportingTestUtils.createReportingInstitution;
import static org.pmiops.workbench.testconfig.fixtures.ReportingUserFixture.USER__CITY;
import static org.pmiops.workbench.testconfig.fixtures.ReportingUserFixture.USER__INSTITUTION_ID;
import static org.pmiops.workbench.utils.TimeAssertions.assertTimeApprox;

import com.google.cloud.bigquery.InsertAllRequest;
import com.google.cloud.bigquery.InsertAllRequest.RowToInsert;
import com.google.cloud.bigquery.InsertAllResponse;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableResult;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
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
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.model.BillingStatus;
import org.pmiops.workbench.model.ReportingSnapshot;
import org.pmiops.workbench.model.ReportingUser;
import org.pmiops.workbench.model.ReportingWorkspace;
import org.pmiops.workbench.reporting.insertion.InsertAllRequestPayloadTransformer;
import org.pmiops.workbench.reporting.insertion.WorkspaceColumnValueExtractor;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.testconfig.ReportingTestConfig;
import org.pmiops.workbench.testconfig.ReportingTestUtils;
import org.pmiops.workbench.testconfig.fixtures.ReportingTestFixture;
import org.springframework.beans.factory.annotation.Autowired;
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

  @MockBean private BigQueryService mockBigQueryService;

  @Autowired private ReportingUploadService reportingUploadServiceStreamingImpl;

  @Autowired private ReportingTestFixture<DbUser, ReportingUser> userFixture;

  @Captor private ArgumentCaptor<QueryJobConfiguration> queryJobConfigurationCaptor;
  @Captor private ArgumentCaptor<InsertAllRequest> insertAllRequestCaptor;

  @TestConfiguration
  @Import({ReportingUploadServiceImpl.class, ReportingTestConfig.class})
  @MockBean({ReportingVerificationService.class})
  public static class config {
    @Bean
    public Clock getClock() {
      return new FakeClock(NOW);
    }
  }

  @Before
  public void setup() {
    reportingSnapshot =
        createEmptySnapshot()
            .captureTimestamp(NOW.toEpochMilli())
            .cohorts(ImmutableList.of(createReportingCohort()))
            .datasets(ImmutableList.of(createReportingDataset()))
            .institutions(ImmutableList.of(createReportingInstitution()))
            .users(
                ImmutableList.of(
                    userFixture.createDto(),
                    new ReportingUser()
                        .username("ted@aou.biz")
                        .givenName("Ted")
                        .disabled(true)
                        .userId(202L),
                    new ReportingUser()
                        .username("socrates@aou.biz")
                        .givenName("So-Crates")
                        .disabled(false)
                        .userId(303L),
                    userFixture.createDto()))
            .workspaces(
                ImmutableList.of(
                    new ReportingWorkspace()
                        .workspaceId(201L)
                        .name("Circle K")
                        .creationTime(THEN)
                        .creatorId(101L),
                    new ReportingWorkspace()
                        .workspaceId(202L)
                        .name("Wyld Stallyns")
                        .creationTime(THEN)
                        .creatorId(101L),
                    new ReportingWorkspace()
                        .workspaceId(203L)
                        .name("You-us said what we-us are saying right now.")
                        .creationTime(THEN)
                        .creatorId(202L)));

    snapshotWithNulls =
        createEmptySnapshot()
            .captureTimestamp(NOW.toEpochMilli())
            .institutions(ImmutableList.of(createReportingInstitution().displayName(null)))
            .users(
                ImmutableList.of(
                    userFixture.createDto(),
                    new ReportingUser()
                        .username("america@usa.gov")
                        .givenName(null)
                        .disabled(false)
                        .userId(202L),
                    new ReportingUser().username(null).givenName(null).disabled(true).userId(303L)))
            .workspaces(
                ImmutableList.of(
                    new ReportingWorkspace()
                        .workspaceId(201L)
                        .name(null)
                        .creationTime(THEN)
                        .creatorId(101L),
                    new ReportingWorkspace()
                        .workspaceId(202L)
                        .name("Work Work Work")
                        .creationTime(THEN)
                        .creatorId(101L),
                    new ReportingWorkspace()
                        .workspaceId(203L)
                        .name(null)
                        .creationTime(THEN)
                        .creatorId(202L)));

    final TableResult mockTableResult = mock(TableResult.class);
    doReturn(99L).when(mockTableResult).getTotalRows();

    doReturn(mockTableResult)
        .when(mockBigQueryService)
        .executeQuery(any(QueryJobConfiguration.class), anyLong());

    final InsertAllResponse mockInsertAllResponse = mock(InsertAllResponse.class);
    doReturn(Collections.emptyMap()).when(mockInsertAllResponse).getInsertErrors();

    doReturn(mockInsertAllResponse)
        .when(mockBigQueryService)
        .insertAll(any(InsertAllRequest.class));
  }

  @Test
  public void testUploadSnapshot() {
    final InsertAllResponse mockInsertAllResponse = mock(InsertAllResponse.class);
    doReturn(Collections.emptyMap()).when(mockInsertAllResponse).getInsertErrors();

    doReturn(mockInsertAllResponse)
        .when(mockBigQueryService)
        .insertAll(any(InsertAllRequest.class));
    reportingUploadServiceStreamingImpl.uploadSnapshot(reportingSnapshot);

    verify(mockBigQueryService, times(7)).insertAll(insertAllRequestCaptor.capture());
    final List<InsertAllRequest> requests = insertAllRequestCaptor.getAllValues();

    // assume we need at least one split collection
    assertThat(requests.size()).isGreaterThan(countPopulatedTables(reportingSnapshot));

    final Multimap<String, InsertAllRequest> tableIdToInsertAllRequest =
        Multimaps.index(requests, r -> r.getTable().getTable());
    final Collection<InsertAllRequest> userRequests = tableIdToInsertAllRequest.get("user");
    assertThat(userRequests).isNotEmpty();

    final List<RowToInsert> userRows = userRequests.stream().findFirst().get().getRows();
    assertThat(userRows).hasSize(2); // batch size
    assertThat(userRows.get(0).getId())
        .hasLength(InsertAllRequestPayloadTransformer.INSERT_ID_LENGTH);
    final Map<String, Object> userColumnToValue = userRows.get(0).getContent();
    assertThat((String) userColumnToValue.get("city")).isEqualTo(USER__CITY);
    assertThat((long) userColumnToValue.get("institution_id")).isEqualTo(USER__INSTITUTION_ID);

    final Optional<InsertAllRequest> workspaceRequest =
        tableIdToInsertAllRequest.get("workspace").stream().findFirst();
    assertThat(workspaceRequest).isPresent();
    assertThat(workspaceRequest.get().getRows()).hasSize(2);

    final Map<String, Object> workspaceColumnValues =
        workspaceRequest.get().getRows().get(0).getContent();
    assertThat(
            workspaceColumnValues.get(
                WorkspaceColumnValueExtractor.WORKSPACE_ID.getParameterName()))
        .isEqualTo(201L);
    final Optional<OffsetDateTime> creationTime =
        rowToInsertStringToOffsetTimestamp(
            (String)
                workspaceColumnValues.get(
                    WorkspaceColumnValueExtractor.CREATION_TIME.getParameterName()));
    assertThat(creationTime).isPresent();
    assertTimeApprox(creationTime.get(), THEN);
    assertThat(
            workspaceColumnValues.get(WorkspaceColumnValueExtractor.CREATOR_ID.getParameterName()))
        .isEqualTo(101L);

    final Optional<InsertAllRequest> institutionRequest =
        tableIdToInsertAllRequest.get("institution").stream().findFirst();
    assertThat(institutionRequest).isPresent();
    assertThat(institutionRequest.get().getRows()).hasSize(1);
    final RowToInsert firstInstitutionRow = institutionRequest.get().getRows().get(0);
    assertThat(firstInstitutionRow.getContent().get("short_name"))
        .isEqualTo(INSTITUTION__SHORT_NAME);
  }

  @Test
  public void testUploadSnapshot_streaming_with_nulls() {
    final InsertAllResponse mockInsertAllResponse = mock(InsertAllResponse.class);
    doReturn(Collections.emptyMap()).when(mockInsertAllResponse).getInsertErrors();

    doReturn(mockInsertAllResponse)
        .when(mockBigQueryService)
        .insertAll(any(InsertAllRequest.class));
    reportingUploadServiceStreamingImpl.uploadSnapshot(snapshotWithNulls);

    verify(mockBigQueryService, times(5)).insertAll(insertAllRequestCaptor.capture());
    final List<InsertAllRequest> requests = insertAllRequestCaptor.getAllValues();

    // assume we need at least one split collection
    assertThat(requests.size()).isGreaterThan(countPopulatedTables(snapshotWithNulls));

    final Multimap<String, InsertAllRequest> tableIdToInsertAllRequest =
        Multimaps.index(requests, r -> r.getTable().getTable());

    final Collection<InsertAllRequest> userRequests = tableIdToInsertAllRequest.get("user");
    assertThat(userRequests).isNotEmpty();

    final List<RowToInsert> userRows = userRequests.stream().findFirst().get().getRows();
    assertThat(userRows).hasSize(2); // batch size
    assertThat(userRows.get(0).getId())
        .hasLength(InsertAllRequestPayloadTransformer.INSERT_ID_LENGTH);
    final Map<String, Object> userColumnToValue = userRows.get(0).getContent();
    assertThat((String) userColumnToValue.get("city")).isEqualTo(USER__CITY);
    assertThat((long) userColumnToValue.get("institution_id")).isEqualTo(USER__INSTITUTION_ID);

    final Optional<InsertAllRequest> workspaceRequest =
        tableIdToInsertAllRequest.get("workspace").stream().findFirst();
    assertThat(workspaceRequest).isPresent();
    assertThat(workspaceRequest.get().getRows()).hasSize(2);

    final Map<String, Object> workspaceColumnValues =
        workspaceRequest.get().getRows().get(0).getContent();
    assertThat(
            workspaceColumnValues.get(
                WorkspaceColumnValueExtractor.WORKSPACE_ID.getParameterName()))
        .isEqualTo(201L);
    final Optional<OffsetDateTime> creationTime =
        rowToInsertStringToOffsetTimestamp(
            (String)
                workspaceColumnValues.get(
                    WorkspaceColumnValueExtractor.CREATION_TIME.getParameterName()));
    assertThat(creationTime).isPresent();
    assertTimeApprox(creationTime.get(), THEN);
    assertThat(
            workspaceColumnValues.get(WorkspaceColumnValueExtractor.CREATOR_ID.getParameterName()))
        .isEqualTo(101L);

    final Optional<InsertAllRequest> institutionRequest =
        tableIdToInsertAllRequest.get("institution").stream().findFirst();
    assertThat(institutionRequest).isPresent();
    assertThat(institutionRequest.get().getRows()).hasSize(1);
    final RowToInsert firstInstitutionRow = institutionRequest.get().getRows().get(0);
    assertThat(firstInstitutionRow.getContent().get("short_name"))
        .isEqualTo(INSTITUTION__SHORT_NAME);
  }

  @Test
  public void testUploadSnapshot_streaming_empty() {
    reportingUploadServiceStreamingImpl.uploadSnapshot(ReportingTestUtils.EMPTY_SNAPSHOT);
    verify(mockBigQueryService, never()).insertAll(any());
  }

  @Test
  public void testUploadSnapshot_nullEnum() {
    final ReportingWorkspace workspace = new ReportingWorkspace();
    workspace.setWorkspaceId(101L);
    workspace.setBillingStatus(null);
    final ReportingSnapshot snapshot =
        createEmptySnapshot().captureTimestamp(0L).workspaces(ImmutableList.of(workspace));
    reportingUploadServiceStreamingImpl.uploadSnapshot(snapshot);
    verify(mockBigQueryService).insertAll(insertAllRequestCaptor.capture());

    final InsertAllRequest insertAllRequest = insertAllRequestCaptor.getValue();
    assertThat(insertAllRequest.getRows()).hasSize(1);
    final Map<String, Object> content = insertAllRequest.getRows().get(0).getContent();
    assertThat(content).hasSize(2);
    assertThat(content.getOrDefault("billing_status", BillingStatus.INACTIVE))
        .isEqualTo(BillingStatus.INACTIVE);
  }

  @Test
  public void testUploadWithManyBatches() {
    final ReportingSnapshot largeSnapshot =
        createEmptySnapshot()
            .captureTimestamp(NOW.toEpochMilli())
            .users(
                IntStream.range(0, 21)
                    .mapToObj(
                        id ->
                            new ReportingUser()
                                .username("bill@aou.biz")
                                .givenName("Bill")
                                .disabled(false)
                                .userId((long) id))
                    .collect(ImmutableList.toImmutableList()))
            .workspaces(
                ImmutableList.of(
                    new ReportingWorkspace()
                        .workspaceId(303L)
                        .name("Circle K")
                        .creationTime(THEN)
                        .creatorId(101L)))
            .cohorts(ImmutableList.of(ReportingTestUtils.createReportingCohort()))
            .institutions(ImmutableList.of(ReportingTestUtils.createReportingInstitution()));

    reportingUploadServiceStreamingImpl.uploadSnapshot(largeSnapshot);
    verify(mockBigQueryService, times(14)).insertAll(insertAllRequestCaptor.capture());
  }
}
