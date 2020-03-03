package org.pmiops.workbench.cdr.dao;

import org.pmiops.workbench.cdr.model.DbConceptRelationship;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ConceptRelationshipDao extends CrudRepository<DbConceptRelationship, Long> {

  List<DbConceptRelationship> findAll();
}
