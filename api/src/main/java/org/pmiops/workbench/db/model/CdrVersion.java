package org.pmiops.workbench.db.model;

import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.joda.time.DateTime;
import org.pmiops.workbench.model.DataAccessLevel;

import java.sql.Timestamp;

@Entity
@Table(name = "cdr_version")
public class CdrVersion {

  private long cdrVersionId;
  private String name;
  private DataAccessLevel dataAccessLevel;
  private short releaseNumber;
  private String bigqueryProject;
  private String bigqueryDataset;
  private Timestamp creationTime;
  private int numParticipants;
  private String cdrDbName;
  private String publicDbName;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "cdr_version_id")
  public long getCdrVersionId() {
    return cdrVersionId;
  }

  public void setCdrVersionId(long cdrVersionId) {
    this.cdrVersionId = cdrVersionId;
  }

  @Column(name = "name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Column(name = "data_access_level")
  public DataAccessLevel getDataAccessLevel() {
    return dataAccessLevel;
  }

  public void setDataAccessLevel(DataAccessLevel dataAccessLevel) {
    this.dataAccessLevel = dataAccessLevel;
  }

  @Column(name = "release_number")
  public short getReleaseNumber() {
    return releaseNumber;
  }

  public void setReleaseNumber(short releaseNumber) {
    this.releaseNumber = releaseNumber;
  }

  @Column(name = "bigquery_project")
  public String getBigqueryProject() {
    return bigqueryProject;
  }

  public void setBigqueryProject(String bigqueryProject) {
    this.bigqueryProject = bigqueryProject;
  }

  @Column(name = "bigquery_dataset")
  public String getBigqueryDataset() {
    return bigqueryDataset;
  }

  public void setBigqueryDataset(String bigqueryDataset) {
    this.bigqueryDataset = bigqueryDataset;
  }

  @Column(name = "creation_time")
  public Timestamp getCreationTime() {
    return creationTime;
  }

  public void setCreationTime(Timestamp creationTime) {
    this.creationTime = creationTime;
  }

  @Column(name = "num_participants")
  public int getNumParticipants() {
    return numParticipants;
  }

  public void setNumParticipants(int numParticipants) {
    this.numParticipants = numParticipants;
  }

  @Column(name = "cdr_db_name")
  public String getCdrDbName() { return cdrDbName; }

  public void setCdrDbName(String cdrDbName) { this.cdrDbName = cdrDbName; }

  @Column(name = "public_db_name")
  public String getPublicDbName() { return publicDbName; }

  public void setPublicDbName(String publicDbName) { this.publicDbName = publicDbName; }

  @Override
  public int hashCode() {
    return Objects.hash(cdrVersionId, name, dataAccessLevel, releaseNumber, bigqueryProject,
        bigqueryDataset, creationTime, numParticipants, publicDbName, cdrDbName);
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof CdrVersion)) {
      return false;
    }
    CdrVersion that = (CdrVersion) obj;
    return new EqualsBuilder().append(this.cdrVersionId, that.cdrVersionId)
        .append(this.name, that.name)
        .append(this.dataAccessLevel, that.dataAccessLevel)
        .append(this.releaseNumber, that.releaseNumber)
        .append(this.bigqueryProject, that.bigqueryProject)
        .append(this.creationTime, that.creationTime)
        .append(this.numParticipants, that.numParticipants)
        .append(this.publicDbName, that.publicDbName)
        .append(this.cdrDbName, that.cdrDbName)
        .build();
  }
}
