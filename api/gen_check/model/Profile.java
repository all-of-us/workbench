package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import org.pmiops.workbench.model.Address;
import org.pmiops.workbench.model.Authority;
import org.pmiops.workbench.model.DataAccessLevel;
import org.pmiops.workbench.model.DemographicSurvey;
import org.pmiops.workbench.model.EmailVerificationStatus;
import org.pmiops.workbench.model.InstitutionalAffiliation;
import org.pmiops.workbench.model.PageVisit;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * Profile
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T12:00:54.413-05:00")

public class Profile   {
  @JsonProperty("userId")
  private Long userId = null;

  @JsonProperty("username")
  private String username = null;

  @JsonProperty("creationNonce")
  private String creationNonce = null;

  @JsonProperty("contactEmail")
  private String contactEmail = null;

  @JsonProperty("contactEmailFailure")
  private Boolean contactEmailFailure = false;

  @JsonProperty("firstSignInTime")
  private Long firstSignInTime = null;

  @JsonProperty("dataAccessLevel")
  private DataAccessLevel dataAccessLevel = null;

  @JsonProperty("givenName")
  private String givenName = null;

  @JsonProperty("familyName")
  private String familyName = null;

  @JsonProperty("phoneNumber")
  private String phoneNumber = null;

  @JsonProperty("currentPosition")
  private String currentPosition = null;

  @JsonProperty("organization")
  private String organization = null;

  @JsonProperty("authorities")
  private List<Authority> authorities = null;

  @JsonProperty("pageVisits")
  private List<PageVisit> pageVisits = null;

  @JsonProperty("demographicSurveyCompletionTime")
  private Long demographicSurveyCompletionTime = null;

  @JsonProperty("disabled")
  private Boolean disabled = false;

  @JsonProperty("emailVerificationStatus")
  private EmailVerificationStatus emailVerificationStatus = null;

  @JsonProperty("aboutYou")
  private String aboutYou = null;

  @JsonProperty("areaOfResearch")
  private String areaOfResearch = null;

  @JsonProperty("institutionalAffiliations")
  private List<InstitutionalAffiliation> institutionalAffiliations = null;

  @JsonProperty("demographicSurvey")
  private DemographicSurvey demographicSurvey = null;

  @JsonProperty("idVerificationCompletionTime")
  private Long idVerificationCompletionTime = null;

  @JsonProperty("idVerificationBypassTime")
  private Long idVerificationBypassTime = null;

  @JsonProperty("twoFactorAuthCompletionTime")
  private Long twoFactorAuthCompletionTime = null;

  @JsonProperty("twoFactorAuthBypassTime")
  private Long twoFactorAuthBypassTime = null;

  @JsonProperty("eraCommonsLinkedNihUsername")
  private String eraCommonsLinkedNihUsername = null;

  @JsonProperty("eraCommonsLinkExpireTime")
  private Long eraCommonsLinkExpireTime = 0l;

  @JsonProperty("eraCommonsCompletionTime")
  private Long eraCommonsCompletionTime = null;

  @JsonProperty("eraCommonsBypassTime")
  private Long eraCommonsBypassTime = null;

  @JsonProperty("complianceTrainingCompletionTime")
  private Long complianceTrainingCompletionTime = null;

  @JsonProperty("complianceTrainingBypassTime")
  private Long complianceTrainingBypassTime = null;

  @JsonProperty("betaAccessBypassTime")
  private Long betaAccessBypassTime = null;

  @JsonProperty("betaAccessRequestTime")
  private Long betaAccessRequestTime = null;

  @JsonProperty("emailVerificationCompletionTime")
  private Long emailVerificationCompletionTime = null;

  @JsonProperty("emailVerificationBypassTime")
  private Long emailVerificationBypassTime = null;

  @JsonProperty("dataUseAgreementCompletionTime")
  private Long dataUseAgreementCompletionTime = null;

  @JsonProperty("dataUseAgreementBypassTime")
  private Long dataUseAgreementBypassTime = null;

  @JsonProperty("dataUseAgreementSignedVersion")
  private Integer dataUseAgreementSignedVersion = null;

  @JsonProperty("address")
  private Address address = null;

  @JsonProperty("freeTierUsage")
  private Double freeTierUsage = null;

  @JsonProperty("freeTierDollarQuota")
  private Double freeTierDollarQuota = null;

  public Profile userId(Long userId) {
    this.userId = userId;
    return this;
  }

   /**
   * researchallofus userId
   * @return userId
  **/
  @ApiModelProperty(value = "researchallofus userId")


  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public Profile username(String username) {
    this.username = username;
    return this;
  }

