package org.pmiops.workbench.db.model;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.data.annotation.ReadOnlyProperty;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.sql.Timestamp;

@Entity
@Table(name = "notebook_cohort_cache")
public class NotebookCohortCache {
  private Timestamp lastAccessDate;
  private int id;
  private Long cohortId;
  private String notebookName;
  private WorkspaceUserRole workspaceUser;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  @ManyToOne
  @JoinColumn(name = "user_workspace_id")
  public WorkspaceUserRole getUserWorkspaceId() {
    return workspaceUser;
  }

  public void setUserWorkspaceId(WorkspaceUserRole workspaceUser) {
    this.workspaceUser = workspaceUser;
  }

  @Column(name = "cohort_id")
  public long getCohortId() {
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

