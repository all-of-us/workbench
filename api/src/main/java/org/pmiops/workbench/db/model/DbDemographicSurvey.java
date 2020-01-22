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
  private List<Short> sexAtBirth;
  private int year_of_birth;

  public DbDemographicSurvey() {}

  public DbDemographicSurvey(org.pmiops.workbench.model.DemographicSurvey demographicSurvey) {
    this.disability = DemographicSurveyEnum.disabilityToStorage(demographicSurvey.getDisability());
    this.education = DemographicSurveyEnum.educationToStorage(demographicSurvey.getEducation());
    this.ethnicity = DemographicSurveyEnum.ethnicityToStorage(demographicSurvey.getEthnicity());
    this.identifiesAsLgbtq = demographicSurvey.getIdentifiesAsLgbtq();
    this.lgbtqIdentity = demographicSurvey.getLgbtqIdentity();
    this.race =
        demographicSurvey.getRace().stream()
            .map(DemographicSurveyEnum::raceToStorage)
            .collect(Collectors.toList());
    this.sexAtBirth =
        demographicSurvey.getSexAtBirth().stream()
            .map(DemographicSurveyEnum::sexAtBirthToStorage)
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

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "demographic_survey_id")
  public long getId() {
    return id;
  }

  public void setId(long demographic_survey_id) {
    this.id = demographic_survey_id;
  }

  @Column(name = "identifies_as_lgbtq")
  public Boolean getIdentifiesAsLgbtq() {
    return identifiesAsLgbtq;
  }

  public void setIdentifiesAsLgbtq(Boolean identifiesAsLgbtq) {
    this.identifiesAsLgbtq = identifiesAsLgbtq;
  }

  @Column(name = "lgbtq_identity")
  public String getLgbtqIdentity() {
    return lgbtqIdentity;
  }

  public void setLgbtqIdentity(String lgbtqIdentity) {
    this.lgbtqIdentity = lgbtqIdentity;
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
        raceList.stream().map(DemographicSurveyEnum::raceToStorage).collect(Collectors.toList());
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

  @Column(name = "year_of_birth")
  public int getYear_of_birth() {
    return year_of_birth;
  }

  public void setYear_of_birth(int year_of_birth) {
    this.year_of_birth = year_of_birth;
  }
}
