package org.pmiops.workbench.db.model;

import java.sql.Timestamp;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "user_terms_of_service")
@EntityListeners(AuditingEntityListener.class)
public class DbUserTermsOfService {
  private long userTermsOfServiceId;
  private long userId;
  private int tosVersion;
  private Timestamp agreementTime;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "user_terms_of_service_id")
  public long getUserTermsOfServiceId() {
    return userTermsOfServiceId;
  }

  public void setUserTermsOfServiceId(long userTermsOfServiceId) {
    this.userTermsOfServiceId = userTermsOfServiceId;
  }

  @Column(name = "user_id", nullable = false)
  public long getUserId() {
    return userId;
  }

  public void setUserId(long userId) {
    this.userId = userId;
  }

  @Column(name = "tos_version", nullable = false)
  public int getTosVersion() {
    return tosVersion;
  }

  public void setTosVersion(int tosVersion) {
    this.tosVersion = tosVersion;
  }

  // This column is non-nullable in our CloudSQL schema (see Liquibase changelog #123),
  // but marking it as nullable=false in the column annotation causes unit tests to fail. This
  // may be due to the way the in-memory H2 database interacts with a column that is auto-populated
  // via the Hibernate CreationTimestamp annotation.
  @Column(name = "agreement_time")
  @CreatedDate
  public Timestamp getAgreementTime() {
    return agreementTime;
  }

  public void setAgreementTime(Timestamp agreementTime) {
    this.agreementTime = agreementTime;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DbUserTermsOfService that = (DbUserTermsOfService) o;
    return userTermsOfServiceId == that.userTermsOfServiceId
        && userId == that.userId
        && tosVersion == that.tosVersion
        && Objects.equals(agreementTime, that.agreementTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userTermsOfServiceId, userId, tosVersion, agreementTime);
  }
}