   /**
   * researchallofus username
   * @return username
  **/
  @ApiModelProperty(required = true, value = "researchallofus username")
  @NotNull


  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public Profile creationNonce(String creationNonce) {
    this.creationNonce = creationNonce;
    return this;
  }

   /**
   * A value which can be used to secure API calls during the account creation flow, prior to account login. 
   * @return creationNonce
  **/
  @ApiModelProperty(value = "A value which can be used to secure API calls during the account creation flow, prior to account login. ")


  public String getCreationNonce() {
    return creationNonce;
  }

  public void setCreationNonce(String creationNonce) {
    this.creationNonce = creationNonce;
  }

  public Profile contactEmail(String contactEmail) {
    this.contactEmail = contactEmail;
    return this;
  }

   /**
   * email address that can be used to contact the user
   * @return contactEmail
  **/
  @ApiModelProperty(value = "email address that can be used to contact the user")


  public String getContactEmail() {
    return contactEmail;
  }

  public void setContactEmail(String contactEmail) {
    this.contactEmail = contactEmail;
  }

  public Profile contactEmailFailure(Boolean contactEmailFailure) {
    this.contactEmailFailure = contactEmailFailure;
    return this;
  }

   /**
   * Whether or not contact email could be added to verification list
   * @return contactEmailFailure
  **/
  @ApiModelProperty(value = "Whether or not contact email could be added to verification list")


  public Boolean getContactEmailFailure() {
    return contactEmailFailure;
  }

  public void setContactEmailFailure(Boolean contactEmailFailure) {
    this.contactEmailFailure = contactEmailFailure;
  }

  public Profile firstSignInTime(Long firstSignInTime) {
    this.firstSignInTime = firstSignInTime;
    return this;
  }

   /**
   * Get firstSignInTime
   * @return firstSignInTime
  **/
  @ApiModelProperty(value = "")


  public Long getFirstSignInTime() {
    return firstSignInTime;
  }

  public void setFirstSignInTime(Long firstSignInTime) {
    this.firstSignInTime = firstSignInTime;
  }

  public Profile dataAccessLevel(DataAccessLevel dataAccessLevel) {
    this.dataAccessLevel = dataAccessLevel;
    return this;
  }

   /**
   * what level of data access the user has
   * @return dataAccessLevel
  **/
  @ApiModelProperty(required = true, value = "what level of data access the user has")
  @NotNull

  @Valid

  public DataAccessLevel getDataAccessLevel() {
    return dataAccessLevel;
  }

  public void setDataAccessLevel(DataAccessLevel dataAccessLevel) {
    this.dataAccessLevel = dataAccessLevel;
  }

  public Profile givenName(String givenName) {
    this.givenName = givenName;
    return this;
  }

   /**
   * the user's given name (e.g. Alice)
   * @return givenName
  **/
  @ApiModelProperty(value = "the user's given name (e.g. Alice)")


  public String getGivenName() {
    return givenName;
  }

  public void setGivenName(String givenName) {
    this.givenName = givenName;
  }

  public Profile familyName(String familyName) {
    this.familyName = familyName;
    return this;
  }

   /**
   * the user's family  name (e.g. Jones)
   * @return familyName
  **/
  @ApiModelProperty(value = "the user's family  name (e.g. Jones)")


  public String getFamilyName() {
    return familyName;
  }

  public void setFamilyName(String familyName) {
    this.familyName = familyName;
  }

  public Profile phoneNumber(String phoneNumber) {
    this.phoneNumber = phoneNumber;
    return this;
  }

   /**
   * the user's phone number
   * @return phoneNumber
  **/
  @ApiModelProperty(value = "the user's phone number")


  public String getPhoneNumber() {
    return phoneNumber;
  }

  public void setPhoneNumber(String phoneNumber) {
    this.phoneNumber = phoneNumber;
  }

  public Profile currentPosition(String currentPosition) {
    this.currentPosition = currentPosition;
    return this;
  }

   /**
   * the user's curent position (job title)
   * @return currentPosition
  **/
  @ApiModelProperty(value = "the user's curent position (job title)")


  public String getCurrentPosition() {
    return currentPosition;
  }

  public void setCurrentPosition(String currentPosition) {
    this.currentPosition = currentPosition;
  }

  public Profile organization(String organization) {
    this.organization = organization;
    return this;
  }

   /**
   * the user's current organization
   * @return organization
  **/
  @ApiModelProperty(value = "the user's current organization")


