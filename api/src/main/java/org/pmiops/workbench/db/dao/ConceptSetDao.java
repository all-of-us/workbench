package org.pmiops.workbench.db.dao;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.pmiops.workbench.db.model.DbConceptSet;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface ConceptSetDao extends CrudRepository<DbConceptSet, Long> {

  Optional<DbConceptSet> findByWorkspaceIdAndConceptSetId(long workspaceId, long conceptId);

  List<DbConceptSet> findByWorkspaceId(long workspaceId);

  /** Returns the concept set in the workspace with the specified name, or null if there is none. */
  DbConceptSet findConceptSetByNameAndWorkspaceId(String name, long workspaceId);

  List<DbConceptSet> findAllByConceptSetIdIn(Collection<Long> conceptSetIds);

  int countByWorkspaceId(long workspaceId);

  @Query(
      value =
          "select count(*) "
              + "from concept_set_concept_id "
              + "where concept_set_id = :conceptSetId",
      nativeQuery = true)
  long countByConceptSetId(@Param("conceptSetId") Long conceptSetId);
}
