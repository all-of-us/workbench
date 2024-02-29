package org.pmiops.workbench.db.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
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
  private String lastModifiedBy;
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
      String lastModifiedBy,
      Timestamp lastModifiedTime) {
    setVersion(version);
    setName(name);
    setDomain(domain);
    setSurvey(survey);
    setDescription(description);
    setWorkspaceId(workspaceId);
    setCreator(creator);
    setCreationTime(creationTime);
    setLastModifiedBy(lastModifiedBy);
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
    setLastModifiedBy(cs.getLastModifiedBy());
    setLastModifiedTime(cs.getLastModifiedTime());
    setConceptSetConceptIds(new HashSet<>(cs.getConceptSetConceptIds()));
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "concept_set_id")
  public long getConceptSetId() {
    return conceptSetId;
  }

  public DbConceptSet setConceptSetId(long conceptSetId) {
    this.conceptSetId = conceptSetId;
    return this;
  }

  @Version
  @Column(name = "version")
  public int getVersion() {
    return version;
  }

  public DbConceptSet setVersion(int version) {
    this.version = version;
    return this;
  }

  @Column(name = "name")
  public String getName() {
    return name;
  }

  public DbConceptSet setName(String name) {
    this.name = name;
    return this;
  }

  @Column(name = "domain")
  public short getDomain() {
    return domain;
  }

  public DbConceptSet setDomain(short domain) {
    this.domain = domain;
    return this;
  }

  @Transient
  public Domain getDomainEnum() {
    return DbStorageEnums.domainFromStorage(domain);
  }

  public DbConceptSet setDomainEnum(Domain domain) {
    this.domain = DbStorageEnums.domainToStorage(domain);
    return this;
  }

  @Column(name = "survey")
  public Short getSurvey() {
    return survey;
  }

  public DbConceptSet setSurvey(Short survey) {
    this.survey = survey;
    return this;
  }

  @Transient
  public Surveys getSurveysEnum() {
    return DbStorageEnums.surveysFromStorage(survey);
  }

  public DbConceptSet setSurveysEnum(Surveys survey) {
    this.survey = DbStorageEnums.surveysToStorage(survey);
    return this;
  }

  @Column(name = "description")
  public String getDescription() {
    return description;
  }

  public DbConceptSet setDescription(String description) {
    this.description = description;
    return this;
  }

  @Column(name = "workspace_id")
  public long getWorkspaceId() {
    return workspaceId;
  }

  public DbConceptSet setWorkspaceId(long workspaceId) {
    this.workspaceId = workspaceId;
    return this;
  }

  @ManyToOne
  @JoinColumn(name = "creator_id")
  public DbUser getCreator() {
    return creator;
  }

  public DbConceptSet setCreator(DbUser creator) {
    this.creator = creator;
    return this;
  }

  @Column(name = "creation_time")
  public Timestamp getCreationTime() {
    return creationTime;
  }

  public DbConceptSet setCreationTime(Timestamp creationTime) {
    this.creationTime = creationTime;
    return this;
  }

  @Column(name = "last_modified_by")
  public String getLastModifiedBy() {
    return lastModifiedBy;
  }

  public DbConceptSet setLastModifiedBy(String lastModifiedBy) {
    this.lastModifiedBy = lastModifiedBy;
    return this;
  }

  @Column(name = "last_modified_time")
  public Timestamp getLastModifiedTime() {
    return lastModifiedTime;
  }

  public DbConceptSet setLastModifiedTime(Timestamp lastModifiedTime) {
    this.lastModifiedTime = lastModifiedTime;
    return this;
  }

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(
      name = "concept_set_concept_id",
      joinColumns = @JoinColumn(name = "concept_set_id"))
  @Column(name = "concept_id")
  public Set<DbConceptSetConceptId> getConceptSetConceptIds() {
    return dbConceptSetConceptIds;
  }

  public DbConceptSet setConceptSetConceptIds(Set<DbConceptSetConceptId> dbConceptSetConceptIds) {
    this.dbConceptSetConceptIds = dbConceptSetConceptIds;
    return this;
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
        && Objects.equals(lastModifiedBy, that.lastModifiedBy)
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
