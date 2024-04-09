package org.pmiops.workbench.db.model;

import java.sql.Timestamp;
import java.util.Set;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "demographic_survey_v2")
@EntityListeners(AuditingEntityListener.class)
public class DbDemographicSurveyV2 {
  private long id;
  private DbUser user;
  private Timestamp completionTime;
  private String ethnicityAiAnOtherText;
  private String ethnicityAsianOtherText;
  private String ethnicityBlackOtherText;
  private String ethnicityHispanicOtherText;
  private String ethnicityMeNaOtherText;
  private String ethnicityNhPiOtherText;
  private String ethnicityWhiteOtherText;
  private String ethnicityOtherText;
  private String genderOtherText;
  private String orientationOtherText;
  private DbSexAtBirthV2 sexAtBirth;
  private String sexAtBirthOtherText;
  private Long yearOfBirth;
  private Boolean yearOfBirthPreferNot;
  private DbYesNoPreferNot disabilityHearing;
  private DbYesNoPreferNot disabilitySeeing;
  private DbYesNoPreferNot disabilityConcentrating;
  private DbYesNoPreferNot disabilityWalking;
  private DbYesNoPreferNot disabilityDressing;
  private DbYesNoPreferNot disabilityErrands;
  private String disabilityOtherText;
  private DbEducationV2 education;
  private DbYesNoPreferNot disadvantaged;
  private String surveyComments;

  // singular names for these because Hibernate seems to require
  // that they match the column names in the join tables