  public String getOrganization() {
    return organization;
  }

  public void setOrganization(String organization) {
    this.organization = organization;
  }

  public Profile authorities(List<Authority> authorities) {
    this.authorities = authorities;
    return this;
  }

  public Profile addAuthoritiesItem(Authority authoritiesItem) {
    if (this.authorities == null) {
      this.authorities = new ArrayList<Authority>();
    }
    this.authorities.add(authoritiesItem);
    return this;
  }

   /**
   * authorities granted to this user
   * @return authorities
  **/
  @ApiModelProperty(value = "authorities granted to this user")

  @Valid

  public List<Authority> getAuthorities() {
    return authorities;
  }

  public void setAuthorities(List<Authority> authorities) {
    this.authorities = authorities;
  }

  public Profile pageVisits(List<PageVisit> pageVisits) {
    this.pageVisits = pageVisits;
    return this;
  }

  public Profile addPageVisitsItem(PageVisit pageVisitsItem) {
    if (this.pageVisits == null) {
      this.pageVisits = new ArrayList<PageVisit>();
    }
    this.pageVisits.add(pageVisitsItem);
    return this;
  }

   /**
   * pages user has visited
   * @return pageVisits
  **/
  @ApiModelProperty(value = "pages user has visited")

  @Valid

  public List<PageVisit> getPageVisits() {
    return pageVisits;
  }

  public void setPageVisits(List<PageVisit> pageVisits) {
    this.pageVisits = pageVisits;
  }

  public Profile demographicSurveyCompletionTime(Long demographicSurveyCompletionTime) {
    this.demographicSurveyCompletionTime = demographicSurveyCompletionTime;
    return this;
  }

   /**
   * Timestamp when the user completed a demographic survey in milliseconds since the UNIX epoch.
   * @return demographicSurveyCompletionTime
  **/
  @ApiModelProperty(value = "Timestamp when the user completed a demographic survey in milliseconds since the UNIX epoch.")


  public Long getDemographicSurveyCompletionTime() {
    return demographicSurveyCompletionTime;
  }

  public void setDemographicSurveyCompletionTime(Long demographicSurveyCompletionTime) {
    this.demographicSurveyCompletionTime = demographicSurveyCompletionTime;
  }

  public Profile disabled(Boolean disabled) {
    this.disabled = disabled;
    return this;
  }

   /**
   * Get disabled
   * @return disabled
  **/
  @ApiModelProperty(value = "")


  public Boolean getDisabled() {
    return disabled;
  }

  public void setDisabled(Boolean disabled) {
    this.disabled = disabled;
  }

  public Profile emailVerificationStatus(EmailVerificationStatus emailVerificationStatus) {
    this.emailVerificationStatus = emailVerificationStatus;
    return this;
  }

   /**
   * Get emailVerificationStatus
   * @return emailVerificationStatus
  **/
  @ApiModelProperty(value = "")

  @Valid

  public EmailVerificationStatus getEmailVerificationStatus() {
    return emailVerificationStatus;
  }

  public void setEmailVerificationStatus(EmailVerificationStatus emailVerificationStatus) {
    this.emailVerificationStatus = emailVerificationStatus;
  }

  public Profile aboutYou(String aboutYou) {
    this.aboutYou = aboutYou;
    return this;
  }

   /**
   * Get aboutYou
   * @return aboutYou
  **/
  @ApiModelProperty(value = "")


  public String getAboutYou() {
    return aboutYou;
  }

  public void setAboutYou(String aboutYou) {
    this.aboutYou = aboutYou;
  }

  public Profile areaOfResearch(String areaOfResearch) {
    this.areaOfResearch = areaOfResearch;
    return this;
  }

   /**
   * Get areaOfResearch
   * @return areaOfResearch
  **/
  @ApiModelProperty(value = "")


  public String getAreaOfResearch() {
    return areaOfResearch;
  }

  public void setAreaOfResearch(String areaOfResearch) {
    this.areaOfResearch = areaOfResearch;
  }

  public Profile institutionalAffiliations(List<InstitutionalAffiliation> institutionalAffiliations) {
    this.institutionalAffiliations = institutionalAffiliations;
    return this;
  }

  public Profile addInstitutionalAffiliationsItem(InstitutionalAffiliation institutionalAffiliationsItem) {
    if (this.institutionalAffiliations == null) {
      this.institutionalAffiliations = new ArrayList<InstitutionalAffiliation>();
    }
    this.institutionalAffiliations.add(institutionalAffiliationsItem);
    return this;
  }

