package org.pmiops.workbench.cdr.dao;

import org.pmiops.workbench.cdr.model.Criteria;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CriteriaDao extends CrudRepository<Criteria, Long> {

    List<Criteria> findCriteriaByTypeAndParentIdOrderByCodeAsc(@Param("type") String type, @Param("parentId") Long parentId);

    @Query(value = "select distinct c.domain_id as domainId from criteria c " +
            "where c.parent_id in (" +
            "select id from criteria " +
            "where type = :type " +
            "and code like :code% " +
            "and is_selectable = 1 " +
            "and is_group = 1) " +
            "and c.is_group = 0 and c.is_selectable = 1", nativeQuery = true)
    List<String> findCriteriaByTypeAndCode(@Param("type") String type, @Param("code") String code);

    @Query(value = "select distinct c.domain_id as domainId from criteria c " +
            "where c.parent_id in (" +
            "select id from criteria " +
            "where type = :type " +
            "and subtype = :subtype " +
            "and code like :code% " +
            "and is_selectable = 1 " +
            "and is_group = 1) " +
            "and c.is_group = 0 and c.is_selectable = 1", nativeQuery = true)
    List<String> findCriteriaByTypeAndSubtypeAndCode(@Param("type") String type, @Param("subtype") String subtype, @Param("code") String code);

    @Query(value = "select * from criteria c " +
            "where c.type = :type " +
            "and (match(c.name) against(:value in boolean mode) or match(c.code) against(:value in boolean mode)) " +
            "and c.is_selectable = 1 " +
            "order by c.code asc", nativeQuery = true)
    List<Criteria> findCriteriaByTypeAndNameOrCode(@Param("type") String type, @Param("value") String value);

}
