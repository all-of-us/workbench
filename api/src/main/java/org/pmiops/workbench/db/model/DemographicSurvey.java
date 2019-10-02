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

@Entity
@Table(name = "demographic_survey")
public class DemographicSurvey {
  private long id;
  private List<Short> race;
  private Short ethnicity;
  private List<Short> gender;
  private int year_of_birth;
  private Short education;
  private Short disability;
  private User user;

  public DemographicSurvey() {}

  public DemographicSurvey(org.pmiops.workbench.model.DemographicSurvey demographicSurvey) {
    this.race =
        demographicSurvey.getRace().stream()
            .map((race) -> DemographicSurveyEnum.raceToStorage(race))
            .collect(Collectors.toList());
    this.ethnicity = DemographicSurveyEnum.ethnicityToStorage(demographicSurvey.getEthnicity());
    this.gender =
        demographicSurvey.getGender().stream()
            .map((gender) -> DemographicSurveyEnum.genderToStorage(gender))
            .collect(Collectors.toList());
    this.year_of_birth = demographicSurvey.getYearOfBirth().intValue();
    this.education = DemographicSurveyEnum.educationToStorage(demographicSurvey.getEducation());
    this.disability = DemographicSurveyEnum.disabilityToStorage(demographicSurvey.getDisability());
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
        .map(
            (raceObject) -> {
              return DemographicSurveyEnum.raceFromStorage(raceObject);
            })
        .collect(Collectors.toList());
  }

  public void setRaceEnum(List<Race> raceList) {
    this.race =
        raceList.stream()
            .map(
                (race) -> {
                  return DemographicSurveyEnum.raceToStorage(race);
                })
            .collect(Collectors.toList());
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
        .map(
            (gender) -> {
              return DemographicSurveyEnum.genderFromStorage(gender);
            })
        .collect(Collectors.toList());
  }

  public void setGenderEnum(List<Gender> genderList) {
    this.gender =
        genderList.stream()
            .map(
                (gender) -> {
                  return DemographicSurveyEnum.genderToStorage(gender);
                })
            .collect(Collectors.toList());
  }

  @Column(name = "year_of_birth")
  public int getYear_of_birth() {
    return year_of_birth;
  }

  public void setYear_of_birth(int year_of_birth) {
    this.year_of_birth = year_of_birth;
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

  @ManyToOne
  @JoinColumn(name = "user_id")
  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }
}
