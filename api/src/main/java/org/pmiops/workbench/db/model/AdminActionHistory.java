package org.pmiops.workbench.db.model;

import java.sql.Timestamp;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.Clock;
import org.pmiops.workbench.db.dao.AdminActionHistoryDao;

@Entity
@Table(name = "admin_action_history")
public class AdminActionHistory {
  private long historyId;
  private long userId;
  private long targetId;
  private String action;
  private Timestamp timestamp;
  private AdminActionHistoryDao adminActionHistoryDao;
  private Clock clock;

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

  public void setTimestamp() { this.timestamp = new Timestamp(clock.instant().toEpochMilli()); }

  public void logAdminAction(String action, long userId, long targetId) {
    AdminActionHistory adminActionHistory = new AdminActionHistory();
    adminActionHistory.setAction(action);
    adminActionHistory.setUserId(userId);
    adminActionHistory.setTargetId(targetId);
    adminActionHistory.setTimestamp();
    adminActionHistoryDao.save(adminActionHistory);
  }
}
