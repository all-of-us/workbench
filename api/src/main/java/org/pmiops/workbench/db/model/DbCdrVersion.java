package org.pmiops.workbench.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotNull;
import java.sql.Timestamp;
import java.util.Objects;
import org.pmiops.workbench.model.ArchivalStatus;

@Entity
@Table(name = "cdr_version")
public class DbCdrVersion {
  private long cdrVersionId;
  private Boolean isDefault;
  private String name;
  private DbAccessTier accessTier;
  private short archivalStatus;
  private String bigqueryProject;
  private String bigqueryDataset;
  private Timestamp creationTime;
  private int numParticipants;
  private String cdrDbName;
  private String wgsBigqueryDataset;
  private String wgsFilterSetName;
  private Boolean hasFitbitData;
  private Boolean hasCopeSurveyData;
  private Boolean hasFitbitSleepData;
  private Boolean hasFitbitDeviceData;
  private Boolean hasSurveyConductData;
  private Boolean tanagraEnabled;
  private String storageBasePath;
  private String wgsVcfMergedStoragePath;
  private String wgsHailStoragePath;
  private String wgsCramManifestPath;
  private String microarrayHailStoragePath;
  // Older CDR versions only, new CDRs should use microarrayVcfManifestpath.
  private String microarrayVcfSingleSampleStoragePath;
  private String microarrayVcfManifestPath;
  private String microarrayIdatManifestPath;
  // 2023Q1 CDR Release
  private String wgsVdsPath;
  private String wgsExomeMultiHailPath;
  private String wgsExomeSplitHailPath;
  private String wgsExomeVcfPath;

  private String wgsAcafThresholdMultiHailPath;
  private String wgsAcafThresholdSplitHailPath;
  private String wgsAcafThresholdVcfPath;

  private String wgsClinvarMultiHailPath;
  private String wgsClinvarSplitHailPath;
  private String wgsClinvarVcfPath;
  private String wgsLongReadsManifestPath;
  private String wgsLongReadsHailGRCh38;
  private String wgsLongReadsHailT2T;
  private String wgsLongReadsJointVcfGRCh38;
  private String wgsLongReadsJointVcfT2T;

  private String vwbTemplateId;

  private int publicReleaseNumber;

  @Id
  @Column(name = "cdr_version_id")
  public long getCdrVersionId() {
    return cdrVersionId;
  }

  public DbCdrVersion setCdrVersionId(long cdrVersionId) {
    this.cdrVersionId = cdrVersionId;
    return this;
  }

  // I changed the type to object Boolean because Hibernate started complaining about assigning
  // nulls to primitive boolean.  TODO why now / what changed?

  @Column(name = "is_default")
  public Boolean getIsDefault() {
    return isDefault;
  }

  public DbCdrVersion setIsDefault(Boolean isDefault) {
    this.isDefault = isDefault;
    return this;
  }

  @Transient
  @NotNull
  public boolean getIsDefaultNotNull() {
    return Boolean.TRUE.equals(getIsDefault());
  }

  @Column(name = "name")
  public String getName() {
    return name;
  }

  public DbCdrVersion setName(String name) {
    this.name = name;
    return this;
  }

  @ManyToOne
  @JoinColumn(name = "access_tier")
  public DbAccessTier getAccessTier() {
    return accessTier;
  }

  public DbCdrVersion setAccessTier(DbAccessTier accessTier) {
    this.accessTier = accessTier;
    return this;
  }

  @Column(name = "archival_status")
  public Short getArchivalStatus() {
    return archivalStatus;
  }

  public DbCdrVersion setArchivalStatus(Short archivalStatus) {
    this.archivalStatus = archivalStatus;
    return this;
  }

  @Transient
  public ArchivalStatus getArchivalStatusEnum() {
    return DbStorageEnums.archivalStatusFromStorage(getArchivalStatus());
  }

  public DbCdrVersion setArchivalStatusEnum(ArchivalStatus archivalStatus) {
    setArchivalStatus(DbStorageEnums.archivalStatusToStorage(archivalStatus));
    return this;
  }

  @Column(name = "bigquery_project")
  public String getBigqueryProject() {
    return bigqueryProject;
  }

  public DbCdrVersion setBigqueryProject(String bigqueryProject) {
    this.bigqueryProject = bigqueryProject;
    return this;
  }

  @Column(name = "bigquery_dataset")
  public String getBigqueryDataset() {
    return bigqueryDataset;
  }

