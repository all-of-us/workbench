package org.pmiops.workbench.reporting;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.pmiops.workbench.cohortbuilder.util.QueryParameterValues.rowToInsertStringToOffsetTimestamp;
import static org.pmiops.workbench.testconfig.ReportingTestUtils.EMPTY_SNAPSHOT;
import static org.pmiops.workbench.testconfig.ReportingTestUtils.createEmptySnapshot;
import static org.pmiops.workbench.testconfig.ReportingTestUtils.createReportingNewUserSatisfactionSurvey;
import static org.pmiops.workbench.utils.TimeAssertions.assertTimeApprox;

import com.google.cloud.bigquery.InsertAllRequest;
import com.google.cloud.bigquery.InsertAllResponse;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableResult;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUser.DbGeneralDiscoverySource;
import org.pmiops.workbench.db.model.DbUser.DbPartnerDiscoverySource;
import org.pmiops.workbench.model.BillingStatus;
import org.pmiops.workbench.model.ReportingCohort;
import org.pmiops.workbench.model.ReportingNewUserSatisfactionSurvey;
import org.pmiops.workbench.model.ReportingSnapshot;
import org.pmiops.workbench.model.ReportingUser;
import org.pmiops.workbench.model.ReportingUserGeneralDiscoverySource;
import org.pmiops.workbench.model.ReportingUserPartnerDiscoverySource;
import org.pmiops.workbench.model.ReportingWorkspace;
import org.pmiops.workbench.reporting.insertion.CohortColumnValueExtractor;
import org.pmiops.workbench.reporting.insertion.NewUserSatisfactionSurveyColumnValueExtractor;
import org.pmiops.workbench.reporting.insertion.UserColumnValueExtractor;
import org.pmiops.workbench.reporting.insertion.UserGeneralDiscoverySourceColumnValueExtractor;
import org.pmiops.workbench.reporting.insertion.UserPartnerDiscoverySourceColumnValueExtractor;
import org.pmiops.workbench.reporting.insertion.WorkspaceColumnValueExtractor;
import org.pmiops.workbench.testconfig.ReportingTestConfig;
import org.pmiops.workbench.testconfig.fixtures.ReportingTestFixture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * Test all implementations of ReportingUploadService to save on setup code. If this becomes too
 * complex (e.g. by having multiple public methods on each service), then we could share the setup
 * code and have separate tests.
 */
@SpringJUnitConfig
public class ReportingUploadServiceTest {

  private static final Instant NOW = FakeClockConfiguration.NOW.toInstant();
  private static final Instant THEN_INSTANT = Instant.parse("1989-02-17T00:00:00.00Z");
  private static final OffsetDateTime THEN = OffsetDateTime.ofInstant(THEN_INSTANT, ZoneOffset.UTC);

  private ReportingSnapshot reportingSnapshot;
  private ReportingSnapshot snapshotWithNulls;
  private List<ReportingWorkspace> reportingWorkspaces;

  @MockBean private BigQueryService mockBigQueryService;

  @Autowired private ReportingUploadService reportingUploadService;

  @Autowired private ReportingTestFixture<DbUser, ReportingUser> userFixture;

  @Captor private ArgumentCaptor<InsertAllRequest> insertAllRequestCaptor;

  @TestConfiguration
  @Import({
    FakeClockConfiguration.class,
    ReportingUploadServiceImpl.class,
    ReportingTestConfig.class
  })
  @MockBean({ReportingVerificationService.class})
  public static class Config {}

