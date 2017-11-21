package org.pmiops.workbench.cdr.dao;
import org.pmiops.workbench.cdr.model.Concept;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ConceptDao extends CrudRepository<Concept, Long> {

    @Query("select c from Concept c   where  c.domainId in ('Condition','Observation','Procedure', 'Measurement', 'Drug') and c.conceptName like :conceptName%")
    List<Concept> findConceptLikeName(@Param("conceptName") String conceptName);

    List<Concept> findByConceptName(String conceptName);
}
