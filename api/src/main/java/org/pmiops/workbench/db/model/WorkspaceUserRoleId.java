package org.pmiops.workbench.db.model;


import java.io.Serializable;

public class WorkspaceUserRoleId implements Serializable {

  private long workspaceId;

  private long userId;

  public long getWorkspaceId() {
    return this.workspaceId;
  }

  public void setWorkspaceId(long workspaceId) {
    this.workspaceId = workspaceId;
  }

  public long getUserId() {
    return this.userId;
  }

  public void setUserId(long userId) {
    this.userId = userId;
  }


  public int hashCode() {
    return (int)(workspaceId + userId);
  }

  public boolean equals(Object object) {
    if (object instanceof WorkspaceUserRoleId) {
      WorkspaceUserRoleId otherId = (WorkspaceUserRoleId) object;
      return (otherId.workspaceId == this.workspaceId) && (otherId.userId == this.userId);
    }
    return false;
  }

}
