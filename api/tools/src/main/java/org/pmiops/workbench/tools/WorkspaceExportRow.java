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

  public String getInstitution() {
    return institution;
  }

  public void setInstitution(String institution) {
    this.institution = institution;
  }

  @CsvBindByName(column = "Institution")
  @CsvBindByPosition(position = 2)
  private String institution;

  public String getCreatorFirstSignIn() {
    return creatorFirstSignIn;
  }

  public void setCreatorFirstSignIn(String creatorFirstSignIn) {
    this.creatorFirstSignIn = creatorFirstSignIn;
  }

  @CsvBindByName(column = "First Sign In")
  @CsvBindByPosition(position = 3)
  private String creatorFirstSignIn;

  public String getCreatorRegistrationState() {
    return creatorRegistrationState;
  }

  public void setCreatorRegistrationState(String creatorRegistrationState) {
    this.creatorRegistrationState = creatorRegistrationState;
  }

  @CsvBindByName(column = "Registration State")
  @CsvBindByPosition(position = 4)
  private String creatorRegistrationState;

  public String getTwoFactorAuthCompletionDate() {
    return twoFactorAuthCompletionDate;
  }

  public void setTwoFactorAuthCompletionDate(String twoFactorAuthCompletionDate) {
    this.twoFactorAuthCompletionDate = twoFactorAuthCompletionDate;
  }

  @CsvBindByName(column = "2fa Completion Date")
  @CsvBindByPosition(position = 5)
  private String twoFactorAuthCompletionDate;

  public String getEraCompletionDate() {
    return eraCompletionDate;
  }

  public void setEraCompletionDate(String eraCompletionDate) {
    this.eraCompletionDate = eraCompletionDate;
  }

  @CsvBindByName(column = "eRA Completion Date")
  @CsvBindByPosition(position = 6)
  private String eraCompletionDate;

  public String getTrainingCompletionDate() {
    return trainingCompletionDate;
  }

  public void setTrainingCompletionDate(String trainingCompletionDate) {
    this.trainingCompletionDate = trainingCompletionDate;
  }

  @CsvBindByName(column = "Training Completion Date")
  @CsvBindByPosition(position = 7)
  private String trainingCompletionDate;

  public String getDuccCompletionDate() {
    return duccCompletionDate;
  }

  public void setDuccCompletionDate(String duccCompletionDate) {
    this.duccCompletionDate = duccCompletionDate;
  }

  @CsvBindByName(column = "DUCC Completion Date")
  @CsvBindByPosition(position = 8)
  private String duccCompletionDate;

  public String getProjectId() {
    return projectId;
  }

  public void setProjectId(String projectId) {
    this.projectId = projectId;
  }

  @CsvBindByName(column = "Workspace Project ID")
  @CsvBindByPosition(position = 9)
  private String projectId;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @CsvBindByName(column = "Workspace Name")
  @CsvBindByPosition(position = 10)
  private String name;

  public String getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(String createdDate) {
    this.createdDate = createdDate;
  }

  @CsvBindByName(column = "Workspace Created Date")
  @CsvBindByPosition(position = 11)
  private String createdDate;

  public String getCollaborators() {
    return collaborators;
  }

  public void setCollaborators(String collaborators) {
    this.collaborators = collaborators;
  }

  @CsvBindByName(column = "Other users in the workspace")
  @CsvBindByPosition(position = 12)
  private String collaborators;

  public String getCohortNames() {
    return cohortNames;
  }

  public void setCohortNames(String cohortNames) {
    this.cohortNames = cohortNames;
  }

  @CsvBindByName(column = "Cohort Names")
  @CsvBindByPosition(position = 13)
  private String cohortNames;

  public String getCohortCount() {
    return cohortCount;
  }

  public void setCohortCount(String cohortCount) {
    this.cohortCount = cohortCount;
  }

  @CsvBindByName(column = "Workspace Cohort Count")
  @CsvBindByPosition(position = 14)
  private String cohortCount;

  public String getConceptSetNames() {
    return conceptSetNames;
  }

  public void setConceptSetNames(String conceptSetNames) {
    this.conceptSetNames = conceptSetNames;
  }

  @CsvBindByName(column = "Concept Set Names")
  @CsvBindByPosition(position = 15)
  private String conceptSetNames;

  public String getConceptSetCount() {
    return conceptSetCount;
  }

  public void setConceptSetCount(String conceptSetCount) {
    this.conceptSetCount = conceptSetCount;
  }

  @CsvBindByName(column = "Workspace Concept Set Count")
  @CsvBindByPosition(position = 16)
  private String conceptSetCount;

  public String getDatasetNames() {
    return datasetNames;
  }

  public void setDatasetNames(String datasetNames) {
    this.datasetNames = datasetNames;
  }

  @CsvBindByName(column = "Dataset Names")
  @CsvBindByPosition(position = 17)
  private String datasetNames;

  public String getDatasetCount() {
    return datasetCount;
  }

  public void setDatasetCount(String datasetCount) {
    this.datasetCount = datasetCount;
  }

  @CsvBindByName(column = "Workspace Dataset Count")
  @CsvBindByPosition(position = 18)
  private String datasetCount;

  public String getNotebookNames() {
    return notebookNames;
  }

  public void setNotebookNames(String notebookNames) {
    this.notebookNames = notebookNames;
  }

  @CsvBindByName(column = "Notebook Names")
  @CsvBindByPosition(position = 19)
  private String notebookNames;

  public String getNotebooksCount() {
    return notebooksCount;
  }

  public void setNotebooksCount(String notebooksCount) {
    this.notebooksCount = notebooksCount;
  }

  @CsvBindByName(column = "Workspace Notebook Count")
  @CsvBindByPosition(position = 20)
  private String notebooksCount;

  public String getWorkspaceSpending() {
    return workspaceSpending;
  }

  public void setWorkspaceSpending(String workspaceSpending) {
    this.workspaceSpending = workspaceSpending;
  }

  @CsvBindByName(column = "Workspace Spending")
  @CsvBindByPosition(position = 21)
  private String workspaceSpending;

  public String getReviewForStigmatizingResearch() {
    return reviewForStigmatizingResearch;
  }

  public void setReviewForStigmatizingResearch(String reviewForStigmatizingResearch) {
    this.reviewForStigmatizingResearch = reviewForStigmatizingResearch;
  }

  @CsvBindByName(column = "Review for Stigmatizing research (Y/N)")
  @CsvBindByPosition(position = 22)
  private String reviewForStigmatizingResearch;

  public String getWorkspaceLastUpdatedDate() {
    return workspaceLastUpdatedDate;
  }

  public void setWorkspaceLastUpdatedDate(String workspaceLastUpdatedDate) {
    this.workspaceLastUpdatedDate = workspaceLastUpdatedDate;
  }

  @CsvBindByName(column = "Workspace Last Updated Date")
  @CsvBindByPosition(position = 23)
  private String workspaceLastUpdatedDate;

  public String getActive() {
    return active;
  }

  public void setActive(String active) {
    this.active = active;
  }

  @CsvBindByName(column = "Is Workspace active? (Y/N)")
  @CsvBindByPosition(position = 24)
  private String active;
}
