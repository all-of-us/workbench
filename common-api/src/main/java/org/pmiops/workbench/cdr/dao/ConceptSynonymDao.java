package org.pmiops.workbench.cdr.dao;

import org.pmiops.workbench.cdr.model.ConceptSynonym;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import java.util.List;
import java.util.ArrayList;

public interface ConceptSynonymDao extends CrudRepository<ConceptSynonym, Long> {

    List<ConceptSynonym> findByConceptIdIn(List<Long> conceptIds);

    @Query(value = "select distinct cs.concept_synonym_name from concept_synonym cs where cs.concept_id=?1", nativeQuery = true)
    ArrayList<String> findSynonymNamesByConceptId(Long conceptId);

}
