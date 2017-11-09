package org.pmiops.workbench.db.model;


import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

import org.pmiops.workbench.model.WorkspaceAccessLevel;


@Entity
@Table(name = "user_workspace")
@IdClass(WorkspaceUserRoleId.class)
public class WorkspaceUserRole {
  @Id
  private long userId;
  private User user;
  @Id
  private long workspaceId;
  private Workspace workspace;
  private WorkspaceAccessLevel role;


  @Column(name="user_id")
  public long getUserId() {
    return userId;
  }

  public void setUserId(long userId) {
    this.userId = userId;
  }

  @ManyToOne
  @PrimaryKeyJoinColumn(name="user_id", referencedColumnName="user_id")
  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  @Column(name="workspace_id")
  public long getWorkspaceId() {
    return workspaceId;
  }

  public void setWorkspaceId(long workspaceId) {
    this.workspaceId = workspaceId;
  }

  @ManyToOne
  @PrimaryKeyJoinColumn(name="workspace_id", referencedColumnName="workspace_id")
  public Workspace getWorkspace() {
    return workspace;
  }

  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }


  @Column(name="role")
  public WorkspaceAccessLevel getRole() {
    return this.role;
  }

  public void setRole(WorkspaceAccessLevel role) {
    this.role = role;
  }
}
