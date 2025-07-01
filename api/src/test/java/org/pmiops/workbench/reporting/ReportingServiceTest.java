package org.pmiops.workbench.reporting;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.pmiops.workbench.cloudtasks.TaskQueueService;
import org.pmiops.workbench.db.dao.ReportingUploadVerificationDao;
import org.pmiops.workbench.db.jdbc.ReportingQueryService;
import org.pmiops.workbench.model.ReportingBase;
import org.pmiops.workbench.model.ReportingCohort;
import org.pmiops.workbench.model.ReportingUser;

@ExtendWith(MockitoExtension.class)
public class ReportingServiceTest {

  private static final Instant NOW = Instant.parse("2023-06-15T10:15:30.00Z");
  private static final long NOW_MILLIS = NOW.toEpochMilli();

  @Mock private Clock mockClock;
  @Mock private ReportingTableService mockReportingTableService;
  @Mock private ReportingQueryService mockReportingQueryService;
  @Mock private ReportingUploadService mockReportingUploadService;
  @Mock private ReportingVerificationService mockReportingVerificationService;
  @Mock private ReportingUploadVerificationDao mockReportingUploadVerificationDao;
  @Mock private TaskQueueService mockTaskQueueService;

  private ReportingServiceImpl reportingService;

  @BeforeEach
  public void setUp() {
    reportingService =
        new ReportingServiceImpl(
            mockClock,
            mockReportingTableService,
            mockReportingQueryService,
            mockReportingUploadService,
            mockReportingVerificationService,
            mockReportingUploadVerificationDao,
            mockTaskQueueService);
  }

  @Test
  public void testSplitUploadIntoTasksAndQueue_singleTable() {
    // Arrange
    ReportingTableParams<ReportingCohort> cohortTableParams = createMockTableParams("cohort");

    when(mockReportingTableService.getAll()).thenReturn(List.of(cohortTableParams));
    when(mockClock.millis()).thenReturn(NOW_MILLIS);

    // Act
    reportingService.splitUploadIntoTasksAndQueue();

    // Assert
    verify(mockClock).millis();
    verify(mockReportingTableService).getAll();

    // Verify verification entry is created for the cohort table
    ArgumentCaptor<String> tableNameCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Timestamp> timestampCaptor = ArgumentCaptor.forClass(Timestamp.class);
    verify(mockReportingUploadVerificationDao)
        .createVerificationEntry(tableNameCaptor.capture(), timestampCaptor.capture());
    assertThat(tableNameCaptor.getValue()).isEqualTo("cohort");
    assertThat(timestampCaptor.getValue()).isEqualTo(new Timestamp(NOW_MILLIS));

    // Verify task is queued for the cohort table
    ArgumentCaptor<String> taskTableNameCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Long> taskTimestampCaptor = ArgumentCaptor.forClass(Long.class);
    verify(mockTaskQueueService)
        .pushReportingUploadTask(taskTableNameCaptor.capture(), taskTimestampCaptor.capture());
    assertThat(taskTableNameCaptor.getValue()).isEqualTo("cohort");
    assertThat(taskTimestampCaptor.getValue()).isEqualTo(NOW_MILLIS);
  }

  @Test
  public void testSplitUploadIntoTasksAndQueue_multipleTables() {
    // Arrange
    ReportingTableParams<ReportingCohort> cohortTableParams = createMockTableParams("cohort");
    ReportingTableParams<ReportingUser> userTableParams = createMockTableParams("user");

    when(mockReportingTableService.getAll())
        .thenReturn(List.of(cohortTableParams, userTableParams));
    when(mockClock.millis()).thenReturn(NOW_MILLIS);

    // Act
    reportingService.splitUploadIntoTasksAndQueue();

    // Assert
    verify(mockClock).millis();
    verify(mockReportingTableService).getAll();

    // Verify verification entries are created for both tables
    ArgumentCaptor<String> tableNameCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Timestamp> timestampCaptor = ArgumentCaptor.forClass(Timestamp.class);
    verify(mockReportingUploadVerificationDao, times(2))
        .createVerificationEntry(tableNameCaptor.capture(), timestampCaptor.capture());
    List<String> capturedTableNames = tableNameCaptor.getAllValues();
    assertThat(capturedTableNames).containsExactly("cohort", "user");
    List<Timestamp> capturedTimestamps = timestampCaptor.getAllValues();
    assertThat(capturedTimestamps).hasSize(2);
    assertThat(capturedTimestamps.get(0)).isEqualTo(new Timestamp(NOW_MILLIS));
    assertThat(capturedTimestamps.get(1)).isEqualTo(new Timestamp(NOW_MILLIS));

    // Verify tasks are queued for both tables
    ArgumentCaptor<String> taskTableNameCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Long> taskTimestampCaptor = ArgumentCaptor.forClass(Long.class);
    verify(mockTaskQueueService, times(2))
        .pushReportingUploadTask(taskTableNameCaptor.capture(), taskTimestampCaptor.capture());
    List<String> capturedTaskTableNames = taskTableNameCaptor.getAllValues();
    assertThat(capturedTaskTableNames).containsExactly("cohort", "user");
    List<Long> capturedTaskTimestamps = taskTimestampCaptor.getAllValues();
    assertThat(capturedTaskTimestamps).hasSize(2);
    assertThat(capturedTaskTimestamps.get(0)).isEqualTo(NOW_MILLIS);
    assertThat(capturedTaskTimestamps.get(1)).isEqualTo(NOW_MILLIS);
  }

