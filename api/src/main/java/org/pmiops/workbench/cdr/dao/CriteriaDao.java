package org.pmiops.workbench.cdr.dao;

import org.pmiops.workbench.cdr.model.CodeDomainLookup;
import org.pmiops.workbench.cdr.model.Criteria;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CriteriaDao extends CrudRepository<Criteria, Long> {

    List<Criteria> findCriteriaByTypeAndParentIdOrderByCodeAsc(@Param("type") String type, @Param("parentId") Long parentId);

    /** TODO: implement dynamic switching of schemas **/
    @Query(value = "select c.code, c.domain_id as domainId from cdr.criteria c " +
            "where c.parent_id in (" +
            "select id from cdr.criteria " +
            "where type = :type " +
            "and code = :code " +
            "and is_selectable = 1 " +
            "and is_group = 1) order by c.code asc", nativeQuery = true)
    List<CodeDomainLookup> findCriteriaByTypeAndCode(@Param("type") String type, @Param("code") String code);

    /** TODO: implement dynamic switching of schemas **/
    @Query(value = "select * from cdr.criteria c " +
            "where c.type = :type " +
            "and (match(c.name) against(:value in boolean mode) or match(c.code) against(:value in boolean mode)) " +
            "and c.is_selectable = 1 " +
            "order by c.code asc", nativeQuery = true)
    List<Criteria> findCriteriaByTypeAndNameOrCode(@Param("type") String type, @Param("value") String value);

}