   /**
   * Get institutionalAffiliations
   * @return institutionalAffiliations
  **/
  @ApiModelProperty(value = "")

  @Valid

  public List<InstitutionalAffiliation> getInstitutionalAffiliations() {
    return institutionalAffiliations;
  }

  public void setInstitutionalAffiliations(List<InstitutionalAffiliation> institutionalAffiliations) {
    this.institutionalAffiliations = institutionalAffiliations;
  }

  public Profile demographicSurvey(DemographicSurvey demographicSurvey) {
    this.demographicSurvey = demographicSurvey;
    return this;
  }

   /**
   * Get demographicSurvey
   * @return demographicSurvey
  **/
  @ApiModelProperty(value = "")

  @Valid

  public DemographicSurvey getDemographicSurvey() {
    return demographicSurvey;
  }

  public void setDemographicSurvey(DemographicSurvey demographicSurvey) {
    this.demographicSurvey = demographicSurvey;
  }

  public Profile idVerificationCompletionTime(Long idVerificationCompletionTime) {
    this.idVerificationCompletionTime = idVerificationCompletionTime;
    return this;
  }

   /**
   * Timestamp when the user completes identity verification.
   * @return idVerificationCompletionTime
  **/
  @ApiModelProperty(value = "Timestamp when the user completes identity verification.")


  public Long getIdVerificationCompletionTime() {
    return idVerificationCompletionTime;
  }

  public void setIdVerificationCompletionTime(Long idVerificationCompletionTime) {
    this.idVerificationCompletionTime = idVerificationCompletionTime;
  }

  public Profile idVerificationBypassTime(Long idVerificationBypassTime) {
    this.idVerificationBypassTime = idVerificationBypassTime;
    return this;
  }

   /**
   * Timestamp when the user is bypassed for completing identity verification
   * @return idVerificationBypassTime
  **/
  @ApiModelProperty(value = "Timestamp when the user is bypassed for completing identity verification")


  public Long getIdVerificationBypassTime() {
    return idVerificationBypassTime;
  }

  public void setIdVerificationBypassTime(Long idVerificationBypassTime) {
    this.idVerificationBypassTime = idVerificationBypassTime;
  }

  public Profile twoFactorAuthCompletionTime(Long twoFactorAuthCompletionTime) {
    this.twoFactorAuthCompletionTime = twoFactorAuthCompletionTime;
    return this;
  }

   /**
   * Timestamp when the user completed two factor authentication in milliseconds since the UNIX epoch.
   * @return twoFactorAuthCompletionTime
  **/
  @ApiModelProperty(value = "Timestamp when the user completed two factor authentication in milliseconds since the UNIX epoch.")


  public Long getTwoFactorAuthCompletionTime() {
    return twoFactorAuthCompletionTime;
  }

  public void setTwoFactorAuthCompletionTime(Long twoFactorAuthCompletionTime) {
    this.twoFactorAuthCompletionTime = twoFactorAuthCompletionTime;
  }

  public Profile twoFactorAuthBypassTime(Long twoFactorAuthBypassTime) {
    this.twoFactorAuthBypassTime = twoFactorAuthBypassTime;
    return this;
  }

   /**
   * Timestamp when the user was bypassed for completing two factor authentication in milliseconds since the UNIX epoch.
   * @return twoFactorAuthBypassTime
  **/
  @ApiModelProperty(value = "Timestamp when the user was bypassed for completing two factor authentication in milliseconds since the UNIX epoch.")


  public Long getTwoFactorAuthBypassTime() {
    return twoFactorAuthBypassTime;
  }

  public void setTwoFactorAuthBypassTime(Long twoFactorAuthBypassTime) {
    this.twoFactorAuthBypassTime = twoFactorAuthBypassTime;
  }

  public Profile eraCommonsLinkedNihUsername(String eraCommonsLinkedNihUsername) {
    this.eraCommonsLinkedNihUsername = eraCommonsLinkedNihUsername;
    return this;
  }

   /**
   * The user's NIH username
   * @return eraCommonsLinkedNihUsername
  **/
  @ApiModelProperty(value = "The user's NIH username")


  public String getEraCommonsLinkedNihUsername() {
    return eraCommonsLinkedNihUsername;
  }

  public void setEraCommonsLinkedNihUsername(String eraCommonsLinkedNihUsername) {
    this.eraCommonsLinkedNihUsername = eraCommonsLinkedNihUsername;
  }

  public Profile eraCommonsLinkExpireTime(Long eraCommonsLinkExpireTime) {
    this.eraCommonsLinkExpireTime = eraCommonsLinkExpireTime;
    return this;
  }

