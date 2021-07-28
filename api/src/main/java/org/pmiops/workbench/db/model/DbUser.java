package org.pmiops.workbench.db.model;

import com.google.common.annotations.VisibleForTesting;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Version;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.pmiops.workbench.model.Authority;
import org.pmiops.workbench.model.Degree;

@Entity
@Table(name = "user")
public class DbUser {

  private static final String RUNTIME_NAME_PREFIX = "all-of-us-";
  private static final String PD_NAME_PREFIX = "all-of-us-pd-";

  // user "system account" fields besides those related to access modules

  private long userId;
  private int version;
  // A nonce which can be used during the account creation flow to verify
  // unauthenticated API calls after account creation, but before initial login.
  private Long creationNonce;
  // The full G Suite email address that the user signs in with, e.g. "joe@researchallofus.org".
  private String username;
  // The email address that can be used to contact the user.
  private String contactEmail;
  private Double freeTierCreditsLimitDollarsOverride = null;
  private Timestamp firstSignInTime;
  private Set<Short> authorities = new HashSet<>();
  private boolean disabled;
  private Set<DbPageVisit> pageVisits = new HashSet<>();
  private Timestamp demographicSurveyCompletionTime;
  private Timestamp creationTime;
  private Timestamp lastModifiedTime;

  // user-editable Profile fields

  private String givenName;
  private String familyName;
  private String professionalUrl;
  private List<Short> degrees;
  private String areaOfResearch;
  private DbDemographicSurvey demographicSurvey;
  private DbAddress address;

  // Access module fields go here. See http://broad.io/aou-access-modules for docs.

  private String eraCommonsLinkedNihUsername;
  private Timestamp eraCommonsLinkExpireTime;
  private Timestamp eraCommonsCompletionTime;
  private Timestamp eraCommonsBypassTime;

  private String rasLinkLoginGovUsername;
  private Timestamp rasLinkLoginGovCompletionTime;
  private Timestamp rasLinkLoginGovExpireTime;
  private Timestamp rasLinkLoginGovBypassTime;

  private Timestamp dataUseAgreementCompletionTime;
  private Timestamp dataUseAgreementBypassTime;
  private Integer dataUseAgreementSignedVersion;

  private Timestamp complianceTrainingCompletionTime;
  private Timestamp complianceTrainingBypassTime;
  private Timestamp complianceTrainingExpirationTime;

  private Timestamp twoFactorAuthCompletionTime;
  private Timestamp twoFactorAuthBypassTime;

  private Timestamp profileLastConfirmedTime;

  private Timestamp publicationsLastConfirmedTime;

