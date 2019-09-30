package org.pmiops.workbench.db.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;
import org.pmiops.workbench.model.GarbageCollectedProject;

@Entity
@Table(name = "billing_project_garbage_collection")
public class BillingProjectGarbageCollection {
  private String fireCloudProjectName;
  private String owner;

  @Id
  @Column(name = "firecloud_project_name")
  public String getFireCloudProjectName() {
    return fireCloudProjectName;
  }

  public void setFireCloudProjectName(String fireCloudProjectName) {
    this.fireCloudProjectName = fireCloudProjectName;
  }

  @Column(name = "owner")
  public String getOwner() {
    return owner;
  }

  public void setOwner(String owner) {
    this.owner = owner;
  }

  // convert to Swagger model class
  public GarbageCollectedProject toGarbageCollectedProject() {
    return new GarbageCollectedProject().project(fireCloudProjectName).newOwner(owner);
  }
}
