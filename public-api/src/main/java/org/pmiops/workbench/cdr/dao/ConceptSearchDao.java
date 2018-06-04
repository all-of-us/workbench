package org.pmiops.workbench.cdr.dao;

import org.pmiops.workbench.cdr.model.Concept;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ConceptSearchDao extends JpaRepository<Concept,Long>,JpaSpecificationExecutor<Concept>{

}
