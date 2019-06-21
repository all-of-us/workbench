package org.pmiops.workbench.cdr.dao;

import java.util.List;
import java.util.Set;
import org.pmiops.workbench.cdr.model.CBCriteria;
import org.pmiops.workbench.cdr.model.StandardProjection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface CBCriteriaDao extends CrudRepository<CBCriteria, Long> {

  /**
   * This query returns the parents in addition to the criteria ancestors for each leave in order to
   * allow the client to determine the relationship between the returned Criteria.
   */
  @Query(
      value =
          "select * from ("
              + "select distinct id,parent_id,domain_id,is_standard,type,subtype,ca.descendant_id as concept_id,code,name,value,est_count,is_group,is_selectable,has_attribute,has_hierarchy,"
              + "has_ancestor_data,path,synonyms "
              + "from cb_criteria_ancestor ca "
              + "   join (select a.* "
              + "           from cb_criteria a "
              + "           join (select concat('%.', id, '%') as path, id "
              + "                   from cb_criteria "
              + "                  where concept_id in (:parentConceptIds) "
              + "                    and domain_id = :domain "
              + "                    and type = :type "
              + "                    and is_group = 1) b "
              + "             on (a.path like b.path or a.id = b.id) "
              + "          where domain_id = :domain "
              + "            and is_group = 0  "
              + "            and is_selectable = 1 "
              + "            and type = 'RXNORM') c "
              + "    on (ca.ancestor_id = c.concept_id) "
              + "union "
              + "select a.id,parent_id,domain_id,is_standard,type,subtype,concept_id,code,name,value,est_count,is_group,is_selectable,has_attribute,has_hierarchy,"
              + " has_ancestor_data,a.path,synonyms "
              + "  from cb_criteria a "
              + "  join (select concat('%.', id, '%') as path, id "
              + "          from cb_criteria "
              + "         where concept_id in (:parentConceptIds) "
              + "           and domain_id = :domain "
              + "           and type = :type "
              + "           and is_group = 1) b "
              + "    on (a.path like b.path) "
              + " where domain_id = :domain "
              + "   and type = :type) d "
              + "order by id",
      nativeQuery = true)
  List<CBCriteria> findCriteriaAncestors(
      @Param("domain") String domain,
      @Param("type") String type,
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

  @Query(
      value =
          "select c from CBCriteria c where match(path, :path) > 0 and match(synonyms, concat('+[', :domain, '_rank1]')) > 0) order by id asc")
  List<CBCriteria> findCriteriaLeavesAndParentsByDomainAndPath(
      @Param("domain") String domain, @Param("path") String path);

  @Query(
      value =
          "select * from cb_criteria where domain_id = :domain and type = :type and code regexp :parentCodeRegex and is_group = 0 and is_selectable = 1",
      nativeQuery = true)
  List<CBCriteria> findCriteriaLeavesByDomainAndTypeAndParentCodeRegex(
      @Param("domain") String domain,
      @Param("type") String type,
      @Param("parentCodeRegex") String parentCodeRegex);

  @Query(
      value =
          "select cr from CBCriteria cr where domain_id = ?1 and type = ?2 and is_group = 0 and is_selectable = 1")
  List<CBCriteria> findCriteriaLeavesByDomainAndType(String domain, String type);

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
          "select * from cb_criteria where domain_id=:domain and type=:type and is_standard=:standard and parent_id=:parentId and has_hierarchy = 1 order by id asc",
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
          "select c.standard as standard from CBCriteria c where domainId=:domain and code=:term order by standard desc")
  List<StandardProjection> findStandardProjectionByCode(
      @Param("domain") String domain, @Param("term") String term);

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
          "select c from CBCriteria c where domainId=:domain and standard=:standard and match(synonyms, concat(:term, '+[', :domain, '_rank1]')) > 0 order by c.count desc")
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
          "select cr.concept_id_2 from cb_criteria_relationship cr join concept c1 on (cr.concept_id_2 = c1.concept_id and cr.concept_id_1 in (:conceptIds) and c1.concept_class_id = 'Ingredient')",
      nativeQuery = true)
  List<Integer> findDrugConceptId2ByConceptId1(@Param("conceptIds") List<Long> conceptIds);

  @Query(
      value =
          "select c from CBCriteria c where conceptId in (:conceptIds) and domainId = :domain and type = :type and match(synonyms, concat('+[', :domain, '_rank1]')) > 0 order by c.count desc")
  List<CBCriteria> findDrugIngredientByConceptId(
      @Param("conceptIds") List<String> conceptIds,
      @Param("domain") String domain,
      @Param("type") String type);
}
