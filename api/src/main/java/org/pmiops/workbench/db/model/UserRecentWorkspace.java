package org.pmiops.workbench.db.model;

import java.sql.Timestamp;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "user_recent_workspace")
public class UserRecentWorkspace {
  private Timestamp lastAccessDate;
  private long id;
  private Long userId;
  private Long workspaceId;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  @Column(name = "user_id")
  public long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  @Column(name = "workspace_id")
  public long getWorkspaceId() {
    return workspaceId;
  }

  public void setWorkspaceId(long workspaceId) {
    this.workspaceId = workspaceId;
  }

  @Column(name = "last_access_date")
  public Timestamp getLastAccessDate() {
    return lastAccessDate;
  }

  public void setLastAccessDate(Timestamp lastAccessDate) {
    this.lastAccessDate = lastAccessDate;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    UserRecentWorkspace that = (UserRecentWorkspace) o;
    return id == that.id
        && lastAccessDate.equals(that.lastAccessDate)
        && userId.equals(that.userId)
        && workspaceId.equals(that.workspaceId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(lastAccessDate, id, userId, workspaceId);
  }

  public UserRecentWorkspace() {}

  public UserRecentWorkspace(long workspaceId, long userId, Timestamp lastAccessDate) {
    this.workspaceId = workspaceId;
    this.userId = userId;
    this.lastAccessDate = lastAccessDate;
  }
}
