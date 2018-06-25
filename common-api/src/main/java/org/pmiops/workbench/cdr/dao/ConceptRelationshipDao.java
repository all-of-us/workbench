package org.pmiops.workbench.cdr.dao;

import java.util.List;
import org.pmiops.workbench.cdr.model.ConceptRelationship;
import org.springframework.data.repository.CrudRepository;

public interface ConceptRelationshipDao extends CrudRepository<ConceptRelationship, Long> {

    List<ConceptRelationship> findAll();
}
