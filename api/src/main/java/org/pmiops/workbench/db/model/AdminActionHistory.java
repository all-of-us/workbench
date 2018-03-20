package org.pmiops.workbench.db.model;

import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.Clock;
import org.pmiops.workbench.db.dao.AdminActionHistoryDao;
import org.springframework.beans.factory.annotation.Autowired;

@Entity
@Table(name = "admin_action_history")
public class AdminActionHistory {
  private long historyId;
  private long userId;
  private long targetId;
  private String action;
  private Timestamp timestamp;
  private static AdminActionHistoryDao adminActionHistoryDao;

  @Id
  @Column(name = "history_id")
  public long getHistoryId() {
    return historyId;
  }

  public void setHistoryId(long historyId) {
    this.historyId = historyId;
  }

  @Column(name = "user_id")
  public long getUserId() {
    return userId;
  }

  public void setUserId(long userId) {
    this.userId = userId;
  }

  @Column(name = "target_id")
  public long getTargetId() { return targetId; }

  public void setTargetId(long targetId) { this.targetId = targetId; }

  @Column(name = "action")
  public String getAction() { return action; }

  public void setAction(String action) { this.action = action; }

  @Column(name = "timestamp")
  public Timestamp getTimestamp() { return timestamp; }

  public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
  public void setTimestamp() {
    this.timestamp = new Timestamp(Instant.now().toEpochMilli());
  }
}
