package org.pmiops.workbench.db.model;

import java.sql.Timestamp;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "user_data_use_agreement")
public class DbUserDataUseAgreement {
  private long userDataUseAgreementId;
  private long userId;
  private String userGivenName;
  private String userFamilyName;
  private String userInitials;
  private boolean userNameOutOfDate;
  private int dataUseAgreementSignedVersion;
  private Timestamp completionTime;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  // TODO(RW-4391): rename this column with a Liquibase change once we're post-beta launch!
  @Column(name = "user_data_user_agreement_id")
  public long getUserDataUseAgreementId() {
    return userDataUseAgreementId;
  }

  public void setUserDataUseAgreementId(long userDataUseAgreementId) {
    this.userDataUseAgreementId = userDataUseAgreementId;
  }

  @Column(name = "user_id")
  public long getUserId() {
    return userId;
  }

  public void setUserId(long userId) {
    this.userId = userId;
  }

  @Column(name = "user_given_name")
  public String getUserGivenName() {
    return userGivenName;
  }

  public void setUserGivenName(String userGivenName) {
    this.userGivenName = userGivenName;
  }

  @Column(name = "user_family_name")
  public String getUserFamilyName() {
    return userFamilyName;
  }

  public void setUserFamilyName(String userFamilyName) {
    this.userFamilyName = userFamilyName;
  }

  @Column(name = "user_initials")
  public String getUserInitials() {
    return userInitials;
  }

  public void setUserInitials(String userInitials) {
    this.userInitials = userInitials;
  }

  // This is set to 'true' whenever user_given_name or user_family_name are not the same as the
  // given_name or family_name on the User entry referenced by user_id.
  @Column(name = "user_name_out_of_date")
  public boolean isUserNameOutOfDate() {
    return userNameOutOfDate;
  }

  public void setUserNameOutOfDate(boolean userNameOutOfDate) {
    this.userNameOutOfDate = userNameOutOfDate;
  }

  @Column(name = "data_use_agreement_signed_version")
  public int getDataUseAgreementSignedVersion() {
    return dataUseAgreementSignedVersion;
  }

  public void setDataUseAgreementSignedVersion(int dataUseAgreementSignedVersion) {
    this.dataUseAgreementSignedVersion = dataUseAgreementSignedVersion;
  }

  @Column(name = "completion_time")
  public Timestamp getCompletionTime() {
    return completionTime;
  }

  public void setCompletionTime(Timestamp completionTime) {
    this.completionTime = completionTime;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DbUserDataUseAgreement that = (DbUserDataUseAgreement) o;
    return userDataUseAgreementId == that.userDataUseAgreementId
        && userId == that.userId
        && userNameOutOfDate == that.userNameOutOfDate
        && dataUseAgreementSignedVersion == that.dataUseAgreementSignedVersion
        && userGivenName.equals(that.userGivenName)
        && userFamilyName.equals(that.userFamilyName)
        && userInitials.equals(that.userInitials)
        && completionTime.equals(that.completionTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        userDataUseAgreementId,
        userId,
        userGivenName,
        userFamilyName,
        userInitials,
        userNameOutOfDate,
        dataUseAgreementSignedVersion,
        completionTime);
  }
}
