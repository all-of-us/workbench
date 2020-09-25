package org.pmiops.workbench.db.dao;

import java.util.Collection;
import java.util.List;
import org.pmiops.workbench.db.dao.projection.ProjectedReportingCohort;
import org.pmiops.workbench.db.model.DbCohort;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface CohortDao extends CrudRepository<DbCohort, Long> {

  /** Returns the cohort in the workspace with the specified name, or null if there is none. */
  DbCohort findCohortByNameAndWorkspaceId(String name, long workspaceId);

  List<DbCohort> findAllByCohortIdIn(Collection<Long> cohortIds);

  List<DbCohort> findByWorkspaceId(long workspaceId);

  int countByWorkspaceId(long workspaceId);

// This JPQL query corresponds to the projection interface ProjectedReportingCohort. Its
// types and argument order must match the column names selected exactly, in name,
// type, and order. Note that in some cases a projection query should JOIN one or more
// other tables. Currently this is done by hand (with suitable renamings of the other entries
//  in the projection

// This code was generated using reporting-wizard.rb at 2020-09-24T13:40:02-04:00.
// Manual modification should be avoided if possible as this is a one-time generation
// and does not run on every build and updates must be merged manually for now.

  @Query("SELECT\n"
      + "  c.cohortId,\n"
      + "  c.creationTime,\n"
      + "  c.creator.userId AS creatorId,\n"
      + "  c.criteria,\n"
      + "  c.description,\n"
      + "  c.lastModifiedTime,\n"
      + "  c.name,\n"
      + "  c.type,\n"
      + "  c.version,\n"
      + "  c.workspaceId\n"
      + "FROM DbCohort c")
  List<ProjectedReportingCohort> getReportingCohorts();
}
