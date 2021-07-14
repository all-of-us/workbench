package org.pmiops.workbench.db.model;

import java.sql.Timestamp;
import java.util.Objects;
import java.util.Optional;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import org.pmiops.workbench.model.ArchivalStatus;

@Entity
@Table(name = "cdr_version")
public class DbCdrVersion {
  private long cdrVersionId;
  private Boolean isDefault;
  private String name;
  private DbAccessTier accessTier;
  private short releaseNumber;
  private short archivalStatus;
  private String bigqueryProject;
  private String bigqueryDataset;
  private Timestamp creationTime;
  private int numParticipants;
  private String cdrDbName;
  private String elasticIndexBaseName;
  private String wgsBigqueryDataset;
  private Boolean hasFitbitData;
  private Boolean hasCopeSurveyData;
  private String allSamplesWgsDataBucket;
  private String singleSampleArrayDataBucket;

  @Id
  @Column(name = "cdr_version_id")
  public long getCdrVersionId() {
    return cdrVersionId;
  }

  public void setCdrVersionId(long cdrVersionId) {
    this.cdrVersionId = cdrVersionId;
  }

  // I changed the type to object Boolean because Hibernate started complaining about assigning
  // nulls to primitive boolean.  TODO why now / what changed?

  @Column(name = "is_default")
  public Boolean getIsDefault() {
    return isDefault;
  }

  public void setIsDefault(Boolean isDefault) {
    this.isDefault = isDefault;
  }

  @Transient
  @NotNull
  public boolean getIsDefaultNotNull() {
    return Optional.ofNullable(isDefault).orElse(false);
  }

  @Column(name = "name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @ManyToOne
  @JoinColumn(name = "access_tier")
  public DbAccessTier getAccessTier() {
    return accessTier;
  }

  public void setAccessTier(DbAccessTier accessTier) {
    this.accessTier = accessTier;
  }

  @Column(name = "archival_status")
  public Short getArchivalStatus() {
    return archivalStatus;
  }

  public void setArchivalStatus(Short archivalStatus) {
    this.archivalStatus = archivalStatus;
  }

  @Transient
  public ArchivalStatus getArchivalStatusEnum() {
    return DbStorageEnums.archivalStatusFromStorage(getArchivalStatus());
  }

  public void setArchivalStatusEnum(ArchivalStatus archivalStatus) {
    setArchivalStatus(DbStorageEnums.archivalStatusToStorage(archivalStatus));
  }

  @Column(name = "release_number")
  public short getReleaseNumber() {
    return releaseNumber;
  }

  public void setReleaseNumber(short releaseNumber) {
    this.releaseNumber = releaseNumber;
  }

  @Column(name = "bigquery_project")
  public String getBigqueryProject() {
    return bigqueryProject;
  }

  public void setBigqueryProject(String bigqueryProject) {
    this.bigqueryProject = bigqueryProject;
  }

  @Column(name = "bigquery_dataset")
  public String getBigqueryDataset() {
    return bigqueryDataset;
  }

  public void setBigqueryDataset(String bigqueryDataset) {
    this.bigqueryDataset = bigqueryDataset;
  }

  @Column(name = "creation_time")
  public Timestamp getCreationTime() {
    return creationTime;
  }

  public void setCreationTime(Timestamp creationTime) {
    this.creationTime = creationTime;
  }

  @Column(name = "num_participants")
  public int getNumParticipants() {
    return numParticipants;
  }

  public void setNumParticipants(int numParticipants) {
    this.numParticipants = numParticipants;
  }

  @Column(name = "cdr_db_name")
  public String getCdrDbName() {
    return cdrDbName;
  }

  public void setCdrDbName(String cdrDbName) {
    this.cdrDbName = cdrDbName;
  }

  @Column(name = "elastic_index_base_name")
  public String getElasticIndexBaseName() {
    return elasticIndexBaseName;
  }

  public void setElasticIndexBaseName(String elasticIndexBaseName) {
    this.elasticIndexBaseName = elasticIndexBaseName;
  }

  @Column(name = "wgs_bigquery_dataset")
  public String getWgsBigqueryDataset() {
    return wgsBigqueryDataset;
  }

  public void setWgsBigqueryDataset(String wgsBigqueryDataset) {
    this.wgsBigqueryDataset = wgsBigqueryDataset;
  }

  @Column(name = "has_fitbit_data")
  public Boolean getHasFitbitData() {
    return hasFitbitData == null ? false : hasFitbitData;
  }

  public void setHasFitbitData(Boolean hasFitbitData) {
    this.hasFitbitData = hasFitbitData;
  }

  @Column(name = "has_copesurvey_data")
  public Boolean getHasCopeSurveyData() {
    return hasCopeSurveyData == null ? false : hasCopeSurveyData;
  }

  public void setHasCopeSurveyData(Boolean hasCopeSurveyData) {
    this.hasCopeSurveyData = hasCopeSurveyData;
  }

  @Column(name = "all_samples_wgs_data_bucket")
  public String getAllSamplesWgsDataBucket() {
    return allSamplesWgsDataBucket;
  }

  public void setAllSamplesWgsDataBucket(String allSamplesWgsDataBucket) {
    this.allSamplesWgsDataBucket = allSamplesWgsDataBucket;
  }

  @Column(name = "single_sample_array_data_bucket")
  public String getSingleSampleArrayDataBucket() {
    return singleSampleArrayDataBucket;
  }

  public void setSingleSampleArrayDataBucket(String singleSampleArrayDataBucket) {
    this.singleSampleArrayDataBucket = singleSampleArrayDataBucket;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        cdrVersionId,
        isDefault,
        name,
        accessTier,
        releaseNumber,
        archivalStatus,
        bigqueryProject,
        bigqueryDataset,
        creationTime,
        numParticipants,
        cdrDbName,
        elasticIndexBaseName,
        wgsBigqueryDataset,
        hasFitbitData,
        hasCopeSurveyData,
        allSamplesWgsDataBucket);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DbCdrVersion that = (DbCdrVersion) o;
    return cdrVersionId == that.cdrVersionId
        && isDefault == that.isDefault
        && releaseNumber == that.releaseNumber
        && archivalStatus == that.archivalStatus
        && numParticipants == that.numParticipants
        && Objects.equals(name, that.name)
        && Objects.equals(accessTier, that.accessTier)
        && Objects.equals(bigqueryProject, that.bigqueryProject)
        && Objects.equals(bigqueryDataset, that.bigqueryDataset)
        && Objects.equals(creationTime, that.creationTime)
        && Objects.equals(cdrDbName, that.cdrDbName)
        && Objects.equals(elasticIndexBaseName, that.elasticIndexBaseName)
        && Objects.equals(wgsBigqueryDataset, that.wgsBigqueryDataset)
        && Objects.equals(hasFitbitData, that.hasFitbitData)
        && Objects.equals(hasCopeSurveyData, that.hasCopeSurveyData)
        && Objects.equals(allSamplesWgsDataBucket, that.allSamplesWgsDataBucket);
  }
}
