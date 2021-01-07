package org.pmiops.workbench.db.model;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
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
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Version;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.Surveys;

@Entity
@Table(name = "concept_set")
public class DbConceptSet {
  public static final int INITIAL_VERSION = 1;

  private long conceptSetId;
  private int version;
  private String name;
  private short domain;
  private Short survey;
  private String description;
  private long workspaceId;
  private DbUser creator;
  private Timestamp creationTime;
  private Timestamp lastModifiedTime;
  private Set<DbConceptSetConceptId> dbConceptSetConceptIds = new HashSet<>();

  public DbConceptSet() {
    setVersion(DbConceptSet.INITIAL_VERSION);
  }

  public DbConceptSet(
      String name,
      int version,
      short domain,
      Short survey,
      String description,
      long workspaceId,
      DbUser creator,
      Timestamp creationTime,
      Timestamp lastModifiedTime) {
    setVersion(version);
    setName(name);
    setDomain(domain);
    setSurvey(survey);
    setDescription(description);
    setWorkspaceId(workspaceId);
    setCreator(creator);
    setCreationTime(creationTime);
    setLastModifiedTime(lastModifiedTime);
  }

  public DbConceptSet(DbConceptSet cs) {
    setDescription(cs.getDescription());
    setName(cs.getName());
    setDomain(cs.getDomain());
    setSurvey(cs.getSurvey());
    setCreator(cs.getCreator());
    setVersion(DbConceptSet.INITIAL_VERSION);
    setWorkspaceId(cs.getWorkspaceId());
    setCreationTime(cs.getCreationTime());
    setLastModifiedTime(cs.getLastModifiedTime());
    setConceptSetConceptIds(new HashSet<>(cs.getConceptSetConceptIds()));
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "concept_set_id")
  public long getConceptSetId() {
    return conceptSetId;
  }

  public void setConceptSetId(long conceptSetId) {
    this.conceptSetId = conceptSetId;
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

  @Column(name = "domain")
  public short getDomain() {
    return domain;
  }

  public void setDomain(short domain) {
    this.domain = domain;
  }

  @Transient
  public Domain getDomainEnum() {
    return DbStorageEnums.domainFromStorage(domain);
  }

  public void setDomainEnum(Domain domain) {
    this.domain = DbStorageEnums.domainToStorage(domain);
  }

  @Column(name = "survey")
  public Short getSurvey() {
    return survey;
  }

  public void setSurvey(Short survey) {
    this.survey = survey;
  }

  @Transient
  public Surveys getSurveysEnum() {
    return DbStorageEnums.surveysFromStorage(survey);
  }

  public void setSurveysEnum(Surveys survey) {
    this.survey = DbStorageEnums.surveysToStorage(survey);
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

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(
      name = "concept_set_concept_id",
      joinColumns = @JoinColumn(name = "concept_set_id"))
  @Column(name = "concept_id")
  public Set<DbConceptSetConceptId> getConceptSetConceptIds() {
    return dbConceptSetConceptIds;
  }

  public void setConceptSetConceptIds(Set<DbConceptSetConceptId> dbConceptSetConceptIds) {
    this.dbConceptSetConceptIds = dbConceptSetConceptIds;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DbConceptSet that = (DbConceptSet) o;
    return version == that.version
        && domain == that.domain
        && workspaceId == that.workspaceId
        && Objects.equals(name, that.name)
        && Objects.equals(survey, that.survey)
        && Objects.equals(description, that.description)
        && Objects.equals(creator, that.creator)
        && Objects.equals(creationTime, that.creationTime)
        && Objects.equals(lastModifiedTime, that.lastModifiedTime)
        && Objects.equals(dbConceptSetConceptIds, that.dbConceptSetConceptIds);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        version,
        name,
        domain,
        survey,
        description,
        workspaceId,
        creator,
        creationTime,
        lastModifiedTime,
        dbConceptSetConceptIds);
  }
}
