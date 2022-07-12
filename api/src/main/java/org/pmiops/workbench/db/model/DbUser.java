package org.pmiops.workbench.db.model;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.hash.Hashing;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
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

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.pmiops.workbench.model.Authority;
import org.pmiops.workbench.model.Degree;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "user")
@EntityListeners(AuditingEntityListener.class)
public class DbUser {

  private static final String RUNTIME_NAME_PREFIX = "all-of-us-";
  private static final String PD_NAME_PREFIX = "all-of-us-pd-";
  @VisibleForTesting static final int PD_UUID_SUFFIX_SIZE = 4;

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
  private Timestamp computeSecuritySuspendedUntil;

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
  private String rasLinkLoginGovUsername;
  private DbUserCodeOfConductAgreement duccAgreement;
  private DbDemographicSurveyV2 demographicSurveyV2;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "user_id")
  public long getUserId() {
    return userId;
  }

  public DbUser setUserId(long userId) {
    this.userId = userId;
    return this;
  }

  @Version
  @Column(name = "version")
  public int getVersion() {
    return version;
  }

  public DbUser setVersion(int version) {
    this.version = version;
    return this;
  }

  @Column(name = "creation_nonce")
  public Long getCreationNonce() {
    return creationNonce;
  }

  public DbUser setCreationNonce(Long creationNonce) {
    this.creationNonce = creationNonce;
    return this;
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

  public DbUser setUsername(String userName) {
    this.username = userName;
    return this;
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

  public DbUser setContactEmail(String contactEmail) {
    this.contactEmail = contactEmail;
    return this;
  }

  @Column(name = "given_name")
  public String getGivenName() {
    return givenName;
  }

  public DbUser setGivenName(String givenName) {
    this.givenName = givenName;
    return this;
  }

  @Column(name = "family_name")
  public String getFamilyName() {
    return familyName;
  }

  public DbUser setFamilyName(String familyName) {
    this.familyName = familyName;
    return this;
  }

  @Column(name = "free_tier_credits_limit_dollars_override")
  public Double getFreeTierCreditsLimitDollarsOverride() {
    return freeTierCreditsLimitDollarsOverride;
  }

  public DbUser setFreeTierCreditsLimitDollarsOverride(Double freeTierCreditsLimitDollarsOverride) {
    this.freeTierCreditsLimitDollarsOverride = freeTierCreditsLimitDollarsOverride;
    return this;
  }

  @Column(name = "first_sign_in_time")
  public Timestamp getFirstSignInTime() {
    return firstSignInTime;
  }

  public DbUser setFirstSignInTime(Timestamp firstSignInTime) {
    this.firstSignInTime = firstSignInTime;
    return this;
  }
  // Authorities (special permissions) are granted using api/project.rb set-authority.
  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(name = "authority", joinColumns = @JoinColumn(name = "user_id"))
  @Column(name = "authority")
  public Set<Short> getAuthorities() {
    return authorities;
  }

  public DbUser setAuthorities(Set<Short> newAuthorities) {
    this.authorities = newAuthorities;
    return this;
  }

  @Transient
  public Set<Authority> getAuthoritiesEnum() {
    Set<Short> from = getAuthorities();
    if (from == null) {
      return null;
    }
    return from.stream().map(DbStorageEnums::authorityFromStorage).collect(Collectors.toSet());
  }

  public DbUser setAuthoritiesEnum(Set<Authority> newAuthorities) {
    this.setAuthorities(
        newAuthorities.stream()
            .map(DbStorageEnums::authorityToStorage)
            .collect(Collectors.toSet()));
    return this;
  }

  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(name = "user_degree", joinColumns = @JoinColumn(name = "user_id"))
  @Column(name = "degree")
  public List<Short> getDegrees() {
    return degrees;
  }

  public DbUser setDegrees(List<Short> degree) {
    this.degrees = degree;
    return this;
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

  public DbUser setDegreesEnum(List<Degree> degreeList) {
    this.degrees =
        degreeList.stream().map(DbStorageEnums::degreeToStorage).collect(Collectors.toList());
    return this;
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

  public DbUser setPageVisits(Set<DbPageVisit> newPageVisits) {
    this.pageVisits = newPageVisits;
    return this;
  }

  @Column(name = "demographic_survey_completion_time")
  public Timestamp getDemographicSurveyCompletionTime() {
    return demographicSurveyCompletionTime;
  }

  public DbUser setDemographicSurveyCompletionTime(Timestamp demographicSurveyCompletionTime) {
    this.demographicSurveyCompletionTime = demographicSurveyCompletionTime;
    return this;
  }

  @Column(name = "disabled")
  public boolean getDisabled() {
    return disabled;
  }

  public DbUser setDisabled(boolean disabled) {
    this.disabled = disabled;
    return this;
  }

  @Column(name = "area_of_research")
  public String getAreaOfResearch() {
    return areaOfResearch;
  }

  public DbUser setAreaOfResearch(String areaOfResearch) {
    this.areaOfResearch = areaOfResearch;
    return this;
  }

  @Column(name = "era_commons_linked_nih_username")
  public String getEraCommonsLinkedNihUsername() {
    return eraCommonsLinkedNihUsername;
  }

  public DbUser setEraCommonsLinkedNihUsername(String eraCommonsLinkedNihUsername) {
    this.eraCommonsLinkedNihUsername = eraCommonsLinkedNihUsername;
    return this;
  }

  @Column(name = "era_commons_link_expire_time")
  public Timestamp getEraCommonsLinkExpireTime() {
    return eraCommonsLinkExpireTime;
  }

  public DbUser setEraCommonsLinkExpireTime(Timestamp eraCommonsLinkExpireTime) {
    this.eraCommonsLinkExpireTime = eraCommonsLinkExpireTime;
    return this;
  }

  @Column(name = "ras_link_login_gov_username")
  public String getRasLinkLoginGovUsername() {
    return rasLinkLoginGovUsername;
  }

  public DbUser setRasLinkLoginGovUsername(String rasLinkLoginGovUsername) {
    this.rasLinkLoginGovUsername = rasLinkLoginGovUsername;
    return this;
  }

  @OneToOne(
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY,
      mappedBy = "user")
  public DbUserCodeOfConductAgreement getDuccAgreement() {
    return duccAgreement;
  }

  public DbUser setDuccAgreement(DbUserCodeOfConductAgreement duccAgreement) {
    this.duccAgreement = duccAgreement;
    return this;
  }

  @OneToOne(
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY,
      mappedBy = "user")
  public DbDemographicSurvey getDemographicSurvey() {
    return demographicSurvey;
  }

  public DbUser setDemographicSurvey(DbDemographicSurvey demographicSurvey) {
    this.demographicSurvey = demographicSurvey;
    return this;
  }

  @LastModifiedDate
  @Column(name = "last_modified_time")
  public Timestamp getLastModifiedTime() {
    return lastModifiedTime;
  }

  @VisibleForTesting
  public DbUser setLastModifiedTime(Timestamp lastModifiedTime) {
    this.lastModifiedTime = lastModifiedTime;
    return this;
  }

  @CreatedDate
  @Column(name = "creation_time")
  public Timestamp getCreationTime() {
    return creationTime;
  }

  @VisibleForTesting
  public DbUser setCreationTime(Timestamp creationTime) {
    this.creationTime = creationTime;
    return this;
  }

  @Column(name = "professional_url")
  public String getProfessionalUrl() {
    return professionalUrl;
  }

  public DbUser setProfessionalUrl(String professionalUrl) {
    this.professionalUrl = professionalUrl;
    return this;
  }

  @OneToOne(
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY,
      mappedBy = "user")
  public DbAddress getAddress() {
    return address;
  }

  public DbUser setAddress(DbAddress address) {
    this.address = address;
    return this;
  }

  @Column(name = "compute_security_suspended_until")
  public Timestamp getComputeSecuritySuspendedUntil() {
    return computeSecuritySuspendedUntil;
  }

  public DbUser setComputeSecuritySuspendedUntil(Timestamp computeSecuritySuspendedUntil) {
    this.computeSecuritySuspendedUntil = computeSecuritySuspendedUntil;
    return this;
  }

  @OneToOne(
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY,
      mappedBy = "user")
  public DbDemographicSurveyV2 getDemographicSurveyV2() {
    return demographicSurveyV2;
  }

  public DbUser setDemographicSurveyV2(DbDemographicSurveyV2 demographicSurveyV2) {
    this.demographicSurveyV2 = demographicSurveyV2;
    return this;
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
    return RUNTIME_NAME_PREFIX + getUserId();
  }

  @Transient
  public String getUserPDNamePrefix() {
    return PD_NAME_PREFIX + getUserId();
  }

  /** Returns a name for the Persistent Disk to be created for this user. */
  @Transient
  public String generatePDName() {
    String randomString =
        Hashing.sha256()
            .hashUnencodedChars(UUID.randomUUID().toString())
            .toString()
            .substring(0, PD_UUID_SUFFIX_SIZE);
    return getUserPDNamePrefix() + "-" + randomString;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;

    if (!(o instanceof DbUser)) return false;

    DbUser dbUser = (DbUser) o;

    return new EqualsBuilder().append(userId, dbUser.userId)
            .append(disabled, dbUser.disabled)
            .append(username, dbUser.username)
            .append(contactEmail, dbUser.contactEmail)
            .append(freeTierCreditsLimitDollarsOverride, dbUser.freeTierCreditsLimitDollarsOverride)
            .append(firstSignInTime, dbUser.firstSignInTime)
            .append(creationTime, dbUser.creationTime)
            .append(givenName, dbUser.givenName)
            .append(familyName, dbUser.familyName)
            .append(address, dbUser.address).isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37).append(userId).append(username).append(contactEmail).append(freeTierCreditsLimitDollarsOverride).append(firstSignInTime).append(disabled).append(creationTime).append(givenName).append(familyName).append(address).toHashCode();
  }
}
