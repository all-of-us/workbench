package org.pmiops.workbench.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.sql.Timestamp;

@Entity
@Table(name = "user_group_action")
public class DbUserGroupAction {

  public enum UserGroupActionStatus {
    INCOMPLETE,
    COMPLETE,
    FAILED
  }

  private long id;
  private String userEmail;
  private long institutionId;
  private String groupName;
  private Timestamp addedTime;
  private Timestamp completedTime;
  private String userGroupAction;
  private String userGroupActionStatus;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  public long getId() {
    return id;
  }

  public DbUserGroupAction setId(long id) {
    this.id = id;
    return this;
  }

  @Column(name = "user_email", nullable = false)
  public String getUserEmail() {
    return userEmail;
  }

  public DbUserGroupAction setUserEmail(String userEmail) {
    this.userEmail = userEmail;
    return this;
  }

  @Column(name = "institution_id")
  public long getInstitutionId() {
    return institutionId;
  }

  public DbUserGroupAction setInstitutionId(long institutionId) {
    this.institutionId = institutionId;
    return this;
  }

  @Column(name = "group_name")
  public String getGroupName() {
    return groupName;
  }

  public DbUserGroupAction setGroupName(String groupName) {
    this.groupName = groupName;
    return this;
  }

  @Column(name = "added_time")
  public Timestamp getAddedTime() {
    return addedTime;
  }

  public DbUserGroupAction setAddedTime(Timestamp addedTime) {
    this.addedTime = addedTime;
    return this;
  }

  @Column(name = "completed_time")
  public Timestamp getCompletedTime() {
    return completedTime;
  }

  public DbUserGroupAction setCompletedTime(Timestamp completedTime) {
    this.completedTime = completedTime;
    return this;
  }

  @Column(name = "action")
  public String getUserGroupAction() {
    return userGroupAction;
  }

  public DbUserGroupAction setUserGroupAction(String userGroupAction) {
    this.userGroupAction = userGroupAction;
    return this;
  }

  @Column(name = "status")
  public String getUserGroupActionStatus() {
    return userGroupActionStatus;
  }

  public DbUserGroupAction setUserGroupActionStatus(String userGroupActionStatus) {
    this.userGroupActionStatus = userGroupActionStatus;
    return this;
  }
}
