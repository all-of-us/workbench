package org.pmiops.workbench.cdr.dao;

import org.pmiops.workbench.cdr.model.Criteria;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CriteriaDao extends CrudRepository<Criteria, Long> {

  List<Criteria> findCriteriaByTypeAndParentIdOrderByIdAsc(@Param("type") String type, @Param("parentId") Long parentId);

  List<Criteria> findCriteriaByType(@Param("type") String type);

  @Query(value = "select * from criteria c " +
    "where c.type = :type " +
    "and c.subtype = :subtype " +
    "order by c.id asc", nativeQuery = true)
  List<Criteria> findCriteriaByTypeAndSubtypeOrderByIdAsc(@Param("type") String type, @Param("subtype") String subtype);

  @Query(value = "select * from criteria c " +
    "where c.type = 'DRUG' " +
    "and c.subtype in ('ATC', 'BRAND') " +
    "and c.is_selectable = 1 " +
    "and upper(c.name) like %:name% " +
    "order by c.name asc", nativeQuery = true)
  List<Criteria> findDrugBrandOrIngredientByName(@Param("name") String name);

  @Query(value = "select * from criteria c " +
    "inner join ( " +
    "select cr.concept_id_2 from concept_relationship cr " +
    "join concept c1 on (cr.concept_id_2 = c1.concept_id " +
    "and cr.concept_id_1 = :conceptId " +
    "and c1.concept_class_id = 'Ingredient') ) cr1 on c.concept_id = cr1.concept_id_2", nativeQuery = true)
  List<Criteria> findDrugIngredientByConceptId(@Param("conceptId") Long conceptId);

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
