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

  public void setDisability(Short disability) {
    this.disability = disability;
  }

  @Transient
  public Disability getDisabilityEnum() {
    return DbStorageEnums.disabilityFromStorage(disability);
  }

  public void setDisabilityEnum(Disability disability) {
    this.disability = DbStorageEnums.disabilityToStorage(disability);
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
    return DbStorageEnums.educationFromStorage(education);
  }

  public void setEducationEnum(Education education) {
    this.education = DbStorageEnums.educationToStorage(education);
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
    return DbStorageEnums.ethnicityFromStorage(ethnicity);
  }

  public void setEthnicityEnum(Ethnicity ethnicity) {
    this.ethnicity = DbStorageEnums.ethnicityToStorage(ethnicity);
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
    return this.race.stream().map(DbStorageEnums::raceFromStorage).collect(Collectors.toList());
  }

  public void setRaceEnum(List<Race> raceList) {
    this.race = raceList.stream().map(DbStorageEnums::raceToStorage).collect(Collectors.toList());
  }

  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(
      name = "demographic_survey_gender_identity",
      joinColumns = @JoinColumn(name = "demographic_survey_id"))
  @Column(name = "gender_identity")
  public List<Short> getGenderIdentityList() {
    return genderIdentityList;
  }

  public void setGenderIdentityList(List<Short> genderIdentityList) {
    this.genderIdentityList = genderIdentityList;
  }

  @Transient
  public List<GenderIdentity> getGenderIdentityEnumList() {
    if (genderIdentityList == null) return new ArrayList<>();
    return this.genderIdentityList.stream()
        .map(DbStorageEnums::genderIdentityFromStorage)
        .collect(Collectors.toList());
  }

  public void setGenderIdentityEnumList(List<GenderIdentity> genderList) {
    this.genderIdentityList =
        genderList.stream()
            .map(DbStorageEnums::genderIdentityToStorage)
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
        .map(DbStorageEnums::sexAtBirthFromStorage)
        .collect(Collectors.toList());
  }

  public void setSexAtBirthEnum(List<SexAtBirth> sexAtBirthList) {
    this.sexAtBirth =
        sexAtBirthList.stream()
            .map(DbStorageEnums::sexAtBirthToStorage)
            .collect(Collectors.toList());
  }

  @Column(name = "year_of_birth")
  public int getYearOfBirth() {
    return yearOfBirth;
  }

  public void setYearOfBirth(int yearOfBirth) {
    this.yearOfBirth = yearOfBirth;
  }
}
