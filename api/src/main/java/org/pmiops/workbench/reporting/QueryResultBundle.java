package org.pmiops.workbench.reporting;

import java.util.List;
import org.pmiops.workbench.db.dao.projection.ProjectedReportingCohort;
import org.pmiops.workbench.db.dao.projection.ProjectedReportingInstitution;
import org.pmiops.workbench.db.dao.projection.ProjectedReportingUser;
import org.pmiops.workbench.db.dao.projection.ProjectedReportingWorkspace;

// Define immutable value class to hold results of queries within a transaction. Mapping to
// Reporting DTO classes will happen outside the transaction.
public class QueryResultBundle {

  private final List<ProjectedReportingCohort> cohorts;
  private final List<ProjectedReportingUser> users;
  private final List<ProjectedReportingInstitution> institutions;
  private final List<ProjectedReportingWorkspace> workspaces;

  public QueryResultBundle(
      List<ProjectedReportingCohort> cohorts,
      List<ProjectedReportingInstitution> institutions,
      List<ProjectedReportingUser> users,
      List<ProjectedReportingWorkspace> workspaces) {
    this.cohorts = cohorts;
    this.users = users;
    this.workspaces = workspaces;
    this.institutions = institutions;
  }

  public List<ProjectedReportingCohort> getCohorts() {
    return cohorts;
  }

  public List<ProjectedReportingInstitution> getInstitutions() {
    return institutions;
  }

  public List<ProjectedReportingUser> getUsers() {
    return users;
  }

  public List<ProjectedReportingWorkspace> getWorkspaces() {
    return workspaces;
  }
}
