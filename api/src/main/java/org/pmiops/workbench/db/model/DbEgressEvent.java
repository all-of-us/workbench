package org.pmiops.workbench.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import java.util.Objects;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "egress_event")
@EntityListeners(AuditingEntityListener.class)
public class DbEgressEvent {
  public enum DbEgressEventStatus {
    PENDING,
    REMEDIATED,
    VERIFIED_FALSE_POSITIVE,
    BYPASSED
  }

  private long egressEventId;
  private DbUser user;
  private DbWorkspace workspace;

  private Float egressMegabytes;

  private Long egressWindowSeconds;
  private Timestamp creationTime;
  private Timestamp lastModifiedTime;
  private DbEgressEventStatus status;
  private String sumologicEvent;

  private String bucketAuditEvent;

  private String vwbWorkspaceId;
  private String vwbVmName;
  private String gcpProjectId;
  private String vwbEgressEventId;
  private boolean isVwb;
  private int vwbIncidentCount;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "egress_event_id", nullable = false)
  public long getEgressEventId() {
    return egressEventId;
  }

  public DbEgressEvent setEgressEventId(long egressEventId) {
    this.egressEventId = egressEventId;
    return this;
  }

  @ManyToOne
  @JoinColumn(name = "user_id")
  public DbUser getUser() {
    return user;
  }

  public DbEgressEvent setUser(DbUser user) {
    this.user = user;
    return this;
  }

  @ManyToOne
  @JoinColumn(name = "workspace_id")
  public DbWorkspace getWorkspace() {
    return workspace;
  }

  public DbEgressEvent setWorkspace(DbWorkspace workspace) {
    this.workspace = workspace;
    return this;
  }

  @CreatedDate
  @Column(name = "creation_time", nullable = false)
  public Timestamp getCreationTime() {
    return creationTime;
  }

  public DbEgressEvent setCreationTime(Timestamp creationTime) {
    this.creationTime = creationTime;
    return this;
  }

  @LastModifiedDate
  @Column(name = "last_modified_time", nullable = false)
  public Timestamp getLastModifiedTime() {
    return lastModifiedTime;
  }

  public DbEgressEvent setLastModifiedTime(Timestamp lastModifiedTime) {
    this.lastModifiedTime = lastModifiedTime;
    return this;
  }

  @Column(name = "egress_megabytes")
  public Float getEgressMegabytes() {
    return egressMegabytes;
  }

  public DbEgressEvent setEgressMegabytes(Float egressMegabytes) {
    this.egressMegabytes = egressMegabytes;
    return this;
  }

  @Column(name = "egress_window_seconds")
  public Long getEgressWindowSeconds() {
    return egressWindowSeconds;
  }

  public DbEgressEvent setEgressWindowSeconds(Long egressWindowSeconds) {
    this.egressWindowSeconds = egressWindowSeconds;
    return this;
  }

  @Column(name = "sumologic_event")
  public String getSumologicEvent() {
    return sumologicEvent;
  }

  public DbEgressEvent setSumologicEvent(String sumologicEvent) {
    this.sumologicEvent = sumologicEvent;
    return this;
  }

  @Column(name = "bucket_audit_event")
  public String getBucketAuditEvent() {
    return bucketAuditEvent;
  }

  public DbEgressEvent setBucketAuditEvent(String bucketAuditEvent) {
    this.bucketAuditEvent = bucketAuditEvent;
    return this;
  }

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  public DbEgressEventStatus getStatus() {
    return status;
  }

  public DbEgressEvent setStatus(DbEgressEventStatus s) {
    this.status = s;
    return this;
  }

  @Column(name = "vwb_workspace_id")
  public String getVwbWorkspaceId() {
    return vwbWorkspaceId;
  }

  public DbEgressEvent setVwbWorkspaceId(String vwbWorkspaceId) {
    this.vwbWorkspaceId = vwbWorkspaceId;
    return this;
  }

  @Column(name = "vwb_vm_name")
  public String getVwbVmName() {
    return vwbVmName;
  }

  public DbEgressEvent setVwbVmName(String vwbVmName) {
    this.vwbVmName = vwbVmName;
    return this;
  }

  @Column(name = "gcp_project_id")
  public String getGcpProjectId() {
    return gcpProjectId;
  }

  public DbEgressEvent setGcpProjectId(String gcpProjectId) {
    this.gcpProjectId = gcpProjectId;
    return this;
  }

  @Column(name = "vwb_egress_event_id")
  public String getVwbEgressEventId() {
    return vwbEgressEventId;
  }

  public DbEgressEvent setVwbEgressEventId(String vwbEgressEventId) {
    this.vwbEgressEventId = vwbEgressEventId;
    return this;
  }

  @Column(name = "vwb_incident_count")
  public int getVwbIncidentCount() {
    return vwbIncidentCount;
  }

  public DbEgressEvent setVwbIncidentCount(int vwbIncidentCount) {
    this.vwbIncidentCount = vwbIncidentCount;
    return this;
  }

  @Column(name = "is_vwb")
  public boolean getIsVwb() {
    return isVwb;
  }

  public DbEgressEvent setIsVwb(boolean isVwb) {
    this.isVwb = isVwb;
    return this;
  }

  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DbEgressEvent that = (DbEgressEvent) o;
    return egressEventId == that.egressEventId
        && Objects.equals(user, that.user)
        && Objects.equals(workspace, that.workspace)
        && Objects.equals(egressMegabytes, that.egressMegabytes)
        && Objects.equals(egressWindowSeconds, that.egressWindowSeconds)
        && Objects.equals(creationTime, that.creationTime)
        && Objects.equals(lastModifiedTime, that.lastModifiedTime)
        && status == that.status
        && Objects.equals(sumologicEvent, that.sumologicEvent)
        && Objects.equals(vwbWorkspaceId, that.vwbWorkspaceId)
        && Objects.equals(vwbVmName, that.vwbVmName)
        && Objects.equals(gcpProjectId, that.gcpProjectId)
        && Objects.equals(vwbIncidentCount, that.vwbIncidentCount)
        && Objects.equals(isVwb, that.isVwb);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        egressEventId,
        user,
        workspace,
        egressMegabytes,
        egressWindowSeconds,
        creationTime,
        lastModifiedTime,
        status,
        sumologicEvent,
        vwbWorkspaceId,
        vwbVmName,
        gcpProjectId,
        vwbIncidentCount,
        isVwb);
  }
}
