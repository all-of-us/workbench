package org.pmiops.workbench.db.model;

import java.sql.Timestamp;
import java.util.List;
import java.util.stream.Collectors;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Version;
import org.pmiops.workbench.model.PrePackagedConceptSetSelection;

@Entity
@Table(name = "data_set")
public class DbDataset {
  private static final int INITIAL_VERSION = 1;

  private long dataSetId;
  private long workspaceId;
  private String name;
  private int version;
  private String description;
  private long creatorId;
  private Timestamp creationTime;
  private Timestamp lastModifiedTime;
  private Boolean invalid;
  private Boolean includesAllParticipants;
  private List<Long> conceptSetIds;
  private List<Long> cohortIds;
  private List<DbDatasetValue> values;
  private short prePackagedConceptSet;

  public DbDataset() {
    setVersion(DbDataset.INITIAL_VERSION);
  }

  public DbDataset(
      long dataSetId,
      long workspaceId,
      String name,
      String description,
      long creatorId,
      Timestamp creationTime,
      Boolean invalid) {
    this.dataSetId = dataSetId;
    this.workspaceId = workspaceId;
    this.name = name;
    this.version = DbDataset.INITIAL_VERSION;
    this.description = description;
    this.creatorId = creatorId;
    this.creationTime = creationTime;
    this.invalid = invalid;
  }

  public DbDataset(DbDataset dataSet) {
    setName(dataSet.getName());
    setVersion(DbDataset.INITIAL_VERSION);
    setDescription(dataSet.getDescription());
    setInvalid(dataSet.getInvalid());
    setIncludesAllParticipants(dataSet.getIncludesAllParticipants());
    setValues(dataSet.getValues().stream().map(DbDatasetValue::new).collect(Collectors.toList()));
    setPrePackagedConceptSet(dataSet.getPrePackagedConceptSet());
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "data_set_id")
  public long getDataSetId() {
    return dataSetId;
  }

  public void setDataSetId(long dataSetId) {
    this.dataSetId = dataSetId;
  }

  @Version
  @Column(name = "version")
  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
  }

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

  @Column(name = "creator_id")
  public long getCreatorId() {
    return creatorId;
  }

  public void setCreatorId(long creatorId) {
    this.creatorId = creatorId;
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

  @Column(name = "invalid")
  public Boolean getInvalid() {
    return invalid;
  }

  public void setInvalid(Boolean invalid) {
    this.invalid = invalid;
  }

  @Column(name = "includes_all_participants")
  public Boolean getIncludesAllParticipants() {
    return includesAllParticipants;
  }

  public void setIncludesAllParticipants(Boolean includesAllParticipants) {
    this.includesAllParticipants = includesAllParticipants;
  }

  @ElementCollection
  @CollectionTable(name = "data_set_concept_set", joinColumns = @JoinColumn(name = "data_set_id"))
  @Column(name = "concept_set_id")
  public List<Long> getConceptSetIds() {
    return conceptSetIds;
  }

  public void setConceptSetIds(List<Long> conceptSetIds) {
    this.conceptSetIds = conceptSetIds;
  }

  @ElementCollection
  @CollectionTable(name = "data_set_cohort", joinColumns = @JoinColumn(name = "data_set_id"))
  @Column(name = "cohort_id")
  public List<Long> getCohortIds() {
    return cohortIds;
  }

  public void setCohortIds(List<Long> cohortIds) {
    this.cohortIds = cohortIds;
  }

  @ElementCollection
  @CollectionTable(name = "data_set_values", joinColumns = @JoinColumn(name = "data_set_id"))
  @Column(name = "values")
  public List<DbDatasetValue> getValues() {
    return values;
  }

  public void setValues(List<DbDatasetValue> values) {
    this.values = values;
  }

  @Column(name = "pre_packaged_concept_set")
  public short getPrePackagedConceptSet() {
    return prePackagedConceptSet;
  }

  public void setPrePackagedConceptSet(short prePackagedConceptSet) {
    this.prePackagedConceptSet = prePackagedConceptSet;
  }

  @Transient
  public PrePackagedConceptSetSelection getPrePackagedConceptSetSelection() {
    return DbStorageEnums.prePackageConceptSetsFromStorage(prePackagedConceptSet);
  }

  public void setPrePackagedConceptSetSelection(PrePackagedConceptSetSelection domain) {
    this.prePackagedConceptSet = DbStorageEnums.prePackageConceptSetsToStorage(domain);
  }
}
