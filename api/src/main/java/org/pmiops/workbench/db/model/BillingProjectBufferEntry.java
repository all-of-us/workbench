package org.pmiops.workbench.db.model;

import java.sql.Timestamp;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class BillingProjectBufferEntry {

  private long id;
  private String projectName;
  private Timestamp creationTime;
  private String status;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "billingprojectbufferentry_id")
  public long getId() {
    return id;
  }
  public void setId(long id) {
    this.id = id;
  }

  @Column(name = "project_name")
  public String getProjectName() {
    return projectName;
  }
  public void setProjectName(String projectName) {
    this.projectName = projectName;
  }

  @Column(name = "creation_time")
  public Timestamp getCreationTime() {
    return creationTime;
  }
  public void setCreationTime(Timestamp creationTime) {
    this.creationTime = creationTime;
  }

  @Column(name = "status")
  public String getStatus() {
    return status;
  }
  public void setStatus(String status) {
    this.status = status;
  }

}
