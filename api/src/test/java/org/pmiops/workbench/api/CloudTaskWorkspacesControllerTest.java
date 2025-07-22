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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.impersonation.ImpersonatedWorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class CloudTaskWorkspacesControllerTest {

  @Autowired private CloudTaskWorkspacesController controller;

  @MockBean private ImpersonatedWorkspaceService mockImpersonatedWorkspaceService;

  @TestConfiguration
  @Import({
    FakeClockConfiguration.class,
    CloudTaskWorkspacesController.class,
  })
  @MockBean({
    ImpersonatedWorkspaceService.class,
  })
  static class Configuration {}

  @BeforeEach
  public void setUp() {
    // Reset mocks before each test
  }

  @Test
  public void testCleanupOrphanedWorkspacesBatch_success() {
    List<String> namespaces = List.of("workspace-ns-1", "workspace-ns-2", "workspace-ns-3");

    doNothing()
        .when(mockImpersonatedWorkspaceService)
        .cleanupWorkspace(any(String.class), eq("CleanupOrphanedWorkspaces Cron Job"));

    ResponseEntity<Void> response = controller.cleanupOrphanedWorkspacesBatch(namespaces);

    assertEquals("Response should be OK", ResponseEntity.ok().build(), response);
    verify(mockImpersonatedWorkspaceService, times(3))
        .cleanupWorkspace(any(String.class), eq("CleanupOrphanedWorkspaces Cron Job"));
    verify(mockImpersonatedWorkspaceService)
        .cleanupWorkspace("workspace-ns-1", "CleanupOrphanedWorkspaces Cron Job");
    verify(mockImpersonatedWorkspaceService)
        .cleanupWorkspace("workspace-ns-2", "CleanupOrphanedWorkspaces Cron Job");
    verify(mockImpersonatedWorkspaceService)
        .cleanupWorkspace("workspace-ns-3", "CleanupOrphanedWorkspaces Cron Job");
  }

  @Test
  public void testCleanupOrphanedWorkspacesBatch_singleWorkspace() {
    List<String> namespaces = List.of("single-workspace-ns");

    doNothing()
        .when(mockImpersonatedWorkspaceService)
        .cleanupWorkspace("single-workspace-ns", "CleanupOrphanedWorkspaces Cron Job");

    ResponseEntity<Void> response = controller.cleanupOrphanedWorkspacesBatch(namespaces);

    assertEquals("Response should be OK", ResponseEntity.ok().build(), response);
    verify(mockImpersonatedWorkspaceService, times(1))
        .cleanupWorkspace("single-workspace-ns", "CleanupOrphanedWorkspaces Cron Job");
  }

  @Test
  public void testCleanupOrphanedWorkspacesBatch_emptyList() {
    List<String> namespaces = Collections.emptyList();

    ResponseEntity<Void> response = controller.cleanupOrphanedWorkspacesBatch(namespaces);

    assertEquals("Response should be OK", ResponseEntity.ok().build(), response);
    verifyNoInteractions(mockImpersonatedWorkspaceService);
  }

  @Test
  public void testCleanupOrphanedWorkspacesBatch_workspaceNotFound() {
    List<String> namespaces =
        List.of("existing-workspace", "non-existent-workspace", "another-workspace");

    // Configure mock to throw NotFoundException for the second workspace
    doNothing()
        .when(mockImpersonatedWorkspaceService)
        .cleanupWorkspace("existing-workspace", "CleanupOrphanedWorkspaces Cron Job");
    doThrow(new NotFoundException("Workspace not found"))
        .when(mockImpersonatedWorkspaceService)
        .cleanupWorkspace("non-existent-workspace", "CleanupOrphanedWorkspaces Cron Job");
    doNothing()
        .when(mockImpersonatedWorkspaceService)
        .cleanupWorkspace("another-workspace", "CleanupOrphanedWorkspaces Cron Job");

    ResponseEntity<Void> response = controller.cleanupOrphanedWorkspacesBatch(namespaces);

    // Should still return OK even when some workspaces are not found
    assertEquals("Response should be OK", ResponseEntity.ok().build(), response);

    // Verify all cleanup attempts were made
    verify(mockImpersonatedWorkspaceService)
        .cleanupWorkspace("existing-workspace", "CleanupOrphanedWorkspaces Cron Job");
    verify(mockImpersonatedWorkspaceService)
        .cleanupWorkspace("non-existent-workspace", "CleanupOrphanedWorkspaces Cron Job");
    verify(mockImpersonatedWorkspaceService)
        .cleanupWorkspace("another-workspace", "CleanupOrphanedWorkspaces Cron Job");
    verify(mockImpersonatedWorkspaceService, times(3))
        .cleanupWorkspace(any(String.class), eq("CleanupOrphanedWorkspaces Cron Job"));
  }

  @Test
  public void testCleanupOrphanedWorkspacesBatch_allWorkspacesNotFound() {
    List<String> namespaces = List.of("non-existent-1", "non-existent-2");

    doThrow(new NotFoundException("Workspace not found"))
        .when(mockImpersonatedWorkspaceService)
        .cleanupWorkspace(any(String.class), eq("CleanupOrphanedWorkspaces Cron Job"));

    ResponseEntity<Void> response = controller.cleanupOrphanedWorkspacesBatch(namespaces);

    assertEquals("Response should be OK", ResponseEntity.ok().build(), response);
    verify(mockImpersonatedWorkspaceService, times(2))
        .cleanupWorkspace(any(String.class), eq("CleanupOrphanedWorkspaces Cron Job"));
  }

  @Test
  public void testCleanupOrphanedWorkspacesBatch_mixedExceptionsAndSuccess() {
    List<String> namespaces = List.of("workspace-1", "workspace-2", "workspace-3", "workspace-4");

    // Configure different behaviors for different workspaces
    doNothing()
        .when(mockImpersonatedWorkspaceService)
        .cleanupWorkspace("workspace-1", "CleanupOrphanedWorkspaces Cron Job");
    doThrow(new NotFoundException("Not found"))
        .when(mockImpersonatedWorkspaceService)
        .cleanupWorkspace("workspace-2", "CleanupOrphanedWorkspaces Cron Job");
    doNothing()
        .when(mockImpersonatedWorkspaceService)
        .cleanupWorkspace("workspace-3", "CleanupOrphanedWorkspaces Cron Job");
    doThrow(new NotFoundException("Another not found"))
        .when(mockImpersonatedWorkspaceService)
        .cleanupWorkspace("workspace-4", "CleanupOrphanedWorkspaces Cron Job");

    ResponseEntity<Void> response = controller.cleanupOrphanedWorkspacesBatch(namespaces);

    assertEquals("Response should be OK", ResponseEntity.ok().build(), response);
    verify(mockImpersonatedWorkspaceService, times(4))
        .cleanupWorkspace(any(String.class), eq("CleanupOrphanedWorkspaces Cron Job"));
  }

  @Test
  public void testCleanupOrphanedWorkspacesBatch_duplicateNamespaces() {
    List<String> namespaces = List.of("workspace-ns", "workspace-ns", "workspace-ns");

    doNothing()
        .when(mockImpersonatedWorkspaceService)
        .cleanupWorkspace("workspace-ns", "CleanupOrphanedWorkspaces Cron Job");

    ResponseEntity<Void> response = controller.cleanupOrphanedWorkspacesBatch(namespaces);

    assertEquals("Response should be OK", ResponseEntity.ok().build(), response);
    // Should be called 3 times even with duplicates (forEach processes all elements)
    verify(mockImpersonatedWorkspaceService, times(3))
        .cleanupWorkspace("workspace-ns", "CleanupOrphanedWorkspaces Cron Job");
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
    verify(mockImpersonatedWorkspaceService)
        .cleanupWorkspace("test-workspace", "CleanupOrphanedWorkspaces Cron Job");
    verify(mockImpersonatedWorkspaceService, never())
        .cleanupWorkspace(eq("test-workspace"), eq("system"));
    verify(mockImpersonatedWorkspaceService, never())
        .cleanupWorkspace(eq("test-workspace"), eq("admin"));
  }
}
