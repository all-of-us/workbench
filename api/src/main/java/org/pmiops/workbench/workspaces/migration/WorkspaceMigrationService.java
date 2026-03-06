package org.pmiops.workbench.workspaces.migration;

public interface WorkspaceMigrationService {
  void startWorkspaceMigration(String namespace, String terraName);
}
