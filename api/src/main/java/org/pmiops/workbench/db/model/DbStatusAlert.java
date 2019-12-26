package org.pmiops.workbench.db.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

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

  public void setStatusAlertId(long statusAlertId) {
    this.statusAlertId = statusAlertId;
  }

  @Column(name = "title")
  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  @Column(name = "message")
  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  @Column(name = "link")
  public String getLink() {
    return link;
  }

  public void setLink(String link) {
    this.link = link;
  }
}
