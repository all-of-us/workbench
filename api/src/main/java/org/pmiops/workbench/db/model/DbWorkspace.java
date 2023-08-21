package org.pmiops.workbench.db.model;

import java.sql.Timestamp;
import java.util.HashSet;
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
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.pmiops.workbench.model.BillingStatus;
import org.pmiops.workbench.model.DisseminateResearchEnum;
import org.pmiops.workbench.model.ResearchOutcomeEnum;
import org.pmiops.workbench.model.SpecificPopulationEnum;
import org.pmiops.workbench.model.WorkspaceActiveStatus;

@Entity
@Table(name = "workspace")
public class DbWorkspace {
  private String firecloudUuid;

  private long workspaceId;
  private int version;
  private String name;
  private String workspaceNamespace;
  private String firecloudName;
  private DbCdrVersion cdrVersion;
  private DbUser creator;
  private String lastModifiedBy;
  private Timestamp creationTime;
  private Timestamp lastModifiedTime;
  private Set<DbCohort> cohorts = new HashSet<>();
  private Set<DbConceptSet> conceptSets = new HashSet<>();
  private Set<DbDataset> dataSets = new HashSet<>();
  private Short activeStatus;
  private boolean published;

  private boolean diseaseFocusedResearch;
  private String diseaseOfFocus;
  private boolean methodsDevelopment;
  private boolean controlSet;
  private boolean ancestry;
  private boolean commercialPurpose;
  private Set<Short> populationDetailsSet = new HashSet<>();
  private boolean socialBehavioral;
  private boolean populationHealth;
  private boolean ethics;
  private boolean educational;
  private boolean drugDevelopment;
  private boolean otherPurpose;
  private String otherPurposeDetails;
  private String otherPopulationDetails;
  private String additionalNotes;
  private String reasonForAllOfUs;
  private String intendedStudy;
  private String anticipatedFindings;
  private String scientificApproach;

  private Set<Short> disseminateResearchSet = new HashSet<Short>();
  private String disseminateResearchOther;
  private Set<Short> researchOutcomeSet = new HashSet<>();

  private Boolean reviewRequested;
  private Boolean approved;
  private Timestamp timeRequested;
  private Short billingStatus = DbStorageEnums.billingStatusToStorage(BillingStatus.ACTIVE);
  private String billingAccountName;
  private String googleProject;
  private boolean adminLocked;
  private String adminLockedReason;

