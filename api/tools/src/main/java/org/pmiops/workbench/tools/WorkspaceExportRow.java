package org.pmiops.workbench.tools;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvBindByPosition;

public class WorkspaceExportRow {

  public String getCreatorContactEmail() {
    return creatorContactEmail;
  }

  public void setCreatorContactEmail(String creatorContactEmail) {
    this.creatorContactEmail = creatorContactEmail;
  }

  @CsvBindByName(column = "User email")
  @CsvBindByPosition(position = 0)
  private String creatorContactEmail;

  public String getCreatorUsername() {
    return creatorUsername;
  }

  public void setCreatorUsername(String creatorUsername) {
    this.creatorUsername = creatorUsername;
  }

  @CsvBindByName(column = "Username")
  @CsvBindByPosition(position = 1)
  private String creatorUsername;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @CsvBindByName(column = "name")
  @CsvBindByPosition(position = 2)
  private String name;

  public String getInstitution() {
    return institution;
  }

  public void setInstitution(String institution) {
    this.institution = institution;
  }

  @CsvBindByName(column = "Institution")
  @CsvBindByPosition(position = 3)
  private String institution;

  public String getInstitutionalRole() {
    return institutionalRole;
  }

  public void setInstitutionalRole(String institutionalRole) {
    this.institutionalRole = institutionalRole;
  }

  @CsvBindByName(column = "Institutional role")
  @CsvBindByPosition(position = 4)
  private String institutionalRole;

  public String getCreatorFirstSignIn() {
    return creatorFirstSignIn;
  }

  public void setCreatorFirstSignIn(String creatorFirstSignIn) {
    this.creatorFirstSignIn = creatorFirstSignIn;
  }

  @CsvBindByName(column = "First Sign In")
  @CsvBindByPosition(position = 5)
  private String creatorFirstSignIn;

  public String getCreatorRegistrationState() {
    return creatorRegistrationState;
  }

  public void setCreatorRegistrationState(String creatorRegistrationState) {
    this.creatorRegistrationState = creatorRegistrationState;
  }

  @CsvBindByName(column = "Registration State")
  @CsvBindByPosition(position = 6)
  private String creatorRegistrationState;

  public String getTwoFactorAuthCompletionDate() {
    return twoFactorAuthCompletionDate;
  }

  public void setTwoFactorAuthCompletionDate(String twoFactorAuthCompletionDate) {
    this.twoFactorAuthCompletionDate = twoFactorAuthCompletionDate;
  }

  @CsvBindByName(column = "2fa Completion Time (CST)")
  @CsvBindByPosition(position = 7)
  private String twoFactorAuthCompletionDate;

  public String getEraCompletionDate() {
    return eraCompletionDate;
  }

  public void setEraCompletionDate(String eraCompletionDate) {
    this.eraCompletionDate = eraCompletionDate;
  }

  @CsvBindByName(column = "eRA Completion Time (CST)")
  @CsvBindByPosition(position = 8)
  private String eraCompletionDate;

  public String getTrainingCompletionDate() {
    return trainingCompletionDate;
  }

  public void setTrainingCompletionDate(String trainingCompletionDate) {
    this.trainingCompletionDate = trainingCompletionDate;
  }

  @CsvBindByName(column = "Training Completion Time (CST)")
  @CsvBindByPosition(position = 9)
  private String trainingCompletionDate;

  public String getDuccCompletionDate() {
    return duccCompletionDate;
  }

  public void setDuccCompletionDate(String duccCompletionDate) {
    this.duccCompletionDate = duccCompletionDate;
  }

  @CsvBindByName(column = "DUCC Completion Time (CST)")
  @CsvBindByPosition(position = 10)
  private String duccCompletionDate;

  public String getDegrees() {
    return degrees;
  }

  public void setDegrees(String degrees) {
    this.degrees = degrees;
  }

  @CsvBindByName(column = "Degrees")
  @CsvBindByPosition(position = 11)
  private String degrees;

  public String getRace() {
    return race;
  }

  public void setRace(String race) {
    this.race = race;
  }

  @CsvBindByName(column = "Race")
  @CsvBindByPosition(position = 12)
  private String race;

  public String getEthnicity() {
    return ethnicity;
  }

  public void setEthnicity(String ethnicity) {
    this.ethnicity = ethnicity;
  }

  @CsvBindByName(column = "Ethnicity")
  @CsvBindByPosition(position = 13)
  private String ethnicity;

  public String getGenderIdentity() {
    return genderIdentity;
  }

  public void setGenderIdentity(String genderIdentity) {
    this.genderIdentity = genderIdentity;
  }

  @CsvBindByName(column = "Gender Identity")
  @CsvBindByPosition(position = 14)
  private String genderIdentity;

  public String getIdentifyAsLgbtq() {
    return identifyAsLgbtq;
  }

  public void setIdentifyAsLgbtq(String identifyAsLgbtq) {
    this.identifyAsLgbtq = identifyAsLgbtq;
  }

  @CsvBindByName(column = "Identify as LGBTQ")
  @CsvBindByPosition(position = 15)
  private String identifyAsLgbtq;

  public String getLgbtqIdentity() {
    return lgbtqIdentity;
  }

  public void setLgbtqIdentity(String lgbtqIdentity) {
    this.lgbtqIdentity = lgbtqIdentity;
  }

  @CsvBindByName(column = "LGBTQ Identity")
  @CsvBindByPosition(position = 16)
  private String lgbtqIdentity;

  public String getSexAtBirth() {
    return sexAtBirth;
  }

  public void setSexAtBirth(String sexAtBirth) {
    this.sexAtBirth = sexAtBirth;
  }

  @CsvBindByName(column = "Sex at birth")
  @CsvBindByPosition(position = 17)
  private String sexAtBirth;

