package org.pmiops.workbench.cdr.dao;

import org.pmiops.workbench.cdr.model.ConceptSynonym;
import org.springframework.data.repository.CrudRepository;
import java.util.List;

public interface ConceptSynonymDao extends CrudRepository<ConceptSynonym, Long> {

    List<ConceptSynonym> findByConceptIdIn(List<Long> conceptIds);

}