  public DbWorkspace() {
    setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.ACTIVE);
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "workspace_id")
  public long getWorkspaceId() {
    return workspaceId;
  }

  public DbWorkspace setWorkspaceId(long workspaceId) {
    this.workspaceId = workspaceId;
    return this;
  }

  @Version
  @Column(name = "version")
  public int getVersion() {
    return version;
  }

  public DbWorkspace setVersion(int version) {
    this.version = version;
    return this;
  }

  @Column(name = "name")
  public String getName() {
    return name;
  }

  public DbWorkspace setName(String name) {
    this.name = name;
    return this;
  }

  @Column(name = "workspace_namespace")
  public String getWorkspaceNamespace() {
    return workspaceNamespace;
  }

  public DbWorkspace setWorkspaceNamespace(String workspaceNamespace) {
    this.workspaceNamespace = workspaceNamespace;
    return this;
  }

  @Column(name = "firecloud_name")
  public String getFirecloudName() {
    return firecloudName;
  }

  public DbWorkspace setFirecloudName(String firecloudName) {
    this.firecloudName = firecloudName;
    return this;
  }

  @Transient
  public boolean isTerraV2Workspace() {
    // Before Terra PPW, workspace namespace and project were identical.
    return !workspaceNamespace.equals(googleProject);
  }

  @ManyToOne
  @JoinColumn(name = "cdr_version_id")
  public DbCdrVersion getCdrVersion() {
    return cdrVersion;
  }

  public DbWorkspace setCdrVersion(DbCdrVersion cdrVersion) {
    this.cdrVersion = cdrVersion;
    return this;
  }

  @ManyToOne
  @JoinColumn(name = "creator_id")
  public DbUser getCreator() {
    return creator;
  }

  public DbWorkspace setCreator(DbUser creator) {
    this.creator = creator;
    return this;
  }

  @Column(name = "creation_time")
  public Timestamp getCreationTime() {
    return creationTime;
  }

  public DbWorkspace setCreationTime(Timestamp creationTime) {
    this.creationTime = creationTime;
    return this;
  }

  @Column(name = "last_modified_by")
  public String getLastModifiedBy() {
    return lastModifiedBy;
  }

  public DbWorkspace setLastModifiedBy(String lastModifiedBy) {
    this.lastModifiedBy = lastModifiedBy;
    return this;
  }

  @Column(name = "last_modified_time")
  public Timestamp getLastModifiedTime() {
    return lastModifiedTime;
  }

  public DbWorkspace setLastModifiedTime(Timestamp lastModifiedTime) {
    this.lastModifiedTime = lastModifiedTime;
    return this;
  }

  @Column(name = "published")
  public boolean getPublished() {
    return published;
  }

  public DbWorkspace setPublished(boolean published) {
    this.published = published;
    return this;
  }

  @Column(name = "rp_disease_focused_research")
  public boolean getDiseaseFocusedResearch() {
    return this.diseaseFocusedResearch;
  }

  public DbWorkspace setDiseaseFocusedResearch(boolean diseaseFocusedResearch) {
    this.diseaseFocusedResearch = diseaseFocusedResearch;
    return this;
  }

  @Column(name = "rp_disease_of_focus")
  public String getDiseaseOfFocus() {
    return this.diseaseOfFocus;
  }

  public DbWorkspace setDiseaseOfFocus(String diseaseOfFocus) {
    this.diseaseOfFocus = diseaseOfFocus;
    return this;
  }

  @Column(name = "rp_methods_development")
  public boolean getMethodsDevelopment() {
    return this.methodsDevelopment;
  }

  public DbWorkspace setMethodsDevelopment(boolean methodsDevelopment) {
    this.methodsDevelopment = methodsDevelopment;
    return this;
  }

  @Column(name = "rp_control_set")
  public boolean getControlSet() {
    return this.controlSet;
  }

  public DbWorkspace setControlSet(boolean controlSet) {
    this.controlSet = controlSet;
    return this;
  }

  @Column(name = "rp_ancestry")
  public boolean getAncestry() {
    return this.ancestry;
  }

  public DbWorkspace setAncestry(boolean ancestry) {
    this.ancestry = ancestry;
    return this;
  }

  @Column(name = "rp_commercial_purpose")
  public boolean getCommercialPurpose() {
    return this.commercialPurpose;
  }

  public DbWorkspace setCommercialPurpose(boolean commercialPurpose) {
    this.commercialPurpose = commercialPurpose;
    return this;
  }

  @Column(name = "rp_social_behavioral")
  public boolean getSocialBehavioral() {
    return this.socialBehavioral;
  }

  public DbWorkspace setSocialBehavioral(boolean socialBehavioral) {
    this.socialBehavioral = socialBehavioral;
    return this;
  }

  @Column(name = "rp_population_health")
  public boolean getPopulationHealth() {
    return this.populationHealth;
  }

  public DbWorkspace setPopulationHealth(boolean populationHealth) {
    this.populationHealth = populationHealth;
    return this;
  }

  @Column(name = "rp_ethics")
  public boolean getEthics() {
    return ethics;
  }

  public DbWorkspace setEthics(boolean ethics) {
    this.ethics = ethics;
    return this;
  }

  @Column(name = "rp_educational")
  public boolean getEducational() {
    return this.educational;
  }

  public DbWorkspace setEducational(boolean educational) {
    this.educational = educational;
    return this;
  }

  @Column(name = "rp_drug_development")
  public boolean getDrugDevelopment() {
    return this.drugDevelopment;
  }

  public DbWorkspace setDrugDevelopment(boolean drugDevelopment) {
    this.drugDevelopment = drugDevelopment;
    return this;
  }

  @Column(name = "rp_other_purpose")
  public boolean getOtherPurpose() {
    return this.otherPurpose;
  }

  public DbWorkspace setOtherPurpose(boolean otherPurpose) {
    this.otherPurpose = otherPurpose;
    return this;
  }

  @Column(name = "rp_other_purpose_details")
  public String getOtherPurposeDetails() {
    return this.otherPurposeDetails;
  }

  public DbWorkspace setOtherPurposeDetails(String otherPurposeDetails) {
    this.otherPurposeDetails = otherPurposeDetails;
    return this;
  }

  @Column(name = "google_project")
  public String getGoogleProject() {
    return googleProject;
  }

  public DbWorkspace setGoogleProject(String googleProject) {
    this.googleProject = googleProject;
    return this;
  }

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "specific_populations", joinColumns = @JoinColumn(name = "workspace_id"))
  @Column(name = "specific_population")
  private Set<Short> getPopulationDetails() {
    return populationDetailsSet;
  }

  private DbWorkspace setPopulationDetails(Set<Short> newPopulationDetailsSet) {
    this.populationDetailsSet = newPopulationDetailsSet;
    return this;
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

  public DbWorkspace setSpecificPopulationsEnum(Set<SpecificPopulationEnum> newPopulationDetails) {
    return setPopulationDetails(
        newPopulationDetails.stream()
            .map(DbStorageEnums::specificPopulationToStorage)
            .collect(Collectors.toSet()));
  }

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(
      name = "workspace_disseminate_research",
      joinColumns = @JoinColumn(name = "workspace_id"))
  @Column(name = "disseminate")
  public Set<Short> getDisseminateResearchSet() {
    return disseminateResearchSet;
  }

  public DbWorkspace setDisseminateResearchSet(Set<Short> disseminateResearchSet) {
    this.disseminateResearchSet = disseminateResearchSet;
    return this;
  }

  @Transient
  public Set<DisseminateResearchEnum> getDisseminateResearchEnumSet() {
    Set<Short> from = getDisseminateResearchSet();
    if (from == null) {
      return new HashSet<DisseminateResearchEnum>();
    }
    return from.stream()
        .map(DbStorageEnums::disseminateResearchEnumFromStorage)
        .collect(Collectors.toSet());
  }

  public DbWorkspace setDisseminateResearchEnumSet(
      Set<DisseminateResearchEnum> disseminateResearchEnumEnum) {
    return setDisseminateResearchSet(
        disseminateResearchEnumEnum.stream()
            .map(DbStorageEnums::disseminateResearchToStorage)
            .collect(Collectors.toSet()));
  }

  @Column(name = "disseminate_research_other")
  public String getDisseminateResearchOther() {
    return disseminateResearchOther;
  }

  public DbWorkspace setDisseminateResearchOther(String disseminateResearchOther) {
    this.disseminateResearchOther = disseminateResearchOther;
    return this;
  }

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(
      name = "workspace_research_outcomes",
      joinColumns = @JoinColumn(name = "workspace_id"))
  @Column(name = "research_outcome")
  public Set<Short> getResearchOutcomeSet() {
    return researchOutcomeSet;
  }

  public DbWorkspace setResearchOutcomeSet(Set<Short> researchOutcomeSet) {
    this.researchOutcomeSet = researchOutcomeSet;
    return this;
  }

  @Transient
  public Set<ResearchOutcomeEnum> getResearchOutcomeEnumSet() {
    Set<Short> from = getResearchOutcomeSet();
    if (from == null) {
      return new HashSet<ResearchOutcomeEnum>();
    }
    return from.stream()
        .map(DbStorageEnums::researchOutcomeEnumFromStorage)
        .collect(Collectors.toSet());
  }

  public DbWorkspace setResearchOutcomeEnumSet(Set<ResearchOutcomeEnum> researchOutcomeEnum) {
    return setResearchOutcomeSet(
        researchOutcomeEnum.stream()
            .map(DbStorageEnums::researchOutcomeToStorage)
            .collect(Collectors.toSet()));
  }

  @Column(name = "rp_other_population_details")
  public String getOtherPopulationDetails() {
    return this.otherPopulationDetails;
  }

  public DbWorkspace setOtherPopulationDetails(String otherPopulationDetails) {
    this.otherPopulationDetails = otherPopulationDetails;
    return this;
  }

  @Column(name = "rp_additional_notes")
  public String getAdditionalNotes() {
    return this.additionalNotes;
  }

  public DbWorkspace setAdditionalNotes(String additionalNotes) {
    this.additionalNotes = additionalNotes;
    return this;
  }

  @Column(name = "rp_reason_for_all_of_us")
  public String getReasonForAllOfUs() {
    return this.reasonForAllOfUs;
  }

  public DbWorkspace setReasonForAllOfUs(String reasonForAllOfUs) {
    this.reasonForAllOfUs = reasonForAllOfUs;
    return this;
  }

  @Column(name = "rp_intended_study")
  public String getIntendedStudy() {
    return this.intendedStudy;
  }

  public DbWorkspace setIntendedStudy(String intendedStudy) {
    this.intendedStudy = intendedStudy;
    return this;
  }

  @Column(name = "rp_anticipated_findings")
  public String getAnticipatedFindings() {
    return this.anticipatedFindings;
  }

  public DbWorkspace setAnticipatedFindings(String anticipatedFindings) {
    this.anticipatedFindings = anticipatedFindings;
    return this;
  }

  @Column(name = "rp_scientific_approach")
  public String getScientificApproach() {
    return scientificApproach;
  }

  public DbWorkspace setScientificApproach(String scientificApproach) {
    this.scientificApproach = scientificApproach;
    return this;
  }

  @Column(name = "rp_review_requested")
  public Boolean getReviewRequested() {
    return this.reviewRequested;
  }

  public DbWorkspace setReviewRequested(Boolean reviewRequested) {
    if (reviewRequested != null && reviewRequested && this.timeRequested == null) {
      this.timeRequested = new Timestamp(System.currentTimeMillis());
    }
    this.reviewRequested = reviewRequested;
    return this;
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

  public DbWorkspace setApproved(Boolean approved) {
    this.approved = approved;
    return this;
  }

  @Column(name = "rp_time_requested")
  public Timestamp getTimeRequested() {
    return this.timeRequested;
  }

  public DbWorkspace setTimeRequested(Timestamp timeRequested) {
    this.timeRequested = timeRequested;
    return this;
  }

  @OneToMany(mappedBy = "workspaceId", orphanRemoval = true, cascade = CascadeType.ALL)
  public Set<DbCohort> getCohorts() {
    return cohorts;
  }

  public DbWorkspace setCohorts(Set<DbCohort> cohorts) {
    this.cohorts = cohorts;
    return this;
  }

  public void addCohort(DbCohort cohort) {
    this.cohorts.add(cohort);
  }

  @OneToMany(mappedBy = "workspaceId", orphanRemoval = true, cascade = CascadeType.ALL)
  public Set<DbConceptSet> getConceptSets() {
    return conceptSets;
  }

  public DbWorkspace setConceptSets(Set<DbConceptSet> conceptSets) {
    this.conceptSets = conceptSets;
    return this;
  }

  @OneToMany(mappedBy = "workspaceId", orphanRemoval = true, cascade = CascadeType.ALL)
  public Set<DbDataset> getDataSets() {
    return dataSets;
  }

  public DbWorkspace setDataSets(Set<DbDataset> dataSets) {
    this.dataSets = dataSets;
    return this;
  }

  public void addDataSet(DbDataset dataSet) {
    this.dataSets.add(dataSet);
  }

  @Column(name = "firecloud_uuid")
  public String getFirecloudUuid() {
    return this.firecloudUuid;
  }

  public DbWorkspace setFirecloudUuid(String firecloudUuid) {
    this.firecloudUuid = firecloudUuid;
    return this;
  }

  @Column(name = "active_status")
  private Short getActiveStatus() {
    return activeStatus;
  }

  private DbWorkspace setActiveStatus(Short activeStatus) {
    this.activeStatus = activeStatus;
    return this;
  }

  @Transient
  public WorkspaceActiveStatus getWorkspaceActiveStatusEnum() {
    return DbStorageEnums.workspaceActiveStatusFromStorage(getActiveStatus());
  }

  public DbWorkspace setWorkspaceActiveStatusEnum(WorkspaceActiveStatus activeStatus) {
    setActiveStatus(DbStorageEnums.workspaceActiveStatusToStorage(activeStatus));
    return this;
  }

  @Transient
  public boolean isActive() {
    return WorkspaceActiveStatus.ACTIVE.equals(getWorkspaceActiveStatusEnum());
  }

  @Column(name = "billing_status")
  public BillingStatus getBillingStatus() {
    return DbStorageEnums.billingStatusFromStorage(billingStatus);
  }

  public DbWorkspace setBillingStatus(BillingStatus billingStatus) {
    this.billingStatus = DbStorageEnums.billingStatusToStorage(billingStatus);
    return this;
  }

  @Column(name = "billing_account_name")
  public String getBillingAccountName() {
    return billingAccountName;
  }

  public DbWorkspace setBillingAccountName(String billingAccountName) {
    this.billingAccountName = billingAccountName;
    return this;
  }

  @Column(name = "admin_locked")
  public boolean isAdminLocked() {
    return adminLocked;
  }

  public DbWorkspace setAdminLocked(boolean adminLocked) {
    this.adminLocked = adminLocked;
    return this;
  }

  @Column(name = "admin_locked_reason")
  public String getAdminLockedReason() {
    return adminLockedReason;
  }

  public DbWorkspace setAdminLockedReason(String adminLockedReason) {
    this.adminLockedReason = adminLockedReason;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;

    if (!(o instanceof DbWorkspace)) return false;

    DbWorkspace workspace = (DbWorkspace) o;

    return new EqualsBuilder()
        .append(billingMigrationStatus, workspace.billingMigrationStatus)
        .append(workspaceId, workspace.workspaceId)
        .append(name, workspace.name)
        .append(workspaceNamespace, workspace.workspaceNamespace)
        .append(cdrVersion, workspace.cdrVersion)
        .append(creator, workspace.creator)
        .append(creationTime, workspace.creationTime)
        .append(activeStatus, workspace.activeStatus)
        .append(billingAccountName, workspace.billingAccountName)
        .append(googleProject, workspace.googleProject)
        .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
        .append(billingMigrationStatus)
        .append(workspaceId)
        .append(name)
        .append(workspaceNamespace)
        .append(cdrVersion)
        .append(creator)
        .append(creationTime)
        .append(activeStatus)
        .append(billingAccountName)
        .append(googleProject)
        .toHashCode();
  }

  /**
   * Preserve the database migration field 'billing_migration_status' for historical purposes, to
   * indicate the status of some older workspaces. All workspaces created going forward have status
   * 1 (NEW).
   *
   * <p>For additional context, see also:
   * https://precisionmedicineinitiative.atlassian.net/browse/RW-2607
   * https://precisionmedicineinitiative.atlassian.net/browse/RW-2705
   * https://precisionmedicineinitiative.atlassian.net/browse/RW-5492
   * https://precisionmedicineinitiative.atlassian.net/browse/RW-5493
   * https://precisionmedicineinitiative.atlassian.net/browse/RW-8705
   * https://precisionmedicineinitiative.atlassian.net/browse/RW-8709
   */
  @Deprecated
  public enum BillingMigrationStatus {
    OLD, // 0 (DEPRECATED) - indicates pre-1PPW; this billing project may be associated with
    // multiple workspaces

    NEW, // 1 - a One Project Per Workspace (1PPW) billing project; all new workspaces

    MIGRATED // 2 (DEPRECATED) - an original pre-1PPW billing project which has been cloned to a
    // 1/NEW workspace, and then deleted.
  }

  private short billingMigrationStatus = 1; // NEW (1PPW) - includes all post-2019 workspaces

  @Column(name = "billing_migration_status")
  @Deprecated // do not use; only retained for Hibernate compatibility
  private short getBillingMigrationStatus() {
    return billingMigrationStatus;
  }

  @Deprecated // do not use; only retained for Hibernate compatibility
  private void setBillingMigrationStatus(Short status) {
    this.billingMigrationStatus = status;
  }
}
