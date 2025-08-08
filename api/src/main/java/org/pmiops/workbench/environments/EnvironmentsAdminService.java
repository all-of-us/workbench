package org.pmiops.workbench.environments;

import java.util.List;

public interface EnvironmentsAdminService {
  long deleteUnsharedWorkspaceEnvironmentsBatch(List<String> workspaceNamespaces);
}
