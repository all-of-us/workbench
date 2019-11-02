package org.pmiops.workbench.db.model;

import java.sql.Timestamp;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "user_recent_resource")
public class UserRecentResource {
  private Timestamp lastAccessDate;
  private int id;
  private String notebookName;
  private Long userId;
  private Long workspaceId;
  private DbCohort cohort;
  private DbConceptSet conceptSet;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public int getId() {
    return id;
  }

  public void setId(int id) {
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

  /**
   * TODO: Rename this column to reflect reality.
   *
   * @return the full GCS URI for the notebook, not just the notebook name as implied
   */
  @Column(name = "notebook_name")
  public String getNotebookName() {
    return this.notebookName;
  }

  public void setNotebookName(String notebookName) {
    this.notebookName = notebookName;
  }

  @Column(name = "lastAccessDate")
  public Timestamp getLastAccessDate() {
    return lastAccessDate;
  }

  public void setLastAccessDate(Timestamp lastAccessDate) {
    this.lastAccessDate = lastAccessDate;
  }

  @ManyToOne
  @JoinColumn(name = "cohort_id")
  public DbCohort getCohort() {
    return cohort;
  }

  public void setCohort(DbCohort cohort) {
    this.cohort = cohort;
  }

  @ManyToOne
  @JoinColumn(name = "concept_set_id")
  public DbConceptSet getConceptSet() {
    return conceptSet;
  }

  public void setConceptSet(DbConceptSet conceptSet) {
    this.conceptSet = conceptSet;
  }

  public UserRecentResource() {}

  public UserRecentResource(
      long workspaceId, long userId, String notebookName, Timestamp lastAccessDate) {
    this.workspaceId = workspaceId;
    this.userId = userId;
    this.notebookName = notebookName;
    this.lastAccessDate = lastAccessDate;
    this.cohort = null;
    this.conceptSet = null;
  }

  public UserRecentResource(long workspaceId, long userId, Timestamp lastAccessDate) {
    this.workspaceId = workspaceId;
    this.userId = userId;
    this.notebookName = null;
    this.lastAccessDate = lastAccessDate;
  }
}
