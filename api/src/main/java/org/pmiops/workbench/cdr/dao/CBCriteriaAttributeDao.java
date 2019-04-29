package org.pmiops.workbench.cdr.dao;

import org.pmiops.workbench.cdr.model.CBCriteriaAttribute;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CBCriteriaAttributeDao extends CrudRepository<CBCriteriaAttribute, Long> {

  List<CBCriteriaAttribute> findCriteriaAttributeByConceptId(@Param("conceptId") Long conceptId);
}
