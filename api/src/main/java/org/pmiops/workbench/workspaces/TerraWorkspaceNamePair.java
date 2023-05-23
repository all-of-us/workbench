package org.pmiops.workbench.workspaces;

import java.util.Objects;

public class TerraWorkspaceNamePair {

  private final String workspaceNamespace;
  private final String workspaceName;

  public TerraWorkspaceNamePair(String workspaceNamespace, String workspaceName) {
    this.workspaceNamespace = workspaceNamespace;
    this.workspaceName = workspaceName;
  }

  public String getWorkspaceNamespace() {
    return workspaceNamespace;
  }

  public String getWorkspaceName() {
    return workspaceName;
  }

  @Override
  public int hashCode() {
    return Objects.hash(workspaceNamespace, workspaceName);
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof TerraWorkspaceNamePair)) {
      return false;
    }
    TerraWorkspaceNamePair that = (TerraWorkspaceNamePair) obj;
    return this.workspaceNamespace.equals(that.workspaceNamespace)
        && this.workspaceName.equals(that.workspaceName);
  }
}
