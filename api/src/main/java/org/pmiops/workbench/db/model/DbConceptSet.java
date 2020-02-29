package org.pmiops.workbench.db.model;

import java.sql.Timestamp;
import java.util.HashSet;
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
  private static final int INITIAL_VERSION = 1;

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
  private int participantCount;
  private Set<Long> conceptIds = new HashSet<>();

  private DbConceptSet(Builder builder) {
    setConceptSetId(builder.conceptSetId);
    setVersion(builder.version);
    setName(builder.name);
    setDomain(builder.domain);
    setSurvey(builder.survey);
    setDescription(builder.description);
    setWorkspaceId(builder.workspaceId);
    setCreator(builder.creator);
    setCreationTime(builder.creationTime);
    setLastModifiedTime(builder.lastModifiedTime);
    setParticipantCount(builder.participantCount);
    setConceptIds(builder.conceptIds);
  }

  public DbConceptSet() {
    setVersion(DbConceptSet.INITIAL_VERSION);
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
    setParticipantCount(cs.getParticipantCount());
    setConceptIds(new HashSet<>(cs.getConceptIds()));
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
    return CommonStorageEnums.domainFromStorage(domain);
  }

  public void setDomainEnum(Domain domain) {
    this.domain = CommonStorageEnums.domainToStorage(domain);
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
    return CommonStorageEnums.surveysFromStorage(survey);
  }

  public void setSurveysEnum(Surveys survey) {
    this.survey = CommonStorageEnums.surveysToStorage(survey);
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

  @Column(name = "participant_count")
  public int getParticipantCount() {
    return participantCount;
  }

  public void setParticipantCount(int participantCount) {
    this.participantCount = participantCount;
  }

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(
      name = "concept_set_concept_id",
      joinColumns = @JoinColumn(name = "concept_set_id"))
  @Column(name = "concept_id")
  public Set<Long> getConceptIds() {
    return conceptIds;
  }

  public void setConceptIds(Set<Long> conceptIds) {
    this.conceptIds = conceptIds;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
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
    private int participantCount;
    private Set<Long> conceptIds = new HashSet<>();

    private Builder() {}

    public DbConceptSet.Builder addConceptSetId(long conceptSetId) {
      this.conceptSetId = conceptSetId;
      return this;
    }

    public DbConceptSet.Builder addVersion(int version) {
      this.version = version;
      return this;
    }

    public DbConceptSet.Builder addName(String name) {
      this.name = name;
      return this;
    }

    public DbConceptSet.Builder addDomain(Short domain) {
      this.domain = domain;
      return this;
    }

    public DbConceptSet.Builder addSurvey(Short survey) {
      this.survey = survey;
      return this;
    }

    public DbConceptSet.Builder addDescription(String description) {
      this.description = description;
      return this;
    }

    public DbConceptSet.Builder addWorkspaceId(long workspaceId) {
      this.workspaceId = workspaceId;
      return this;
    }

    public DbConceptSet.Builder addCreator(DbUser creator) {
      this.creator = creator;
      return this;
    }

    public DbConceptSet.Builder addCreationTime(Timestamp creationTime) {
      this.creationTime = creationTime;
      return this;
    }

    public DbConceptSet.Builder addLastModifiedTime(Timestamp lastModifiedTime) {
      this.lastModifiedTime = lastModifiedTime;
      return this;
    }

    public DbConceptSet.Builder addParticipantCount(int participantCount) {
      this.participantCount = participantCount;
      return this;
    }

    public DbConceptSet.Builder addConceptIds(Set<Long> conceptIds) {
      this.conceptIds = conceptIds;
      return this;
    }

    public DbConceptSet build() {
      return new DbConceptSet(this);
    }
  }
}
