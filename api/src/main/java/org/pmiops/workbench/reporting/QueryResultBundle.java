package org.pmiops.workbench.reporting;

import java.util.List;
import org.pmiops.workbench.model.ReportingCohort;
import org.pmiops.workbench.model.ReportingDataset;
import org.pmiops.workbench.model.ReportingDatasetCohort;
import org.pmiops.workbench.model.ReportingDatasetConceptSet;
import org.pmiops.workbench.model.ReportingDatasetDomainIdValue;
import org.pmiops.workbench.model.ReportingInstitution;
import org.pmiops.workbench.model.ReportingUser;
import org.pmiops.workbench.model.ReportingWorkspace;

// Define immutable value class to hold results of queries within a transaction. Mapping to
// Reporting DTO classes will happen outside the transaction. The main reason for this class
// is to support fine-graining performance measurement by separating the queries from the DTO
// conversion.
@Deprecated
public class QueryResultBundle {

  private final List<ReportingCohort> cohorts;
  private final List<ReportingDataset> datasets;
  private final List<ReportingDatasetCohort> datasetCohorts;
  private final List<ReportingDatasetConceptSet> datasetConceptSets;
  private final List<ReportingDatasetDomainIdValue> datasetDomainIdValues;
  private final List<ReportingUser> users;
  private final List<ReportingInstitution> institutions;
  private final List<ReportingWorkspace> workspaces;

  public QueryResultBundle(
      List<ReportingCohort> cohorts,
      List<ReportingDataset> datasets,
      List<ReportingDatasetCohort> datasetCohorts,
      List<ReportingDatasetConceptSet> datasetConceptSets,
      List<ReportingDatasetDomainIdValue> datasetDomainIdValues,
      List<ReportingInstitution> institutions,
      List<ReportingUser> users,
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

  public List<ReportingCohort> getCohorts() {
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

  public List<ReportingInstitution> getInstitutions() {
    return institutions;
  }

  public List<ReportingUser> getUsers() {
    return users;
  }

  public List<ReportingWorkspace> getWorkspaces() {
    return workspaces;
  }

  public List<ReportingDataset> getDatasets() {
    return datasets;
  }
}
