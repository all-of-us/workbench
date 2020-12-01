package org.pmiops.workbench.db.dao;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import org.pmiops.workbench.db.dao.projection.ProjectedReportingDataset;
import org.pmiops.workbench.db.model.DbDataset;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface DataSetDao extends CrudRepository<DbDataset, Long> {
  List<DbDataset> findByWorkspaceIdAndInvalid(long workspaceId, boolean invalid);

  List<DbDataset> findDataSetsByCohortIds(long cohortId);

  List<DbDataset> findDataSetsByConceptSetIds(long conceptId);

  List<DbDataset> findByWorkspaceId(long workspaceId);

  default Map<Boolean, Long> getInvalidToCountMap() {
    final List<InvalidToCountResult> rows = getInvalidToCount();
    return rows.stream()
        .collect(
            ImmutableMap.toImmutableMap(
                InvalidToCountResult::getIsInvalid, InvalidToCountResult::getInvalidCount));
  }

  @Query(
      "SELECT invalid, count(dataSetId) AS invalidCount FROM DbDataset GROUP BY invalid ORDER BY invalid")
  List<InvalidToCountResult> getInvalidToCount();

  interface InvalidToCountResult {
    Boolean getIsInvalid();

    Long getInvalidCount();
  }

  int countByWorkspaceId(long workspaceId);

  // This JPQL query corresponds to the projection interface ProjectedReportingDataset. Its
  // types and argument order must match the column names selected exactly, in name,
  // type, and order. Note that in some cases a projection query should JOIN one or more
  // other tables. Currently this is done by hand (with suitable renamings of the other entries
  //  in the projection

  // This code was generated using reporting-wizard.rb at 2020-11-18T22:19:46-05:00.
  // Manual modification should be avoided if possible as this is a one-time generation
  // and does not run on every build and updates must be merged manually for now.

  @Query(
      "SELECT\n"
          + "  d.creationTime,\n"
          + "  d.creatorId,\n"
          + "  d.dataSetId,\n"
          + "  d.description,\n"
          + "  d.includesAllParticipants,\n"
          + "  d.invalid,\n"
          + "  d.lastModifiedTime,\n"
          + "  d.name,\n"
          + "  d.version,\n"
          + "  d.workspaceId\n"
          + "FROM DbDataset d")
  List<ProjectedReportingDataset> getReportingDatasets();

  // This JPQL query corresponds to the projection interface ProjectedReportingDatasetCohort. Its
  // types and argument order must match the column names selected exactly, in name,
  // type, and order. Note that in some cases a projection query should JOIN one or more
  // other tables. Currently this is done by hand (with suitable renamings of the other entries
  //  in the projection

  // This code was generated using reporting-wizard.rb at 2020-11-10T11:18:15-05:00.
  // Manual modification should be avoided if possible as this is a one-time generation
  // and does not run on every build and updates must be merged manually for now.

  //  @Query("SELECT\n"
  //      + "  dc.cohortId,\n"
  //      + "  d.datasetId\n"
  //      + "FROM DbDataSet d,\n"
  //      + "INNER JOIN  DbDatasetCohort dc ON d.dataSetId = dc.datasetId")
  //  List<ProjectedReportingDatasetCohort> getReportingDatasetCohorts();
}