  // potentially obsolete access module fields.  These are likely to be deleted in the near future.
  // Moodle badges are indexed by username, not this value.  See ComplianceService.
  @Deprecated private Integer moodleId;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "user_id")
  public long getUserId() {
    return userId;
  }

  public void setUserId(long userId) {
    this.userId = userId;
  }

  @Version
  @Column(name = "version")
  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
  }

  @Column(name = "creation_nonce")
  public Long getCreationNonce() {
    return creationNonce;
  }

  public void setCreationNonce(Long creationNonce) {
    this.creationNonce = creationNonce;
  }

  /**
   * Returns the user's full G Suite email address, e.g. "joe@researchallofus.org". This is named
   * "username" in this entity class to distinguish it from getContactEmail, which is the user's
   * designated contact email address.
   *
   * @return
   */
  @Column(name = "email")
  public String getUsername() {
    return username;
  }

  public void setUsername(String userName) {
    this.username = userName;
  }

  /**
   * Returns the user's designated contact email address, e.g. "joe@gmail.com".
   *
   * @return
   */
  @Column(name = "contact_email")
  public String getContactEmail() {
    return contactEmail;
  }

  public void setContactEmail(String contactEmail) {
    this.contactEmail = contactEmail;
  }

  @Column(name = "given_name")
  public String getGivenName() {
    return givenName;
  }

  public void setGivenName(String givenName) {
    this.givenName = givenName;
  }

  @Column(name = "family_name")
  public String getFamilyName() {
    return familyName;
  }

  public void setFamilyName(String familyName) {
    this.familyName = familyName;
  }

  @Column(name = "free_tier_credits_limit_dollars_override")
  public Double getFreeTierCreditsLimitDollarsOverride() {
    return freeTierCreditsLimitDollarsOverride;
  }

  public void setFreeTierCreditsLimitDollarsOverride(Double freeTierCreditsLimitDollarsOverride) {
    this.freeTierCreditsLimitDollarsOverride = freeTierCreditsLimitDollarsOverride;
  }

  @Column(name = "first_sign_in_time")
  public Timestamp getFirstSignInTime() {
    return firstSignInTime;
  }

  public void setFirstSignInTime(Timestamp firstSignInTime) {
    this.firstSignInTime = firstSignInTime;
  }

  // Authorities (special permissions) are granted using api/project.rb set-authority.
  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(name = "authority", joinColumns = @JoinColumn(name = "user_id"))
  @Column(name = "authority")
  public Set<Short> getAuthorities() {
    return authorities;
  }

  public void setAuthorities(Set<Short> newAuthorities) {
    this.authorities = newAuthorities;
  }

  @Transient
  public Set<Authority> getAuthoritiesEnum() {
    Set<Short> from = getAuthorities();
    if (from == null) {
      return null;
    }
    return from.stream().map(DbStorageEnums::authorityFromStorage).collect(Collectors.toSet());
  }

  public void setAuthoritiesEnum(Set<Authority> newAuthorities) {
    this.setAuthorities(
        newAuthorities.stream()
            .map(DbStorageEnums::authorityToStorage)
            .collect(Collectors.toSet()));
  }

  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(name = "user_degree", joinColumns = @JoinColumn(name = "user_id"))
  @Column(name = "degree")
  public List<Short> getDegrees() {
    return degrees;
  }

  public void setDegrees(List<Short> degree) {
    this.degrees = degree;
  }

  @Transient
  public List<Degree> getDegreesEnum() {
    if (degrees == null) {
      return null;
    }
    return this.degrees.stream()
        .map(DbStorageEnums::degreeFromStorage)
        .collect(Collectors.toList());
  }

  public void setDegreesEnum(List<Degree> degreeList) {
    this.degrees =
        degreeList.stream().map(DbStorageEnums::degreeToStorage).collect(Collectors.toList());
  }

  @OneToMany(
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY,
      mappedBy = "user")
  @Column(name = "page_id")
  public Set<DbPageVisit> getPageVisits() {
    return pageVisits;
  }

  public void setPageVisits(Set<DbPageVisit> newPageVisits) {
    this.pageVisits = newPageVisits;
  }

  @Column(name = "demographic_survey_completion_time")
  public Timestamp getDemographicSurveyCompletionTime() {
    return demographicSurveyCompletionTime;
  }

  public void setDemographicSurveyCompletionTime(Timestamp demographicSurveyCompletionTime) {
    this.demographicSurveyCompletionTime = demographicSurveyCompletionTime;
  }

  @Column(name = "disabled")
  public boolean getDisabled() {
    return disabled;
  }

  public void setDisabled(boolean disabled) {
    this.disabled = disabled;
  }

  @Column(name = "area_of_research")
  public String getAreaOfResearch() {
    return areaOfResearch;
  }

  public void setAreaOfResearch(String areaOfResearch) {
    this.areaOfResearch = areaOfResearch;
  }

  @Deprecated
  @Column(name = "moodle_id")
  public Integer getMoodleId() {
    return moodleId;
  }

  @Deprecated
  public void setMoodleId(Integer moodleId) {
    this.moodleId = moodleId;
  }

  @Column(name = "era_commons_linked_nih_username")
  public String getEraCommonsLinkedNihUsername() {
    return eraCommonsLinkedNihUsername;
  }

  public void setEraCommonsLinkedNihUsername(String eraCommonsLinkedNihUsername) {
    this.eraCommonsLinkedNihUsername = eraCommonsLinkedNihUsername;
  }

  @Column(name = "era_commons_link_expire_time")
  public Timestamp getEraCommonsLinkExpireTime() {
    return eraCommonsLinkExpireTime;
  }

  public void setEraCommonsLinkExpireTime(Timestamp eraCommonsLinkExpireTime) {
    this.eraCommonsLinkExpireTime = eraCommonsLinkExpireTime;
  }

  @Column(name = "era_commons_completion_time")
  public Timestamp getEraCommonsCompletionTime() {
    return eraCommonsCompletionTime;
  }

  public void setEraCommonsCompletionTime(Timestamp eraCommonsCompletionTime) {
    this.eraCommonsCompletionTime = eraCommonsCompletionTime;
  }

  @Column(name = "ras_link_login_gov_username")
  public String getRasLinkLoginGovUsername() {
    return rasLinkLoginGovUsername;
  }

  public void setRasLinkLoginGovUsername(String rasLinkLoginGovUsername) {
    this.rasLinkLoginGovUsername = rasLinkLoginGovUsername;
  }

  @Column(name = "ras_link_login_gov_completion_time")
  public Timestamp getRasLinkLoginGovCompletionTime() {
    return rasLinkLoginGovCompletionTime;
  }

  public void setRasLinkLoginGovCompletionTime(Timestamp rasLinkLoginGovCompletionTime) {
    this.rasLinkLoginGovCompletionTime = rasLinkLoginGovCompletionTime;
  }

  @Column(name = "ras_link_login_gov_bypass_time")
  public Timestamp getRasLinkLoginGovBypassTime() {
    return rasLinkLoginGovBypassTime;
  }

  public void setRasLinkLoginGovBypassTime(Timestamp rasLinkLoginGovBypassTime) {
    this.rasLinkLoginGovBypassTime = rasLinkLoginGovBypassTime;
  }

  @Column(name = "data_use_agreement_completion_time")
  public Timestamp getDataUseAgreementCompletionTime() {
    return dataUseAgreementCompletionTime;
  }

  public void setDataUseAgreementCompletionTime(Timestamp dataUseAgreementCompletionTime) {
    this.dataUseAgreementCompletionTime = dataUseAgreementCompletionTime;
  }

  @Column(name = "data_use_agreement_bypass_time")
  public Timestamp getDataUseAgreementBypassTime() {
    return dataUseAgreementBypassTime;
  }

  public void setDataUseAgreementBypassTime(Timestamp dataUseAgreementBypassTime) {
    this.dataUseAgreementBypassTime = dataUseAgreementBypassTime;
  }

  @Column(name = "data_use_agreement_signed_version")
  public Integer getDataUseAgreementSignedVersion() {
    return dataUseAgreementSignedVersion;
  }

  public void setDataUseAgreementSignedVersion(Integer dataUseAgreementSignedVersion) {
    this.dataUseAgreementSignedVersion = dataUseAgreementSignedVersion;
  }

  @Column(name = "compliance_training_completion_time")
  public Timestamp getComplianceTrainingCompletionTime() {
    return complianceTrainingCompletionTime;
  }

  public void setComplianceTrainingCompletionTime(Timestamp complianceTrainingCompletionTime) {
    this.complianceTrainingCompletionTime = complianceTrainingCompletionTime;
  }

  public void clearComplianceTrainingCompletionTime() {
    this.complianceTrainingCompletionTime = null;
  }

  @Column(name = "compliance_training_bypass_time")
  public Timestamp getComplianceTrainingBypassTime() {
    return complianceTrainingBypassTime;
  }

  public void setComplianceTrainingBypassTime(Timestamp complianceTrainingBypassTime) {
    this.complianceTrainingBypassTime = complianceTrainingBypassTime;
  }

  @Column(name = "compliance_training_expiration_time")
  public Timestamp getComplianceTrainingExpirationTime() {
    return complianceTrainingExpirationTime;
  }

  public void setComplianceTrainingExpirationTime(Timestamp complianceTrainingExpirationTime) {
    this.complianceTrainingExpirationTime = complianceTrainingExpirationTime;
  }

  public void clearComplianceTrainingExpirationTime() {
    this.complianceTrainingExpirationTime = null;
  }

  @Column(name = "era_commons_bypass_time")
  public Timestamp getEraCommonsBypassTime() {
    return eraCommonsBypassTime;
  }

  public void setEraCommonsBypassTime(Timestamp eraCommonsBypassTime) {
    this.eraCommonsBypassTime = eraCommonsBypassTime;
  }

  @Column(name = "two_factor_auth_completion_time")
  public Timestamp getTwoFactorAuthCompletionTime() {
    return twoFactorAuthCompletionTime;
  }

  public void setTwoFactorAuthCompletionTime(Timestamp twoFactorAuthCompletionTime) {
    this.twoFactorAuthCompletionTime = twoFactorAuthCompletionTime;
  }

  @Column(name = "two_factor_auth_bypass_time")
  public Timestamp getTwoFactorAuthBypassTime() {
    return twoFactorAuthBypassTime;
  }

  public void setTwoFactorAuthBypassTime(Timestamp twoFactorAuthBypassTime) {
    this.twoFactorAuthBypassTime = twoFactorAuthBypassTime;
  }

  @OneToOne(
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY,
      mappedBy = "user")
  public DbDemographicSurvey getDemographicSurvey() {
    return demographicSurvey;
  }

  public void setDemographicSurvey(DbDemographicSurvey demographicSurvey) {
    this.demographicSurvey = demographicSurvey;
  }

  @UpdateTimestamp
  @Column(name = "last_modified_time")
  public Timestamp getLastModifiedTime() {
    return lastModifiedTime;
  }

  @VisibleForTesting
  public void setLastModifiedTime(Timestamp lastModifiedTime) {
    this.lastModifiedTime = lastModifiedTime;
  }

  @CreationTimestamp
  @Column(name = "creation_time")
  public Timestamp getCreationTime() {
    return creationTime;
  }

  @VisibleForTesting
  public void setCreationTime(Timestamp creationTime) {
    this.creationTime = creationTime;
  }

  @Column(name = "professional_url")
  public String getProfessionalUrl() {
    return professionalUrl;
  }

  public void setProfessionalUrl(String professionalUrl) {
    this.professionalUrl = professionalUrl;
  }

  @OneToOne(
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY,
      mappedBy = "user")
  public DbAddress getAddress() {
    return address;
  }

  public void setAddress(DbAddress address) {
    this.address = address;
  }

  @Column(name = "profile_last_confirmed_time")
  public Timestamp getProfileLastConfirmedTime() {
    return profileLastConfirmedTime;
  }

  public void setProfileLastConfirmedTime(Timestamp profileLastConfirmedTime) {
    this.profileLastConfirmedTime = profileLastConfirmedTime;
  }

  @Column(name = "publications_last_confirmed_time")
  public Timestamp getPublicationsLastConfirmedTime() {
    return publicationsLastConfirmedTime;
  }

  public void setPublicationsLastConfirmedTime(Timestamp publicationsLastConfirmedTime) {
    this.publicationsLastConfirmedTime = publicationsLastConfirmedTime;
  }

  // null-friendly versions of equals() and hashCode() for DbVerifiedInstitutionalAffiliation
  // can be removed once we have a proper equals() / hashCode()

  public static boolean equalUsernames(DbUser a, DbUser b) {
    return Objects.equals(
        Optional.ofNullable(a).map(DbUser::getUsername),
        Optional.ofNullable(b).map(DbUser::getUsername));
  }

  public static int usernameHashCode(DbUser dbUser) {
    return (dbUser == null) ? 0 : Objects.hashCode(dbUser.getUsername());
  }

  /** Returns a name for the VM / cluster to be created for this user. */
  @Transient
  public String getRuntimeName() {
    return RUNTIME_NAME_PREFIX + getUserId() + "-" + UUID.randomUUID().toString();
  }

  /** Returns a name for the Persistent Disk to be created for this user. */
  @Transient
  public String getPDName() {
    return PD_NAME_PREFIX + getUserId() + "-" + UUID.randomUUID().toString();
  }
}
