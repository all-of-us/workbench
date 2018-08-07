package org.pmiops.workbench.cdr.dao;

import java.util.List;
import org.pmiops.workbench.cdr.model.Concept;
import org.pmiops.workbench.cdr.model.ConceptSynonym;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface ConceptSynonymDao extends CrudRepository<ConceptSynonym, Long> {

}
