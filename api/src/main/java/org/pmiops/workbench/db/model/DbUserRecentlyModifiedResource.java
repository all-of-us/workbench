package org.pmiops.workbench.db.model;

import java.sql.Timestamp;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

@Entity
@Table(name = "user_recently_modified_resource")
public class DbUserRecentlyModifiedResource {
  public enum DbUserRecentlyModifiedResourceType {
    COHORT,
    CONCEPT_SET,
    NOTEBOOK,
    DATA_SET,
    COHORT_REVIEW
  }

  private int id;
  private Long userId;
  private Long workspaceId;
  private DbUserRecentlyModifiedResourceType resourceTypeEnum;
  private String resourceId;
  private Timestamp lastAccessDate;
  private DbCohort cohort;
  private DbConceptSet conceptSet;
  private DbDataset dataSet;
  private String notebookName;

  public DbUserRecentlyModifiedResource() {}

  public DbUserRecentlyModifiedResource(
      long workspaceId,
      long userId,
      DbUserRecentlyModifiedResourceType resourceTypeEnum,
      String resourceId,
      Timestamp lastAccessDate) {
    this.workspaceId = workspaceId;
    this.userId = userId;
    this.resourceId = resourceId;
    this.resourceTypeEnum = resourceTypeEnum;
    this.lastAccessDate = lastAccessDate;
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  @Column(name = "user_id")
  public long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  @Column(name = "workspace_id")
  public long getWorkspaceId() {
    return workspaceId;
  }

  public void setWorkspaceId(long workspaceId) {
    this.workspaceId = workspaceId;
  }

  @Enumerated(EnumType.STRING)
  @Column(name = "resource_type", nullable = false)
  public DbUserRecentlyModifiedResourceType getResourceType() {
    return resourceTypeEnum;
  }

  public void setResourceType(DbUserRecentlyModifiedResourceType s) {
    this.resourceTypeEnum = s;
  }

  @Column(name = "resource_id")
  public String getResourceId() {
    return resourceId;
  }

  public void setResourceId(String resourceId) {
    this.resourceId = resourceId;
  }

  @Column(name = "last_access_date")
  public Timestamp getLastAccessDate() {
    return lastAccessDate;
  }

  public void setLastAccessDate(Timestamp lastAccessDate) {
    this.lastAccessDate = lastAccessDate;
  }

  @Transient
  public DbCohort getCohort() {
    return cohort;
  }

  public void setCohort(DbCohort cohort) {
    this.cohort = cohort;
  }

  @Transient
  public DbConceptSet getConceptSet() {
    return conceptSet;
  }

  public void setConceptSet(DbConceptSet conceptSet) {
    this.conceptSet = conceptSet;
  }

  @Transient
  public DbDataset getDataSet() {
    return dataSet;
  }

  public void setDataSet(DbDataset dataSet) {
    this.dataSet = dataSet;
  }

  @Transient
  public String getNotebookName() {
    return notebookName;
  }

  public void setNotebookName(String notebookName) {
    this.notebookName = notebookName;
  }
}
