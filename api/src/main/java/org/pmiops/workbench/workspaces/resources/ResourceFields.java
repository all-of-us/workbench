package org.pmiops.workbench.workspaces.resources;

import org.pmiops.workbench.model.Cohort;
import org.pmiops.workbench.model.CohortReview;
import org.pmiops.workbench.model.ConceptSet;
import org.pmiops.workbench.model.DataSet;
import org.pmiops.workbench.model.FileDetail;

/**
 * A transitional POJO to assist WorkspaceResourceMapper, consisting of the fields sourced from the
 * resources
 */
public class ResourceFields {
  private Cohort cohort;
  private CohortReview cohortReview;
  private FileDetail notebook;
  private ConceptSet conceptSet;
  private DataSet dataSet;
  private long lastModifiedEpochMillis;

  public Cohort getCohort() {
    return cohort;
  }

  public void setCohort(Cohort cohort) {
    this.cohort = cohort;
  }

  public CohortReview getCohortReview() {
    return cohortReview;
  }

  public void setCohortReview(CohortReview cohortReview) {
    this.cohortReview = cohortReview;
  }

  public FileDetail getNotebook() {
    return notebook;
  }

  public void setNotebook(FileDetail notebook) {
    this.notebook = notebook;
  }

  public ConceptSet getConceptSet() {
    return conceptSet;
  }

  public void setConceptSet(ConceptSet conceptSet) {
    this.conceptSet = conceptSet;
  }

  public DataSet getDataSet() {
    return dataSet;
  }

  public void setDataSet(DataSet dataSet) {
    this.dataSet = dataSet;
  }

  public long getLastModifiedEpochMillis() {
    return lastModifiedEpochMillis;
  }

  public void setLastModifiedEpochMillis(long lastModifiedEpochMillis) {
    this.lastModifiedEpochMillis = lastModifiedEpochMillis;
  }
}
