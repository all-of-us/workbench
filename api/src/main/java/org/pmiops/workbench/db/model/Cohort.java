package org.pmiops.workbench.db.model;

import java.sql.Timestamp;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;

@Entity
@Table(name = "cohort")
public class Cohort {

  private long cohortId;
  private int version;
  private String name;
  private String type;
  private String description;
  private long workspaceId;
  private String criteria;
  private User creator;
  private Timestamp creationTime;
  private Timestamp lastModifiedTime;
  private Set<CohortReview> cohortReviews;

  public Cohort() {}

  public Cohort(Cohort c) {
    setCriteria(c.getCriteria());
    setDescription(c.getDescription());
    setName(c.getName());
    setType(c.getType());
    setCreator(c.getCreator());
    setWorkspaceId(c.getWorkspaceId());
    setCreationTime(c.getCreationTime());
    setLastModifiedTime(c.getLastModifiedTime());
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "cohort_id")
  public long getCohortId() {
    return cohortId;
  }

  public void setCohortId(long cohortId) {
    this.cohortId = cohortId;
  }

  @Version
  @Column(name = "version")
  public int getVersion() {
    return version;
  }

  public void setVersion(int version) { this.version = version; }

  @Column(name = "name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Column(name = "type")
  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  @Column(name = "description")
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @Column(name = "workspace_id")
  public long getWorkspaceId() {
    return workspaceId;
  }

  public void setWorkspaceId(long workspaceId) {
    this.workspaceId = workspaceId;
  }

  @Lob
  @Column(name = "criteria")
  public String getCriteria() {
    return criteria;
  }

  public void setCriteria(String criteria) {
    this.criteria = criteria;
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

  @OneToMany(mappedBy = "cohortId", orphanRemoval = true, cascade = CascadeType.ALL)
  public Set<CohortReview> getCohortReviews() {
    return cohortReviews;
  }
  
  public void setCohortReviews(Set<CohortReview> cohortReviews) {
    if (this.cohortReviews == null) {
      this.cohortReviews = cohortReviews;
      return;
    }
    this.cohortReviews.addAll(cohortReviews);
  }

  public void addCohortReview(CohortReview cohortReview) {
    this.cohortReviews.add(cohortReview);
  }
  
}
