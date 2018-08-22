package org.pmiops.workbench.db.dao;

import org.pmiops.workbench.db.model.ConceptSet;
import org.springframework.data.repository.CrudRepository;

public interface ConceptSetDao extends CrudRepository<ConceptSet, Long> {
}
