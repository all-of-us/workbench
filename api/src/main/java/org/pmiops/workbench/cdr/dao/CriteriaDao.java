package org.pmiops.workbench.cdr.dao;

import org.pmiops.workbench.cdr.model.Criteria;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CriteriaDao extends CrudRepository<Criteria, Long> {

    @Query("select c from Criteria c where c.type like :type% and c.parentId = :parentId order by c.code asc")
    List<Criteria> findCriteriaByTypeAndParentId(@Param("type") String type, @Param("parentId") Long parentId);

}