   /**
   * The FireCloud-calculated expiration time
   * @return eraCommonsLinkExpireTime
  **/
  @ApiModelProperty(value = "The FireCloud-calculated expiration time")


  public Long getEraCommonsLinkExpireTime() {
    return eraCommonsLinkExpireTime;
  }

  public void setEraCommonsLinkExpireTime(Long eraCommonsLinkExpireTime) {
    this.eraCommonsLinkExpireTime = eraCommonsLinkExpireTime;
  }

  public Profile eraCommonsCompletionTime(Long eraCommonsCompletionTime) {
    this.eraCommonsCompletionTime = eraCommonsCompletionTime;
    return this;
  }

   /**
   * Timestamp when the user completed era commons linking.
   * @return eraCommonsCompletionTime
  **/
  @ApiModelProperty(value = "Timestamp when the user completed era commons linking.")


  public Long getEraCommonsCompletionTime() {
    return eraCommonsCompletionTime;
  }

  public void setEraCommonsCompletionTime(Long eraCommonsCompletionTime) {
    this.eraCommonsCompletionTime = eraCommonsCompletionTime;
  }

  public Profile eraCommonsBypassTime(Long eraCommonsBypassTime) {
    this.eraCommonsBypassTime = eraCommonsBypassTime;
    return this;
  }

   /**
   * Timestamp when the user was bypassed for completing era commons linking.
   * @return eraCommonsBypassTime
  **/
  @ApiModelProperty(value = "Timestamp when the user was bypassed for completing era commons linking.")


  public Long getEraCommonsBypassTime() {
    return eraCommonsBypassTime;
  }

  public void setEraCommonsBypassTime(Long eraCommonsBypassTime) {
    this.eraCommonsBypassTime = eraCommonsBypassTime;
  }

  public Profile complianceTrainingCompletionTime(Long complianceTrainingCompletionTime) {
    this.complianceTrainingCompletionTime = complianceTrainingCompletionTime;
    return this;
  }

   /**
   * Timestamp when a user completed compliance training.
   * @return complianceTrainingCompletionTime
  **/
  @ApiModelProperty(value = "Timestamp when a user completed compliance training.")


  public Long getComplianceTrainingCompletionTime() {
    return complianceTrainingCompletionTime;
  }

  public void setComplianceTrainingCompletionTime(Long complianceTrainingCompletionTime) {
    this.complianceTrainingCompletionTime = complianceTrainingCompletionTime;
  }

  public Profile complianceTrainingBypassTime(Long complianceTrainingBypassTime) {
    this.complianceTrainingBypassTime = complianceTrainingBypassTime;
    return this;
  }

   /**
   * Timestamp when a user was bypassed for completing compliance training
   * @return complianceTrainingBypassTime
  **/
  @ApiModelProperty(value = "Timestamp when a user was bypassed for completing compliance training")


  public Long getComplianceTrainingBypassTime() {
    return complianceTrainingBypassTime;
  }

  public void setComplianceTrainingBypassTime(Long complianceTrainingBypassTime) {
    this.complianceTrainingBypassTime = complianceTrainingBypassTime;
  }

  public Profile betaAccessBypassTime(Long betaAccessBypassTime) {
    this.betaAccessBypassTime = betaAccessBypassTime;
    return this;
  }

   /**
   * Timestamp when a user was bypassed for beta access
   * @return betaAccessBypassTime
  **/
  @ApiModelProperty(value = "Timestamp when a user was bypassed for beta access")


  public Long getBetaAccessBypassTime() {
    return betaAccessBypassTime;
  }

  public void setBetaAccessBypassTime(Long betaAccessBypassTime) {
    this.betaAccessBypassTime = betaAccessBypassTime;
  }

  public Profile betaAccessRequestTime(Long betaAccessRequestTime) {
    this.betaAccessRequestTime = betaAccessRequestTime;
    return this;
  }

   /**
   * Timestamp when the user requests beta access.
   * @return betaAccessRequestTime
  **/
  @ApiModelProperty(value = "Timestamp when the user requests beta access.")


  public Long getBetaAccessRequestTime() {
    return betaAccessRequestTime;
  }

  public void setBetaAccessRequestTime(Long betaAccessRequestTime) {
    this.betaAccessRequestTime = betaAccessRequestTime;
  }

  public Profile emailVerificationCompletionTime(Long emailVerificationCompletionTime) {
    this.emailVerificationCompletionTime = emailVerificationCompletionTime;
    return this;
  }

