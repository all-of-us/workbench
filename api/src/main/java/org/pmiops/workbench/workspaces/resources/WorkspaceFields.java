package org.pmiops.workbench.workspaces.resources;

import org.pmiops.workbench.model.BillingStatus;

/**
 * A transitional POJO to assist WorkspaceResourceMapper, consisting of the fields sourced from
 * DbWorkspace and WorkspaceAccessLevel
 */
public class WorkspaceFields {
  private Long workspaceId;
  private String workspaceNamespace;
  private String workspaceFirecloudName;
  private BillingStatus workspaceBillingStatus;
  private String cdrVersionId;
  private String accessTierShortName;
  private String permission;
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

  public BillingStatus getWorkspaceBillingStatus() {
    return workspaceBillingStatus;
  }

  public void setWorkspaceBillingStatus(BillingStatus workspaceBillingStatus) {
    this.workspaceBillingStatus = workspaceBillingStatus;
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

  public String getPermission() {
    return permission;
  }

  public void setPermission(String permission) {
    this.permission = permission;
  }

  public boolean isAdminLocked() {
    return adminLocked;
  }

  public void setAdminLocked(boolean adminLocked) {
    this.adminLocked = adminLocked;
  }
}
