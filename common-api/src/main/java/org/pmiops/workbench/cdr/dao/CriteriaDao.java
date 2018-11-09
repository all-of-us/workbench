package org.pmiops.workbench.cdr.dao;

import org.pmiops.workbench.cdr.model.Criteria;
import org.pmiops.workbench.cdr.model.CriteriaId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CriteriaDao extends CrudRepository<Criteria, Long> {

  List<Criteria> findCriteriaByTypeAndParentIdOrderByIdAsc(@Param("type") String type,
                                                           @Param("parentId") Long parentId);

  @Query(value = "select * " +
    "from criteria " +
    "where type = :type " +
    "and (path like concat('%.',:parentId) or path like concat('%.',:parentId, '.%')) " +
    "and is_group = 0 " +
    "and is_selectable = 1", nativeQuery = true)
  List<Criteria> findCriteriaChildrenByTypeAndParentId(@Param("type") String type,
                                                       @Param("parentId") Long parentId);

  List<Criteria> findCriteriaByTypeAndSubtypeAndParentIdOrderByIdAsc(@Param("type") String type,
                                                                     @Param("subtype") String subtype,
                                                                     @Param("parentId") Long parentId);

  List<Criteria> findCriteriaByType(@Param("type") String type);

  @Query(value = "select min(cr.id) as id from Criteria cr " +
    "    where cr.type = upper(?1) " +
    "    and (match(synonyms, ?2) > 0 or cr.code like upper(concat(?3,'%')))" +
    "    group by cr.name, cr.count " +
    "    order by convert(cr.count, decimal) desc")
  List<CriteriaId> findCriteriaByTypeForCodeOrName(String type,
                                                   String modifiedValue,
                                                   String value,
                                                   Pageable page);

  @Query(value = "select min(cr.id) as id from Criteria cr " +
    "    where cr.type = upper(?1) " +
    "    and cr.subtype = upper(?2) " +
    "    and (match(synonyms, ?3) > 0 or cr.code like upper(concat(?4,'%')))" +
    "    group by cr.name, cr.count " +
    "    order by convert(cr.count, decimal) desc")
  List<CriteriaId> findCriteriaByTypeAndSubtypeForCodeOrName(String type,
                                                             String subtype,
                                                             String modifiedValue,
                                                             String value,
                                                             Pageable page);

  @Query(value = "select min(cr.id) as id from Criteria cr " +
    "    where cr.type = upper(?1) " +
    "    and cr.subtype = upper(?2) " +
    "    and match(synonyms, ?3) > 0 " +
    "    group by cr.name, cr.count " +
    "    order by convert(cr.count, decimal) desc")
  List<CriteriaId> findCriteriaByTypeAndSubtypeForCodeOrName(String type,
                                                             String subtype,
                                                             String value,
                                                             Pageable page);

  @Query(value = "select c from Criteria c where c.id in :ids")
  List<Criteria> findCriteriaByIds(@Param("ids") List<Long> ids);

  @Query(value = "select * from criteria c " +
    "where c.type = :type " +
    "and c.subtype = :subtype " +
    "order by c.id asc", nativeQuery = true)
  List<Criteria> findCriteriaByTypeAndSubtypeOrderByIdAsc(@Param("type") String type,
                                                          @Param("subtype") String subtype);

  @Query(value = "select * from criteria c " +
    "where c.type = 'DRUG' " +
    "and c.subtype in ('ATC', 'BRAND') " +
    "and c.is_selectable = 1 " +
    "and (upper(c.name) like upper(concat('%',:value,'%')) " +
    "or upper(c.code) like upper(concat('%',:value,'%'))) " +
    "order by c.name asc " +
    "limit :limit", nativeQuery = true)
  List<Criteria> findDrugBrandOrIngredientByValue(@Param("value") String value,
                                                  @Param("limit") Long limit);

  @Query(value = "select * from criteria c " +
    "inner join ( " +
    "select cr.concept_id_2 from concept_relationship cr " +
    "join concept c1 on (cr.concept_id_2 = c1.concept_id " +
    "and cr.concept_id_1 = :conceptId " +
    "and c1.concept_class_id = 'Ingredient') ) cr1 on c.concept_id = cr1.concept_id_2", nativeQuery = true)
  List<Criteria> findDrugIngredientByConceptId(@Param("conceptId") Long conceptId);

  @Query(value = "select distinct domain_id as domainId " +
    "from criteria " +
    "where is_group = 0 " +
    "and is_selectable = 1 " +
    "and (path = ( " +
    "select concat( path, '.', id) as path " +
    "from criteria " +
    "where type = :type " +
    "and subtype = :subtype " +
    "and code = :code " +
    "and is_group = 1 " +
    "and is_selectable = 1) " +
    "or path like (" +
    "select concat( path, '.', id, '.%') as path " +
    "from criteria " +
    "where type = :type " +
    "and subtype = :subtype " +
    "and code = :code " +
    "and is_group = 1 " +
    "and is_selectable = 1))", nativeQuery = true)
  List<String> findCriteriaByTypeAndSubtypeAndCode(@Param("type") String type,
                                                   @Param("subtype") String subtype,
                                                   @Param("code") String code);

  @Query(value = "select * from criteria c " +
    "where c.type = :type " +
    "and (match(c.name) against(:value in boolean mode) or match(c.code) against(:value in boolean mode)) " +
    "and c.is_selectable = 1 " +
    "order by c.code asc", nativeQuery = true)
  List<Criteria> findCriteriaByTypeAndNameOrCode(@Param("type") String type,
                                                 @Param("value") String value);

}