  private Set<DbEthnicCategory> ethnicCategory;
  private Set<DbGenderIdentityV2> genderIdentity;
  private Set<DbSexualOrientationV2> sexualOrientation;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "demographic_survey_v2_id", nullable = false)
  public long getId() {
    return id;
  }

  public DbDemographicSurveyV2 setId(long demographic_survey_id) {
    this.id = demographic_survey_id;
    return this;
  }

  @OneToOne
  @JoinColumn(name = "user_id", nullable = false)
  public DbUser getUser() {
    return user;
  }

  public DbDemographicSurveyV2 setUser(DbUser user) {
    this.user = user;
    return this;
  }

  @LastModifiedDate
  @Column(name = "completion_time")
  public Timestamp getCompletionTime() {
    return completionTime;
  }

  public DbDemographicSurveyV2 setCompletionTime(Timestamp completionTime) {
    this.completionTime = completionTime;
    return this;
  }

  @ElementCollection(fetch = FetchType.LAZY)
  @Enumerated(EnumType.STRING)
  @CollectionTable(
      name = "demographic_survey_v2_ethnic_category",
      joinColumns = @JoinColumn(name = "demographic_survey_v2_id"))
  @Column(name = "ethnic_category")
  public Set<DbEthnicCategory> getEthnicCategory() {
    return ethnicCategory;
  }

  public DbDemographicSurveyV2 setEthnicCategory(Set<DbEthnicCategory> ethnicCategory) {
    this.ethnicCategory = ethnicCategory;
    return this;
  }

  @Column(name = "ethnicity_ai_an_other_text")
  public String getEthnicityAiAnOtherText() {
    return ethnicityAiAnOtherText;
  }

  public DbDemographicSurveyV2 setEthnicityAiAnOtherText(String ethnicityAiAnOtherText) {
    this.ethnicityAiAnOtherText = ethnicityAiAnOtherText;
    return this;
  }

  @Column(name = "ethnicity_asian_other_text")
  public String getEthnicityAsianOtherText() {
    return ethnicityAsianOtherText;
  }

  public DbDemographicSurveyV2 setEthnicityAsianOtherText(String ethnicityAsianOtherText) {
    this.ethnicityAsianOtherText = ethnicityAsianOtherText;
    return this;
  }

  @Column(name = "ethnicity_black_other_text")
  public String getEthnicityBlackOtherText() {
    return ethnicityBlackOtherText;
  }

  public DbDemographicSurveyV2 setEthnicityBlackOtherText(String ethnicityBlackOtherText) {
    this.ethnicityBlackOtherText = ethnicityBlackOtherText;
    return this;
  }

  @Column(name = "ethnicity_hispanic_other_text")
  public String getEthnicityHispanicOtherText() {
    return ethnicityHispanicOtherText;
  }

  public DbDemographicSurveyV2 setEthnicityHispanicOtherText(String ethnicityHispanicOtherText) {
    this.ethnicityHispanicOtherText = ethnicityHispanicOtherText;
    return this;
  }

  @Column(name = "ethnicity_me_na_other_text")
  public String getEthnicityMeNaOtherText() {
    return ethnicityMeNaOtherText;
  }

  public DbDemographicSurveyV2 setEthnicityMeNaOtherText(String ethnicityMeNaOtherText) {
    this.ethnicityMeNaOtherText = ethnicityMeNaOtherText;
    return this;
  }

  @Column(name = "ethnicity_nh_pi_other_text")
  public String getEthnicityNhPiOtherText() {
    return ethnicityNhPiOtherText;
  }

  public DbDemographicSurveyV2 setEthnicityNhPiOtherText(String ethnicityNhPiOtherText) {
    this.ethnicityNhPiOtherText = ethnicityNhPiOtherText;
    return this;
  }

  @Column(name = "ethnicity_white_other_text")
  public String getEthnicityWhiteOtherText() {
    return ethnicityWhiteOtherText;
  }

  public DbDemographicSurveyV2 setEthnicityWhiteOtherText(String ethnicityWhiteOtherText) {
    this.ethnicityWhiteOtherText = ethnicityWhiteOtherText;
    return this;
  }

  @Column(name = "ethnicity_other_text")
  public String getEthnicityOtherText() {
    return ethnicityOtherText;
  }

  public DbDemographicSurveyV2 setEthnicityOtherText(String ethnicityOtherText) {
    this.ethnicityOtherText = ethnicityOtherText;
    return this;
  }

  @ElementCollection(fetch = FetchType.LAZY)
  @Enumerated(EnumType.STRING)
  @CollectionTable(
      name = "demographic_survey_v2_gender_identity",
      joinColumns = @JoinColumn(name = "demographic_survey_v2_id"))
  @Column(name = "gender_identity")
  public Set<DbGenderIdentityV2> getGenderIdentity() {
    return genderIdentity;
  }

  public DbDemographicSurveyV2 setGenderIdentity(Set<DbGenderIdentityV2> genderIdentity) {
    this.genderIdentity = genderIdentity;
    return this;
  }

  @Column(name = "gender_other_text")
  public String getGenderOtherText() {
    return genderOtherText;
  }

  public DbDemographicSurveyV2 setGenderOtherText(String genderOtherText) {
    this.genderOtherText = genderOtherText;
    return this;
  }

  @ElementCollection(fetch = FetchType.LAZY)
  @Enumerated(EnumType.STRING)
  @CollectionTable(
      name = "demographic_survey_v2_sexual_orientation",
      joinColumns = @JoinColumn(name = "demographic_survey_v2_id"))
  @Column(name = "sexual_orientation")
  public Set<DbSexualOrientationV2> getSexualOrientation() {
    return sexualOrientation;
  }

  public DbDemographicSurveyV2 setSexualOrientation(Set<DbSexualOrientationV2> sexualOrientation) {
    this.sexualOrientation = sexualOrientation;
    return this;
  }

  @Column(name = "orientation_other_text")
  public String getOrientationOtherText() {
    return orientationOtherText;
  }

  public DbDemographicSurveyV2 setOrientationOtherText(String orientationOtherText) {
    this.orientationOtherText = orientationOtherText;
    return this;
  }

  @Enumerated(EnumType.STRING)
  @Column(name = "sex_at_birth")
  public DbSexAtBirthV2 getSexAtBirth() {
    return sexAtBirth;
  }

  public DbDemographicSurveyV2 setSexAtBirth(DbSexAtBirthV2 sexAtBirth) {
    this.sexAtBirth = sexAtBirth;
    return this;
  }

  @Column(name = "sex_at_birth_other_text")
  public String getSexAtBirthOtherText() {
    return sexAtBirthOtherText;
  }

  public DbDemographicSurveyV2 setSexAtBirthOtherText(String sexAtBirthOtherText) {
    this.sexAtBirthOtherText = sexAtBirthOtherText;
    return this;
  }

  @Column(name = "year_of_birth")
  public Long getYearOfBirth() {
    return yearOfBirth;
  }

  public DbDemographicSurveyV2 setYearOfBirth(Long yearOfBirth) {
    this.yearOfBirth = yearOfBirth;
    return this;
  }

  @Column(name = "year_of_birth_prefer_not")
  public Boolean getYearOfBirthPreferNot() {
    return yearOfBirthPreferNot;
  }

  public DbDemographicSurveyV2 setYearOfBirthPreferNot(Boolean yearOfBirthPreferNot) {
    this.yearOfBirthPreferNot = yearOfBirthPreferNot;
    return this;
  }

  @Enumerated(EnumType.STRING)
  @Column(name = "disability_hearing")
  public DbYesNoPreferNot getDisabilityHearing() {
    return disabilityHearing;
  }

  public DbDemographicSurveyV2 setDisabilityHearing(DbYesNoPreferNot disabilityHearing) {
    this.disabilityHearing = disabilityHearing;
    return this;
  }

  @Enumerated(EnumType.STRING)
  @Column(name = "disability_seeing")
  public DbYesNoPreferNot getDisabilitySeeing() {
    return disabilitySeeing;
  }

  public DbDemographicSurveyV2 setDisabilitySeeing(DbYesNoPreferNot disabilitySeeing) {
    this.disabilitySeeing = disabilitySeeing;
    return this;
  }

  @Enumerated(EnumType.STRING)
  @Column(name = "disability_concentrating")
  public DbYesNoPreferNot getDisabilityConcentrating() {
    return disabilityConcentrating;
  }

  public DbDemographicSurveyV2 setDisabilityConcentrating(
      DbYesNoPreferNot disabilityConcentrating) {
    this.disabilityConcentrating = disabilityConcentrating;
    return this;
  }

  @Enumerated(EnumType.STRING)
  @Column(name = "disability_walking")
  public DbYesNoPreferNot getDisabilityWalking() {
    return disabilityWalking;
  }

  public DbDemographicSurveyV2 setDisabilityWalking(DbYesNoPreferNot disabilityWalking) {
    this.disabilityWalking = disabilityWalking;
    return this;
  }

  @Enumerated(EnumType.STRING)
  @Column(name = "disability_dressing")
  public DbYesNoPreferNot getDisabilityDressing() {
    return disabilityDressing;
  }

  public DbDemographicSurveyV2 setDisabilityDressing(DbYesNoPreferNot disabilityDressing) {
    this.disabilityDressing = disabilityDressing;
    return this;
  }

  @Enumerated(EnumType.STRING)
  @Column(name = "disability_errands")
  public DbYesNoPreferNot getDisabilityErrands() {
    return disabilityErrands;
  }

  public DbDemographicSurveyV2 setDisabilityErrands(DbYesNoPreferNot disabilityErrands) {
    this.disabilityErrands = disabilityErrands;
    return this;
  }

  @Column(name = "disability_other_text")
  public String getDisabilityOtherText() {
    return disabilityOtherText;
  }

  public DbDemographicSurveyV2 setDisabilityOtherText(String disabilityOtherText) {
    this.disabilityOtherText = disabilityOtherText;
    return this;
  }

  @Enumerated(EnumType.STRING)
  @Column(name = "education")
  public DbEducationV2 getEducation() {
    return education;
  }

  public DbDemographicSurveyV2 setEducation(DbEducationV2 education) {
    this.education = education;
    return this;
  }

  @Enumerated(EnumType.STRING)
  @Column(name = "disadvantaged")
  public DbYesNoPreferNot getDisadvantaged() {
    return disadvantaged;
  }

  public DbDemographicSurveyV2 setDisadvantaged(DbYesNoPreferNot disadvantaged) {
    this.disadvantaged = disadvantaged;
    return this;
  }

  @Column(name = "survey_comments")
  public String getSurveyComments() {
    return surveyComments;
  }

  public DbDemographicSurveyV2 setSurveyComments(String surveyComments) {
    this.surveyComments = surveyComments;
    return this;
  }

  public enum DbEthnicCategory {
    AI_AN,
    AI_AN_AMERICAN_INDIAN,
    AI_AN_ALASKA_NATIVE,
    AI_AN_CENTRAL_SOUTH,
    AI_AN_OTHER,

    ASIAN,
    ASIAN_INDIAN,
    ASIAN_CAMBODIAN,
    ASIAN_CHINESE,
    ASIAN_FILIPINO,
    ASIAN_HMONG,
    ASIAN_JAPANESE,
    ASIAN_KOREAN,
    ASIAN_LAO,
    ASIAN_PAKISTANI,
    ASIAN_VIETNAMESE,
    ASIAN_OTHER,

    BLACK,
    BLACK_AA,
    BLACK_BARBADIAN,
    BLACK_CARIBBEAN,
    BLACK_ETHIOPIAN,
    BLACK_GHANAIAN,
    BLACK_HAITIAN,
    BLACK_JAMAICAN,
    BLACK_LIBERIAN,
    BLACK_NIGERIAN,
    BLACK_SOMALI,
    BLACK_SOUTH_AFRICAN,
    BLACK_OTHER,

    HISPANIC,
    HISPANIC_COLOMBIAN,
    HISPANIC_CUBAN,
    HISPANIC_DOMINICAN,
    HISPANIC_ECUADORIAN,
    HISPANIC_HONDURAN,
    HISPANIC_MEXICAN,
    HISPANIC_PUERTO_RICAN,
    HISPANIC_SALVADORAN,
    HISPANIC_SPANISH,
    HISPANIC_OTHER,

    MENA,
    MENA_AFGHAN,
    MENA_ALGERIAN,
    MENA_EGYPTIAN,
    MENA_IRANIAN,
    MENA_IRAQI,
    MENA_ISRAELI,
    MENA_LEBANESE,
    MENA_MOROCCAN,
    MENA_SYRIAN,
    MENA_TUNISIAN,
    MENA_OTHER,

    NHPI,
    NHPI_CHAMORRO,
    NHPI_CHUUKESE,
    NHPI_FIJIAN,
    NHPI_MARSHALLESE,
    NHPI_HAWAIIAN,
    NHPI_PALAUAN,
    NHPI_SAMOAN,
    NHPI_TAHITIAN,
    NHPI_TONGAN,
    NHPI_OTHER,

    WHITE,
    WHITE_DUTCH,
    WHITE_ENGLISH,
    WHITE_EUROPEAN,
    WHITE_FRENCH,
    WHITE_GERMAN,
    WHITE_IRISH,
    WHITE_ITALIAN,
    WHITE_NORWEGIAN,
    WHITE_POLISH,
    WHITE_SCOTTISH,
    WHITE_SPANISH,
    WHITE_OTHER,

    OTHER,
    PREFER_NOT_TO_ANSWER
  }

  public enum DbGenderIdentityV2 {
    GENDERQUEER,
    MAN,
    NON_BINARY,
    QUESTIONING,
    TRANS_MAN,
    TRANS_WOMAN,
    TWO_SPIRIT,
    WOMAN,
    OTHER,
    PREFER_NOT_TO_ANSWER
  }

  public enum DbSexualOrientationV2 {
    ASEXUAL,
    BISEXUAL,
    GAY,
    LESBIAN,
    POLYSEXUAL,
    QUEER,
    QUESTIONING,
    SAME_GENDER,
    STRAIGHT,
    TWO_SPIRIT,
    OTHER,
    PREFER_NOT_TO_ANSWER
  }

  public enum DbSexAtBirthV2 {
    FEMALE,
    INTERSEX,
    MALE,
    OTHER,
    PREFER_NOT_TO_ANSWER
  }

  public enum DbEducationV2 {
    NO_EDUCATION,
    GRADES_1_12,
    UNDERGRADUATE,
    COLLEGE_GRADUATE,
    MASTER,
    DOCTORATE,
    PREFER_NOT_TO_ANSWER
  }

  public enum DbYesNoPreferNot {
    YES,
    NO,
    PREFER_NOT_TO_ANSWER
  }
}
