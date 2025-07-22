package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.impersonation.ImpersonatedWorkspaceService;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class CloudTaskWorkspacesControllerTest {

  @Mock private ImpersonatedWorkspaceService mockImpersonatedWorkspaceService;
  @Mock private WorkspaceService mockWorkspaceService;

  private CloudTaskWorkspacesController controller;

  @BeforeEach
  void setUp() {
    controller =
        new CloudTaskWorkspacesController(mockImpersonatedWorkspaceService, mockWorkspaceService);
  }

  @Test
  void cleanupOrphanedWorkspacesBatch_withValidNamespaces_deletesWorkspacesAndReturnsOk() {
    // Arrange
    List<String> namespaces = Arrays.asList("namespace1", "namespace2", "namespace3");

    DbWorkspace workspace1 = createMockWorkspace("namespace1");
    DbWorkspace workspace2 = createMockWorkspace("namespace2");
    DbWorkspace workspace3 = createMockWorkspace("namespace3");

    when(mockWorkspaceService.lookupWorkspaceByNamespace("namespace1")).thenReturn(workspace1);
    when(mockWorkspaceService.lookupWorkspaceByNamespace("namespace2")).thenReturn(workspace2);
    when(mockWorkspaceService.lookupWorkspaceByNamespace("namespace3")).thenReturn(workspace3);

    doNothing().when(mockWorkspaceService).deleteWorkspace(any(DbWorkspace.class), eq(false));

    // Act
    ResponseEntity<Void> response = controller.cleanupOrphanedWorkspacesBatch(namespaces);

    // Assert
    assertValidResponse(response);
    verify(mockWorkspaceService).lookupWorkspaceByNamespace("namespace1");
    verify(mockWorkspaceService).lookupWorkspaceByNamespace("namespace2");
    verify(mockWorkspaceService).lookupWorkspaceByNamespace("namespace3");
    verify(mockWorkspaceService).deleteWorkspace(workspace1, false);
    verify(mockWorkspaceService).deleteWorkspace(workspace2, false);
    verify(mockWorkspaceService).deleteWorkspace(workspace3, false);
    verifyNoMoreInteractions(mockWorkspaceService);
  }

  @Test
  void cleanupOrphanedWorkspacesBatch_withEmptyList_doesNothingAndReturnsOk() {
    // Arrange
    List<String> emptyNamespaces = Collections.emptyList();

    // Act
    ResponseEntity<Void> response = controller.cleanupOrphanedWorkspacesBatch(emptyNamespaces);

    // Assert
    assertValidResponse(response);
    verifyNoMoreInteractions(mockWorkspaceService);
  }

  @Test
  void cleanupOrphanedWorkspacesBatch_withSingleNamespace_deletesWorkspaceAndReturnsOk() {
    // Arrange
    List<String> namespaces = Collections.singletonList("single-namespace");
    DbWorkspace workspace = createMockWorkspace("single-namespace");

    when(mockWorkspaceService.lookupWorkspaceByNamespace("single-namespace")).thenReturn(workspace);
    doNothing().when(mockWorkspaceService).deleteWorkspace(workspace, false);

    // Act
    ResponseEntity<Void> response = controller.cleanupOrphanedWorkspacesBatch(namespaces);

    // Assert
    assertValidResponse(response);
    verify(mockWorkspaceService).lookupWorkspaceByNamespace("single-namespace");
    verify(mockWorkspaceService).deleteWorkspace(workspace, false);
    verifyNoMoreInteractions(mockWorkspaceService);
  }

  @Test
  void cleanupOrphanedWorkspacesBatch_withNotFoundWorkspace_continuesProcessingAndReturnsOk() {
    // Arrange
    List<String> namespaces =
        Arrays.asList("existing-namespace", "missing-namespace", "another-existing");

    DbWorkspace existingWorkspace1 = createMockWorkspace("existing-namespace");
    DbWorkspace existingWorkspace2 = createMockWorkspace("another-existing");

    when(mockWorkspaceService.lookupWorkspaceByNamespace("existing-namespace"))
        .thenReturn(existingWorkspace1);
    when(mockWorkspaceService.lookupWorkspaceByNamespace("missing-namespace"))
        .thenThrow(new NotFoundException("Workspace not found"));
    when(mockWorkspaceService.lookupWorkspaceByNamespace("another-existing"))
        .thenReturn(existingWorkspace2);

    doNothing().when(mockWorkspaceService).deleteWorkspace(any(DbWorkspace.class), eq(false));

    // Act
    ResponseEntity<Void> response = controller.cleanupOrphanedWorkspacesBatch(namespaces);

    // Assert
    assertValidResponse(response);
    verify(mockWorkspaceService).lookupWorkspaceByNamespace("existing-namespace");
    verify(mockWorkspaceService).lookupWorkspaceByNamespace("missing-namespace");
    verify(mockWorkspaceService).lookupWorkspaceByNamespace("another-existing");
    verify(mockWorkspaceService).deleteWorkspace(existingWorkspace1, false);
    verify(mockWorkspaceService).deleteWorkspace(existingWorkspace2, false);
    verifyNoMoreInteractions(mockWorkspaceService);
  }

  @Test
  void cleanupOrphanedWorkspacesBatch_withDeleteFailure_throwsException() {
    // Arrange
    List<String> namespaces = Arrays.asList("namespace1", "namespace2");

    DbWorkspace workspace1 = createMockWorkspace("namespace1");
    DbWorkspace workspace2 = createMockWorkspace("namespace2");

    when(mockWorkspaceService.lookupWorkspaceByNamespace("namespace1")).thenReturn(workspace1);
    when(mockWorkspaceService.lookupWorkspaceByNamespace("namespace2")).thenReturn(workspace2);

    doNothing().when(mockWorkspaceService).deleteWorkspace(workspace1, false);
    doThrow(new RuntimeException("Delete failed"))
        .when(mockWorkspaceService)
        .deleteWorkspace(workspace2, false);

    // Act & Assert
    RuntimeException exception =
        org.junit.jupiter.api.Assertions.assertThrows(
            RuntimeException.class, () -> controller.cleanupOrphanedWorkspacesBatch(namespaces));

    assertThat(exception.getMessage()).isEqualTo("Delete failed");
    verify(mockWorkspaceService).lookupWorkspaceByNamespace("namespace1");
    verify(mockWorkspaceService).lookupWorkspaceByNamespace("namespace2");
    verify(mockWorkspaceService).deleteWorkspace(workspace1, false);
    verify(mockWorkspaceService).deleteWorkspace(workspace2, false);
    verifyNoMoreInteractions(mockWorkspaceService);
  }

  @Test
  void cleanupOrphanedWorkspacesBatch_withNotFoundAndSuccessfulDelete_processesAllAndReturnsOk() {
    // Arrange
    List<String> namespaces = Arrays.asList("success1", "notfound", "success2");

    DbWorkspace successWorkspace1 = createMockWorkspace("success1");
    DbWorkspace successWorkspace2 = createMockWorkspace("success2");

    when(mockWorkspaceService.lookupWorkspaceByNamespace("success1")).thenReturn(successWorkspace1);
    when(mockWorkspaceService.lookupWorkspaceByNamespace("notfound"))
        .thenThrow(new NotFoundException("Not found"));
    when(mockWorkspaceService.lookupWorkspaceByNamespace("success2")).thenReturn(successWorkspace2);

    doNothing().when(mockWorkspaceService).deleteWorkspace(successWorkspace1, false);
    doNothing().when(mockWorkspaceService).deleteWorkspace(successWorkspace2, false);

    // Act
    ResponseEntity<Void> response = controller.cleanupOrphanedWorkspacesBatch(namespaces);

    // Assert
    assertValidResponse(response);
    verify(mockWorkspaceService).lookupWorkspaceByNamespace("success1");
    verify(mockWorkspaceService).lookupWorkspaceByNamespace("notfound");
    verify(mockWorkspaceService).lookupWorkspaceByNamespace("success2");
    verify(mockWorkspaceService).deleteWorkspace(successWorkspace1, false);
    verify(mockWorkspaceService).deleteWorkspace(successWorkspace2, false);
    verifyNoMoreInteractions(mockWorkspaceService);
  }

  private DbWorkspace createMockWorkspace(String namespace) {
    DbWorkspace workspace = new DbWorkspace();
    workspace.setWorkspaceNamespace(namespace);
    return workspace;
  }

  private void assertValidResponse(ResponseEntity<Void> response) {
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNull();
  }
}