   /**
   * Timestamp when a user completed email verification
   * @return emailVerificationCompletionTime
  **/
  @ApiModelProperty(value = "Timestamp when a user completed email verification")


  public Long getEmailVerificationCompletionTime() {
    return emailVerificationCompletionTime;
  }

  public void setEmailVerificationCompletionTime(Long emailVerificationCompletionTime) {
    this.emailVerificationCompletionTime = emailVerificationCompletionTime;
  }

  public Profile emailVerificationBypassTime(Long emailVerificationBypassTime) {
    this.emailVerificationBypassTime = emailVerificationBypassTime;
    return this;
  }

   /**
   * Timestamp when a user was bypassed for completing email verification
   * @return emailVerificationBypassTime
  **/
  @ApiModelProperty(value = "Timestamp when a user was bypassed for completing email verification")


  public Long getEmailVerificationBypassTime() {
    return emailVerificationBypassTime;
  }

  public void setEmailVerificationBypassTime(Long emailVerificationBypassTime) {
    this.emailVerificationBypassTime = emailVerificationBypassTime;
  }

  public Profile dataUseAgreementCompletionTime(Long dataUseAgreementCompletionTime) {
    this.dataUseAgreementCompletionTime = dataUseAgreementCompletionTime;
    return this;
  }

   /**
   * Timestamp when a user completed the data use agreement.
   * @return dataUseAgreementCompletionTime
  **/
  @ApiModelProperty(value = "Timestamp when a user completed the data use agreement.")


  public Long getDataUseAgreementCompletionTime() {
    return dataUseAgreementCompletionTime;
  }

  public void setDataUseAgreementCompletionTime(Long dataUseAgreementCompletionTime) {
    this.dataUseAgreementCompletionTime = dataUseAgreementCompletionTime;
  }

  public Profile dataUseAgreementBypassTime(Long dataUseAgreementBypassTime) {
    this.dataUseAgreementBypassTime = dataUseAgreementBypassTime;
    return this;
  }

   /**
   * Timestamp when a user was bypassed for completing the data use agreement.
   * @return dataUseAgreementBypassTime
  **/
  @ApiModelProperty(value = "Timestamp when a user was bypassed for completing the data use agreement.")


  public Long getDataUseAgreementBypassTime() {
    return dataUseAgreementBypassTime;
  }

  public void setDataUseAgreementBypassTime(Long dataUseAgreementBypassTime) {
    this.dataUseAgreementBypassTime = dataUseAgreementBypassTime;
  }

  public Profile dataUseAgreementSignedVersion(Integer dataUseAgreementSignedVersion) {
    this.dataUseAgreementSignedVersion = dataUseAgreementSignedVersion;
    return this;
  }

   /**
   * Version of the data use agreement that the user last signed.
   * @return dataUseAgreementSignedVersion
  **/
  @ApiModelProperty(value = "Version of the data use agreement that the user last signed.")


  public Integer getDataUseAgreementSignedVersion() {
    return dataUseAgreementSignedVersion;
  }

  public void setDataUseAgreementSignedVersion(Integer dataUseAgreementSignedVersion) {
    this.dataUseAgreementSignedVersion = dataUseAgreementSignedVersion;
  }

  public Profile address(Address address) {
    this.address = address;
    return this;
  }

   /**
   * Get address
   * @return address
  **/
  @ApiModelProperty(value = "")

  @Valid

  public Address getAddress() {
    return address;
  }

  public void setAddress(Address address) {
    this.address = address;
  }

  public Profile freeTierUsage(Double freeTierUsage) {
    this.freeTierUsage = freeTierUsage;
    return this;
  }

   /**
   * Get freeTierUsage
   * @return freeTierUsage
  **/
  @ApiModelProperty(value = "")


  public Double getFreeTierUsage() {
    return freeTierUsage;
  }

  public void setFreeTierUsage(Double freeTierUsage) {
    this.freeTierUsage = freeTierUsage;
  }

  public Profile freeTierDollarQuota(Double freeTierDollarQuota) {
    this.freeTierDollarQuota = freeTierDollarQuota;
    return this;
  }

   /**
   * Get freeTierDollarQuota
   * @return freeTierDollarQuota
  **/
  @ApiModelProperty(value = "")


  public Double getFreeTierDollarQuota() {
    return freeTierDollarQuota;
  }

