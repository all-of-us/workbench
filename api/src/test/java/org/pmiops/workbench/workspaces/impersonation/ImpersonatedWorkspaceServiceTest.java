package org.pmiops.workbench.workspaces.impersonation;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.actionaudit.auditors.BillingProjectAuditor;
import org.pmiops.workbench.db.dao.FeaturedWorkspaceDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.impersonation.ImpersonatedFirecloudService;
import org.pmiops.workbench.impersonation.ImpersonatedWorkspaceService;
import org.pmiops.workbench.impersonation.ImpersonatedWorkspaceServiceImpl;
import org.pmiops.workbench.initialcredits.InitialCreditsService;
import org.pmiops.workbench.model.WorkspaceActiveStatus;
import org.pmiops.workbench.utils.mappers.FirecloudMapper;
import org.pmiops.workbench.utils.mappers.WorkspaceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class ImpersonatedWorkspaceServiceTest {

  @TestConfiguration
  @Import({
    FakeClockConfiguration.class,
    ImpersonatedWorkspaceServiceImpl.class,
  })
  @MockBean({
    BillingProjectAuditor.class,
    FeaturedWorkspaceDao.class,
    FireCloudService.class,
    FirecloudMapper.class,
    ImpersonatedFirecloudService.class,
    InitialCreditsService.class,
    UserDao.class,
    WorkspaceMapper.class
  })
  static class Configuration {}

  @Autowired private ImpersonatedWorkspaceService impersonatedWorkspaceService;
  @MockBean private WorkspaceDao workspaceDao;

  private static final String WORKSPACE_NAMESPACE = "test-workspace-namespace";
  private static final String LAST_MODIFIED_BY = "test-user@example.com";

  // Custom log handler to capture log messages for testing
  private static class TestLogHandler extends Handler {
    private LogRecord lastLogRecord;

    @Override
    public void publish(LogRecord record) {
      this.lastLogRecord = record;
    }

    /** Implementation of flush is not needed for this test handler. */
    @Override
    public void flush() {}

    /** Implementation of close is not needed for this test handler. */
    @Override
    public void close() throws SecurityException {}

    public LogRecord getLastLogRecord() {
      return lastLogRecord;
    }
  }

  @Test
  public void testCleanupWorkspace_WorkspaceExists_SetsDeletedStatusAndLastModifiedBy() {
    // Arrange
    DbWorkspace dbWorkspace = createTestWorkspace();
    when(workspaceDao.getByNamespace(WORKSPACE_NAMESPACE)).thenReturn(Optional.of(dbWorkspace));

    // Act
    impersonatedWorkspaceService.cleanupWorkspace(WORKSPACE_NAMESPACE, LAST_MODIFIED_BY);

    // Assert
    ArgumentCaptor<DbWorkspace> workspaceCaptor = ArgumentCaptor.forClass(DbWorkspace.class);
    verify(workspaceDao).save(workspaceCaptor.capture());

    DbWorkspace savedWorkspace = workspaceCaptor.getValue();
    assertThat(savedWorkspace.getLastModifiedBy()).isEqualTo(LAST_MODIFIED_BY);
    assertThat(savedWorkspace.getWorkspaceActiveStatusEnum())
        .isEqualTo(WorkspaceActiveStatus.DELETED);

    // Verify that the same workspace object was modified
    assertThat(savedWorkspace).isSameInstanceAs(dbWorkspace);
  }

  @Test
  public void testCleanupWorkspace_WorkspaceDoesNotExist_NoOperationPerformed() {
    // Arrange
    when(workspaceDao.getByNamespace(WORKSPACE_NAMESPACE)).thenReturn(Optional.empty());

    // Act
    impersonatedWorkspaceService.cleanupWorkspace(WORKSPACE_NAMESPACE, LAST_MODIFIED_BY);

    // Assert
    verify(workspaceDao, never()).save(any(DbWorkspace.class));
  }

  @Test
  public void testCleanupWorkspace_WorkspaceAlreadyDeleted_NoOperationPerformed() {
    // Arrange
    DbWorkspace dbWorkspace = createTestWorkspace();
    dbWorkspace.setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.DELETED);
    dbWorkspace.setLastModifiedBy("previous-user@example.com");
    when(workspaceDao.getByNamespace(WORKSPACE_NAMESPACE)).thenReturn(Optional.of(dbWorkspace));

    // Act
    impersonatedWorkspaceService.cleanupWorkspace(WORKSPACE_NAMESPACE, LAST_MODIFIED_BY);

    // Assert - No save should be called since workspace is already deleted
    verify(workspaceDao, never()).save(any(DbWorkspace.class));
  }

  @Test
  public void testCleanupWorkspace_WorkspaceActiveStatus_UpdatesIfNotDeleted() {
    // Test that only non-deleted workspaces are updated
    DbWorkspace activeWorkspace = createTestWorkspace();
    activeWorkspace.setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.ACTIVE);
    when(workspaceDao.getByNamespace(WORKSPACE_NAMESPACE)).thenReturn(Optional.of(activeWorkspace));

    impersonatedWorkspaceService.cleanupWorkspace(WORKSPACE_NAMESPACE, LAST_MODIFIED_BY);

    // Should call save for active workspace
    verify(workspaceDao).save(activeWorkspace);
    assertThat(activeWorkspace.getWorkspaceActiveStatusEnum())
        .isEqualTo(WorkspaceActiveStatus.DELETED);
    assertThat(activeWorkspace.getLastModifiedBy()).isEqualTo(LAST_MODIFIED_BY);
  }

  @Test
  public void testCleanupWorkspace_DaoThrowsException_ExceptionPropagated() {
    // Arrange
    DbWorkspace dbWorkspace = createTestWorkspace();
    when(workspaceDao.getByNamespace(WORKSPACE_NAMESPACE)).thenReturn(Optional.of(dbWorkspace));
    RuntimeException expectedException = new RuntimeException("Database error");
    when(workspaceDao.save(any(DbWorkspace.class))).thenThrow(expectedException);

    // Act & Assert
    RuntimeException thrownException =
        assertThrows(
            RuntimeException.class,
            () ->
                impersonatedWorkspaceService.cleanupWorkspace(
                    WORKSPACE_NAMESPACE, LAST_MODIFIED_BY));

    assertThat(thrownException).isSameInstanceAs(expectedException);
    assertThat(thrownException.getMessage()).isEqualTo("Database error");

    // Verify that save was attempted
    verify(workspaceDao).save(any(DbWorkspace.class));
  }

  @Test
  public void testCleanupWorkspace_LogsWorkspaceCleanupDetails() {
    // Arrange
    DbWorkspace dbWorkspace = createTestWorkspace();
    String originalUser = "original-user@example.com";
    Timestamp originalTimestamp = Timestamp.from(Instant.parse("2025-07-20T10:15:30.00Z"));

    dbWorkspace.setLastModifiedBy(originalUser);
    dbWorkspace.setLastModifiedTime(originalTimestamp);

    when(workspaceDao.getByNamespace(WORKSPACE_NAMESPACE)).thenReturn(Optional.of(dbWorkspace));

    // Set up log capture
    Logger logger =
        Logger.getLogger("org.pmiops.workbench.impersonation.ImpersonatedWorkspaceServiceImpl");
    TestLogHandler testHandler = new TestLogHandler();
    logger.addHandler(testHandler);
    logger.setLevel(Level.INFO);

    try {
      // Act
      impersonatedWorkspaceService.cleanupWorkspace(WORKSPACE_NAMESPACE, LAST_MODIFIED_BY);

      // Assert
      LogRecord logRecord = testHandler.getLastLogRecord();
      assertThat(logRecord).isNotNull();
      assertThat(logRecord.getLevel()).isEqualTo(Level.INFO);

      String logMessage = logRecord.getMessage();
      assertThat(logMessage).contains("Workspace (" + WORKSPACE_NAMESPACE + ")");
      assertThat(logMessage).contains("that was last updated by " + originalUser);
      assertThat(logMessage).contains("on " + originalTimestamp);
      assertThat(logMessage).contains("has been cleaned up by " + LAST_MODIFIED_BY);

      // Verify the full expected message format
      String expectedMessage =
          String.format(
              "Workspace (%s), that was last updated by %s on %s, has been cleaned up by %s",
              WORKSPACE_NAMESPACE, originalUser, originalTimestamp, LAST_MODIFIED_BY);
      assertThat(logMessage).isEqualTo(expectedMessage);

    } finally {
      // Clean up
      logger.removeHandler(testHandler);
    }
  }

  private DbWorkspace createTestWorkspace() {
    DbWorkspace workspace = new DbWorkspace();
    workspace.setWorkspaceNamespace(WORKSPACE_NAMESPACE);
    workspace.setFirecloudName("test-firecloud-name");
    workspace.setLastModifiedBy("original-user@example.com");
    workspace.setLastModifiedTime(Timestamp.from(Instant.parse("2025-07-22T09:00:00.00Z")));
    workspace.setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.ACTIVE);
    return workspace;
  }
}
