package org.pmiops.workbench.db.model;

import java.sql.Timestamp;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "new_user_satisfaction_survey_response")
@EntityListeners(AuditingEntityListener.class)
public class DbNewUserSatisfactionSurveyResponse {
  private long id;
  private DbUser user;
  private Timestamp creationTime;
  private Satisfaction satisfaction;
  private String additionalInfo;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "new_user_satisfaction_survey_response_id", nullable = false)
  public long getId() {
    return id;
  }

  public DbNewUserSatisfactionSurveyResponse setId(long new_user_satisfaction_survey_response_id) {
    this.id = new_user_satisfaction_survey_response_id;
    return this;
  }

  @OneToOne
  @JoinColumn(name = "user_id", nullable = false)
  public DbUser getUser() {
    return user;
  }

  public DbNewUserSatisfactionSurveyResponse setUser(DbUser user) {
    this.user = user;
    return this;
  }

  @CreationTimestamp
  @Column(name = "creation_time", nullable = false)
  public Timestamp getCreationTime() {
    return creationTime;
  }

  public DbNewUserSatisfactionSurveyResponse setCreationTime(Timestamp creationTime) {
    this.creationTime = creationTime;
    return this;
  }

  @Enumerated(EnumType.STRING)
  @Column(name = "satisfaction", nullable = false)
  public Satisfaction getSatisfaction() {
    return satisfaction;
  }

  public DbNewUserSatisfactionSurveyResponse setSatisfaction(Satisfaction satisfaction) {
    this.satisfaction = satisfaction;
    return this;
  }

  @Column(name = "additional_info", nullable = false, length = 500)
  public String getAdditionalInfo() {
    return additionalInfo;
  }

  public DbNewUserSatisfactionSurveyResponse setAdditionalInfo(String additionalInfo) {
    this.additionalInfo = additionalInfo;
    return this;
  }

  public enum Satisfaction {
    VERY_UNSATISFIED,
    UNSATISFIED,
    NEUTRAL,
    SATISFIED,
    VERY_SATISFIED
  }
}
