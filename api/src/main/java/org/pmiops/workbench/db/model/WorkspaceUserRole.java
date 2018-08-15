package org.pmiops.workbench.db.model;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.pmiops.workbench.model.WorkspaceAccessLevel;

@Entity
@Table(name = "user_workspace")
public class WorkspaceUserRole {
  private static final BiMap<WorkspaceAccessLevel, Short> CLIENT_TO_STORAGE_WORKSPACE_ACCESS =
      ImmutableBiMap.<WorkspaceAccessLevel, Short>builder()
      .put(WorkspaceAccessLevel.NO_ACCESS, (short) 0)
      .put(WorkspaceAccessLevel.READER, (short) 1)
      .put(WorkspaceAccessLevel.WRITER, (short) 2)
      .put(WorkspaceAccessLevel.OWNER, (short) 3)
      .build();
  public static WorkspaceAccessLevel accessLevelFromStorage(Short level) {
    return CLIENT_TO_STORAGE_WORKSPACE_ACCESS.inverse().get(level);
  }

  public static Short accessLevelToStorage(WorkspaceAccessLevel level) {
    return CLIENT_TO_STORAGE_WORKSPACE_ACCESS.get(level);
  }

  private long userWorkspaceId;
  private User user;
  private Workspace workspace;
  private Short role;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
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
  public Short getRole() {
    return this.role;
  }

  public void setRole(Short role) {
    this.role = role;
  }
}
