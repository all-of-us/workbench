package org.pmiops.workbench.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.sql.Timestamp;

/**
 * A notification created in Verily Workbench from the All of Us admin banner page. Verily Workbench
 * owns the notification itself; this row records the ID it assigned so we can list what All of Us
 * created and delete it again.
 */
@Entity
@Table(name = "vwb_system_notification")
public class DbVwbSystemNotification {
  private long vwbSystemNotificationId;
  private String vwbNotificationId;
  private String title;
  private String message;
  private String notificationType;
  private String notificationPriority;
  private Timestamp startTime;
  private Timestamp endTime;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "vwb_system_notification_id")
  public long getVwbSystemNotificationId() {
    return vwbSystemNotificationId;
  }

  public DbVwbSystemNotification setVwbSystemNotificationId(long vwbSystemNotificationId) {
    this.vwbSystemNotificationId = vwbSystemNotificationId;
    return this;
  }

  @Column(name = "vwb_notification_id")
  public String getVwbNotificationId() {
    return vwbNotificationId;
  }

  public DbVwbSystemNotification setVwbNotificationId(String vwbNotificationId) {
    this.vwbNotificationId = vwbNotificationId;
    return this;
  }

  @Column(name = "title")
  public String getTitle() {
    return title;
  }

  public DbVwbSystemNotification setTitle(String title) {
    this.title = title;
    return this;
  }

  @Column(name = "message")
  public String getMessage() {
    return message;
  }

  public DbVwbSystemNotification setMessage(String message) {
    this.message = message;
    return this;
  }

  /** BLOCKING or PASSIVE, stored as the Verily Workbench enum value. */
  @Column(name = "notification_type")
  public String getNotificationType() {
    return notificationType;
  }

  public DbVwbSystemNotification setNotificationType(String notificationType) {
    this.notificationType = notificationType;
    return this;
  }

  /** INFO, WARNING or ERROR, stored as the Verily Workbench enum value. */
  @Column(name = "notification_priority")
  public String getNotificationPriority() {
    return notificationPriority;
  }

  public DbVwbSystemNotification setNotificationPriority(String notificationPriority) {
    this.notificationPriority = notificationPriority;
    return this;
  }

  /** Null means the notification is shown immediately. */
  @Column(name = "start_time")
  public Timestamp getStartTime() {
    return startTime;
  }

  public DbVwbSystemNotification setStartTime(Timestamp startTime) {
    this.startTime = startTime;
    return this;
  }

  /** Null means the notification never expires. */
  @Column(name = "end_time")
  public Timestamp getEndTime() {
    return endTime;
  }

  public DbVwbSystemNotification setEndTime(Timestamp endTime) {
    this.endTime = endTime;
    return this;
  }
}
