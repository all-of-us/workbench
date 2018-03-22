package org.pmiops.workbench.db.model;

import java.sql.Timestamp;
import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "admin_action_history")
public class AdminActionHistory {
  private long historyId;
  private long adminUserId;
  private Long targetUserId;
  private Long targetWorkspaceId;
  private String targetAction;
  private String oldValue;
  private String newValue;
  private Timestamp timestamp;

  @Id
  @Column(name = "history_id")
  public long getHistoryId() {
    return historyId;
  }

  public void setHistoryId(long historyId) {
    this.historyId = historyId;
  }

  @Column(name = "admin_user_id")
  public long getAdminUserId() {
    return adminUserId;
  }

  public void setAdminUserId(long adminUserId) {
    this.adminUserId = adminUserId;
  }

  @Column(name = "target_user_id")
  public Long getTargetUserId() {
    return targetUserId;
  }

  public void setTargetUserId(Long targetUserId) {
    this.targetUserId = targetUserId;
  }

  @Column(name = "target_workspace_id")
  public Long getTargetWorkspaceId() {
    return targetWorkspaceId;
  }

  public void setTargetWorkspaceId(Long targetWorkspaceId) {
    this.targetWorkspaceId = targetWorkspaceId;
  }

  @Column(name = "target_action")
  public String getTargetAction() {
    return targetAction;
  }

  public void setTargetAction(String targetAction) {
    this.targetAction = targetAction;
  }

  @Column(name = "old_value_as_string")
  public String getOldValue() {
    return oldValue;
  }

  public void setOldValue(String oldValue) {
    this.oldValue = oldValue;
  }

  @Column(name = "new_value_as_string")
  public String getNewValue() {
    return newValue;
  }

  public void setNewValue(String newValue) {
    this.newValue = newValue;
  }

  @Column(name = "timestamp")
  public Timestamp getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Timestamp timestamp) {
    this.timestamp = timestamp;
  }
  public void setTimestamp() {
    this.timestamp = new Timestamp(Instant.now().toEpochMilli());
  }
}
