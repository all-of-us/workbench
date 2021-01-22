package org.pmiops.workbench.db.dao;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.pmiops.workbench.db.model.DbConceptSet;
import org.springframework.data.repository.CrudRepository;

public interface ConceptSetDao extends CrudRepository<DbConceptSet, Long> {

  Optional<DbConceptSet> findByConceptSetIdAndWorkspaceId(long conceptId, long workspaceId);

  List<DbConceptSet> findByWorkspaceId(long workspaceId);

  /** Returns the concept set in the workspace with the specified name, or null if there is none. */
  DbConceptSet findConceptSetByNameAndWorkspaceId(String name, long workspaceId);

  List<DbConceptSet> findAllByConceptSetIdIn(Collection<Long> conceptSetIds);

  int countByWorkspaceId(long workspaceId);
}
