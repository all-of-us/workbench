package org.pmiops.workbench.workspaces.migration;

import org.pmiops.workbench.model.MigrationBucketContentsResponse;

public interface WorkspaceMigrationService {
  void startWorkspaceMigration(String namespace, String terraName);

  MigrationBucketContentsResponse getBucketContents(String namespace, String terraName);
}
