package org.pmiops.workbench.cdr.dao;

import org.pmiops.workbench.cdr.model.DbConceptSynonym;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

// NOTE: This class and ConceptSynonym exist only to make CriteriaDao work in tests;
// if we stop using concept_synonym there at some point we can get rid of them.
public interface ConceptSynonymDao extends CrudRepository<DbConceptSynonym, Long> {

  List<DbConceptSynonym> findByConceptIdIn(List<Long> conceptIds);
}
