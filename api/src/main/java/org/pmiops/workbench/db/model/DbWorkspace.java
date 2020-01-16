package org.pmiops.workbench.db.model;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Version;
import org.pmiops.workbench.model.BillingStatus;
import org.pmiops.workbench.model.DataAccessLevel;
import org.pmiops.workbench.model.SpecificPopulationEnum;
import org.pmiops.workbench.model.WorkspaceActiveStatus;

@Entity
@Table(name = "workspace")
public class DbWorkspace {
  private String firecloudUuid;

  public static class FirecloudWorkspaceId {
    private final String workspaceNamespace;
    private final String workspaceName;

    public FirecloudWorkspaceId(String workspaceNamespace, String workspaceName) {
      this.workspaceNamespace = workspaceNamespace;
      this.workspaceName = workspaceName;
    }

    public String getWorkspaceNamespace() {
      return workspaceNamespace;
    }

    public String getWorkspaceName() {
      return workspaceName;
    }

    @Override
    public int hashCode() {
      return Objects.hash(workspaceNamespace, workspaceName);
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof FirecloudWorkspaceId)) {
        return false;
      }
      FirecloudWorkspaceId that = (FirecloudWorkspaceId) obj;
      return this.workspaceNamespace.equals(that.workspaceNamespace)
          && this.workspaceName.equals(that.workspaceName);
    }
  }

  // see https://precisionmedicineinitiative.atlassian.net/browse/RW-2705
  public enum BillingMigrationStatus {
    OLD, // pre-1PPW; this billing project may be associated with multiple workspaces
    NEW, // a One Project Per DbWorkspace (1PPW) billing project
    MIGRATED // a pre-1PPW billing project which has been cloned and is now ready to be deleted
  }

  private long workspaceId;
  private int version;
  private String name;
  private String workspaceNamespace;
  private String firecloudName;
  private Short dataAccessLevel;
  private DbCdrVersion cdrVersion;
  private DbUser creator;
  private Timestamp creationTime;
  private Timestamp lastModifiedTime;
  private Timestamp lastAccessedTime;
  private Set<DbCohort> cohorts = new HashSet<>();
  private Set<DbConceptSet> conceptSets = new HashSet<>();
  private Set<DbDataset> dataSets = new HashSet<>();
  private Short activeStatus;
  private Short billingMigrationStatus =
      DbStorageEnums.billingMigrationStatusToStorage(BillingMigrationStatus.OLD);
  private boolean published;

  private boolean diseaseFocusedResearch;
  private String diseaseOfFocus;
  private boolean methodsDevelopment;
  private boolean controlSet;
  private boolean ancestry;
  private boolean commercialPurpose;
  private boolean population;
  private Set<Short> populationDetailsSet = new HashSet<>();
  private boolean socialBehavioral;
  private boolean populationHealth;
  private boolean educational;
  private boolean drugDevelopment;
  private boolean otherPurpose;
  private String otherPurposeDetails;
  private String otherPopulationDetails;
  private String additionalNotes;
  private String reasonForAllOfUs;
  private String intendedStudy;
  private String anticipatedFindings;

  private Boolean reviewRequested;
  private Boolean approved;
  private Timestamp timeRequested;
  private Short billingStatus = DbStorageEnums.billingStatusToStorage(BillingStatus.ACTIVE);
  private String billingAccountName;

  public DbWorkspace() {
    setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.ACTIVE);
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "workspace_id")
  public long getWorkspaceId() {
    return workspaceId;
  }

  public void setWorkspaceId(long workspaceId) {
    this.workspaceId = workspaceId;
  }

  @Version
  @Column(name = "version")
  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
  }

  @Column(name = "name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Column(name = "workspace_namespace")
  public String getWorkspaceNamespace() {
    return workspaceNamespace;
  }

  public void setWorkspaceNamespace(String workspaceNamespace) {
    this.workspaceNamespace = workspaceNamespace;
  }

  @Column(name = "firecloud_name")
  public String getFirecloudName() {
    return firecloudName;
  }

  public void setFirecloudName(String firecloudName) {
    this.firecloudName = firecloudName;
  }

  @Column(name = "data_access_level")
  public Short getDataAccessLevel() {
    return dataAccessLevel;
  }

  public void setDataAccessLevel(Short dataAccessLevel) {
    this.dataAccessLevel = dataAccessLevel;
  }

  @Transient
  public DataAccessLevel getDataAccessLevelEnum() {
    return CommonStorageEnums.dataAccessLevelFromStorage(getDataAccessLevel());
  }

  public void setDataAccessLevelEnum(DataAccessLevel dataAccessLevel) {
    setDataAccessLevel(CommonStorageEnums.dataAccessLevelToStorage(dataAccessLevel));
  }

  @ManyToOne
  @JoinColumn(name = "cdr_version_id")
  public DbCdrVersion getCdrVersion() {
    return cdrVersion;
  }

  public void setCdrVersion(DbCdrVersion cdrVersion) {
    this.cdrVersion = cdrVersion;
  }

  @ManyToOne
  @JoinColumn(name = "creator_id")
  public DbUser getCreator() {
    return creator;
  }

  public void setCreator(DbUser creator) {
    this.creator = creator;
  }

  @Column(name = "creation_time")
  public Timestamp getCreationTime() {
    return creationTime;
  }

  public void setCreationTime(Timestamp creationTime) {
    this.creationTime = creationTime;
  }

  @Column(name = "last_modified_time")
  public Timestamp getLastModifiedTime() {
    return lastModifiedTime;
  }

  public void setLastModifiedTime(Timestamp lastModifiedTime) {
    this.lastModifiedTime = lastModifiedTime;
  }

  @Column(name = "last_accessed_time")
  public Timestamp getLastAccessedTime() {
    return lastAccessedTime;
  }

  public void setLastAccessedTime(Timestamp lastAccessedTime) {
    this.lastAccessedTime = lastAccessedTime;
  }

  @Column(name = "published")
  public boolean getPublished() {
    return published;
  }

  public void setPublished(boolean published) {
    this.published = published;
  }

  @Column(name = "rp_disease_focused_research")
  public boolean getDiseaseFocusedResearch() {
    return this.diseaseFocusedResearch;
  }

  public void setDiseaseFocusedResearch(boolean diseaseFocusedResearch) {
    this.diseaseFocusedResearch = diseaseFocusedResearch;
  }

  @Column(name = "rp_disease_of_focus")
  public String getDiseaseOfFocus() {
    return this.diseaseOfFocus;
  }

  public void setDiseaseOfFocus(String diseaseOfFocus) {
    this.diseaseOfFocus = diseaseOfFocus;
  }

  @Column(name = "rp_methods_development")
  public boolean getMethodsDevelopment() {
    return this.methodsDevelopment;
  }

  public void setMethodsDevelopment(boolean methodsDevelopment) {
    this.methodsDevelopment = methodsDevelopment;
  }

  @Column(name = "rp_control_set")
  public boolean getControlSet() {
    return this.controlSet;
  }

  public void setControlSet(boolean controlSet) {
    this.controlSet = controlSet;
  }

  @Column(name = "rp_ancestry")
  public boolean getAncestry() {
    return this.ancestry;
  }

  public void setAncestry(boolean ancestry) {
    this.ancestry = ancestry;
  }

  @Column(name = "rp_commercial_purpose")
  public boolean getCommercialPurpose() {
    return this.commercialPurpose;
  }

  public void setCommercialPurpose(boolean commercialPurpose) {
    this.commercialPurpose = commercialPurpose;
  }

  @Column(name = "rp_social_behavioral")
  public boolean getSocialBehavioral() {
    return this.socialBehavioral;
  }

  public void setSocialBehavioral(boolean socialBehavioral) {
    this.socialBehavioral = socialBehavioral;
  }

  @Column(name = "rp_population_health")
  public boolean getPopulationHealth() {
    return this.populationHealth;
  }

  public void setPopulationHealth(boolean populationHealth) {
    this.populationHealth = populationHealth;
  }

  @Column(name = "rp_educational")
  public boolean getEducational() {
    return this.educational;
  }

  public void setEducational(boolean educational) {
    this.educational = educational;
  }

  @Column(name = "rp_drug_development")
  public boolean getDrugDevelopment() {
    return this.drugDevelopment;
  }

  public void setDrugDevelopment(boolean drugDevelopment) {
    this.drugDevelopment = drugDevelopment;
  }

  @Column(name = "rp_other_purpose")
  public boolean getOtherPurpose() {
    return this.otherPurpose;
  }

  public void setOtherPurpose(boolean otherPurpose) {
    this.otherPurpose = otherPurpose;
  }

  @Column(name = "rp_other_purpose_details")
  public String getOtherPurposeDetails() {
    return this.otherPurposeDetails;
  }

  public void setOtherPurposeDetails(String otherPurposeDetails) {
    this.otherPurposeDetails = otherPurposeDetails;
  }

  @Column(name = "rp_population")
  public boolean getPopulation() {
    return this.population;
  }

  public void setPopulation(boolean population) {
    this.population = population;
  }

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "specific_populations", joinColumns = @JoinColumn(name = "workspace_id"))
  @Column(name = "specific_population")
  public Set<Short> getPopulationDetails() {
    return populationDetailsSet;
  }

  public void setPopulationDetails(Set<Short> newPopulationDetailsSet) {
    this.populationDetailsSet = newPopulationDetailsSet;
  }

  @Transient
  public Set<SpecificPopulationEnum> getSpecificPopulationsEnum() {
    Set<Short> from = getPopulationDetails();
    if (from == null) {
      return null;
    }
    return from.stream()
        .map(DbStorageEnums::specificPopulationFromStorage)
        .collect(Collectors.toSet());
  }

  public void setSpecificPopulationsEnum(Set<SpecificPopulationEnum> newPopulationDetails) {
    setPopulationDetails(
        newPopulationDetails.stream()
            .map(DbStorageEnums::specificPopulationToStorage)
            .collect(Collectors.toSet()));
  }

  @Column(name = "rp_other_population_details")
  public String getOtherPopulationDetails() {
    return this.otherPopulationDetails;
  }

  public void setOtherPopulationDetails(String otherPopulationDetails) {
    this.otherPopulationDetails = otherPopulationDetails;
  }

  @Column(name = "rp_additional_notes")
  public String getAdditionalNotes() {
    return this.additionalNotes;
  }

  public void setAdditionalNotes(String additionalNotes) {
    this.additionalNotes = additionalNotes;
  }

  @Column(name = "rp_reason_for_all_of_us")
  public String getReasonForAllOfUs() {
    return this.reasonForAllOfUs;
  }

  public void setReasonForAllOfUs(String reasonForAllOfUs) {
    this.reasonForAllOfUs = reasonForAllOfUs;
  }

  @Column(name = "rp_intended_study")
  public String getIntendedStudy() {
    return this.intendedStudy;
  }

  public void setIntendedStudy(String intendedStudy) {
    this.intendedStudy = intendedStudy;
  }

  @Column(name = "rp_anticipated_findings")
  public String getAnticipatedFindings() {
    return this.anticipatedFindings;
  }

  public void setAnticipatedFindings(String anticipatedFindings) {
    this.anticipatedFindings = anticipatedFindings;
  }

  @Column(name = "rp_review_requested")
  public Boolean getReviewRequested() {
    return this.reviewRequested;
  }

  public void setReviewRequested(Boolean reviewRequested) {
    if (reviewRequested != null && reviewRequested && this.timeRequested == null) {
      this.timeRequested = new Timestamp(System.currentTimeMillis());
    }
    this.reviewRequested = reviewRequested;
  }

  /**
   * This "bit" is used as a tri-state. TODO(jaycarlton) replace this column with an enum like {
   * NOT_SET, APPROVED, REJECTED } and make all Boolean columns non-nullable with explicit defaults.
   */
  @Nullable
  @Column(name = "rp_approved")
  public Boolean getApproved() {
    return this.approved;
  }

  public void setApproved(Boolean approved) {
    this.approved = approved;
  }

  @Column(name = "rp_time_requested")
  public Timestamp getTimeRequested() {
    return this.timeRequested;
  }

  public void setTimeRequested(Timestamp timeRequested) {
    this.timeRequested = timeRequested;
  }

  @OneToMany(mappedBy = "workspaceId", orphanRemoval = true, cascade = CascadeType.ALL)
  public Set<DbCohort> getCohorts() {
    return cohorts;
  }

  public void setCohorts(Set<DbCohort> cohorts) {
    this.cohorts = cohorts;
  }

  public void addCohort(DbCohort cohort) {
    this.cohorts.add(cohort);
  }

  @OneToMany(mappedBy = "workspaceId", orphanRemoval = true, cascade = CascadeType.ALL)
  public Set<DbConceptSet> getConceptSets() {
    return conceptSets;
  }

  public void setConceptSets(Set<DbConceptSet> conceptSets) {
    this.conceptSets = conceptSets;
  }

  @OneToMany(mappedBy = "workspaceId", orphanRemoval = true, cascade = CascadeType.ALL)
  public Set<DbDataset> getDataSets() {
    return dataSets;
  }

  public void setDataSets(Set<DbDataset> dataSets) {
    this.dataSets = dataSets;
  }

  public void addDataSet(DbDataset dataSet) {
    this.dataSets.add(dataSet);
  }

  @Transient
  public FirecloudWorkspaceId getFirecloudWorkspaceId() {
    return new FirecloudWorkspaceId(workspaceNamespace, firecloudName);
  }

  @Column(name = "firecloud_uuid")
  public String getFirecloudUuid() {
    return this.firecloudUuid;
  }

  public void setFirecloudUuid(String firecloudUuid) {
    this.firecloudUuid = firecloudUuid;
  }

  @Column(name = "active_status")
  private Short getActiveStatus() {
    return activeStatus;
  }

  private void setActiveStatus(Short activeStatus) {
    this.activeStatus = activeStatus;
  }

  @Transient
  public WorkspaceActiveStatus getWorkspaceActiveStatusEnum() {
    return DbStorageEnums.workspaceActiveStatusFromStorage(getActiveStatus());
  }

  public void setWorkspaceActiveStatusEnum(WorkspaceActiveStatus activeStatus) {
    setActiveStatus(DbStorageEnums.workspaceActiveStatusToStorage(activeStatus));
  }

  @Transient
  public boolean isActive() {
    return WorkspaceActiveStatus.ACTIVE.equals(getWorkspaceActiveStatusEnum());
  }

  @Transient
  public BillingMigrationStatus getBillingMigrationStatusEnum() {
    return DbStorageEnums.billingMigrationStatusFromStorage(billingMigrationStatus);
  }

  public void setBillingMigrationStatusEnum(BillingMigrationStatus status) {
    this.billingMigrationStatus = DbStorageEnums.billingMigrationStatusToStorage(status);
  }

  @Column(name = "billing_migration_status")
  private short getBillingMigrationStatus() {
    return this.billingMigrationStatus;
  }

  private void setBillingMigrationStatus(short s) {
    this.billingMigrationStatus = s;
  }

  @Column(name = "billing_status")
  public BillingStatus getBillingStatus() {
    return DbStorageEnums.billingStatusFromStorage(billingStatus);
  }

  public void setBillingStatus(BillingStatus billingStatus) {
    this.billingStatus = DbStorageEnums.billingStatusToStorage(billingStatus);
  }

  @Column(name = "billing_account_name")
  public String getBillingAccountName() {
    return billingAccountName;
  }

  public void setBillingAccountName(String billingAccountName) {
    this.billingAccountName = billingAccountName;
  }
}
