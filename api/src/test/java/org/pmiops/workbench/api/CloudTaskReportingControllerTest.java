package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.pmiops.workbench.model.ReportingUploadQueueTaskRequest;
import org.pmiops.workbench.reporting.ReportingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class CloudTaskReportingControllerTest {

  @Mock
  private ReportingService mockReportingService;

  private CloudTaskReportingController controller;
  private static final long SNAPSHOT_TIMESTAMP = 1640995200000L; // 2022-01-01T00:00:00.000Z

  @BeforeEach
  void setUp() {
    controller = new CloudTaskReportingController(mockReportingService);
  }

  @Test
  void processReportingUploadQueueTask_withValidRequest_callsServiceAndReturnsNoContent() {
    //Arrange
    List<String> tables = Arrays.asList("table1", "table2", "table3");

    ReportingUploadQueueTaskRequest request = new ReportingUploadQueueTaskRequest();
    request.setTables(tables);
    request.setSnapshotTimestamp(SNAPSHOT_TIMESTAMP);

    doNothing().when(mockReportingService).collectRecordsAndUpload(tables, SNAPSHOT_TIMESTAMP);

    //Act
    ResponseEntity<Void> response = controller.processReportingUploadQueueTask(request);

    //Assert
    assertValidResponse(response);
    verify(mockReportingService).collectRecordsAndUpload(eq(tables), eq(SNAPSHOT_TIMESTAMP));
    verifyNoMoreInteractions(mockReportingService);
  }

  @Test
  void processReportingUploadQueueTask_withEmptyTablesList_callsServiceWithEmptyList() {
    // Arrange
    List<String> emptyTables = Collections.emptyList();
    ReportingUploadQueueTaskRequest request = new ReportingUploadQueueTaskRequest();
    request.setTables(emptyTables);
    request.setSnapshotTimestamp(SNAPSHOT_TIMESTAMP);

    doNothing().when(mockReportingService).collectRecordsAndUpload(emptyTables, SNAPSHOT_TIMESTAMP);

    // Act
    ResponseEntity<Void> response = controller.processReportingUploadQueueTask(request);

    // Assert
    assertValidResponse(response);
    verify(mockReportingService).collectRecordsAndUpload(eq(emptyTables), eq(SNAPSHOT_TIMESTAMP));
    verifyNoMoreInteractions(mockReportingService);
  }

  private void assertValidResponse(ResponseEntity<Void> response) {
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(response.getBody()).isNull();
  }

}
