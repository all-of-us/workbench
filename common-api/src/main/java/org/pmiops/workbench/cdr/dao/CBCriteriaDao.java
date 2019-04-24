package org.pmiops.workbench.cdr.dao;

import org.pmiops.workbench.cdr.model.CBCriteria;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CBCriteriaDao extends CrudRepository<CBCriteria, Long> {

  List<CBCriteria> findCriteriaByDomainIdAndTypeAndParentIdOrderByIdAsc(@Param("domainId") String domainId,
                                                                        @Param("type") String type,
                                                                        @Param("parentId") Long parentId);

  @Query(value = "select c from CBCriteria c where domainId=:domain and type=:type order by id asc")
  List<CBCriteria> findCriteriaByDomainAndTypeOrderByIdAsc(@Param("domain") String domain,
                                                           @Param("type") String type);

  @Query(value = "select * from cb_criteria where domain_id=:domain and type=:type and (path like concat('%.',:parentId) or path like concat('%.',:parentId, '.%')) " +
    "and is_group = 0 and is_selectable = 1", nativeQuery = true)
  List<CBCriteria> findCriteriaChildrenByDomainAndTypeAndParentId(@Param("domain") String domain,
                                                                  @Param("type") String type,
                                                                  @Param("parentId") Long parentId);

  @Query(value = "select c from CBCriteria c where domainId=:domain and standard=:standard and match(synonyms, :term) > 0 order by c.count desc")
  List<CBCriteria> findCriteriaByDomainAndSearchTerm(@Param("domain") String domain,
                                                     @Param("standard") Boolean isStandard,
                                                     @Param("term") String term,
                                                     Pageable page);

  @Query(value = "select c from CBCriteria c where domainId=:domain and type=:type and (match(synonyms, :modifiedTerm) > 0 or code like upper(concat(:term,'%'))) order by c.count desc")
  List<CBCriteria> findCriteriaByDomainAndTypeForCodeOrName(@Param("domain") String domain,
                                                            @Param("type") String type,
                                                            @Param("modifiedTerm") String modifiedTerm,
                                                            @Param("term") String term,
                                                            Pageable page);

  @Query(value = "select c from CBCriteria c where domainId = :domain and type = :type and match(synonyms, :term) > 0 order by c.count desc")
  List<CBCriteria> findCriteriaByDomainAndTypeForName(@Param("domain") String domain,
                                                      @Param("type") String type,
                                                      @Param("term") String term,
                                                      Pageable page);

  @Query(value = "select * from cb_criteria c where domain_id = 'DRUG' and type in ('ATC', 'BRAND') " +
    "and c.is_selectable = 1 and (upper(c.name) like upper(concat('%',:value,'%')) or upper(c.code) like upper(concat('%',:value,'%'))) " +
    "order by c.name asc limit :limit", nativeQuery = true)
  List<CBCriteria> findDrugBrandOrIngredientByValue(@Param("value") String value,
                                                    @Param("limit") Long limit);

  @Query(value = "select * from cb_criteria c inner join ( select cr.concept_id_2 from criteria_relationship cr join concept c1 on (cr.concept_id_2 = c1.concept_id " +
    "and cr.concept_id_1 = :conceptId and c1.concept_class_id = 'Ingredient') ) cr1 on c.concept_id = cr1.concept_id_2", nativeQuery = true)
  List<CBCriteria> findDrugIngredientByConceptId(@Param("conceptId") Long conceptId);
}
