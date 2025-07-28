package org.pmiops.workbench.workspaces.impersonation;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
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
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
  @MockBean private FeaturedWorkspaceDao featuredWorkspaceDao;

  private static final String WORKSPACE_NAMESPACE = "test-workspace-namespace";
  private static final String LAST_MODIFIED_BY = "test-user@example.com";

  // Custom log handler to capture log messages for testing
  private static class TestLogHandler extends Handler {
    private LogRecord lastLogRecord;

    @Override
    public void publish(LogRecord record) {
      this.lastLogRecord = record;
    }

    @Override
    public void flush() {
      /* Implementation of flush is not needed for this test handler. */
    }

    @Override
    public void close() throws SecurityException {
      /* Implementation of close is not needed for this test handler. */
    }

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
    verifyWorkspaceUpdated(dbWorkspace);
    verify(featuredWorkspaceDao).deleteDbFeaturedWorkspaceByWorkspace(dbWorkspace);
  }

  @ParameterizedTest
  @MethodSource("noOperationScenarios")
  public void testCleanupWorkspace_NoOperationPerformed(
      String scenarioName, Optional<DbWorkspace> workspaceOptional) {
    // Arrange
    when(workspaceDao.getByNamespace(WORKSPACE_NAMESPACE)).thenReturn(workspaceOptional);

    // Act
    impersonatedWorkspaceService.cleanupWorkspace(WORKSPACE_NAMESPACE, LAST_MODIFIED_BY);

    // Assert
    verify(workspaceDao, never()).save(any(DbWorkspace.class));
    verify(featuredWorkspaceDao, never()).deleteDbFeaturedWorkspaceByWorkspace(any(DbWorkspace.class));
  }

  private static Stream<Arguments> noOperationScenarios() {
    // Workspace does not exist scenario
    Optional<DbWorkspace> emptyOptional = Optional.empty();

    // Workspace already deleted scenario
    DbWorkspace deletedWorkspace = new DbWorkspace();
    deletedWorkspace.setWorkspaceNamespace(WORKSPACE_NAMESPACE);
    deletedWorkspace.setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.DELETED);
    deletedWorkspace.setLastModifiedBy("previous-user@example.com");
    Optional<DbWorkspace> deletedOptional = Optional.of(deletedWorkspace);

    return Stream.of(
        Arguments.of("WorkspaceDoesNotExist", emptyOptional),
        Arguments.of("WorkspaceAlreadyDeleted", deletedOptional));
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

    // Verify that save was attempted but featured workspace cleanup was not called due to exception
    verify(workspaceDao).save(any(DbWorkspace.class));
    verify(featuredWorkspaceDao, never()).deleteDbFeaturedWorkspaceByWorkspace(any(DbWorkspace.class));
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

  @Test
  public void testCleanupWorkspace_WhenWorkspaceNotFound() {
    // Arrange
    when(workspaceDao.getByNamespace(WORKSPACE_NAMESPACE)).thenReturn(Optional.empty());

    // Act
    impersonatedWorkspaceService.cleanupWorkspace(WORKSPACE_NAMESPACE, LAST_MODIFIED_BY);

    // Assert
    verify(workspaceDao, never()).save(any(DbWorkspace.class));
    verify(featuredWorkspaceDao, never()).deleteDbFeaturedWorkspaceByWorkspace(any(DbWorkspace.class));
  }

  @Test
  public void testCleanupWorkspace_WhenWorkspaceAlreadyDeleted() {
    // Arrange
    DbWorkspace deletedWorkspace = createTestWorkspace();
    deletedWorkspace.setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.DELETED);
    when(workspaceDao.getByNamespace(WORKSPACE_NAMESPACE)).thenReturn(Optional.of(deletedWorkspace));

    // Act
    impersonatedWorkspaceService.cleanupWorkspace(WORKSPACE_NAMESPACE, LAST_MODIFIED_BY);

    // Assert
    verify(workspaceDao, never()).save(any(DbWorkspace.class));
    verify(featuredWorkspaceDao, never()).deleteDbFeaturedWorkspaceByWorkspace(any(DbWorkspace.class));
  }

  @Test
  public void testCleanupWorkspace_FeaturedWorkspaceDaoThrowsException_ExceptionPropagated() {
    // Arrange
    DbWorkspace dbWorkspace = createTestWorkspace();
    when(workspaceDao.getByNamespace(WORKSPACE_NAMESPACE)).thenReturn(Optional.of(dbWorkspace));
    RuntimeException expectedException = new RuntimeException("Featured workspace DAO error");
    doThrow(expectedException).when(featuredWorkspaceDao).deleteDbFeaturedWorkspaceByWorkspace(dbWorkspace);

    // Act & Assert
    RuntimeException thrownException =
        assertThrows(
            RuntimeException.class,
            () ->
                impersonatedWorkspaceService.cleanupWorkspace(
                    WORKSPACE_NAMESPACE, LAST_MODIFIED_BY));

    assertThat(thrownException).isSameInstanceAs(expectedException);
    assertThat(thrownException.getMessage()).isEqualTo("Featured workspace DAO error");

    // Verify that both workspace save and featured workspace cleanup were attempted during the transaction
    // Note: Due to @Transactional annotation, the workspace save will be rolled back when the 
    // featured workspace cleanup fails, but both method calls should still occur
    verify(workspaceDao).save(any(DbWorkspace.class));
    verify(featuredWorkspaceDao).deleteDbFeaturedWorkspaceByWorkspace(dbWorkspace);
  }

  private void verifyWorkspaceUpdated(DbWorkspace dbWorkspace) {
    ArgumentCaptor<DbWorkspace> workspaceCaptor = ArgumentCaptor.forClass(DbWorkspace.class);
    verify(workspaceDao).save(workspaceCaptor.capture());

    DbWorkspace savedWorkspace = workspaceCaptor.getValue();
    assertThat(savedWorkspace.getLastModifiedBy()).isEqualTo(LAST_MODIFIED_BY);
    assertThat(savedWorkspace.getWorkspaceActiveStatusEnum())
        .isEqualTo(WorkspaceActiveStatus.DELETED);

    // Verify that the same workspace object was modified
    assertThat(savedWorkspace).isSameInstanceAs(dbWorkspace);
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
