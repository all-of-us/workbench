package org.pmiops.workbench.workspaces.resources;

import javax.annotation.Nullable;
import org.pmiops.workbench.model.Cohort;
import org.pmiops.workbench.model.CohortReview;
import org.pmiops.workbench.model.ConceptSet;
import org.pmiops.workbench.model.DataSet;
import org.pmiops.workbench.model.FileDetail;

/**
 * A transitional POJO to assist WorkspaceResourceMapper, consisting of the fields sourced from the
 * resources
 */
class ResourceFields {

  private Long recentlyModifiedId;
  private Cohort cohort;
  private CohortReview cohortReview;
  private FileDetail notebook;
  private ConceptSet conceptSet;
  private DataSet dataSet;
  private String lastModifiedBy;
  private Long lastModifiedEpochMillis;

  public Cohort getCohort() {
    return cohort;
  }

  public ResourceFields setCohort(Cohort cohort) {
    this.cohort = cohort;
    return this;
  }

  public CohortReview getCohortReview() {
    return cohortReview;
  }

  public ResourceFields setCohortReview(CohortReview cohortReview) {
    this.cohortReview = cohortReview;
    return this;
  }

  public FileDetail getNotebook() {
    return notebook;
  }

  public ResourceFields setNotebook(FileDetail notebook) {
    this.notebook = notebook;
    return this;
  }

  public ConceptSet getConceptSet() {
    return conceptSet;
  }

  public ResourceFields setConceptSet(ConceptSet conceptSet) {
    this.conceptSet = conceptSet;
    return this;
  }

  public DataSet getDataSet() {
    return dataSet;
  }

  public ResourceFields setDataSet(DataSet dataSet) {
    this.dataSet = dataSet;
    return this;
  }

  public Long getRecentlyModifiedId() {
    return recentlyModifiedId;
  }

  public ResourceFields setRecentlyModifiedId(Long recentlyModifiedId) {
    this.recentlyModifiedId = recentlyModifiedId;
    return this;
  }

  public String getLastModifiedBy() {
    return lastModifiedBy;
  }

  public ResourceFields setLastModifiedBy(String lastModifiedBy) {
    this.lastModifiedBy = lastModifiedBy;
    return this;
  }

  public Long getLastModifiedEpochMillis() {
    return lastModifiedEpochMillis;
  }

  public ResourceFields setLastModifiedEpochMillis(@Nullable Long lastModifiedEpochMillis) {
    this.lastModifiedEpochMillis = lastModifiedEpochMillis;
    return this;
  }
}
