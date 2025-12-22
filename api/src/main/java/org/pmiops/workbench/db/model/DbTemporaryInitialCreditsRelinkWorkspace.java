package org.pmiops.workbench.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import java.util.Objects;

@Entity
@Table(name = "temporary_initial_credits_relink_workspace")
public class DbTemporaryInitialCreditsRelinkWorkspace {
  private long id;
  private String sourceWorkspaceNamespace;
  private String destinationWorkspaceNamespace;
  private Timestamp created;
  private Timestamp cloneCompleted;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public long getId() {
    return id;
  }

  public DbTemporaryInitialCreditsRelinkWorkspace setId(long id) {
    this.id = id;
    return this;
  }

  @Column(name = "source_workspace_namespace")
  public String getSourceWorkspaceNamespace() {
    return sourceWorkspaceNamespace;
  }

  public DbTemporaryInitialCreditsRelinkWorkspace setSourceWorkspaceNamespace(
      String sourceWorkspaceNamespace) {
    this.sourceWorkspaceNamespace = sourceWorkspaceNamespace;
    return this;
  }

  @Column(name = "destination_workspace_namespace")
  public String getDestinationWorkspaceNamespace() {
    return destinationWorkspaceNamespace;
  }

  public DbTemporaryInitialCreditsRelinkWorkspace setDestinationWorkspaceNamespace(
      String destinationWorkspaceNamespace) {
    this.destinationWorkspaceNamespace = destinationWorkspaceNamespace;
    return this;
  }

  @Column(name = "created")
  public Timestamp getCreated() {
    return created;
  }

  public DbTemporaryInitialCreditsRelinkWorkspace setCreated(Timestamp created) {
    this.created = created;
    return this;
  }

  @Column(name = "clone_completed")
  public Timestamp getCloneCompleted() {
    return cloneCompleted;
  }

  public DbTemporaryInitialCreditsRelinkWorkspace setCloneCompleted(Timestamp cloneCompleted) {
    this.cloneCompleted = cloneCompleted;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DbTemporaryInitialCreditsRelinkWorkspace that = (DbTemporaryInitialCreditsRelinkWorkspace) o;
    return id == that.id
        && Objects.equals(sourceWorkspaceNamespace, that.sourceWorkspaceNamespace)
        && Objects.equals(destinationWorkspaceNamespace, that.destinationWorkspaceNamespace)
        && Objects.equals(created, that.created)
        && Objects.equals(cloneCompleted, that.cloneCompleted);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        id, sourceWorkspaceNamespace, destinationWorkspaceNamespace, created, cloneCompleted);
  }

  public DbTemporaryInitialCreditsRelinkWorkspace() {}

  public DbTemporaryInitialCreditsRelinkWorkspace(
      String sourceWorkspaceNamespace,
      String destinationWorkspaceNamespace,
      Timestamp created,
      Timestamp cloneCompleted) {
    this.sourceWorkspaceNamespace = sourceWorkspaceNamespace;
    this.destinationWorkspaceNamespace = destinationWorkspaceNamespace;
    this.created = created;
    this.cloneCompleted = cloneCompleted;
  }
}
