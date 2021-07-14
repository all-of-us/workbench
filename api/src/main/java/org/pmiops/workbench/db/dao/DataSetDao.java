package org.pmiops.workbench.db.dao;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.pmiops.workbench.db.model.DbDataset;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface DataSetDao extends CrudRepository<DbDataset, Long> {
  List<DbDataset> findByWorkspaceIdAndInvalid(long workspaceId, boolean invalid);

  List<DbDataset> findDataSetsByCohortIdsAndWorkspaceIdAndInvalid(
      long cohortId, long workspaceId, boolean dirty);

  List<DbDataset> findDataSetsByConceptSetIdsAndWorkspaceIdAndInvalid(
      long conceptId, long workspaceId, boolean dirty);

  List<DbDataset> findByWorkspaceId(long workspaceId);

  Optional<DbDataset> findByDataSetIdAndWorkspaceId(long dataSetId, long workspaceId);

  default Map<Boolean, Long> getInvalidToCountMap() {
    final List<InvalidToCountResult> rows = getInvalidToCount();
    return rows.stream()
        .collect(
            ImmutableMap.toImmutableMap(
                InvalidToCountResult::getIsInvalid, InvalidToCountResult::getInvalidCount));
  }

  @Query(
      "SELECT invalid AS isInvalid, count(dataSetId) AS invalidCount FROM DbDataset GROUP BY invalid ORDER BY invalid")
  List<InvalidToCountResult> getInvalidToCount();

  interface InvalidToCountResult {
    Boolean getIsInvalid();

    Long getInvalidCount();
  }

  int countByWorkspaceId(long workspaceId);
}
