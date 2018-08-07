package org.pmiops.workbench.db.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.sql.Timestamp;

@Entity
@Table(name = "user_recent_resource")
public class UserRecentResource {
  private Timestamp lastAccessDate;
  private int id;
  private Long cohortId;
  private String notebookName;
  private Long userId;
  private Long workspaceId;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  @Column(name = "user_id")
  public long getUserId() {return userId;}

  public void setUserId(Long userId ) {this.userId = userId;}

  @Column(name = "workspace_id")
  public long getWorkspaceId() { return workspaceId; }

  public void setWorkspaceId(long workspaceId ) {this.workspaceId = workspaceId;}

  @Column(name = "cohort_id")
  public Long getCohortId() {
    return cohortId;
  }

  public void setCohortId(Long cohortId) {
    this.cohortId = cohortId;
  }


  @Column(name = "notebook_name")
  public String getNotebookName() {
    return this.notebookName;
  }

  public void setNotebookName(String notebookName) {
    this.notebookName = notebookName;
  }

  @Column(name = "lastAccessDate")
  public Timestamp getLastAccessTime() {
    return lastAccessDate;
  }

  public void setLastAccessTime(Timestamp lastAccessDate) {
    this.lastAccessDate = lastAccessDate;
  }
}

