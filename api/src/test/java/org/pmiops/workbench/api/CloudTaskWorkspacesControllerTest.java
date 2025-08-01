package org.pmiops.workbench.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.util.AssertionErrors.assertEquals;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.impersonation.ImpersonatedWorkspaceService;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
public class CloudTaskWorkspacesControllerTest {

  @Mock private ImpersonatedWorkspaceService mockImpersonatedWorkspaceService;

  @InjectMocks private CloudTaskWorkspacesController controller;

  private static final String CLEANUP_REASON = "CleanupOrphanedWorkspaces Cron Job";

  // Helper method for common response assertion
  private void assertOkResponse(ResponseEntity<Void> response) {
    assertEquals("Response should be OK", ResponseEntity.ok().build(), response);
  }

  // Parameterized test data for successful scenarios
  static Stream<Arguments> successfulCleanupScenarios() {
    return Stream.of(
        Arguments.of("Single workspace", List.of("single-workspace-ns"), 1),
        Arguments.of(
            "Multiple workspaces",
            List.of("workspace-ns-1", "workspace-ns-2", "workspace-ns-3"),
            3),
        Arguments.of(
            "Duplicate workspaces", List.of("workspace-ns", "workspace-ns", "workspace-ns"), 3));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("successfulCleanupScenarios")
  void testCleanupOrphanedWorkspacesBatch_successfulScenarios(
      String scenario, List<String> namespaces, int expectedCalls) {
    // Arrange
    doNothing()
        .when(mockImpersonatedWorkspaceService)
        .cleanupWorkspace(any(String.class), eq(CLEANUP_REASON));

    // Act
    ResponseEntity<Void> response = controller.cleanupOrphanedWorkspacesBatch(namespaces);

    // Assert
    assertOkResponse(response);
    verify(mockImpersonatedWorkspaceService, times(expectedCalls))
        .cleanupWorkspace(any(String.class), eq(CLEANUP_REASON));
  }

  @Test
  public void testCleanupOrphanedWorkspacesBatch_emptyList() {
    List<String> namespaces = Collections.emptyList();

    ResponseEntity<Void> response = controller.cleanupOrphanedWorkspacesBatch(namespaces);

    assertOkResponse(response);
    verifyNoInteractions(mockImpersonatedWorkspaceService);
  }

  @Test
  public void testCleanupOrphanedWorkspacesBatch_workspaceNotFound() {
    List<String> namespaces =
        List.of("existing-workspace", "non-existent-workspace", "another-workspace");

    // Configure mock to throw NotFoundException for the second workspace
    doNothing()
        .when(mockImpersonatedWorkspaceService)
        .cleanupWorkspace("existing-workspace", CLEANUP_REASON);
    doThrow(new NotFoundException("Workspace not found"))
        .when(mockImpersonatedWorkspaceService)
        .cleanupWorkspace("non-existent-workspace", CLEANUP_REASON);
    doNothing()
        .when(mockImpersonatedWorkspaceService)
        .cleanupWorkspace("another-workspace", CLEANUP_REASON);

    ResponseEntity<Void> response = controller.cleanupOrphanedWorkspacesBatch(namespaces);

    // Should still return OK even when some workspaces are not found
    assertOkResponse(response);

    // Verify all cleanup attempts were made
    verify(mockImpersonatedWorkspaceService).cleanupWorkspace("existing-workspace", CLEANUP_REASON);
    verify(mockImpersonatedWorkspaceService)
        .cleanupWorkspace("non-existent-workspace", CLEANUP_REASON);
    verify(mockImpersonatedWorkspaceService).cleanupWorkspace("another-workspace", CLEANUP_REASON);
    verify(mockImpersonatedWorkspaceService, times(3))
        .cleanupWorkspace(any(String.class), eq(CLEANUP_REASON));
  }

  @Test
  public void testCleanupOrphanedWorkspacesBatch_allWorkspacesNotFound() {
    List<String> namespaces = List.of("non-existent-1", "non-existent-2");

    doThrow(new NotFoundException("Workspace not found"))
        .when(mockImpersonatedWorkspaceService)
        .cleanupWorkspace(any(String.class), eq(CLEANUP_REASON));

    ResponseEntity<Void> response = controller.cleanupOrphanedWorkspacesBatch(namespaces);

    assertOkResponse(response);
    verify(mockImpersonatedWorkspaceService, times(2))
        .cleanupWorkspace(any(String.class), eq(CLEANUP_REASON));
  }

  @Test
  public void testCleanupOrphanedWorkspacesBatch_usesCorrectLastModifiedBy() {
    List<String> namespaces = List.of("test-workspace");

    doNothing()
        .when(mockImpersonatedWorkspaceService)
        .cleanupWorkspace(any(String.class), any(String.class));

    controller.cleanupOrphanedWorkspacesBatch(namespaces);

    // Verify the exact string "CleanupOrphanedWorkspaces Cron Job" is used as lastModifiedBy
    // parameter
    verify(mockImpersonatedWorkspaceService).cleanupWorkspace("test-workspace", CLEANUP_REASON);
    verify(mockImpersonatedWorkspaceService, never())
        .cleanupWorkspace(eq("test-workspace"), eq("system"));
    verify(mockImpersonatedWorkspaceService, never())
        .cleanupWorkspace(eq("test-workspace"), eq("admin"));
  }
}