  public void setFreeTierDollarQuota(Double freeTierDollarQuota) {
    this.freeTierDollarQuota = freeTierDollarQuota;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Profile profile = (Profile) o;
    return Objects.equals(this.userId, profile.userId) &&
        Objects.equals(this.username, profile.username) &&
        Objects.equals(this.creationNonce, profile.creationNonce) &&
        Objects.equals(this.contactEmail, profile.contactEmail) &&
        Objects.equals(this.contactEmailFailure, profile.contactEmailFailure) &&
        Objects.equals(this.firstSignInTime, profile.firstSignInTime) &&
        Objects.equals(this.dataAccessLevel, profile.dataAccessLevel) &&
        Objects.equals(this.givenName, profile.givenName) &&
        Objects.equals(this.familyName, profile.familyName) &&
        Objects.equals(this.phoneNumber, profile.phoneNumber) &&
        Objects.equals(this.currentPosition, profile.currentPosition) &&
        Objects.equals(this.organization, profile.organization) &&
        Objects.equals(this.authorities, profile.authorities) &&
        Objects.equals(this.pageVisits, profile.pageVisits) &&
        Objects.equals(this.demographicSurveyCompletionTime, profile.demographicSurveyCompletionTime) &&
        Objects.equals(this.disabled, profile.disabled) &&
        Objects.equals(this.emailVerificationStatus, profile.emailVerificationStatus) &&
        Objects.equals(this.aboutYou, profile.aboutYou) &&
        Objects.equals(this.areaOfResearch, profile.areaOfResearch) &&
        Objects.equals(this.institutionalAffiliations, profile.institutionalAffiliations) &&
        Objects.equals(this.demographicSurvey, profile.demographicSurvey) &&
        Objects.equals(this.idVerificationCompletionTime, profile.idVerificationCompletionTime) &&
        Objects.equals(this.idVerificationBypassTime, profile.idVerificationBypassTime) &&
        Objects.equals(this.twoFactorAuthCompletionTime, profile.twoFactorAuthCompletionTime) &&
        Objects.equals(this.twoFactorAuthBypassTime, profile.twoFactorAuthBypassTime) &&
        Objects.equals(this.eraCommonsLinkedNihUsername, profile.eraCommonsLinkedNihUsername) &&
        Objects.equals(this.eraCommonsLinkExpireTime, profile.eraCommonsLinkExpireTime) &&
        Objects.equals(this.eraCommonsCompletionTime, profile.eraCommonsCompletionTime) &&
        Objects.equals(this.eraCommonsBypassTime, profile.eraCommonsBypassTime) &&
        Objects.equals(this.complianceTrainingCompletionTime, profile.complianceTrainingCompletionTime) &&
        Objects.equals(this.complianceTrainingBypassTime, profile.complianceTrainingBypassTime) &&
        Objects.equals(this.betaAccessBypassTime, profile.betaAccessBypassTime) &&
        Objects.equals(this.betaAccessRequestTime, profile.betaAccessRequestTime) &&
        Objects.equals(this.emailVerificationCompletionTime, profile.emailVerificationCompletionTime) &&
        Objects.equals(this.emailVerificationBypassTime, profile.emailVerificationBypassTime) &&
        Objects.equals(this.dataUseAgreementCompletionTime, profile.dataUseAgreementCompletionTime) &&
        Objects.equals(this.dataUseAgreementBypassTime, profile.dataUseAgreementBypassTime) &&
        Objects.equals(this.dataUseAgreementSignedVersion, profile.dataUseAgreementSignedVersion) &&
        Objects.equals(this.address, profile.address) &&
        Objects.equals(this.freeTierUsage, profile.freeTierUsage) &&
        Objects.equals(this.freeTierDollarQuota, profile.freeTierDollarQuota);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, username, creationNonce, contactEmail, contactEmailFailure, firstSignInTime, dataAccessLevel, givenName, familyName, phoneNumber, currentPosition, organization, authorities, pageVisits, demographicSurveyCompletionTime, disabled, emailVerificationStatus, aboutYou, areaOfResearch, institutionalAffiliations, demographicSurvey, idVerificationCompletionTime, idVerificationBypassTime, twoFactorAuthCompletionTime, twoFactorAuthBypassTime, eraCommonsLinkedNihUsername, eraCommonsLinkExpireTime, eraCommonsCompletionTime, eraCommonsBypassTime, complianceTrainingCompletionTime, complianceTrainingBypassTime, betaAccessBypassTime, betaAccessRequestTime, emailVerificationCompletionTime, emailVerificationBypassTime, dataUseAgreementCompletionTime, dataUseAgreementBypassTime, dataUseAgreementSignedVersion, address, freeTierUsage, freeTierDollarQuota);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Profile {\n");
    
    sb.append("    userId: ").append(toIndentedString(userId)).append("\n");
    sb.append("    username: ").append(toIndentedString(username)).append("\n");
    sb.append("    creationNonce: ").append(toIndentedString(creationNonce)).append("\n");
    sb.append("    contactEmail: ").append(toIndentedString(contactEmail)).append("\n");
    sb.append("    contactEmailFailure: ").append(toIndentedString(contactEmailFailure)).append("\n");
    sb.append("    firstSignInTime: ").append(toIndentedString(firstSignInTime)).append("\n");
    sb.append("    dataAccessLevel: ").append(toIndentedString(dataAccessLevel)).append("\n");
    sb.append("    givenName: ").append(toIndentedString(givenName)).append("\n");
    sb.append("    familyName: ").append(toIndentedString(familyName)).append("\n");
    sb.append("    phoneNumber: ").append(toIndentedString(phoneNumber)).append("\n");
    sb.append("    currentPosition: ").append(toIndentedString(currentPosition)).append("\n");
    sb.append("    organization: ").append(toIndentedString(organization)).append("\n");
    sb.append("    authorities: ").append(toIndentedString(authorities)).append("\n");
    sb.append("    pageVisits: ").append(toIndentedString(pageVisits)).append("\n");
    sb.append("    demographicSurveyCompletionTime: ").append(toIndentedString(demographicSurveyCompletionTime)).append("\n");
    sb.append("    disabled: ").append(toIndentedString(disabled)).append("\n");
    sb.append("    emailVerificationStatus: ").append(toIndentedString(emailVerificationStatus)).append("\n");
    sb.append("    aboutYou: ").append(toIndentedString(aboutYou)).append("\n");
    sb.append("    areaOfResearch: ").append(toIndentedString(areaOfResearch)).append("\n");
    sb.append("    institutionalAffiliations: ").append(toIndentedString(institutionalAffiliations)).append("\n");
    sb.append("    demographicSurvey: ").append(toIndentedString(demographicSurvey)).append("\n");
    sb.append("    idVerificationCompletionTime: ").append(toIndentedString(idVerificationCompletionTime)).append("\n");
    sb.append("    idVerificationBypassTime: ").append(toIndentedString(idVerificationBypassTime)).append("\n");
    sb.append("    twoFactorAuthCompletionTime: ").append(toIndentedString(twoFactorAuthCompletionTime)).append("\n");
    sb.append("    twoFactorAuthBypassTime: ").append(toIndentedString(twoFactorAuthBypassTime)).append("\n");
    sb.append("    eraCommonsLinkedNihUsername: ").append(toIndentedString(eraCommonsLinkedNihUsername)).append("\n");
    sb.append("    eraCommonsLinkExpireTime: ").append(toIndentedString(eraCommonsLinkExpireTime)).append("\n");
    sb.append("    eraCommonsCompletionTime: ").append(toIndentedString(eraCommonsCompletionTime)).append("\n");
    sb.append("    eraCommonsBypassTime: ").append(toIndentedString(eraCommonsBypassTime)).append("\n");
    sb.append("    complianceTrainingCompletionTime: ").append(toIndentedString(complianceTrainingCompletionTime)).append("\n");
    sb.append("    complianceTrainingBypassTime: ").append(toIndentedString(complianceTrainingBypassTime)).append("\n");
    sb.append("    betaAccessBypassTime: ").append(toIndentedString(betaAccessBypassTime)).append("\n");
    sb.append("    betaAccessRequestTime: ").append(toIndentedString(betaAccessRequestTime)).append("\n");
    sb.append("    emailVerificationCompletionTime: ").append(toIndentedString(emailVerificationCompletionTime)).append("\n");
    sb.append("    emailVerificationBypassTime: ").append(toIndentedString(emailVerificationBypassTime)).append("\n");
    sb.append("    dataUseAgreementCompletionTime: ").append(toIndentedString(dataUseAgreementCompletionTime)).append("\n");
    sb.append("    dataUseAgreementBypassTime: ").append(toIndentedString(dataUseAgreementBypassTime)).append("\n");
    sb.append("    dataUseAgreementSignedVersion: ").append(toIndentedString(dataUseAgreementSignedVersion)).append("\n");
    sb.append("    address: ").append(toIndentedString(address)).append("\n");
    sb.append("    freeTierUsage: ").append(toIndentedString(freeTierUsage)).append("\n");
    sb.append("    freeTierDollarQuota: ").append(toIndentedString(freeTierDollarQuota)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

