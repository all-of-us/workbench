package org.pmiops.workbench.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import org.springframework.data.annotation.CreatedDate;

@Entity
@Table(name = "workspace_inaccessible_to_sa")
public class DbWorkspaceInaccessibleToSa {

  private long id;
  private DbWorkspace workspace;
  private Timestamp creationTime;
  private String note;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "workspace_inaccessible_to_sa_id", nullable = false)
  public long getId() {
    return id;
  }

  public DbWorkspaceInaccessibleToSa setId(long workspaceInaccessibleToSaId) {
    this.id = workspaceInaccessibleToSaId;
    return this;
  }

  @OneToOne
  @JoinColumn(name = "workspace_id", nullable = false)
  public DbWorkspace getWorkspace() {
    return workspace;
  }

  public DbWorkspaceInaccessibleToSa setWorkspace(DbWorkspace workspace) {
    this.workspace = workspace;
    return this;
  }

  @CreatedDate
  @Column(name = "creation_time", nullable = false)
  public Timestamp getCreationTime() {
    return creationTime;
  }

  public DbWorkspaceInaccessibleToSa setCreationTime(Timestamp creationTime) {
    this.creationTime = creationTime;
    return this;
  }

  @Column(name = "note", nullable = false)
  public String getNote() {
    return note;
  }

  public DbWorkspaceInaccessibleToSa setNote(String note) {
    this.note = note;
    return this;
  }
}
