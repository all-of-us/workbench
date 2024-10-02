package org.pmiops.workbench.tools;

public record ListDisksRow(
    String name,
    String environmentType,
    String sizeInGb,
    String googleProject,
    String status,
    String creator,
    String createdDate,
    String dateAccessed,
    String workspaceNamespace,
    String workspaceDisplayName,
    String workspaceTerraName,
    String workspaceCreator,
    String billingAccountType) {

  public String[] toArray() {
    return new String[] {
      name,
      environmentType,
      sizeInGb,
      googleProject,
      status,
      creator,
      createdDate,
      dateAccessed,
      workspaceNamespace,
      workspaceDisplayName,
      workspaceTerraName,
      workspaceCreator,
      billingAccountType
    };
  }
}