  public DbCdrVersion setBigqueryDataset(String bigqueryDataset) {
    this.bigqueryDataset = bigqueryDataset;
    return this;
  }

  @Column(name = "creation_time")
  public Timestamp getCreationTime() {
    return creationTime;
  }

  public DbCdrVersion setCreationTime(Timestamp creationTime) {
    this.creationTime = creationTime;
    return this;
  }

  @Column(name = "num_participants")
  public int getNumParticipants() {
    return numParticipants;
  }

  public DbCdrVersion setNumParticipants(int numParticipants) {
    this.numParticipants = numParticipants;
    return this;
  }

  @Column(name = "cdr_db_name")
  public String getCdrDbName() {
    return cdrDbName;
  }

  public DbCdrVersion setCdrDbName(String cdrDbName) {
    this.cdrDbName = cdrDbName;
    return this;
  }

  @Column(name = "wgs_bigquery_dataset")
  public String getWgsBigqueryDataset() {
    return wgsBigqueryDataset;
  }

  public DbCdrVersion setWgsBigqueryDataset(String wgsBigqueryDataset) {
    this.wgsBigqueryDataset = wgsBigqueryDataset;
    return this;
  }

  @Column(name = "wgs_filter_set_name")
  public String getWgsFilterSetName() {
    return wgsFilterSetName;
  }

  public DbCdrVersion setWgsFilterSetName(String wgsFilterSetName) {
    this.wgsFilterSetName = wgsFilterSetName;
    return this;
  }

  @Column(name = "has_fitbit_data")
  public Boolean getHasFitbitData() {
    return Boolean.TRUE.equals(hasFitbitData);
  }

  public DbCdrVersion setHasFitbitData(Boolean hasFitbitData) {
    this.hasFitbitData = hasFitbitData;
    return this;
  }

  @Column(name = "has_copesurvey_data")
  public Boolean getHasCopeSurveyData() {
    return Boolean.TRUE.equals(hasCopeSurveyData);
  }

  public DbCdrVersion setHasCopeSurveyData(Boolean hasCopeSurveyData) {
    this.hasCopeSurveyData = hasCopeSurveyData;
    return this;
  }

  @Column(name = "has_fitbit_sleep_data")
  public Boolean getHasFitbitSleepData() {
    return Boolean.TRUE.equals(hasFitbitSleepData);
  }

  public DbCdrVersion setHasFitbitSleepData(Boolean hasFitbitSleepData) {
    this.hasFitbitSleepData = hasFitbitSleepData;
    return this;
  }

  @Column(name = "has_fitbit_device_data")
  public Boolean getHasFitbitDeviceData() {
    return Boolean.TRUE.equals(hasFitbitDeviceData);
  }

  public DbCdrVersion setHasFitbitDeviceData(Boolean hasFitbitDeviceData) {
    this.hasFitbitDeviceData = hasFitbitDeviceData;
    return this;
  }

  @Column(name = "has_survey_conduct_data")
  public Boolean getHasSurveyConductData() {
    return Boolean.TRUE.equals(hasSurveyConductData);
  }

  public DbCdrVersion setHasSurveyConductData(Boolean hasSurveyConductData) {
    this.hasSurveyConductData = hasSurveyConductData;
    return this;
  }

  @Column(name = "tanagra_enabled")
  public Boolean getTanagraEnabled() {
    return Boolean.TRUE.equals(tanagraEnabled);
  }

  public DbCdrVersion setTanagraEnabled(Boolean tanagraEnabled) {
    this.tanagraEnabled = tanagraEnabled;
    return this;
  }

  @Column(name = "storage_base_path")
  public String getStorageBasePath() {
    return storageBasePath;
  }

  public DbCdrVersion setStorageBasePath(String storageBasePath) {
    this.storageBasePath = storageBasePath;
    return this;
  }

  @Column(name = "wgs_vcf_merged_storage_path")
  public String getWgsVcfMergedStoragePath() {
    return wgsVcfMergedStoragePath;
  }

  public DbCdrVersion setWgsVcfMergedStoragePath(String wgsVcfMergedStoragePath) {
    this.wgsVcfMergedStoragePath = wgsVcfMergedStoragePath;
    return this;
  }

  @Column(name = "wgs_hail_storage_path")
  public String getWgsHailStoragePath() {
    return wgsHailStoragePath;
  }

  public DbCdrVersion setWgsHailStoragePath(String wgsHailStoragePath) {
    this.wgsHailStoragePath = wgsHailStoragePath;
    return this;
  }

