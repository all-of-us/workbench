package org.pmiops.workbench.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "status_alert")
public class DbStatusAlert {
  private long statusAlertId;
  private String title;
  private String message;
  private String link;

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
}
