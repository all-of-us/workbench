package org.pmiops.workbench.cdr.dao;

import java.util.List;
import java.util.Set;
import org.pmiops.workbench.cdr.model.CBCriteria;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

/**
 * Some of our trees are polyhierarchical(Snomed and drug). Since a node may exist multiple times in
 * this scenario with the same concept_id we only want to return it once from a user search. The
 * query to rank/min this to a single row was expensive. Instead, when building the cb_criteria
 * table we add domain_rank1 to the synonyms column for the first node that matches in the tree for
 * a specific concept_id. This allows us to use the full text index and makes the query much faster.
 */
public interface CBCriteriaDao extends CrudRepository<CBCriteria, Long> {

  /**
   * This query returns the parents in addition to the criteria ancestors for each leave in order to
   * allow the client to determine the relationship between the returned Criteria. Since we use
   * the @Id annotation in the entity, JPA will treat those rows with the given id as a unique
   * element. For example if you have 3 rows where this @Id annotated field is the same, JPA will
   * use the first result elements and just duplicate them.
   *
   * <p>select statement returns:
   *
   * <p>id: 111, concept_id : 111
   *
   * <p>id: 111, concept_id : 222
   *
   * <p>id: 111, concept_id : 333
   *
   * <p>The result will be 3 rows retrieved by JPA:
   *
   * <p>id: 111 concept_id: 111
   *
   * <p>id: 111 concept_id: 111
   *
   * <p>id: 111 concept_id: 111
   *
   * <p>So to keep this from happening we use the descendant_id from cb_criteria_ancestor as the
   * unique id. Please do not use this method if you need proper cb_criteria ids, we don't in the
   * current use case.
   */
  @Query(
      value =
          "select distinct ca.descendant_id as id,parent_id,domain_id,is_standard,type,subtype,ca.descendant_id as concept_id,code,name,value,est_count,is_group,is_selectable,has_attribute,has_hierarchy,"
              + "has_ancestor_data,path,synonyms "
              + "from cb_criteria_ancestor ca "
              + "   join (select a.* "
              + "           from cb_criteria a "
              + "           join (select concat('%.', id, '%') as path "
              + "                   from cb_criteria "
              + "                  where domain_id = :domain "
              + "                    and type = :type "
              + "                    and is_group = :group "
              + "                    and is_selectable = 1 "
              + "                    and concept_id in (:parentConceptIds)) b "
              + "             on (a.path like b.path) "
              + "          where domain_id = :domain "
              + "            and is_group = 0  "
              + "            and is_selectable = 1 "
              + "            and type = 'RXNORM') c "
              + "    on (ca.ancestor_id = c.concept_id) "
              + "union "
              + "select a.id,parent_id,domain_id,is_standard,type,subtype,concept_id,code,name,value,est_count,is_group,is_selectable,has_attribute,has_hierarchy,"
              + " has_ancestor_data,a.path,synonyms "
              + "  from cb_criteria a "
              + "  where domain_id = :domain "
              + "   and type = :type "
              + "   and concept_id in (:parentConceptIds)",
      nativeQuery = true)
  List<CBCriteria> findCriteriaAncestors(
      @Param("domain") String domain,
      @Param("type") String type,
      @Param("group") Boolean group,
      @Param("parentConceptIds") Set<String> parentConceptIds);

  /** This query returns all parents matching the parentConceptIds. */
  @Query(
      value =
          "select c from CBCriteria c where conceptId in (:parentConceptIds) and domainId = :domain and type = :type and standard = :standard and "
              + "match(synonyms, concat('+[', :domain, '_rank1]')) > 0")
  List<CBCriteria> findCriteriaParentsByDomainAndTypeAndParentConceptIds(
      @Param("domain") String domain,
      @Param("type") String type,
      @Param("standard") Boolean isStandard,
      @Param("parentConceptIds") Set<String> parentConceptIds);

  @Query(value = "select c from CBCriteria c where match(path, :path) > 0 order by id asc")
  List<CBCriteria> findCriteriaLeavesAndParentsByPath(@Param("path") String path);

  @Query(
      value =
          "select * from cb_criteria "
              + "where domain_id = :domain "
              + "and type = :type "
              + "and parent_id in (:parentIds)"
              + "or id in (:parentIds)",
      nativeQuery = true)
  List<CBCriteria> findCriteriaLeavesAndParentsByDomainAndTypeAndParentIds(
      @Param("domain") String domain,
      @Param("type") String type,
      @Param("parentIds") List<Long> parentIds);

  @Query(
      value =
          "select cr from CBCriteria cr where domain_id = ?1 and type = ?2 and subtype = ?3 and is_group = 0 and is_selectable = 1")
  List<CBCriteria> findCriteriaLeavesByDomainAndTypeAndSubtype(
      String domain, String type, String subtype);

  @Query(
      value = "select concept_id_2 from cb_criteria_relationship where concept_id_1 = :conceptId",
      nativeQuery = true)
  List<Integer> findConceptId2ByConceptId1(@Param("conceptId") Long conceptId);

