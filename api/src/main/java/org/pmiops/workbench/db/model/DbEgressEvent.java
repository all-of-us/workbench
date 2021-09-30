package org.pmiops.workbench.db.model;

import java.sql.Timestamp;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "egress_event")
public class DbEgressEvent {
  public enum EgressEventStatus {
    PENDING,
    REMEDIATED,
    VERIFIED_FALSE_POSITIVE
  }

  private long egressEventId;
  private DbUser user;
  private DbWorkspace workspace;

  private Float egressMegabytes;

  private Long egressWindowSeconds;
  private Timestamp creationTime;
  private Timestamp lastModifiedTime;
  private EgressEventStatus status;
  private String eventJson;

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

  @CreationTimestamp
  @Column(name = "creation_time", nullable = false)
  public Timestamp getCreationTime() {
    return creationTime;
  }

  public DbEgressEvent setCreationTime(Timestamp creationTime) {
    this.creationTime = creationTime;
    return this;
  }

  @UpdateTimestamp
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

  @Column(name = "event_json")
  public String getEventJson() {
    return eventJson;
  }

  public DbEgressEvent setEventJson(String eventJson) {
    this.eventJson = eventJson;
    return this;
  }

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  public EgressEventStatus getStatus() {
    return status;
  }

  public DbEgressEvent setStatus(EgressEventStatus s) {
    this.status = s;
    return this;
  }
}
