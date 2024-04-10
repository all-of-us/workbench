package org.pmiops.workbench.db.model;

import java.sql.Timestamp;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = "new_user_satisfaction_survey_one_time_code")
public class DbNewUserSatisfactionSurveyOneTimeCode {
  private UUID id;
  private DbUser user;
  private Timestamp creationTime;
  private Timestamp usedTime;

  @Id
  @GeneratedValue(generator = "uuid2")
  @GenericGenerator(name = "uuid2", strategy = "uuid2")
  @Column(
      name = "new_user_satisfaction_survey_one_time_code_id",
      columnDefinition = "VARCHAR(36)",
      nullable = false)
  @Type(type = "org.hibernate.type.UUIDCharType")
  public UUID getId() {
    return id;
  }

  public DbNewUserSatisfactionSurveyOneTimeCode setId(UUID id) {
    this.id = id;
    return this;
  }

  @OneToOne
  @JoinColumn(name = "user_id")
  public DbUser getUser() {
    return user;
  }

  public DbNewUserSatisfactionSurveyOneTimeCode setUser(DbUser user) {
    this.user = user;
    return this;
  }

  @CreationTimestamp
  @Column(name = "creation_time", nullable = false, updatable = false)
  public Timestamp getCreationTime() {
    return creationTime;
  }

  public DbNewUserSatisfactionSurveyOneTimeCode setCreationTime(Timestamp creationTime) {
    this.creationTime = creationTime;
    return this;
  }

  @Column(name = "used_time")
  public Timestamp getUsedTime() {
    return usedTime;
  }

  public DbNewUserSatisfactionSurveyOneTimeCode setUsedTime(Timestamp usedTime) {
    this.usedTime = usedTime;
    return this;
  }
}
