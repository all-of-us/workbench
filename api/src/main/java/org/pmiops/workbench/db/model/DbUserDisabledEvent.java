package org.pmiops.workbench.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.sql.Timestamp;

@Entity
@Table(name = "user_disabled")
public class DbUserDisabledEvent {

  public enum DbUserDisabledStatus {
    DISABLED,
    ENABLED
  }

  private long id;
  private long userId;
  private String updatedBy;
  private Timestamp updateTime;
  private String adminComment;
  private DbUserDisabledStatus status;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  public long getId() {
    return id;
  }

  public DbUserDisabledEvent setId(long id) {
    this.id = id;
    return this;
  }

  @Column(name = "user_id", nullable = false)
  public long getUserId() {
    return userId;
  }

  public DbUserDisabledEvent setUserId(long userId) {
    this.userId = userId;
    return this;
  }

  @Column(name = "update_time")
  public Timestamp getUpdateTime() {
    return updateTime;
  }

  public DbUserDisabledEvent setUpdateTime(Timestamp updateTime) {
    this.updateTime = updateTime;
    return this;
  }

  @Column(name = "updated_by")
  public String getUpdatedBy() {
    return updatedBy;
  }

  public DbUserDisabledEvent setUpdatedBy(String updatedBy) {
    this.updatedBy = updatedBy;
    return this;
  }

  @Column(name = "adminComment")
  public String getAdminComment() {
    return adminComment;
  }

  public DbUserDisabledEvent setAdminComment(String adminComment) {
    this.adminComment = adminComment;
    return this;
  }

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  public DbUserDisabledEvent.DbUserDisabledStatus getStatus() {
    return status;
  }

  public DbUserDisabledEvent setStatus(DbUserDisabledEvent.DbUserDisabledStatus s) {
    this.status = s;
    return this;
  }
}
