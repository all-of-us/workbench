package org.pmiops.workbench.db.model;

import java.sql.Timestamp;
import java.util.Objects;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "user_terms_of_service")
@EntityListeners(AuditingEntityListener.class)
public class DbUserTermsOfService {
  private long userTermsOfServiceId;
  private long userId;
  private int tosVersion;

  // set automatically on DB row creation
  private Timestamp aouAgreementTime;
  // NOT set automatically because we create the Terra user after recording the AoU TOS agreement
  private Timestamp terraAgreementTime;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "user_terms_of_service_id")
  public long getUserTermsOfServiceId() {
    return userTermsOfServiceId;
  }

  public DbUserTermsOfService setUserTermsOfServiceId(long userTermsOfServiceId) {
    this.userTermsOfServiceId = userTermsOfServiceId;
    return this;
  }

  @Column(name = "user_id", nullable = false)
  public long getUserId() {
    return userId;
  }

  public DbUserTermsOfService setUserId(long userId) {
    this.userId = userId;
    return this;
  }

  @Column(name = "tos_version", nullable = false)
  public int getTosVersion() {
    return tosVersion;
  }

  public DbUserTermsOfService setTosVersion(int tosVersion) {
    this.tosVersion = tosVersion;
    return this;
  }

  // This column is non-nullable in our CloudSQL schema (see Liquibase changelog #123),
  // but marking it as nullable=false in the column annotation causes unit tests to fail. This
  // may be due to the way the in-memory H2 database interacts with a column that is auto-populated
  // via the Hibernate CreationTimestamp annotation.
  @Column(name = "agreement_time")
  @CreatedDate
  public Timestamp getAouAgreementTime() {
    return aouAgreementTime;
  }

  public DbUserTermsOfService setAouAgreementTime(Timestamp aouAgreementTime) {
    this.aouAgreementTime = aouAgreementTime;
    return this;
  }

  @Column(name = "terra_agreement_time")
  public Timestamp getTerraAgreementTime() {
    return terraAgreementTime;
  }

  public DbUserTermsOfService setTerraAgreementTime(Timestamp terraAgreementTime) {
    this.terraAgreementTime = terraAgreementTime;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DbUserTermsOfService that = (DbUserTermsOfService) o;
    return userId == that.userId
        && tosVersion == that.tosVersion
        && Objects.equals(aouAgreementTime, that.aouAgreementTime)
        && Objects.equals(terraAgreementTime, that.terraAgreementTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, tosVersion, aouAgreementTime, terraAgreementTime);
  }
}
