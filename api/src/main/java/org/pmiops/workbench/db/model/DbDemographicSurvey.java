package org.pmiops.workbench.db.model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.pmiops.workbench.model.Disability;
import org.pmiops.workbench.model.Education;
import org.pmiops.workbench.model.Ethnicity;
import org.pmiops.workbench.model.GenderIdentity;
import org.pmiops.workbench.model.Race;
import org.pmiops.workbench.model.SexAtBirth;

@Entity
@Table(name = "demographic_survey")
public class DbDemographicSurvey {
  private Short disability;
  private Short education;
  private Short ethnicity;
  private long id;
  private Boolean identifiesAsLgbtq;
  private String lgbtqIdentity;
  private DbUser user;
  private List<Short> race;
  private List<Short> genderIdentityList;
  private List<Short> sexAtBirth;
  private int yearOfBirth;

  @Column(name = "disability")
  public Short getDisability() {
    return disability;
  }

  public DbDemographicSurvey setDisability(Short disability) {
    this.disability = disability;
    return this;
  }

  @Transient
  public Disability getDisabilityEnum() {
    return DbStorageEnums.disabilityFromStorage(disability);
  }

  public DbDemographicSurvey setDisabilityEnum(Disability disability) {
    this.disability = DbStorageEnums.disabilityToStorage(disability);
    return this;
  }

  @Column(name = "education")
  public Short getEducation() {
    return education;
  }

  public DbDemographicSurvey setEducation(Short education) {
    this.education = education;
    return this;
  }

  @Transient
  public Education getEducationEnum() {
    if (education == null) return null;
    return DbStorageEnums.educationFromStorage(education);
  }

  public DbDemographicSurvey setEducationEnum(Education education) {
    this.education = DbStorageEnums.educationToStorage(education);
    return this;
  }

  @Column(name = "ethnicity")
  public Short getEthnicity() {
    return ethnicity;
  }

  public DbDemographicSurvey setEthnicity(Short ethnicity) {
    this.ethnicity = ethnicity;
    return this;
  }

  @Transient
  public Ethnicity getEthnicityEnum() {
    if (ethnicity == null) return null;
    return DbStorageEnums.ethnicityFromStorage(ethnicity);
  }

  public DbDemographicSurvey setEthnicityEnum(Ethnicity ethnicity) {
    this.ethnicity = DbStorageEnums.ethnicityToStorage(ethnicity);
    return this;
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "demographic_survey_id")
  public long getId() {
    return id;
  }

  public DbDemographicSurvey setId(long demographic_survey_id) {
    this.id = demographic_survey_id;
    return this;
  }

  @Column(name = "identifies_as_lgbtq")
  public Boolean getIdentifiesAsLgbtq() {
    return identifiesAsLgbtq;
  }

  public DbDemographicSurvey setIdentifiesAsLgbtq(Boolean identifiesAsLgbtq) {
    this.identifiesAsLgbtq = identifiesAsLgbtq;
    return this;
  }

  @Column(name = "lgbtq_identity")
  public String getLgbtqIdentity() {
    return lgbtqIdentity;
  }

  public DbDemographicSurvey setLgbtqIdentity(String lgbtqIdentity) {
    this.lgbtqIdentity = lgbtqIdentity;
    return this;
  }

  @ManyToOne
  @JoinColumn(name = "user_id")
  public DbUser getUser() {
    return user;
  }

  public DbDemographicSurvey setUser(DbUser user) {
    this.user = user;
    return this;
  }

  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(
      name = "demographic_survey_race",
      joinColumns = @JoinColumn(name = "demographic_survey_id"))
  @Column(name = "race")
  public List<Short> getRace() {
    return race;
  }

  public DbDemographicSurvey setRace(List<Short> race) {
    this.race = race;
    return this;
  }

  @Transient
  public List<Race> getRaceEnum() {
    if (race == null) return null;
    return this.race.stream().map(DbStorageEnums::raceFromStorage).collect(Collectors.toList());
  }

  public DbDemographicSurvey setRaceEnum(List<Race> raceList) {
    this.race = raceList.stream().map(DbStorageEnums::raceToStorage).collect(Collectors.toList());
    return this;
  }

  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(
      name = "demographic_survey_gender_identity",
      joinColumns = @JoinColumn(name = "demographic_survey_id"))
  @Column(name = "gender_identity")
  public List<Short> getGenderIdentityList() {
    return genderIdentityList;
  }

  public DbDemographicSurvey setGenderIdentityList(List<Short> genderIdentityList) {
    this.genderIdentityList = genderIdentityList;
    return this;
  }

  @Transient
  public List<GenderIdentity> getGenderIdentityEnumList() {
    if (genderIdentityList == null) return new ArrayList<>();
    return this.genderIdentityList.stream()
        .map(DbStorageEnums::genderIdentityFromStorage)
        .collect(Collectors.toList());
  }

  public DbDemographicSurvey setGenderIdentityEnumList(List<GenderIdentity> genderList) {
    this.genderIdentityList =
        genderList.stream()
            .map(DbStorageEnums::genderIdentityToStorage)
            .collect(Collectors.toList());
    return this;
  }

  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(
      name = "demographic_survey_sex_at_birth",
      joinColumns = @JoinColumn(name = "demographic_survey_id"))
  @Column(name = "sex_at_birth")
  public List<Short> getSexAtBirth() {
    return sexAtBirth;
  }

  public DbDemographicSurvey setSexAtBirth(List<Short> sexAtBirth) {
    this.sexAtBirth = sexAtBirth;
    return this;
  }

  @Transient
  public List<SexAtBirth> getSexAtBirthEnum() {
    if (sexAtBirth == null) return null;
    return this.sexAtBirth.stream()
        .map(DbStorageEnums::sexAtBirthFromStorage)
        .collect(Collectors.toList());
  }

  public DbDemographicSurvey setSexAtBirthEnum(List<SexAtBirth> sexAtBirthList) {
    this.sexAtBirth =
        sexAtBirthList.stream()
            .map(DbStorageEnums::sexAtBirthToStorage)
            .collect(Collectors.toList());
    return this;
  }

  @Column(name = "year_of_birth")
  public int getYearOfBirth() {
    return yearOfBirth;
  }

  public DbDemographicSurvey setYearOfBirth(int yearOfBirth) {
    this.yearOfBirth = yearOfBirth;
    return this;
  }
}