  @Test
  public void testSplitUploadIntoTasksAndQueue_emptyTableList() {
    // Arrange
    when(mockReportingTableService.getAll()).thenReturn(List.of());
    when(mockClock.millis()).thenReturn(NOW_MILLIS);

    // Act
    reportingService.splitUploadIntoTasksAndQueue();

    // Assert
    verify(mockClock).millis();
    verify(mockReportingTableService).getAll();
    // Verify no verification entries or tasks are created
    verify(mockReportingUploadVerificationDao, times(0)).createVerificationEntry(any(), any());
    verify(mockTaskQueueService, times(0)).pushReportingUploadTask(any(), any());
  }

  @Test
  public void testSplitUploadIntoTasksAndQueue_captureTimestampConsistency() {
    // Arrange
    ReportingTableParams<ReportingCohort> cohortTableParams = createMockTableParams("cohort");
    ReportingTableParams<ReportingUser> userTableParams = createMockTableParams("user");

    when(mockReportingTableService.getAll())
        .thenReturn(List.of(cohortTableParams, userTableParams));
    when(mockClock.millis()).thenReturn(NOW_MILLIS);

    // Act
    reportingService.splitUploadIntoTasksAndQueue();

    // Assert
    // Verify that the same timestamp is used for all verification entries and tasks
    ArgumentCaptor<Timestamp> verificationTimestampCaptor =
        ArgumentCaptor.forClass(Timestamp.class);
    verify(mockReportingUploadVerificationDao, times(2))
        .createVerificationEntry(any(), verificationTimestampCaptor.capture());

    ArgumentCaptor<Long> taskTimestampCaptor = ArgumentCaptor.forClass(Long.class);
    verify(mockTaskQueueService, times(2))
        .pushReportingUploadTask(any(), taskTimestampCaptor.capture());

    // All timestamps should be the same
    List<Long> verificationTimestamps =
        verificationTimestampCaptor.getAllValues().stream().map(Timestamp::getTime).toList();
    List<Long> taskTimestamps = taskTimestampCaptor.getAllValues();
    assertEquals(new HashSet<>(verificationTimestamps), new HashSet<>(taskTimestamps));
  }

  @Test
  public void testCollectRecordsAndUpload_withValidTables_successfulUpload() {
    // Arrange
    List<String> tables = List.of("cohort", "user");
    long captureTimestamp = 1640995200000L;

    ReportingTableParams<ReportingCohort> cohortTableParams =
        createMockTableParams("cohort", false);
    ReportingTableParams<ReportingUser> userTableParams = createMockTableParams("user", false);

    when(mockReportingTableService.getAll(tables))
        .thenReturn(List.of(cohortTableParams, userTableParams));
    when(mockReportingVerificationService.verifySnapshot(captureTimestamp)).thenReturn(true);

    // Act
    reportingService.collectRecordsAndUpload(tables, captureTimestamp);

    // Assert
    verify(mockReportingTableService).getAll(tables);
    verify(mockReportingVerificationService).verifyBatchesAndLog(tables, captureTimestamp);
    verify(mockReportingVerificationService).verifySnapshot(captureTimestamp);
    verify(mockReportingUploadService).uploadVerifiedSnapshot(captureTimestamp);
  }

  @Test
  public void testCollectRecordsAndUpload_withValidTables_failedVerification() {
    // Arrange
    List<String> tables = List.of("cohort", "user");
    long captureTimestamp = 1640995200000L;

    ReportingTableParams<ReportingCohort> cohortTableParams =
        createMockTableParams("cohort", false);
    ReportingTableParams<ReportingUser> userTableParams = createMockTableParams("user", false);

    when(mockReportingTableService.getAll(tables))
        .thenReturn(List.of(cohortTableParams, userTableParams));
    when(mockReportingVerificationService.verifySnapshot(captureTimestamp)).thenReturn(false);

    // Act
    reportingService.collectRecordsAndUpload(tables, captureTimestamp);

    // Assert
    verify(mockReportingTableService).getAll(tables);
    verify(mockReportingVerificationService).verifyBatchesAndLog(tables, captureTimestamp);
    verify(mockReportingVerificationService).verifySnapshot(captureTimestamp);
    // Should NOT upload verified snapshot when verification fails
    verify(mockReportingUploadService, times(0)).uploadVerifiedSnapshot(captureTimestamp);
  }

  @Test
  public void testCollectRecordsAndUpload_withEmptyTables_successfulUpload() {
    // Arrange
    List<String> emptyTables = List.of();
    long captureTimestamp = 1640995200000L;

    when(mockReportingTableService.getAll(emptyTables)).thenReturn(List.of());
    when(mockReportingVerificationService.verifySnapshot(captureTimestamp)).thenReturn(true);

    // Act
    reportingService.collectRecordsAndUpload(emptyTables, captureTimestamp);

    // Assert
    verify(mockReportingTableService).getAll(emptyTables);
    verify(mockReportingVerificationService).verifyBatchesAndLog(emptyTables, captureTimestamp);
    verify(mockReportingVerificationService).verifySnapshot(captureTimestamp);
    verify(mockReportingUploadService).uploadVerifiedSnapshot(captureTimestamp);
  }

  /** Helper method to create a mock ReportingTableParams with the specified table name. */
  @SuppressWarnings("unchecked")
  private <T extends ReportingBase> ReportingTableParams<T> createMockTableParams(
      String tableName, boolean stubBqTableName) {
    ReportingTableParams<T> tableParams = mock(ReportingTableParams.class);
    if (stubBqTableName) {
      when(tableParams.bqTableName()).thenReturn(tableName);
    }
    return tableParams;
  }

  /** Helper method to create a mock ReportingTableParams with bqTableName stubbed. */
  private <T extends ReportingBase> ReportingTableParams<T> createMockTableParams(
      String tableName) {
    return createMockTableParams(tableName, true);
  }
}
