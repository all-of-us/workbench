package org.pmiops.workbench.cdr.dao;

import java.util.List;
import org.pmiops.workbench.cdr.model.DbConceptRelationship;
import org.springframework.data.repository.CrudRepository;

public interface ConceptRelationshipDao extends CrudRepository<DbConceptRelationship, Long> {

  List<DbConceptRelationship> findAll();
}
