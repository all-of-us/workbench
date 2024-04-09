package org.pmiops.workbench.db.model;

import java.sql.Timestamp;
import java.util.Set;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "cohort")
public class DbCohort {

  private long cohortId;
  private int version;
  private String name;
  private String type;
  private String description;
  private long workspaceId;
  private String criteria;
  private DbUser creator;
  private Timestamp creationTime;
  private String lastModifiedBy;
  private Timestamp lastModifiedTime;
  private Set<DbCohortReview> cohortReviews;

  public DbCohort() {}

  public DbCohort(DbCohort c) {
    setCriteria(c.getCriteria());
    setDescription(c.getDescription());
    setName(c.getName());
    setType(c.getType());
    setCreator(c.getCreator());
    setWorkspaceId(c.getWorkspaceId());
    setCreationTime(c.getCreationTime());
    setLastModifiedBy(c.getLastModifiedBy());
    setLastModifiedTime(c.getLastModifiedTime());
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "cohort_id")
  public long getCohortId() {
    return cohortId;
  }

  public DbCohort setCohortId(long cohortId) {
    this.cohortId = cohortId;
    return this;
  }

  @Version
  @Column(name = "version")
  public int getVersion() {
    return version;
  }

  public DbCohort setVersion(int version) {
    this.version = version;
    return this;
  }

  @Column(name = "name")
  public String getName() {
    return name;
  }

  public DbCohort setName(String name) {
    this.name = name;
    return this;
  }

  @Column(name = "type")
  public String getType() {
    return type;
  }

  public DbCohort setType(String type) {
    this.type = type;
    return this;
  }

  @Column(name = "description")
  public String getDescription() {
    return description;
  }

  public DbCohort setDescription(String description) {
    this.description = description;
    return this;
  }

  @Column(name = "workspace_id")
  public long getWorkspaceId() {
    return workspaceId;
  }

  public DbCohort setWorkspaceId(long workspaceId) {
    this.workspaceId = workspaceId;
    return this;
  }

  @Lob
  @Column(name = "criteria")
  public String getCriteria() {
    return criteria;
  }

  public DbCohort setCriteria(String criteria) {
    this.criteria = criteria;
    return this;
  }

  @ManyToOne
  @JoinColumn(name = "creator_id")
  public DbUser getCreator() {
    return creator;
  }

  public DbCohort setCreator(DbUser creator) {
    this.creator = creator;
    return this;
  }

  @Column(name = "creation_time")
  public Timestamp getCreationTime() {
    return creationTime;
  }

  public DbCohort setCreationTime(Timestamp creationTime) {
    this.creationTime = creationTime;
    return this;
  }

  @Column(name = "last_modified_by")
  public String getLastModifiedBy() {
    return lastModifiedBy;
  }

  public DbCohort setLastModifiedBy(String lastModifiedBy) {
    this.lastModifiedBy = lastModifiedBy;
    return this;
  }

  @Column(name = "last_modified_time")
  public Timestamp getLastModifiedTime() {
    return lastModifiedTime;
  }

  public DbCohort setLastModifiedTime(Timestamp lastModifiedTime) {
    this.lastModifiedTime = lastModifiedTime;
    return this;
  }

  @OneToMany(mappedBy = "cohortId", orphanRemoval = true, cascade = CascadeType.ALL)
  public Set<DbCohortReview> getCohortReviews() {
    return cohortReviews;
  }

  public DbCohort setCohortReviews(Set<DbCohortReview> cohortReviews) {
    if (this.cohortReviews == null) {
      this.cohortReviews = cohortReviews;
      return this;
    }
    this.cohortReviews.addAll(cohortReviews);
    return this;
  }

  public DbCohort addCohortReview(DbCohortReview cohortReview) {
    this.cohortReviews.add(cohortReview);
    return this;
  }
}