  @BeforeEach
  public void setup() {
    reportingSnapshot = createEmptySnapshot().captureTimestamp(NOW.toEpochMilli());

    snapshotWithNulls = createEmptySnapshot().captureTimestamp(NOW.toEpochMilli());

    reportingWorkspaces =
        List.of(
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
                .creatorId(202L));

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
  public void testUploadSnapshot_streaming_empty() {
    reportingUploadService.uploadSnapshot(EMPTY_SNAPSHOT);
    verify(mockBigQueryService, never()).insertAll(any());
  }

  @Test
  public void testUploadBatch_workspace() {
    final InsertAllResponse mockInsertAllResponse = mock(InsertAllResponse.class);

    doReturn(Collections.emptyMap()).when(mockInsertAllResponse).getInsertErrors();
    doReturn(mockInsertAllResponse)
        .when(mockBigQueryService)
        .insertAll(any(InsertAllRequest.class));

    reportingUploadService.uploadWorkspaceBatch(reportingWorkspaces, NOW.toEpochMilli());

    verify(mockBigQueryService).insertAll(insertAllRequestCaptor.capture());
    InsertAllRequest request = insertAllRequestCaptor.getValue();
    assertThat(request.getRows().size()).isEqualTo(reportingWorkspaces.size());
    final Map<String, Object> workspaceColumnValues = request.getRows().get(0).getContent();
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
  }

  @Test
  public void testUploadBatch_user() {
    List<ReportingUser> reportingUsers =
        List.of(
            userFixture.createDto(),
            new ReportingUser().username("ted@aou.biz").disabled(true).userId(202L),
            new ReportingUser().username("socrates@aou.biz").disabled(false).userId(303L),
            userFixture.createDto());
    final InsertAllResponse mockInsertAllResponse = mock(InsertAllResponse.class);

    doReturn(Collections.emptyMap()).when(mockInsertAllResponse).getInsertErrors();
    doReturn(mockInsertAllResponse)
        .when(mockBigQueryService)
        .insertAll(any(InsertAllRequest.class));

    reportingUploadService.uploadUserBatch(reportingUsers, NOW.toEpochMilli());

    verify(mockBigQueryService).insertAll(insertAllRequestCaptor.capture());
    InsertAllRequest request = insertAllRequestCaptor.getValue();
    assertThat(request.getRows().size()).isEqualTo(reportingUsers.size());
    final Map<String, Object> userColumnValues = request.getRows().get(0).getContent();
    assertThat(userColumnValues.get(UserColumnValueExtractor.USER_ID.getParameterName()))
        .isEqualTo(reportingUsers.get(0).getUserId());
    assertThat(userColumnValues.get(UserColumnValueExtractor.USERNAME.getParameterName()))
        .isEqualTo(reportingUsers.get(0).getUsername());
  }

  @Test
  public void testUploadBatch_cohort() {
    List<ReportingCohort> reportingCohorts =
        List.of(
            new ReportingCohort().cohortId(100L).name("name1"),
            new ReportingCohort().cohortId(200L).name("name2"));
    final InsertAllResponse mockInsertAllResponse = mock(InsertAllResponse.class);

    doReturn(Collections.emptyMap()).when(mockInsertAllResponse).getInsertErrors();
    doReturn(mockInsertAllResponse)
        .when(mockBigQueryService)
        .insertAll(any(InsertAllRequest.class));

    reportingUploadService.uploadCohortBatch(reportingCohorts, NOW.toEpochMilli());

    verify(mockBigQueryService).insertAll(insertAllRequestCaptor.capture());
    InsertAllRequest request = insertAllRequestCaptor.getValue();
    assertThat(request.getRows().size()).isEqualTo(reportingCohorts.size());
    final Map<String, Object> cohortColumnValues = request.getRows().get(0).getContent();
    assertThat(cohortColumnValues.get(CohortColumnValueExtractor.COHORT_ID.getParameterName()))
        .isEqualTo(reportingCohorts.get(0).getCohortId());
    assertThat(cohortColumnValues.get(CohortColumnValueExtractor.CREATOR_ID.getParameterName()))
        .isEqualTo(reportingCohorts.get(0).getCreatorId());
  }

  @Test
  public void testUploadBatchNewUserSatisfactionSurveys() {
    ReportingNewUserSatisfactionSurvey reportingNewUserSatisfactionSurvey =
        createReportingNewUserSatisfactionSurvey();
    List<ReportingNewUserSatisfactionSurvey> reportingNewUserSatisfactionSurveys =
        List.of(reportingNewUserSatisfactionSurvey, new ReportingNewUserSatisfactionSurvey());
    final InsertAllResponse mockInsertAllResponse = mock(InsertAllResponse.class);

    doReturn(Collections.emptyMap()).when(mockInsertAllResponse).getInsertErrors();
    doReturn(mockInsertAllResponse)
        .when(mockBigQueryService)
        .insertAll(any(InsertAllRequest.class));

    reportingUploadService.uploadNewUserSatisfactionSurveyBatch(
        reportingNewUserSatisfactionSurveys, NOW.toEpochMilli());

    verify(mockBigQueryService).insertAll(insertAllRequestCaptor.capture());
    InsertAllRequest request = insertAllRequestCaptor.getValue();
    assertThat(request.getRows().size()).isEqualTo(reportingNewUserSatisfactionSurveys.size());
    final Map<String, Object> newUserSatisfactionSurveyColumnValues =
        request.getRows().get(0).getContent();

    assertThat(
            newUserSatisfactionSurveyColumnValues.get(
                NewUserSatisfactionSurveyColumnValueExtractor.ID.getParameterName()))
        .isEqualTo(reportingNewUserSatisfactionSurvey.getId());
  }

  @Test
  public void testUploadBatchUserGeneralDiscoverySource() {
    List<ReportingUserGeneralDiscoverySource> userGeneralDiscoverySources =
        List.of(
            new ReportingUserGeneralDiscoverySource()
                .userId(100L)
                .answer(DbGeneralDiscoverySource.ACTIVITY_PRESENTATION_OR_EVENT.toString()),
            new ReportingUserGeneralDiscoverySource()
                .userId(200L)
                .answer(DbGeneralDiscoverySource.OTHER.toString())
                .otherText("other text"));

    final InsertAllResponse mockInsertAllResponse = mock(InsertAllResponse.class);

    doReturn(Collections.emptyMap()).when(mockInsertAllResponse).getInsertErrors();
    doReturn(mockInsertAllResponse)
        .when(mockBigQueryService)
        .insertAll(any(InsertAllRequest.class));

    reportingUploadService.uploadUserGeneralDiscoverySourceBatch(
        userGeneralDiscoverySources, NOW.toEpochMilli());

    verify(mockBigQueryService).insertAll(insertAllRequestCaptor.capture());
    InsertAllRequest request = insertAllRequestCaptor.getValue();
    assertThat(request.getRows().size()).isEqualTo(userGeneralDiscoverySources.size());
    final Map<String, Object> cohortColumnValues = request.getRows().get(0).getContent();
    assertThat(
            cohortColumnValues.get(
                UserGeneralDiscoverySourceColumnValueExtractor.USER_ID.getParameterName()))
        .isEqualTo(userGeneralDiscoverySources.get(0).getUserId());
    assertThat(
            cohortColumnValues.get(
                UserGeneralDiscoverySourceColumnValueExtractor.ANSWER.getParameterName()))
        .isEqualTo(userGeneralDiscoverySources.get(0).getAnswer());
  }

  @Test
  public void testUploadBatchUserPartnerDiscoverySource() {
    List<ReportingUserPartnerDiscoverySource> userPartnerDiscoverySources =
        Collections.singletonList(
            new ReportingUserPartnerDiscoverySource()
                .userId(100L)
                .answer(
                    DbPartnerDiscoverySource.ALL_OF_US_EVENINGS_WITH_GENETICS_RESEARCH_PROGRAM
                        .toString()));

    final InsertAllResponse mockInsertAllResponse = mock(InsertAllResponse.class);

    doReturn(Collections.emptyMap()).when(mockInsertAllResponse).getInsertErrors();
    doReturn(mockInsertAllResponse)
        .when(mockBigQueryService)
        .insertAll(any(InsertAllRequest.class));

    reportingUploadService.uploadUserPartnerDiscoverySourceBatch(
        userPartnerDiscoverySources, NOW.toEpochMilli());

    verify(mockBigQueryService).insertAll(insertAllRequestCaptor.capture());
    InsertAllRequest request = insertAllRequestCaptor.getValue();
    assertThat(request.getRows().size()).isEqualTo(userPartnerDiscoverySources.size());
    final Map<String, Object> cohortColumnValues = request.getRows().get(0).getContent();
    assertThat(
            cohortColumnValues.get(
                UserPartnerDiscoverySourceColumnValueExtractor.USER_ID.getParameterName()))
        .isEqualTo(userPartnerDiscoverySources.get(0).getUserId());
    assertThat(
            cohortColumnValues.get(
                UserPartnerDiscoverySourceColumnValueExtractor.ANSWER.getParameterName()))
        .isEqualTo(userPartnerDiscoverySources.get(0).getAnswer());
  }

  @Test
  public void testUploadSnapshot_nullEnum() {
    final ReportingCohort cohort = new ReportingCohort();
    cohort.setWorkspaceId(101L);
    cohort.setCreatorId(null);

    reportingUploadService.uploadCohortBatch(Collections.singletonList(cohort), NOW.toEpochMilli());
    verify(mockBigQueryService).insertAll(insertAllRequestCaptor.capture());

    final InsertAllRequest insertAllRequest = insertAllRequestCaptor.getValue();
    assertThat(insertAllRequest.getRows()).hasSize(1);
    final Map<String, Object> content = insertAllRequest.getRows().get(0).getContent();
    assertThat(content).hasSize(2);
    assertThat(content.getOrDefault("billing_status", BillingStatus.INACTIVE))
        .isEqualTo(BillingStatus.INACTIVE);
  }
}
