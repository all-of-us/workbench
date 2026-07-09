package org.pmiops.workbench.workspaces.migration;

import java.util.List;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.model.MigrationBucketContentsResponse;
import org.pmiops.workbench.model.PreprodWorkspace;

public interface WorkspaceMigrationService {
  void startWorkspaceMigration(
      String namespace,
      String terraName,
      List<String> folders,
      String podId,
      String researchPurpose);

  void syncWorkspaceFolders(String namespace, String terraName, List<String> folders);

  MigrationBucketContentsResponse getBucketContents(String namespace, String terraName);

  void checkMigrationStatus(String namespace, String terraName);

  void checkFolderSyncStatus(String namespace, String terraName, String jobName);

  void startPreprodWorkspaceMigration(
      PreprodWorkspace preprodWorkspace,
      String email,
      String researchPurpose,
      String bucketName,
      String billingPod);

  WorkspaceDao.WorkspaceArchiveView getNextWorkspaceToArchive();

  void startWorkspaceArchive(String namespace, String terraName);

  void startWorkspaceArchiveTestEnv(String namespace, String terraName);

  void checkArchiveStatus(String workspaceNamespace, String workspaceName);

  void startWorkspaceRecovery(String namespace, String terraName);

  void requestWorkspaceRecovery(String namespace, String terraName, String podId);

  void checkRecoveryStatus(String namespace, String terraName);
}