  @Query(
      value =
          "select cr from CBCriteria cr where cr.conceptId = (:conceptIds) and cr.standard = :standard and cr.domainId = :domain and match(cr.synonyms, concat('+[', :domain, '_rank1]')) > 0)")
  List<CBCriteria> findStandardCriteriaByDomainAndConceptId(
      @Param("domain") String domain,
      @Param("standard") Boolean isStandard,
      @Param("conceptIds") List<String> conceptIds);

  @Query(
      value =
          "select * from cb_criteria where domain_id=:domain and type=:type and is_standard=:standard and parent_id=:parentId order by id asc",
      nativeQuery = true)
  List<CBCriteria> findCriteriaByDomainIdAndTypeAndParentIdOrderByIdAsc(
      @Param("domain") String domain,
      @Param("type") String type,
      @Param("standard") Boolean isStandard,
      @Param("parentId") Long parentId);

  @Query(value = "select c from CBCriteria c where domainId=:domain and type=:type order by id asc")
  List<CBCriteria> findCriteriaByDomainAndTypeOrderByIdAsc(
      @Param("domain") String domain, @Param("type") String type);

  @Query(
      value =
          "select c from CBCriteria c where domainId=:domain and code=:term order by standard desc")
  List<CBCriteria> findExactMatchByCode(@Param("domain") String domain, @Param("term") String term);

  @Query(
      value =
          "select c from CBCriteria c where domainId=:domain and type=:type and standard=:standard and code like upper(concat(:term,'%')) and match(synonyms, concat('+[', :domain, '_rank1]')) > 0 order by c.count desc")
  List<CBCriteria> findCriteriaByDomainAndTypeAndCode(
      @Param("domain") String domain,
      @Param("type") String type,
      @Param("standard") Boolean isStandard,
      @Param("term") String term,
      Pageable page);

  @Query(
      value =
          "select c from CBCriteria c where domainId=:domain and standard=:standard and code like upper(concat(:term,'%')) and match(synonyms, concat('+[', :domain, '_rank1]')) > 0 order by c.count desc")
  List<CBCriteria> findCriteriaByDomainAndCode(
      @Param("domain") String domain,
      @Param("standard") Boolean isStandard,
      @Param("term") String term,
      Pageable page);

  @Query(
      value =
          "select c from CBCriteria c where domainId=:domain and standard=:standard and match(synonyms, concat(:term, '+[', :domain, '_rank1]')) > 0 order by c.count desc, c.name asc")
  List<CBCriteria> findCriteriaByDomainAndSynonyms(
      @Param("domain") String domain,
      @Param("standard") Boolean isStandard,
      @Param("term") String term,
      Pageable page);

  @Query(
      value =
          "select c from CBCriteria c where domainId=:domain and type=:type and standard=:standard and hierarchy=1 and code like upper(concat(:term,'%')) and match(synonyms, concat('+[', :domain, '_rank1]')) > 0 order by c.count desc")
  List<CBCriteria> findCriteriaByDomainAndTypeAndStandardAndCode(
      @Param("domain") String domain,
      @Param("type") String type,
      @Param("standard") Boolean standard,
      @Param("term") String term,
      Pageable page);

  @Query(
      value =
          "select c from CBCriteria c where domainId = :domain and type = :type and standard = :standard and hierarchy=1 and match(synonyms, concat(:term, '+[', :domain, '_rank1]')) > 0 order by c.count desc")
  List<CBCriteria> findCriteriaByDomainAndTypeAndStandardAndSynonyms(
      @Param("domain") String domain,
      @Param("type") String type,
      @Param("standard") Boolean standard,
      @Param("term") String term,
      Pageable page);

  @Query(
      value =
          "select * from cb_criteria c "
              + "where c.domain_id = 'DRUG' "
              + "and c.type in ('ATC', 'BRAND', 'RXNORM') "
              + "and c.is_selectable = 1 "
              + "and (upper(c.name) like upper(concat('%',:value,'%')) "
              + "or upper(c.code) like upper(concat('%',:value,'%'))) "
              + "order by c.name asc "
              + "limit :limit",
      nativeQuery = true)
  List<CBCriteria> findDrugBrandOrIngredientByValue(
      @Param("value") String value, @Param("limit") Long limit);

  @Query(
      value =
          "select * from cb_criteria c "
              + "inner join ( "
              + "select cr.concept_id_2 from cb_criteria_relationship cr "
              + "join concept c1 on (cr.concept_id_2 = c1.concept_id "
              + "and cr.concept_id_1 = :conceptId "
              + "and c1.concept_class_id = 'Ingredient') ) cr1 on c.concept_id = cr1.concept_id_2 "
              + "and c.domain_id = 'DRUG' and c.type = 'RXNORM' and match(synonyms) against('+[drug_rank1]' in boolean mode) order by c.est_count desc",
      nativeQuery = true)
  List<CBCriteria> findDrugIngredientByConceptId(@Param("conceptId") String conceptId);
}
