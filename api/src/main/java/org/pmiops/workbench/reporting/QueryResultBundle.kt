package org.pmiops.workbench.reporting

import org.pmiops.workbench.db.dao.projection.ProjectedReportingCohort
import org.pmiops.workbench.db.dao.projection.ProjectedReportingInstitution
import org.pmiops.workbench.db.dao.projection.ProjectedReportingUser
import org.pmiops.workbench.db.dao.projection.ProjectedReportingWorkspace

// Define immutable value class to hold results of queries within a transaction. Mapping to
// Reporting DTO classes will happen outside the transaction.
data class QueryResultBundle(
        val cohorts: List<ProjectedReportingCohort>,
        val institutions: List<ProjectedReportingInstitution>,
        val users: List<ProjectedReportingUser>,
        val workspaces: List<ProjectedReportingWorkspace>)
