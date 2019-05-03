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
  private String fireCloudProjectName;
  private Timestamp creationTime;
  private Short status;

  public enum BillingProjectBufferStatus {
    CREATING, // Sent a request to FireCloud to create a BillingProject. Status of BillingProject is TBD
    ERROR, // Failed to create BillingProject
    AVAILABLE, // BillingProject is ready to be assigned to a user
    ASSIGNING, //  BillingProject is being assigned to a user
    ASSIGNED // BillingProject has been assigned to a user
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
  public String getFireCloudProjectName() {
    return fireCloudProjectName;
  }
  public void setFireCloudProjectName(String fireCloudProjectName) {
    this.fireCloudProjectName = fireCloudProjectName;
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
  private void setStatusValue(Short s) { this.status = s; }

}