  public String getYearOfBirth() {
    return yearOfBirth;
  }

  public void setYearOfBirth(String yearOfBirth) {
    this.yearOfBirth = yearOfBirth;
  }

  @CsvBindByName(column = "Year of birth")
  @CsvBindByPosition(position = 18)
  private String yearOfBirth;

  public String getDisability() {
    return disability;
  }

  public void setDisability(String disability) {
    this.disability = disability;
  }

  @CsvBindByName(column = "Disability")
  @CsvBindByPosition(position = 19)
  private String disability;

  public String getHighestEducation() {
    return highestEducation;
  }

  public void setHighestEducation(String highestEducation) {
    this.highestEducation = highestEducation;
  }

  @CsvBindByName(column = "HighestEducation")
  @CsvBindByPosition(position = 20)
  private String highestEducation;

  public String getProjectId() {
    return projectId;
  }

  public void setProjectId(String projectId) {
    this.projectId = projectId;
  }

  @CsvBindByName(column = "Workspace Project ID")
  @CsvBindByPosition(position = 21)
  private String projectId;

  public String getWorkspaceName() {
    return workspaceName;
  }

  public void setWorkspaceName(String workspaceName) {
    this.workspaceName = workspaceName;
  }

  @CsvBindByName(column = "Workspace Name")
  @CsvBindByPosition(position = 22)
  private String workspaceName;

  public String getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(String createdDate) {
    this.createdDate = createdDate;
  }

  @CsvBindByName(column = "Workspace Created Time (CST)")
  @CsvBindByPosition(position = 23)
  private String createdDate;

  public String getCollaborators() {
    return collaborators;
  }

  public void setCollaborators(String collaborators) {
    this.collaborators = collaborators;
  }

  @CsvBindByName(column = "Other users in the workspace")
  @CsvBindByPosition(position = 24)
  private String collaborators;

  public String getCohortNames() {
    return cohortNames;
  }

  public void setCohortNames(String cohortNames) {
    this.cohortNames = cohortNames;
  }

  @CsvBindByName(column = "Cohort Names")
  @CsvBindByPosition(position = 25)
  private String cohortNames;

  public String getCohortCount() {
    return cohortCount;
  }

  public void setCohortCount(String cohortCount) {
    this.cohortCount = cohortCount;
  }

  @CsvBindByName(column = "Workspace Cohort Count")
  @CsvBindByPosition(position = 26)
  private String cohortCount;

  public String getConceptSetNames() {
    return conceptSetNames;
  }

  public void setConceptSetNames(String conceptSetNames) {
    this.conceptSetNames = conceptSetNames;
  }

  @CsvBindByName(column = "Concept Set Names")
  @CsvBindByPosition(position = 27)
  private String conceptSetNames;

  public String getConceptSetCount() {
    return conceptSetCount;
  }

  public void setConceptSetCount(String conceptSetCount) {
    this.conceptSetCount = conceptSetCount;
  }

  @CsvBindByName(column = "Workspace Concept Set Count")
  @CsvBindByPosition(position = 28)
  private String conceptSetCount;

  public String getDatasetNames() {
    return datasetNames;
  }

  public void setDatasetNames(String datasetNames) {
    this.datasetNames = datasetNames;
  }

  @CsvBindByName(column = "Dataset Names")
  @CsvBindByPosition(position = 29)
  private String datasetNames;

  public String getDatasetCount() {
    return datasetCount;
  }

  public void setDatasetCount(String datasetCount) {
    this.datasetCount = datasetCount;
  }

  @CsvBindByName(column = "Workspace Dataset Count")
  @CsvBindByPosition(position = 30)
  private String datasetCount;

  public String getNotebookNames() {
    return notebookNames;
  }

  public void setNotebookNames(String notebookNames) {
    this.notebookNames = notebookNames;
  }

  @CsvBindByName(column = "Notebook Names")
  @CsvBindByPosition(position = 31)
  private String notebookNames;

  public String getNotebooksCount() {
    return notebooksCount;
  }

  public void setNotebooksCount(String notebooksCount) {
    this.notebooksCount = notebooksCount;
  }

  @CsvBindByName(column = "Workspace Notebook Count")
  @CsvBindByPosition(position = 32)
  private String notebooksCount;

  public String getWorkspaceSpending() {
    return workspaceSpending;
  }

  public void setWorkspaceSpending(String workspaceSpending) {
    this.workspaceSpending = workspaceSpending;
  }

  @CsvBindByName(column = "Workspace Spending")
  @CsvBindByPosition(position = 33)
  private String workspaceSpending;

  public String getReviewForStigmatizingResearch() {
    return reviewForStigmatizingResearch;
  }

  public void setReviewForStigmatizingResearch(String reviewForStigmatizingResearch) {
    this.reviewForStigmatizingResearch = reviewForStigmatizingResearch;
  }

  @CsvBindByName(column = "Review for Stigmatizing research (Y/N)")
  @CsvBindByPosition(position = 34)
  private String reviewForStigmatizingResearch;

  public String getWorkspaceLastUpdatedDate() {
    return workspaceLastUpdatedDate;
  }

  public void setWorkspaceLastUpdatedDate(String workspaceLastUpdatedDate) {
    this.workspaceLastUpdatedDate = workspaceLastUpdatedDate;
  }

  @CsvBindByName(column = "Workspace Last Updated Time (CST)")
  @CsvBindByPosition(position = 35)
  private String workspaceLastUpdatedDate;

  public String getActive() {
    return active;
  }

  public void setActive(String active) {
    this.active = active;
  }

  @CsvBindByName(column = "Is Workspace active? (Y/N)")
  @CsvBindByPosition(position = 36)
  private String active;
}