  @Column(name = "microarray_hail_storage_path")
  public String getMicroarrayHailStoragePath() {
    return microarrayHailStoragePath;
  }

  public DbCdrVersion setMicroarrayHailStoragePath(String microarrayHailStoragePath) {
    this.microarrayHailStoragePath = microarrayHailStoragePath;
    return this;
  }

  @Column(name = "microarray_vcf_single_sample_storage_path")
  public String getMicroarrayVcfSingleSampleStoragePath() {
    return microarrayVcfSingleSampleStoragePath;
  }

  public DbCdrVersion setMicroarrayVcfSingleSampleStoragePath(
      String microarrayVcfSingleSampleStoragePath) {
    this.microarrayVcfSingleSampleStoragePath = microarrayVcfSingleSampleStoragePath;
    return this;
  }

  @Column(name = "wgs_cram_manifest_path")
  public String getWgsCramManifestPath() {
    return wgsCramManifestPath;
  }

  public DbCdrVersion setWgsCramManifestPath(String wgsCramManifestPath) {
    this.wgsCramManifestPath = wgsCramManifestPath;
    return this;
  }

  @Column(name = "microarray_vcf_manifest_path")
  public String getMicroarrayVcfManifestPath() {
    return microarrayVcfManifestPath;
  }

  public DbCdrVersion setMicroarrayVcfManifestPath(String microarrayVcfManifestPath) {
    this.microarrayVcfManifestPath = microarrayVcfManifestPath;
    return this;
  }

  @Column(name = "microarray_idat_manifest_path")
  public String getMicroarrayIdatManifestPath() {
    return microarrayIdatManifestPath;
  }

  public DbCdrVersion setMicroarrayIdatManifestPath(String microarrayIdatManifestPath) {
    this.microarrayIdatManifestPath = microarrayIdatManifestPath;
    return this;
  }

  @Column(name = "wgs_vds_path")
  public String getWgsVdsPath() {
    return wgsVdsPath;
  }

  public DbCdrVersion setWgsVdsPath(String wgsVdsPath) {
    this.wgsVdsPath = wgsVdsPath;
    return this;
  }

  @Column(name = "wgs_exome_multi_hail_path")
  public String getWgsExomeMultiHailPath() {
    return wgsExomeMultiHailPath;
  }

  public DbCdrVersion setWgsExomeMultiHailPath(String wgsExomeMultiHailPath) {
    this.wgsExomeMultiHailPath = wgsExomeMultiHailPath;
    return this;
  }

  @Column(name = "wgs_exome_split_hail_path")
  public String getWgsExomeSplitHailPath() {
    return wgsExomeSplitHailPath;
  }

  public DbCdrVersion setWgsExomeSplitHailPath(String wgsExomeSplitHailPath) {
    this.wgsExomeSplitHailPath = wgsExomeSplitHailPath;
    return this;
  }

  @Column(name = "wgs_exome_vcf_path")
  public String getWgsExomeVcfPath() {
    return wgsExomeVcfPath;
  }

  public DbCdrVersion setWgsExomeVcfPath(String wgsExomeVcfPath) {
    this.wgsExomeVcfPath = wgsExomeVcfPath;
    return this;
  }

  @Column(name = "wgs_acaf_threshold_multi_hail_path")
  public String getWgsAcafThresholdMultiHailPath() {
    return wgsAcafThresholdMultiHailPath;
  }

  public DbCdrVersion setWgsAcafThresholdMultiHailPath(String wgsAcafThresholdMultiHailPath) {
    this.wgsAcafThresholdMultiHailPath = wgsAcafThresholdMultiHailPath;
    return this;
  }

  @Column(name = "wgs_acaf_threshold_split_hail_path")
  public String getWgsAcafThresholdSplitHailPath() {
    return wgsAcafThresholdSplitHailPath;
  }

  public DbCdrVersion setWgsAcafThresholdSplitHailPath(String wgsAcafThresholdSplitHailPath) {
    this.wgsAcafThresholdSplitHailPath = wgsAcafThresholdSplitHailPath;
    return this;
  }

  @Column(name = "wgs_acaf_threshold_vcf_path")
  public String getWgsAcafThresholdVcfPath() {
    return wgsAcafThresholdVcfPath;
  }

  public DbCdrVersion setWgsAcafThresholdVcfPath(String wgsAcafThresholdVcfPath) {
    this.wgsAcafThresholdVcfPath = wgsAcafThresholdVcfPath;
    return this;
  }

