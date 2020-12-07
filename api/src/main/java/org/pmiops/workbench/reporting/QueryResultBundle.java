package org.pmiops.workbench.reporting;

import java.util.List;
import org.pmiops.workbench.db.dao.projection.ProjectedReportingCohort;
import org.pmiops.workbench.db.dao.projection.ProjectedReportingInstitution;
import org.pmiops.workbench.db.dao.projection.ProjectedReportingUser;
import org.pmiops.workbench.model.ReportingDataset;
import org.pmiops.workbench.model.ReportingDatasetCohort;
import org.pmiops.workbench.model.ReportingDatasetConceptSet;
import org.pmiops.workbench.model.ReportingDatasetDomainIdValue;
import org.pmiops.workbench.model.ReportingWorkspace;

// Define immutable value class to hold results of queries within a transaction. Mapping to
// Reporting DTO classes will happen outside the transaction. The main reason for this class
// is to support fine-graining performance measurement by separating the queries from the DTO
// conversion.
public class QueryResultBundle {

  private final List<ProjectedReportingCohort> cohorts;
  private final List<ReportingDataset> datasets;
  private final List<ReportingDatasetCohort> datasetCohorts;
  private final List<ReportingDatasetConceptSet> datasetConceptSets;
  private final List<ReportingDatasetDomainIdValue> datasetDomainIdValues;
  private final List<ProjectedReportingUser> users;
  private final List<ProjectedReportingInstitution> institutions;
  private final List<ReportingWorkspace> workspaces;

  public QueryResultBundle(
      List<ProjectedReportingCohort> cohorts,
      List<ReportingDataset> datasets,
      List<ReportingDatasetCohort> datasetCohorts,
      List<ReportingDatasetConceptSet> datasetConceptSets,
      List<ReportingDatasetDomainIdValue> datasetDomainIdValues,
      List<ProjectedReportingInstitution> institutions,
      List<ProjectedReportingUser> users,
      List<ReportingWorkspace> workspaces) {
    this.cohorts = cohorts;
    this.datasets = datasets;
    this.datasetCohorts = datasetCohorts;
    this.datasetConceptSets = datasetConceptSets;
    this.datasetDomainIdValues = datasetDomainIdValues;
    this.users = users;
    this.workspaces = workspaces;
    this.institutions = institutions;
  }

  public List<ProjectedReportingCohort> getCohorts() {
    return cohorts;
  }

  // Since the DatasetCohorts "dao" creates DTOs directly, we don't need to work with
  // a projection class for these.
  public List<ReportingDatasetCohort> getDatasetCohorts() {
    return datasetCohorts;
  }

  public List<ReportingDatasetConceptSet> getDatasetConceptSets() {
    return datasetConceptSets;
  }

  public List<ReportingDatasetDomainIdValue> getDatasetDomainIdValues() {
    return datasetDomainIdValues;
  }

  public List<ProjectedReportingInstitution> getInstitutions() {
    return institutions;
  }

  public List<ProjectedReportingUser> getUsers() {
    return users;
  }

  public List<ReportingWorkspace> getWorkspaces() {
    return workspaces;
  }

  public List<ReportingDataset> getDatasets() {
    return datasets;
  }
}
