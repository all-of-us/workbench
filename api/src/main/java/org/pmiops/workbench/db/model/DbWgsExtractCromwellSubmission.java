package org.pmiops.workbench.db.model;

import java.sql.Timestamp;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "wgs_extract_cromwell_submission")
public class DbWgsExtractCromwellSubmission {

  private long wgsExtractCromwellSubmissionId;
  private String submissionId;
  private DbWorkspace workspace;
  private DbUser creator;
  private DbDataset dataset;
  private long sampleCount;
  private Timestamp creationTime;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "wgs_extract_cromwell_submission_id")
  public long getWgsExtractCromwellSubmissionId() {
    return wgsExtractCromwellSubmissionId;
  }

  public void setWgsExtractCromwellSubmissionId(long wgsExtractCromwellSubmissionId) {
    this.wgsExtractCromwellSubmissionId = wgsExtractCromwellSubmissionId;
  }

  @Column(name = "submission_id")
  public String getSubmissionId() {
    return submissionId;
  }

  public void setSubmissionId(String submissionId) {
    this.submissionId = submissionId;
  }

  @ManyToOne
  @JoinColumn(name = "workspace_id")
  public DbWorkspace getWorkspace() {
    return workspace;
  }

  public void setWorkspace(DbWorkspace workspace) {
    this.workspace = workspace;
  }

  @ManyToOne
  @JoinColumn(name = "creator_id")
  public DbUser getCreator() {
    return creator;
  }

  public void setCreator(DbUser creator) {
    this.creator = creator;
  }

  @ManyToOne
  @JoinColumn(name = "data_set_id")
  public DbDataset getDataset() {
    return dataset;
  }

  public void setDataset(DbDataset dataset) {
    this.dataset = dataset;
  }

  @Column(name = "sample_count")
  public long getSampleCount() {
    return sampleCount;
  }

  public void setSampleCount(long sampleCount) {
    this.sampleCount = sampleCount;
  }

  @Column(name = "creation_time")
  public Timestamp getCreationTime() {
    return creationTime;
  }

  public void setCreationTime(Timestamp creationTime) {
    this.creationTime = creationTime;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DbWgsExtractCromwellSubmission that = (DbWgsExtractCromwellSubmission) o;
    return wgsExtractCromwellSubmissionId == that.wgsExtractCromwellSubmissionId
        && sampleCount == that.sampleCount
        && Objects.equals(submissionId, that.submissionId)
        && Objects.equals(workspace, that.workspace)
        && Objects.equals(creator, that.creator)
        && Objects.equals(dataset, that.dataset)
        && Objects.equals(creationTime, that.creationTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        wgsExtractCromwellSubmissionId,
        submissionId,
        workspace,
        creator,
        dataset,
        sampleCount,
        creationTime);
  }
}
