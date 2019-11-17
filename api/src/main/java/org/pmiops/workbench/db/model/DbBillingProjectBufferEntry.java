package org.pmiops.workbench.db.model;

import java.sql.Timestamp;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.jetbrains.annotations.NotNull;

@Entity
@Table(name = "billing_project_buffer_entry")
public class DbBillingProjectBufferEntry {

  private long id;
  private String fireCloudProjectName;
  private Timestamp creationTime;
  private Timestamp lastSyncRequestTime;
  private Timestamp lastStatusChangedTime;
  private Short status;
  private DbUser assignedUser;

  public enum BufferEntryStatus {
    // Sent a request to FireCloud to create a BillingProject. Status of BillingProject is TBD
    CREATING,

    ERROR, // Failed to create BillingProject
    AVAILABLE, // BillingProject is ready to be assigned to a user
    ASSIGNING, //  BillingProject is being assigned to a user
    ASSIGNED, // BillingProject has been assigned to a user

    // The ownership of this project has been transferred from the AoU App Engine SA
    // to an alternate SA, to help ensure that the AoU App Engine SA is not a member of too many
    // groups. See https://precisionmedicineinitiative.atlassian.net/browse/RW-3435
    GARBAGE_COLLECTED,
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

  @Column(name = "last_sync_request_time")
  public Timestamp getLastSyncRequestTime() {
    return lastSyncRequestTime;
  }

  public void setLastSyncRequestTime(Timestamp lastSyncRequestTime) {
    this.lastSyncRequestTime = lastSyncRequestTime;
  }

  @Column(name = "last_status_changed_time")
  public Timestamp getLastStatusChangedTime() {
    return lastStatusChangedTime;
  }

  private void setLastStatusChangedTime(Timestamp lastStatusChangedTime) {
    this.lastStatusChangedTime = lastStatusChangedTime;
  }

  @ManyToOne
  @JoinColumn(name = "assigned_user_id")
  public DbUser getAssignedUser() {
    return assignedUser;
  }

  public void setAssignedUser(DbUser assignedUser) {
    this.assignedUser = assignedUser;
  }

  @Transient
  public BufferEntryStatus getStatusEnum() {
    return DbStorageEnums.billingProjectBufferEntryStatusFromStorage(status);
  }

  public void setStatusEnum(BufferEntryStatus status, Supplier<Timestamp> currentTimestamp) {
    this.setLastStatusChangedTime(currentTimestamp.get());
    this.status = DbStorageEnums.billingProjectBufferEntryStatusToStorage(status);
  }

  // Calculate the timespan between creation and sync, to see if this entry has been
  // stuck in crating too long
  @Transient
  public Duration getLastChangedToLastSyncRequestInterval() {
    return Duration.between(
        getLastStatusChangedTime().toInstant(), getLastSyncRequestTime().toInstant());
  }

  @Column(name = "status")
  private short getStatus() {
    return this.status;
  }

  private void setStatus(short s) {
    this.status = s;
  }

  @Override
  public String toString() {
    return "DbBillingProjectBufferEntry{"
        + "id="
        + id
        + ", fireCloudProjectName='"
        + fireCloudProjectName
        + '\''
        + ", creationTime="
        + nullableTimestampToString(creationTime)
        + ", lastSyncRequestTime="
        + nullableTimestampToString(lastSyncRequestTime)
        + ", lastStatusChangedTime="
        + nullableTimestampToString(lastStatusChangedTime)
        + ", statusEnum="
        + getStatusEnum()
        + ", assignedUser="
        + Optional.ofNullable(assignedUser).map(u -> Long.toString(u.getUserId())).orElse("n/a")
        + '}';
  }

  @NotNull
  private String nullableTimestampToString(Timestamp timestamp) {
    return Optional.ofNullable(timestamp).map(Timestamp::toString).orElse("n/a");
  }
}
