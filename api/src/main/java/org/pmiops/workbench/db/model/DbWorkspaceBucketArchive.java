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
@Table(name = "workspace_bucket_archive")
public class DbWorkspaceBucketArchive {
  private long id;
  private Long legacyWorkspaceId;
  private String gcsPath;
  private Timestamp created;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public long getId() {
    return id;
  }

  public DbWorkspaceBucketArchive setId(long id) {
    this.id = id;
    return this;
  }

  @Column(name = "legacy_workspace_id")
  public Long getLegacyWorkspaceId() {
    return legacyWorkspaceId;
  }

  public DbWorkspaceBucketArchive setLegacyWorkspaceId(Long legacyWorkspaceId) {
    this.legacyWorkspaceId = legacyWorkspaceId;
    return this;
  }

  @Column(name = "gcs_path")
  public String getGcsPath() {
    return gcsPath;
  }

  public DbWorkspaceBucketArchive setGcsPath(String gcsPath) {
    this.gcsPath = gcsPath;
    return this;
  }

  @Column(name = "created")
  public Timestamp getCreated() {
    return created;
  }

  public DbWorkspaceBucketArchive setCreated(Timestamp created) {
    this.created = created;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DbWorkspaceBucketArchive that = (DbWorkspaceBucketArchive) o;
    return id == that.id
        && Objects.equals(legacyWorkspaceId, that.legacyWorkspaceId)
        && Objects.equals(gcsPath, that.gcsPath)
        && Objects.equals(created, that.created);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, legacyWorkspaceId, gcsPath, created);
  }

  public DbWorkspaceBucketArchive() {}

  public DbWorkspaceBucketArchive(Long legacyWorkspaceId, String gcsPath, Timestamp created) {
    this.legacyWorkspaceId = legacyWorkspaceId;
    this.gcsPath = gcsPath;
    this.created = created;
  }
}
