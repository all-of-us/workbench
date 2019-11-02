package org.pmiops.workbench.cdr.dao

import org.pmiops.workbench.cdr.model.CBCriteriaAttribute
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param

interface CBCriteriaAttributeDao : CrudRepository<CBCriteriaAttribute, Long> {

    fun findCriteriaAttributeByConceptId(@Param("conceptId") conceptId: Long?): List<CBCriteriaAttribute>
}
