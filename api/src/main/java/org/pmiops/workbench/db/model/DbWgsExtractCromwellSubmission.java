package org.pmiops.workbench.db.model;

import java.math.BigDecimal;
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
import javax.persistence.Transient;
import org.pmiops.workbench.model.TerraJobStatus;

@Entity
@Table(name = "wgs_extract_cromwell_submission")
public class DbWgsExtractCromwellSubmission {

  private long wgsExtractCromwellSubmissionId;
  private String submissionId;
  private DbWorkspace workspace;
  private DbUser creator;
  private DbDataset dataset;
  private Long sampleCount;
  private BigDecimal userCost;
  private Timestamp creationTime;
  private Timestamp completionTime;
  private Short terraStatus;
  private Timestamp terraSubmissionDate;

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
  public Long getSampleCount() {
    return sampleCount;
  }

  public void setSampleCount(Long sampleCount) {
    this.sampleCount = sampleCount;
  }

  @Column(name = "user_cost")
  public BigDecimal getUserCost() {
    return userCost;
  }

  public void setUserCost(BigDecimal userCost) {
    this.userCost = userCost;
  }

  @Column(name = "creation_time")
  public Timestamp getCreationTime() {
    return creationTime;
  }

  public void setCreationTime(Timestamp creationTime) {
    this.creationTime = creationTime;
  }

  @Column(name = "completion_time")
  public Timestamp getCompletionTime() {
    return completionTime;
  }

  public void setCompletionTime(Timestamp completionTime) {
    this.completionTime = completionTime;
  }

  @Column(name = "terra_status")
  private Short getTerraStatus() {
    return terraStatus;
  }

  private void setTerraStatus(Short terraStatus) {
    this.terraStatus = terraStatus;
  }

  @Transient
  public TerraJobStatus getTerraStatusEnum() {
    return DbStorageEnums.terraJobStatusFromStorage(getTerraStatus());
  }

  public void setTerraStatusEnum(TerraJobStatus terraJobStatus) {
    setTerraStatus(DbStorageEnums.terraJobStatusToStorage(terraJobStatus));
  }

  @Column(name = "terra_submission_date")
  public Timestamp getTerraSubmissionDate() {
    return terraSubmissionDate;
  }

  public void setTerraSubmissionDate(Timestamp terra_submission_date) {
    this.terraSubmissionDate = terra_submission_date;
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
        && Objects.equals(userCost, that.userCost)
        && Objects.equals(creationTime, that.creationTime)
        && Objects.equals(completionTime, that.completionTime)
        && Objects.equals(terraStatus, that.terraStatus)
        && Objects.equals(terraSubmissionDate, that.terraSubmissionDate);
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
        userCost,
        creationTime,
        completionTime,
        terraStatus,
        terraSubmissionDate);
  }
}