  @Column(name = "wgs_clinvar_multi_hail_path")
  public String getWgsClinvarMultiHailPath() {
    return wgsClinvarMultiHailPath;
  }

  public DbCdrVersion setWgsClinvarMultiHailPath(String wgsClinvarMultiHailPath) {
    this.wgsClinvarMultiHailPath = wgsClinvarMultiHailPath;
    return this;
  }

  @Column(name = "wgs_clinvar_split_hail_path")
  public String getWgsClinvarSplitHailPath() {
    return wgsClinvarSplitHailPath;
  }

  public DbCdrVersion setWgsClinvarSplitHailPath(String wgsClinvarSplitHailPath) {
    this.wgsClinvarSplitHailPath = wgsClinvarSplitHailPath;
    return this;
  }

  @Column(name = "wgs_clinvar_vcf_path")
  public String getWgsClinvarVcfPath() {
    return wgsClinvarVcfPath;
  }

  public DbCdrVersion setWgsClinvarVcfPath(String wgsClinvarVcfPath) {
    this.wgsClinvarVcfPath = wgsClinvarVcfPath;
    return this;
  }

  @Column(name = "wgs_long_reads_manifest_path")
  public String getWgsLongReadsManifestPath() {
    return wgsLongReadsManifestPath;
  }

  public DbCdrVersion setWgsLongReadsManifestPath(String longReadsStoragePath) {
    this.wgsLongReadsManifestPath = longReadsStoragePath;
    return this;
  }

  @Column(name = "wgs_longread_hail_grch38")
  public String getWgsLongReadsHailGRCh38() {
    return wgsLongReadsHailGRCh38;
  }

  public DbCdrVersion setWgsLongReadsHailGRCh38(String wgsLongReadsHailGRCh38) {
    this.wgsLongReadsHailGRCh38 = wgsLongReadsHailGRCh38;
    return this;
  }

  @Column(name = "wgs_longread_hail_t2t")
  public String getWgsLongReadsHailT2T() {
    return wgsLongReadsHailT2T;
  }

  public DbCdrVersion setWgsLongReadsHailT2T(String wgsLongReadsHailT2T) {
    this.wgsLongReadsHailT2T = wgsLongReadsHailT2T;
    return this;
  }

  @Column(name = "wgs_longread_joint_vcf_grch38")
  public String getWgsLongReadsJointVcfGRCh38() {
    return wgsLongReadsJointVcfGRCh38;
  }

  public DbCdrVersion setWgsLongReadsJointVcfGRCh38(String wgsLongReadsJointVcfGRCh38) {
    this.wgsLongReadsJointVcfGRCh38 = wgsLongReadsJointVcfGRCh38;
    return this;
  }

  @Column(name = "wgs_longread_joint_vcf_t2t")
  public String getWgsLongReadsJointVcfT2T() {
    return wgsLongReadsJointVcfT2T;
  }

  public DbCdrVersion setWgsLongReadsJointVcfT2T(String wgsLongReadsJointVcfT2T) {
    this.wgsLongReadsJointVcfT2T = wgsLongReadsJointVcfT2T;
    return this;
  }

  @Column(name = "vwb_template_id")
  public String getVwbTemplateId() {
    return vwbTemplateId;
  }

  public DbCdrVersion setVwbTemplateId(String vwbTemplateId) {
    this.vwbTemplateId = vwbTemplateId;
    return this;
  }

  @Column(name = "public_release_number")
  public int getPublicReleaseNumber() {
    return publicReleaseNumber;
  }

