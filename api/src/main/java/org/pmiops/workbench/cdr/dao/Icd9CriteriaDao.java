package org.pmiops.workbench.cdr.dao;

import org.pmiops.workbench.cdr.model.Icd9Criteria;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface Icd9CriteriaDao extends CrudRepository<Icd9Criteria, Long> {

    List<Icd9Criteria> findIcd9CriteriaByParentId(Long parentId);
}
