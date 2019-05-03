package org.pmiops.workbench.db.model;

import java.sql.Timestamp;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class BillingProjectBufferEntry {

  private long id;
  private String projectName;
  private Timestamp creationTime;
  private Short status;

  public enum BillingProjectBufferStatus {
    CREATING,
    ERROR,
    AVAILABLE,
    ASSIGNING,
    ASSIGNED
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "billing_project_buffer_entry_id")
  public long getId() {
    return id;
  }
  public void setId(long id) {
    this.id = id;
  }

  @Column(name = "firecloud_project_name")
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

  public BillingProjectBufferStatus getStatusEnum() {
    return StorageEnums.billingProjectBufferStatusFromStorage(status);
  }
  public void setStatusEnum(BillingProjectBufferStatus status) {
    this.status = StorageEnums.billingProjectBufferStatusToStorage(status);
  }

  @Column(name = "status")
  private Short getStatusValue() { return this.status; }
  private void setStatusValue(Short s) { this.status = status; }

}
