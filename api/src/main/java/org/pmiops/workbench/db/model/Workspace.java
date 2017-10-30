package org.pmiops.workbench.db.model;

import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.pmiops.workbench.model.DataAccessLevel;

@Entity
@Table(name = "workspace")
public class Workspace {

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

  private long workspaceId;
  private String name;
  private String description;
  private String workspaceNamespace;
  private String firecloudName;
  private DataAccessLevel dataAccessLevel;
  private CdrVersion cdrVersion;
  private User creator;
  private Timestamp creationTime;
  private Timestamp lastModifiedTime;
  private List<Cohort> cohorts;

  private boolean diseaseFocusedResearch;
  private String diseaseOfFocus;
  private boolean methodsDevelopment;
  private boolean controlSet;
  private boolean aggregateAnalysis;
  private boolean ancestry;
  private boolean commercialPurpose;
  private boolean population;
  private String populationOfFocus;
  private String additionalNotes;

  private Boolean reviewRequested;
  private Boolean approved;
  private Timestamp timeRequested;
  private Timestamp timeReviewed;

  @Id
  @GeneratedValue
  @Column(name = "workspace_id")
  public long getWorkspaceId() {
    return workspaceId;
  }

  public void setWorkspaceId(long workspaceId) {
    this.workspaceId = workspaceId;
  }

  @Column(name = "name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Column(name = "description")
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
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
  public DataAccessLevel getDataAccessLevel() {
    return dataAccessLevel;
  }

  public void setDataAccessLevel(DataAccessLevel dataAccessLevel) {
    this.dataAccessLevel = dataAccessLevel;
  }

  @ManyToOne
  @JoinColumn(name = "cdr_version_id")
  public CdrVersion getCdrVersion() {
    return cdrVersion;
  }

  public void setCdrVersion(CdrVersion cdrVersion) {
    this.cdrVersion = cdrVersion;
  }

  @ManyToOne
  @JoinColumn(name = "creator_id")
  public User getCreator() {
    return creator;
  }

  public void setCreator(User creator) {
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

  @Column(name = "rp_aggregate_analysis")
  public boolean getAggregateAnalysis() {
    return this.aggregateAnalysis;
  }

  public void setAggregateAnalysis(boolean aggregateAnalysis) {
    this.aggregateAnalysis = aggregateAnalysis;
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

  @Column(name = "rp_population")
  public boolean getPopulation() {
    return this.population;
  }

  public void setPopulation(boolean population) {
    this.population = population;
  }

  @Column(name = "rp_population_of_focus")
  public String getPopulationOfFocus() {
    return this.populationOfFocus;
  }

  public void setPopulationOfFocus(String populationOfFocus) {
    this.populationOfFocus = populationOfFocus;
  }

  @Column(name = "rp_additional_notes")
  public String getAdditionalNotes() {
    return this.additionalNotes;
  }

  public void setAdditionalNotes(String additionalNotes) {
    this.additionalNotes = additionalNotes;
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

  @Column(name = "rp_approved")
  public Boolean getApproved() {
    return this.approved;
  }

  public void setApproved(Boolean approved) {
    if (approved != null && this.timeReviewed == null) {
      this.timeReviewed = new Timestamp(System.currentTimeMillis());
    }
    this.approved = approved;
  }

  @Column(name = "rp_time_requested")
  public Timestamp getTimeRequested() {
    return this.timeRequested;
  }

  public void setTimeRequested(Timestamp timeRequested) {
    this.timeRequested = timeRequested;
  }

  @Column(name = "rp_time_reviewed")
  public Timestamp getTimeReviewed() {
    return this.timeReviewed;
  }

  public void setTimeReviewed(Timestamp timeReviewed) {
    this.timeReviewed = timeReviewed;
  }

  @OneToMany(mappedBy = "workspaceId")
  @OrderBy("name ASC")
  public List<Cohort> getCohorts() {
    return cohorts;
  }

  public void setCohorts(List<Cohort> cohorts) {
    this.cohorts = cohorts;
  }

  @Transient
  public FirecloudWorkspaceId getFirecloudWorkspaceId() {
    return new FirecloudWorkspaceId(workspaceNamespace, firecloudName);
  }
}
