package org.pmiops.workbench.db.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.pmiops.workbench.model.WorkspaceAccessLevel;

@Entity
@Table(name = "user_workspace")
public class WorkspaceUserRole {
  private long userWorkspaceId;
  private User user;
  private Workspace workspace;
  private WorkspaceAccessLevel role;



  @Id
  @GeneratedValue
  @Column(name = "user_workspace_id")
  public long getUserWorkspaceId() {
    return userWorkspaceId;
  }

  public void setUserWorkspaceId(long userWorkspaceId) {
    this.userWorkspaceId = userWorkspaceId;
  }
  
  @ManyToOne
  @JoinColumn(name="user_id")
  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  @ManyToOne
  @JoinColumn(name="workspace_id")
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
