package org.pmiops.workbench.db.model;

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
import org.pmiops.workbench.model.Gender;
import org.pmiops.workbench.model.Race;
import org.pmiops.workbench.model.SexAtBirth;
import org.pmiops.workbench.model.SexualOrientation;

@Entity
@Table(name = "demographic_survey")
public class DbDemographicSurvey {
  private Short disability;
  private Short education;
  private Short ethnicity;
  private List<Short> gender;
  private long id;
  private DbUser user;
  private List<Short> race;
  private List<Short> sexAtBirth;
  private List<Short> sexualOrientation;
  private int year_of_birth;

  public DbDemographicSurvey() {}

  public DbDemographicSurvey(org.pmiops.workbench.model.DemographicSurvey demographicSurvey) {
    this.disability = DemographicSurveyEnum.disabilityToStorage(demographicSurvey.getDisability());
    this.education = DemographicSurveyEnum.educationToStorage(demographicSurvey.getEducation());
    this.ethnicity = DemographicSurveyEnum.ethnicityToStorage(demographicSurvey.getEthnicity());
    this.gender =
        demographicSurvey.getGender().stream()
            .map(DemographicSurveyEnum::genderToStorage)
            .collect(Collectors.toList());
    this.race =
        demographicSurvey.getRace().stream()
            .map(DemographicSurveyEnum::raceToStorage)
            .collect(Collectors.toList());
    this.sexAtBirth =
        demographicSurvey.getSexAtBirth().stream()
            .map(DemographicSurveyEnum::sexAtBirthToStorage)
            .collect(Collectors.toList());
    this.sexualOrientation =
        demographicSurvey.getSexualOrientation().stream()
            .map(DemographicSurveyEnum::sexualOrientationToStorage)
            .collect(Collectors.toList());
    this.year_of_birth = demographicSurvey.getYearOfBirth().intValue();
  }

  @Column(name = "disability")
  public Short getDisability() {
    return disability;
  }

  public void setDisability(Short disability) {
    this.disability = disability;
  }

  @Transient
  public Disability getDisabilityEnum() {
    return DemographicSurveyEnum.disabilityFromStorage(disability);
  }

  public void setDisabilityEnum(Disability disability) {
    this.disability = DemographicSurveyEnum.disabilityToStorage(disability);
  }

  @Column(name = "education")
  public Short getEducation() {
    return education;
  }

  public void setEducation(Short education) {
    this.education = education;
  }

  @Transient
  public Education getEducationEnum() {
    if (education == null) return null;
    return DemographicSurveyEnum.educationFromStorage(education);
  }

  public void setEducationEnum(Education education) {
    this.education = DemographicSurveyEnum.educationToStorage(education);
  }

  @Column(name = "ethnicity")
  public Short getEthnicity() {
    return ethnicity;
  }

  public void setEthnicity(Short ethnicity) {
    this.ethnicity = ethnicity;
  }

  @Transient
  public Ethnicity getEthnicityEnum() {
    if (ethnicity == null) return null;
    return DemographicSurveyEnum.ethnicityFromStorage(ethnicity);
  }

  public void setEthnicityEnum(Ethnicity ethnicity) {
    this.ethnicity = DemographicSurveyEnum.ethnicityToStorage(ethnicity);
  }

  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(
      name = "demographic_survey_gender",
      joinColumns = @JoinColumn(name = "demographic_survey_id"))
  @Column(name = "gender")
  public List<Short> getGender() {
    return gender;
  }

  public void setGender(List<Short> gender) {
    this.gender = gender;
  }

  @Transient
  public List<Gender> getGenderEnum() {
    if (gender == null) return null;
    return this.gender.stream()
        .map(DemographicSurveyEnum::genderFromStorage)
        .collect(Collectors.toList());
  }

  public void setGenderEnum(List<Gender> genderList) {
    this.gender =
        genderList.stream()
            .map(DemographicSurveyEnum::genderToStorage)
            .collect(Collectors.toList());
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "demographic_survey_id")
  public long getId() {
    return id;
  }

  public void setId(long demographic_survey_id) {
    this.id = demographic_survey_id;
  }

  @ManyToOne
  @JoinColumn(name = "user_id")
  public DbUser getUser() {
    return user;
  }

  public void setUser(DbUser user) {
    this.user = user;
  }

  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(
      name = "demographic_survey_race",
      joinColumns = @JoinColumn(name = "demographic_survey_id"))
  @Column(name = "race")
  public List<Short> getRace() {
    return race;
  }

  public void setRace(List<Short> race) {
    this.race = race;
  }

  @Transient
  public List<Race> getRaceEnum() {
    if (race == null) return null;
    return this.race.stream()
        .map(DemographicSurveyEnum::raceFromStorage)
        .collect(Collectors.toList());
  }

  public void setRaceEnum(List<Race> raceList) {
    this.race =
        raceList.stream()
            .map(DemographicSurveyEnum::raceToStorage)
            .collect(Collectors.toList());
  }

  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(
      name = "demographic_survey_sex_at_birth",
      joinColumns = @JoinColumn(name = "demographic_survey_id"))
  @Column(name = "sex_at_birth")
  public List<Short> getSexAtBirth() {
    return sexAtBirth;
  }

  public void setSexAtBirth(List<Short> sexAtBirth) {
    this.sexAtBirth = sexAtBirth;
  }

  @Transient
  public List<SexAtBirth> getSexAtBirthEnum() {
    if (sexAtBirth == null) return null;
    return this.sexAtBirth.stream()
        .map(DemographicSurveyEnum::sexAtBirthFromStorage)
        .collect(Collectors.toList());
  }

  public void setSexAtBirthEnum(List<SexAtBirth> sexAtBirthList) {
    this.sexAtBirth =
        sexAtBirthList.stream()
            .map(DemographicSurveyEnum::sexAtBirthToStorage)
            .collect(Collectors.toList());
  }

  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(
      name = "demographic_survey_sexual_orientation",
      joinColumns = @JoinColumn(name = "demographic_survey_id"))
  @Column(name = "sexual_orientation")
  public List<Short> getSexualOrientation() {
    return sexualOrientation;
  }

  public void setSexualOrientation(List<Short> sexualOrientation) {
    this.sexualOrientation = sexualOrientation;
  }

  @Transient
  public List<SexualOrientation> getSexualOrientationEnum() {
    if (sexualOrientation == null) return null;
    return this.sexualOrientation.stream()
        .map(DemographicSurveyEnum::sexualOrientationFromStorage)
        .collect(Collectors.toList());
  }

  public void setSexualOrientationEnum(List<SexualOrientation> sexualOrientationList) {
    this.sexualOrientation =
        sexualOrientationList.stream()
            .map(DemographicSurveyEnum::sexualOrientationToStorage)
            .collect(Collectors.toList());
  }

  @Column(name = "year_of_birth")
  public int getYear_of_birth() {
    return year_of_birth;
  }

  public void setYear_of_birth(int year_of_birth) {
    this.year_of_birth = year_of_birth;
  }
}
