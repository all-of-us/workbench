package org.pmiops.workbench.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Objects;
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
  private Long vcfSizeMb;
  private String outputDir;
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

  public DbWgsExtractCromwellSubmission setWgsExtractCromwellSubmissionId(
      long wgsExtractCromwellSubmissionId) {
    this.wgsExtractCromwellSubmissionId = wgsExtractCromwellSubmissionId;
    return this;
  }

  @Column(name = "submission_id")
  public String getSubmissionId() {
    return submissionId;
  }

  public DbWgsExtractCromwellSubmission setSubmissionId(String submissionId) {
    this.submissionId = submissionId;
    return this;
  }

  @ManyToOne
  @JoinColumn(name = "workspace_id")
  public DbWorkspace getWorkspace() {
    return workspace;
  }

  public DbWgsExtractCromwellSubmission setWorkspace(DbWorkspace workspace) {
    this.workspace = workspace;
    return this;
  }

  @ManyToOne
  @JoinColumn(name = "creator_id")
  public DbUser getCreator() {
    return creator;
  }

  public DbWgsExtractCromwellSubmission setCreator(DbUser creator) {
    this.creator = creator;
    return this;
  }

  @ManyToOne
  @JoinColumn(name = "data_set_id")
  public DbDataset getDataset() {
    return dataset;
  }

  public DbWgsExtractCromwellSubmission setDataset(DbDataset dataset) {
    this.dataset = dataset;
    return this;
  }

  @Column(name = "sample_count")
  public Long getSampleCount() {
    return sampleCount;
  }

  public DbWgsExtractCromwellSubmission setSampleCount(Long sampleCount) {
    this.sampleCount = sampleCount;
    return this;
  }

  @Column(name = "vcf_size_mb")
  public Long getVcfSizeMb() {
    return vcfSizeMb;
  }

  public DbWgsExtractCromwellSubmission setVcfSizeMb(Long vcfSizeMb) {
    this.vcfSizeMb = vcfSizeMb;
    return this;
  }

  @Column(name = "output_dir")
  public String getOutputDir() {
    return outputDir;
  }

  public DbWgsExtractCromwellSubmission setOutputDir(String outputDir) {
    this.outputDir = outputDir;
    return this;
  }

  @Column(name = "user_cost")
  public BigDecimal getUserCost() {
    return userCost;
  }

  public DbWgsExtractCromwellSubmission setUserCost(BigDecimal userCost) {
    this.userCost = userCost;
    return this;
  }

  @Column(name = "creation_time")
  public Timestamp getCreationTime() {
    return creationTime;
  }

  public DbWgsExtractCromwellSubmission setCreationTime(Timestamp creationTime) {
    this.creationTime = creationTime;
    return this;
  }

  @Column(name = "completion_time")
  public Timestamp getCompletionTime() {
    return completionTime;
  }

  public DbWgsExtractCromwellSubmission setCompletionTime(Timestamp completionTime) {
    this.completionTime = completionTime;
    return this;
  }

  @Column(name = "terra_status")
  private Short getTerraStatus() {
    return terraStatus;
  }

  private DbWgsExtractCromwellSubmission setTerraStatus(Short terraStatus) {
    this.terraStatus = terraStatus;
    return this;
  }

  @Transient
  public TerraJobStatus getTerraStatusEnum() {
    return DbStorageEnums.terraJobStatusFromStorage(getTerraStatus());
  }

  public DbWgsExtractCromwellSubmission setTerraStatusEnum(TerraJobStatus terraJobStatus) {
    setTerraStatus(DbStorageEnums.terraJobStatusToStorage(terraJobStatus));
    return this;
  }

  @Column(name = "terra_submission_date")
  public Timestamp getTerraSubmissionDate() {
    return terraSubmissionDate;
  }

  public DbWgsExtractCromwellSubmission setTerraSubmissionDate(Timestamp terra_submission_date) {
    this.terraSubmissionDate = terra_submission_date;
    return this;
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
