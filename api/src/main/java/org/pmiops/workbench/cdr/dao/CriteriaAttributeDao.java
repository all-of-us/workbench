package org.pmiops.workbench.cdr.dao;

import java.util.List;
import org.pmiops.workbench.cdr.model.CriteriaAttribute;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

// TODO:Remove freemabd
public interface CriteriaAttributeDao extends CrudRepository<CriteriaAttribute, Long> {

  List<CriteriaAttribute> findCriteriaAttributeByConceptId(@Param("conceptId") Long conceptId);
}