  public DbCdrVersion setPublicReleaseNumber(int publicReleaseNumber) {
    this.publicReleaseNumber = publicReleaseNumber;
    return this;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        cdrVersionId,
        isDefault,
        name,
        accessTier,
        archivalStatus,
        bigqueryProject,
        bigqueryDataset,
        creationTime,
        numParticipants,
        cdrDbName,
        wgsBigqueryDataset,
        wgsFilterSetName,
        hasFitbitData,
        hasCopeSurveyData,
        hasFitbitSleepData,
        hasFitbitDeviceData,
        hasSurveyConductData,
        tanagraEnabled,
        storageBasePath,
        wgsVcfMergedStoragePath,
        wgsHailStoragePath,
        wgsCramManifestPath,
        microarrayHailStoragePath,
        microarrayVcfSingleSampleStoragePath,
        microarrayVcfManifestPath,
        microarrayIdatManifestPath,
        wgsVdsPath,
        wgsExomeMultiHailPath,
        wgsExomeSplitHailPath,
        wgsExomeVcfPath,
        wgsAcafThresholdMultiHailPath,
        wgsAcafThresholdSplitHailPath,
        wgsAcafThresholdVcfPath,
        wgsClinvarMultiHailPath,
        wgsClinvarSplitHailPath,
        wgsClinvarVcfPath,
        wgsLongReadsManifestPath,
        wgsLongReadsHailGRCh38,
        wgsLongReadsHailT2T,
        wgsLongReadsJointVcfGRCh38,
        wgsLongReadsJointVcfT2T,
        publicReleaseNumber,
        vwbTemplateId);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof DbCdrVersion that)) {
      return false;
    }
    return cdrVersionId == that.cdrVersionId
        && archivalStatus == that.archivalStatus
        && numParticipants == that.numParticipants
        && publicReleaseNumber == that.publicReleaseNumber
        && Objects.equals(isDefault, that.isDefault)
        && Objects.equals(name, that.name)
        && Objects.equals(accessTier, that.accessTier)
        && Objects.equals(bigqueryProject, that.bigqueryProject)
        && Objects.equals(bigqueryDataset, that.bigqueryDataset)
        && Objects.equals(creationTime, that.creationTime)
        && Objects.equals(cdrDbName, that.cdrDbName)
        && Objects.equals(wgsBigqueryDataset, that.wgsBigqueryDataset)
        && Objects.equals(wgsFilterSetName, that.wgsFilterSetName)
        && Objects.equals(hasFitbitData, that.hasFitbitData)
        && Objects.equals(hasFitbitSleepData, that.hasFitbitSleepData)
        && Objects.equals(hasFitbitDeviceData, that.hasFitbitDeviceData)
        && Objects.equals(hasSurveyConductData, that.hasSurveyConductData)
        && Objects.equals(hasCopeSurveyData, that.hasCopeSurveyData)
        && Objects.equals(tanagraEnabled, that.tanagraEnabled)
        && Objects.equals(storageBasePath, that.storageBasePath)
        && Objects.equals(wgsVcfMergedStoragePath, that.wgsVcfMergedStoragePath)
        && Objects.equals(wgsHailStoragePath, that.wgsHailStoragePath)
        && Objects.equals(wgsCramManifestPath, that.wgsCramManifestPath)
        && Objects.equals(microarrayHailStoragePath, that.microarrayHailStoragePath)
        && Objects.equals(
            microarrayVcfSingleSampleStoragePath, that.microarrayVcfSingleSampleStoragePath)
        && Objects.equals(microarrayVcfManifestPath, that.microarrayVcfManifestPath)
        && Objects.equals(microarrayIdatManifestPath, that.microarrayIdatManifestPath)
        && Objects.equals(wgsVdsPath, that.wgsVdsPath)
        && Objects.equals(wgsExomeMultiHailPath, that.wgsExomeMultiHailPath)
        && Objects.equals(wgsExomeSplitHailPath, that.wgsExomeSplitHailPath)
        && Objects.equals(wgsExomeVcfPath, that.wgsExomeVcfPath)
        && Objects.equals(wgsAcafThresholdMultiHailPath, that.wgsAcafThresholdMultiHailPath)
        && Objects.equals(wgsAcafThresholdSplitHailPath, that.wgsAcafThresholdSplitHailPath)
        && Objects.equals(wgsAcafThresholdVcfPath, that.wgsAcafThresholdVcfPath)
        && Objects.equals(wgsClinvarMultiHailPath, that.wgsClinvarMultiHailPath)
        && Objects.equals(wgsClinvarSplitHailPath, that.wgsClinvarSplitHailPath)
        && Objects.equals(wgsClinvarVcfPath, that.wgsClinvarVcfPath)
        && Objects.equals(wgsLongReadsManifestPath, that.wgsLongReadsManifestPath)
        && Objects.equals(wgsLongReadsHailGRCh38, that.wgsLongReadsHailGRCh38)
        && Objects.equals(wgsLongReadsHailT2T, that.wgsLongReadsHailT2T)
        && Objects.equals(wgsLongReadsJointVcfGRCh38, that.wgsLongReadsJointVcfGRCh38)
        && Objects.equals(wgsLongReadsJointVcfT2T, that.wgsLongReadsJointVcfT2T)
        && Objects.equals(vwbTemplateId, that.vwbTemplateId);
  }
}
