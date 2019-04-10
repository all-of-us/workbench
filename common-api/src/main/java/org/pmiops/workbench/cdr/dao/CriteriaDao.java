package org.pmiops.workbench.cdr.dao;

import java.util.Set;
import org.pmiops.workbench.cdr.model.Criteria;
import org.pmiops.workbench.cdr.model.CriteriaId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CriteriaDao extends CrudRepository<Criteria, Long> {

  Criteria findCriteriaByTypeAndConceptIdAndSelectable(@Param("type") String type, @Param("conceptId") String conceptId, @Param("selectable") Boolean selectable);

  List<Criteria> findCriteriaByTypeAndParentIdOrderByIdAsc(@Param("type") String type,
                                                           @Param("parentId") Long parentId);

  /**
   * This query returns the parents in addition to the leaves in order to allow the client to
   * determine the relationship between the returned Criteria. Parent concept IDs are not otherwise
   * encoded in Criteria children.
   */
  @Query(value = "select a.* from criteria a join " +
      "(select CONCAT( '%.', id, '%') as path, id from criteria " +
      " where concept_id in (:parentConceptIds) and " +
      " type = :type and subtype = :subtype and is_group = 1) b " +
      "on (a.path like b.path OR a.id = b.id) " +
      "where type = :type " +
      "and subtype = :subtype " +
      "and (concept_id in (:parentConceptIds) OR (is_group = 0 and is_selectable = 1))",
      nativeQuery = true)
  List<Criteria> findCriteriaLeavesAndParentsByTypeAndParentConceptIds(
      @Param("type") String type, @Param("subtype") String subtype,
      @Param("parentConceptIds") Set<String> parentConceptIds);

  @Query(value = "select * " +
    "from criteria " +
    "where type = :type " +
    "and subtype = :subtype " +
    "and code regexp :parentCodeRegex " +
    "and is_group = 0 " +
    "and is_selectable = 1", nativeQuery = true)
  List<Criteria> findCriteriaLeavesByTypeAndParentCodeRegex(@Param("type") String type,
                                                            @Param("subtype") String subtype,
                                                            @Param("parentCodeRegex") String parentCodeRegex);

  @Query(value = "select cr " +
    "from Criteria cr " +
    "where type = ?1 " +
    "and subtype = ?2 " +
    "and is_group = 0 " +
    "and is_selectable = 1")
  List<Criteria> findCriteriaLeavesByType(String type, String subtype);

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

  @Query(value = "select cr from Criteria cr " +
    "    where cr.type = upper(?1) " +
    "    and (match(synonyms, ?2) > 0 or cr.code like upper(concat(?3,'%')))" +
    "    order by cr.count desc")
  List<Criteria> findCriteriaByTypeForCodeOrName(String type,
                                                 String modifiedValue,
                                                 String value,
                                                 Pageable page);

  @Query(value = "select cr from Criteria cr " +
    "    where cr.type = upper(?1) " +
    "    and cr.subtype = upper(?2) " +
    "    and (match(synonyms, ?3) > 0 or cr.code like upper(concat(?4,'%')))" +
    "    order by cr.count desc")
  List<Criteria> findCriteriaByTypeAndSubtypeForCodeOrName(String type,
                                                           String subtype,
                                                           String modifiedValue,
                                                           String value,
                                                           Pageable page);

  @Query(value = "select cr from Criteria cr " +
    "    where cr.type = upper(?1) " +
    "    and cr.subtype = upper(?2) " +
    "    and match(synonyms, ?3) > 0 " +
    "    order by cr.count desc")
  List<Criteria> findCriteriaByTypeAndSubtypeForName(String type,
                                                     String subtype,
                                                     String value,
                                                     Pageable page);

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
    "select cr.concept_id_2 from criteria_relationship cr " +
    "join concept c1 on (cr.concept_id_2 = c1.concept_id " +
    "and cr.concept_id_1 = :conceptId " +
    "and c1.concept_class_id = 'Ingredient') ) cr1 on c.concept_id = cr1.concept_id_2", nativeQuery = true)
  List<Criteria> findDrugIngredientByConceptId(@Param("conceptId") Long conceptId);

  @Query(value = "select cr from Criteria cr " +
    "    where cr.domainId = ?1 " +
    "    and cr.standard = ?2 " +
    "    and match(synonyms, ?3) > 0 " +
    "    order by cr.count desc")
  List<Criteria> findCriteriaByDomainAndSearchTerm(String domain,
                                                   Boolean isStandard,
                                                   String term,
                                                   Pageable page);
}
