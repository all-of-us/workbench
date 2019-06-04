package org.pmiops.workbench.cdr.dao;

import java.util.List;
import org.pmiops.workbench.cdr.model.CBCriteriaAttribute;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface CBCriteriaAttributeDao extends CrudRepository<CBCriteriaAttribute, Long> {

  List<CBCriteriaAttribute> findCriteriaAttributeByConceptId(@Param("conceptId") Long conceptId);
}
