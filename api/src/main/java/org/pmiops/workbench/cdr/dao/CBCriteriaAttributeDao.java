package org.pmiops.workbench.cdr.dao;

import java.util.List;
import org.pmiops.workbench.cdr.model.DbCriteriaAttribute;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface CBCriteriaAttributeDao extends CrudRepository<DbCriteriaAttribute, Long> {

  List<DbCriteriaAttribute> findCriteriaAttributeByConceptId(@Param("conceptId") Long conceptId);
}
