package org.pmiops.workbench.db.dao;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.pmiops.workbench.db.model.DbDataset;
import org.pmiops.workbench.db.model.DbUser;
import org.springframework.data.repository.CrudRepository;

public interface DataSetDao extends CrudRepository<DbDataset, Long> {
  List<DbDataset> findByWorkspaceIdAndInvalid(long workspaceId, boolean invalid);

  List<DbDataset> findDataSetsByCohortIds(long cohortId);

  List<DbDataset> findDataSetsByConceptSetIds(long conceptId);

  List<DbDataset> findByWorkspaceId(long workspaceId);

  default Map<Boolean, Long> getInvalidToCountMap() {
    return ImmutableMap.copyOf(StreamSupport.stream(findAll().spliterator(), false)
        .collect(Collectors.groupingBy(DbDataset::getInvalid, Collectors.counting())));
  }
}
