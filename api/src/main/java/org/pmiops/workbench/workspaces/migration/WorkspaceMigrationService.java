package org.pmiops.workbench.workspaces.migration;

public interface WorkspaceMigrationService {

  void startWorkspaceRecovery(String namespace, String researchPurpose, String podId);

  void requestWorkspaceRecovery(String namespace, String podId);

  void checkRecoveryStatus(String namespace);
}
