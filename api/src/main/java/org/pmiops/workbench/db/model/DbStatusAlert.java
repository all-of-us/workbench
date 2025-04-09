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
@Table(name = "status_alert")
public class DbStatusAlert {
  private long statusAlertId;
  private String title;
  private String message;
  private String link;
  private DbAlertLocation alertLocation;
  private Timestamp startTime;
  private Timestamp endTime;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "status_alert_id")
  public long getStatusAlertId() {
    return statusAlertId;
  }

  public DbStatusAlert setStatusAlertId(long statusAlertId) {
    this.statusAlertId = statusAlertId;
    return this;
  }

  @Column(name = "title")
  public String getTitle() {
    return title;
  }

  public DbStatusAlert setTitle(String title) {
    this.title = title;
    return this;
  }

  @Column(name = "message")
  public String getMessage() {
    return message;
  }

  public DbStatusAlert setMessage(String message) {
    this.message = message;
    return this;
  }

  @Column(name = "link")
  public String getLink() {
    return link;
  }

  public DbStatusAlert setLink(String link) {
    this.link = link;
    return this;
  }

  @Enumerated(EnumType.STRING)
  @Column(name = "alert_location", nullable = false)
  public DbAlertLocation getAlertLocation() {
    return alertLocation;
  }

  public DbStatusAlert setAlertLocation(DbAlertLocation alertLocation) {
    this.alertLocation = alertLocation;
    return this;
  }

  @Column(name = "start_time")
  public Timestamp getStartTime() {
    return startTime;
  }

  public DbStatusAlert setStartTime(Timestamp startTime) {
    this.startTime = startTime;
    return this;
  }

  @Column(name = "end_time")
  public Timestamp getEndTime() {
    return endTime;
  }

  public DbStatusAlert setEndTime(Timestamp endTime) {
    this.endTime = endTime;
    return this;
  }

  public enum DbAlertLocation {
    BEFORE_LOGIN,
    AFTER_LOGIN,
  }
}
