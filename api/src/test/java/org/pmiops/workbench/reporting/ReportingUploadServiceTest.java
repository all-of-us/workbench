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
import com.google.common.collect.ImmutableMap;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.db.dao.projection.ProjectedReportingUser;
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

  @Autowired
  private ReportingTestFixture<DbUser, ProjectedReportingUser, ReportingUser> userFixture;

  @Captor private ArgumentCaptor<QueryJobConfiguration> queryJobConfigurationCaptor;
  @Captor private ArgumentCaptor<InsertAllRequest> insertAllRequestCaptor;

  @TestConfiguration
  @Import({ReportingUploadServiceStreamingImpl.class, ReportingTestConfig.class})
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
  public void testUploadSnapshot_streaming() {
    testUploadSnapshot_streaming(reportingSnapshot);
  }

  @Test
  public void testUploadSnapshot_streaming_with_nulls() {
    testUploadSnapshot_streaming(snapshotWithNulls);
  }

  @Test
  public void testUploadSnapshot_streaming_empty() {
    reportingUploadServiceStreamingImpl.uploadSnapshot(ReportingTestUtils.EMPTY_SNAPSHOT);
    verify(mockBigQueryService, never()).insertAll(any());
  }

  private void testUploadSnapshot_streaming(ReportingSnapshot snapshot) {
    final InsertAllResponse mockInsertAllResponse = mock(InsertAllResponse.class);
    doReturn(Collections.emptyMap()).when(mockInsertAllResponse).getInsertErrors();

    doReturn(mockInsertAllResponse)
        .when(mockBigQueryService)
        .insertAll(any(InsertAllRequest.class));
    reportingUploadServiceStreamingImpl.uploadSnapshot(snapshot);

    verify(mockBigQueryService, times(countPopulatedTables(snapshot)))
        .insertAll(insertAllRequestCaptor.capture());
    final List<InsertAllRequest> requests = insertAllRequestCaptor.getAllValues();

    assertThat(requests).hasSize(countPopulatedTables(snapshot));

    final Map<String, InsertAllRequest> tableIdToInsertAllRequest =
        requests.stream()
            .collect(
                ImmutableMap.toImmutableMap(r -> r.getTable().getTable(), Function.identity()));

    final InsertAllRequest userRequest = tableIdToInsertAllRequest.get("user");
    assertThat(userRequest).isNotNull();

    final List<RowToInsert> userRows = userRequest.getRows();
    assertThat(userRows).hasSize(snapshot.getUsers().size());
    assertThat(userRows.get(0).getId())
        .hasLength(InsertAllRequestPayloadTransformer.INSERT_ID_LENGTH);
    final Map<String, Object> userColumnToValue = userRows.get(0).getContent();
    assertThat((String) userColumnToValue.get("city")).isEqualTo(USER__CITY);
    assertThat((long) userColumnToValue.get("institution_id")).isEqualTo(USER__INSTITUTION_ID);

    final InsertAllRequest workspaceRequest = tableIdToInsertAllRequest.get("workspace");
    assertThat(workspaceRequest).isNotNull();
    assertThat(workspaceRequest.getRows()).hasSize(3);

    final Map<String, Object> workspaceColumnValues =
        workspaceRequest.getRows().get(0).getContent();
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

    final InsertAllRequest institutionRequest = tableIdToInsertAllRequest.get("institution");
    assertThat(institutionRequest).isNotNull();
    assertThat(institutionRequest.getRows()).hasSize(1);
    final RowToInsert firstInstitutionRow = institutionRequest.getRows().get(0);
    assertThat(firstInstitutionRow.getContent().get("short_name"))
        .isEqualTo(INSTITUTION__SHORT_NAME);
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
}
