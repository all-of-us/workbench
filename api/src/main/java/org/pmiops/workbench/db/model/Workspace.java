package org.pmiops.workbench.db.model;

import java.sql.Timestamp;
import java.util.List;
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
@Table(name = "workspace",
  indexes = {  @Index(name = "idx_workspace_fc_name", columnList = "firecloud_name",
      unique = true)})
public class Workspace {

  public static class FirecloudWorkspaceId {
    private final String namespace;
    private final String workspaceId;

    public FirecloudWorkspaceId(String namespace, String workspaceId) {
      this.namespace = namespace;
      this.workspaceId = workspaceId;
    }

    public String getNamespace() {
      return namespace;
    }

    public String getWorkspaceId() {
      return workspaceId;
    }

  }

  private long workspaceId;
  private String name;
  private String description;
  private String firecloudName;
  private DataAccessLevel dataAccessLevel;
  private CdrVersion cdrVersion;
  private User creator;
  private Timestamp creationTime;
  private Timestamp lastModifiedTime;
  private List<Cohort> cohorts;

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
    int slashIndex = firecloudName.indexOf('/');
    String namespace = firecloudName.substring(0, slashIndex);
    String workspaceId = firecloudName.substring(slashIndex + 1);
    return new FirecloudWorkspaceId(namespace, workspaceId);
  }

  public static String toFirecloudName(String workspaceNamespace, String workspaceId) {
    return workspaceNamespace + "/" + workspaceId;
  }
}
