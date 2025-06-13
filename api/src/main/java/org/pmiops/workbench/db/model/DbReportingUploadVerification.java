package org.pmiops.workbench.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.sql.Timestamp;

@Entity
@Table(name = "reporting_upload_verification")
public class DbReportingUploadVerification {

  private long id;
  private String tableName;
  private Timestamp snapshotTimestamp;
  private Boolean uploaded;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  public long getId() {
    return id;
  }

  public DbReportingUploadVerification setId(long id) {
    this.id = id;
    return this;
  }

  @Column(name = "table", nullable = false)
  public String getTableName() {
    return tableName;
  }

  public DbReportingUploadVerification setTableName(String tableName) {
    this.tableName = tableName;
    return this;
  }

  @Column(name = "snapshot_timestamp", nullable = false)
  public Timestamp getSnapshotTimestamp() {
    return snapshotTimestamp;
  }

  public DbReportingUploadVerification setSnapshotTimestamp(Timestamp snapshotTimestamp) {
    this.snapshotTimestamp = snapshotTimestamp;
    return this;
  }

  @Column(name = "uploaded")
  public Boolean getUploaded() {
    return uploaded;
  }

  public DbReportingUploadVerification setUploaded(Boolean uploaded) {
    this.uploaded = uploaded;
    return this;
  }
}
