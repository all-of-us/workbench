package org.pmiops.workbench.cdr.dao;

import org.pmiops.workbench.cdr.model.Criteria;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface CriteriaDao extends CrudRepository<Criteria, Long> {

    List<Criteria> findCriteriaByTypeLikeAndParentId(String type, Long parentId);
}
