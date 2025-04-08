package org.pmiops.workbench.workspaces.resources;

/**
 * A transitional POJO to assist WorkspaceResourceMapper, consisting of the fields sourced from
 * DbWorkspace and WorkspaceAccessLevel
 */
class WorkspaceFields {
  private Long workspaceId;
  private String workspaceNamespace;
  private String workspaceFirecloudName;
  private String cdrVersionId;
  private String accessTierShortName;
  private boolean adminLocked;

  public Long getWorkspaceId() {
    return workspaceId;
  }

  public void setWorkspaceId(Long workspaceId) {
    this.workspaceId = workspaceId;
  }

  public String getWorkspaceNamespace() {
    return workspaceNamespace;
  }

  public void setWorkspaceNamespace(String workspaceNamespace) {
    this.workspaceNamespace = workspaceNamespace;
  }

  public String getWorkspaceFirecloudName() {
    return workspaceFirecloudName;
  }

  public void setWorkspaceFirecloudName(String workspaceFirecloudName) {
    this.workspaceFirecloudName = workspaceFirecloudName;
  }

  public String getCdrVersionId() {
    return cdrVersionId;
  }

  public void setCdrVersionId(String cdrVersionId) {
    this.cdrVersionId = cdrVersionId;
  }

  public String getAccessTierShortName() {
    return accessTierShortName;
  }

  public void setAccessTierShortName(String accessTierShortName) {
    this.accessTierShortName = accessTierShortName;
  }

  public boolean isAdminLocked() {
    return adminLocked;
  }

  public void setAdminLocked(boolean adminLocked) {
    this.adminLocked = adminLocked;
  }
}
