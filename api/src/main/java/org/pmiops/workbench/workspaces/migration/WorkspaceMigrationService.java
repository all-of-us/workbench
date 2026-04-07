package org.pmiops.workbench.workspaces.migration;

import java.util.List;
import org.pmiops.workbench.model.MigrationBucketContentsResponse;

public interface WorkspaceMigrationService {
  void startWorkspaceMigration(
      String namespace,
      String terraName,
      List<String> folders,
      String podId,
      String researchPurpose);

  MigrationBucketContentsResponse getBucketContents(String namespace, String terraName);

  void checkMigrationStatus(String namespace, String terraName);
}
