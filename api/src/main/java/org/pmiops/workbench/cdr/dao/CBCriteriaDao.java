package org.pmiops.workbench.cdr.dao;

import java.util.Collection;
import java.util.List;
import org.pmiops.workbench.cdr.model.DbCriteria;
import org.pmiops.workbench.cdr.model.DbSurveyVersion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

/**
 * Some of our trees are polyhierarchical(Snomed and drug). Since a node may exist multiple times in
 * this scenario with the same concept_id we only want to return it once from a user search. The
 * query to rank/min this to a single row was expensive. Instead, when building the cb_criteria
 * table we add domain_rank1 to the full_text column for the first node that matches in the tree for
 * a specific concept_id. This allows us to use the full text index and makes the query much faster.
 */
public interface CBCriteriaDao extends CrudRepository<DbCriteria, Long> {

  @Query(
      value =
          "select c from DbCriteria c where standard = :standard and conceptId in (:conceptIds) and match(fullText, concat('+[', :domainId, '_rank1]')) > 0")
  List<DbCriteria> findCriteriaByDomainIdAndStandardAndConceptIds(
      @Param("domainId") String domainId,
      @Param("standard") Boolean standard,
      @Param("conceptIds") Collection<String> conceptIds);

  @Query(
      value = "select concept_id_2 from cb_criteria_relationship where concept_id_1 = :conceptId",
      nativeQuery = true)
  List<Integer> findConceptId2ByConceptId1(@Param("conceptId") Long conceptId);

  @Query(
      value =
          "select cr from DbCriteria cr where cr.conceptId in (:conceptIds) and cr.standard = :standard and cr.domainId = :domain and match(cr.fullText, concat('+[', :domain, '_rank1]')) > 0")
  List<DbCriteria> findStandardCriteriaByDomainAndConceptId(
      @Param("domain") String domain,
      @Param("standard") Boolean isStandard,
      @Param("conceptIds") List<String> conceptIds);

  @Query(
      value =
          "select * from cb_criteria where domain_id=:domain and type=:type and is_standard=:standard and parent_id=:parentId order by id asc",
      nativeQuery = true)
  List<DbCriteria> findCriteriaByDomainIdAndTypeAndParentIdOrderByIdAsc(
      @Param("domain") String domain,
      @Param("type") String type,
      @Param("standard") Boolean isStandard,
      @Param("parentId") Long parentId);

  @Query(value = "select c from DbCriteria c where domainId=:domain and type=:type order by id asc")
  List<DbCriteria> findCriteriaByDomainAndTypeOrderByIdAsc(
      @Param("domain") String domain, @Param("type") String type);

  @Query(
      value =
          "select c from DbCriteria c where domainId=:domain and code like upper(concat(:term,'%')) and match(fullText, concat('+[', :domain, '_rank1]')) > 0 order by c.count desc")
  Page<DbCriteria> findCriteriaByDomainAndTypeAndCode(
      @Param("domain") String domain, @Param("term") String term, Pageable page);

  @Query(
      value =
          "select c from DbCriteria c where domainId=:domain and standard=:standard and code like upper(concat(:term,'%')) and match(fullText, concat('+[', :domain, '_rank1]')) > 0 order by c.count desc")
  Page<DbCriteria> findCriteriaByDomainAndTypeAndCodeAndStandard(
      @Param("domain") String domain,
      @Param("term") String term,
      @Param("standard") Boolean standard,
      Pageable page);

  @Query(
      value =
          "select c from DbCriteria c where domainId=:domain and match(fullText, concat(:term, '+[', :domain, '_rank1]')) > 0 order by c.count desc, c.name asc")
  Page<DbCriteria> findCriteriaByDomainAndFullText(
      @Param("domain") String domain, @Param("term") String term, Pageable page);

  @Query(
      value =
          "select c from DbCriteria c where domainId=:domain and standard=:standard and match(fullText, concat(:term, '+[', :domain, '_rank1]')) > 0 order by c.count desc, c.name asc")
  Page<DbCriteria> findCriteriaByDomainAndFullTextAndStandard(
      @Param("domain") String domain,
      @Param("term") String term,
      @Param("standard") Boolean standard,
      Pageable page);

  @Query(
      value =
          "select c from DbCriteria c where domainId=:domain and selectable = 1 and match(fullText, concat('+[', :domain, '_rank1]')) > 0 order by c.count desc, c.name asc")
  Page<DbCriteria> findCriteriaTopCounts(@Param("domain") String domain, Pageable page);

  @Query(
      value =
          "select c from DbCriteria c where domainId=:domain and selectable = 1 and standard=:standard and match(fullText, concat('+[', :domain, '_rank1]')) > 0 order by c.count desc, c.name asc")
  Page<DbCriteria> findCriteriaTopCountsByStandard(
      @Param("domain") String domain, @Param("standard") Boolean standard, Pageable page);

  @Query(
      value =
          "select c1 "
              + "from DbCriteria c1 "
              + "where c1.domainId = 'SURVEY' "
              + "and c1.subtype = 'QUESTION' "
              + "and c1.conceptId in ( select c.conceptId "
              + "                      from DbCriteria c "
              + "                     where c.domainId = 'SURVEY' "
              + "                       and match(c.fullText, concat(:term, '+[SURVEY_rank1]')) > 0 "
              + "                       and match(c.path, :id) > 0) "
              + "order by c1.count desc")
  Page<DbCriteria> findSurveyQuestionByPathAndTerm(
      @Param("id") Long id, @Param("term") String term, Pageable page);

  @Query(
      value =
          "select c1 "
              + "from DbCriteria c1 "
              + "where c1.domainId = 'SURVEY' "
              + "and c1.subtype = 'QUESTION' "
              + "and c1.conceptId in ( select c.conceptId "
              + "                      from DbCriteria c "
              + "                     where c.domainId = 'SURVEY' "
              + "                       and match(c.fullText, concat(:term, '+[SURVEY_rank1]')) > 0) "
              + "order by c1.count desc")
  Page<DbCriteria> findSurveyQuestionByTerm(@Param("term") String term, Pageable page);

  @Query(
      value =
          "select c1 "
              + "from DbCriteria c1 "
              + "where c1.domainId = 'SURVEY' "
              + "and c1.subtype = 'QUESTION' "
              + "and c1.conceptId in ( select c.conceptId "
              + "                      from DbCriteria c "
              + "                     where c.domainId = 'SURVEY' "
              + "                       and match(c.path, :id) > 0) "
              + "order by c1.count desc")
  Page<DbCriteria> findSurveyQuestionByPath(@Param("id") Long id, Pageable page);

  @Query(
      value =
          "select c from DbCriteria c where domainId=:domain and type=:type and standard=:standard and hierarchy=1 and code like upper(concat(:term,'%')) and match(fullText, concat('+[', :domain, '_rank1]')) > 0 order by c.count desc")
  List<DbCriteria> findCriteriaByDomainAndTypeAndStandardAndCode(
      @Param("domain") String domain,
      @Param("type") String type,
      @Param("standard") Boolean standard,
      @Param("term") String term,
      Pageable page);

  @Query(
      value =
          "select c from DbCriteria c where domainId = :domain and type = :type and standard = :standard and hierarchy=1 and match(fullText, concat(:term, '+[', :domain, '_rank1]')) > 0 order by c.count desc")
  List<DbCriteria> findCriteriaByDomainAndTypeAndStandardAndFullText(
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
  List<DbCriteria> findDrugBrandOrIngredientByValue(
      @Param("value") String value, @Param("limit") Integer limit);

  @Query(
      value =
          "select * from cb_criteria c "
              + "inner join ( "
              + "select distinct cr.concept_id_2 from cb_criteria_relationship cr "
              + "join cb_criteria c1 on (cr.concept_id_2 = c1.concept_id "
              + "and cr.concept_id_1 = :conceptId "
              + "and c1.domain_id = 'DRUG') ) cr1 on c.concept_id = cr1.concept_id_2 "
              + "and c.domain_id = 'DRUG' and c.type = 'RXNORM' and match(full_text) against('+[drug_rank1]' in boolean mode) order by c.est_count desc",
      nativeQuery = true)
  List<DbCriteria> findDrugIngredientByConceptId(@Param("conceptId") Long conceptId);

  @Query(
      value =
          "select * from cb_criteria where domain_id = 'PERSON' and type in ('GENDER', 'RACE', 'ETHNICITY', 'SEX') order by name asc",
      nativeQuery = true)
  List<DbCriteria> findAllDemographics();

  List<DbCriteria> findByDomainIdAndType(
      @Param("domainId") String domainId, @Param("type") String type, Sort sort);

  @Query(value = "select c.id from DbCriteria c where domainId = 'SURVEY' and name = :name")
  Long findSurveyId(@Param("name") String name);

  @Query(
      value =
          "select count(*) from DbCriteria where match(fullText, concat(:term, '+[', :domain, '_rank1]')) > 0")
  Long findDomainCount(@Param("term") String term, @Param("domain") String domain);

  @Query(
      value =
          "select count(*) from DbCriteria where standard =:standard and match(fullText, concat(:term, '+[', :domain, '_rank1]')) > 0")
  Long findDomainCountAndStandard(
      @Param("term") String term,
      @Param("domain") String domain,
      @Param("standard") Boolean standard);

  @Query(
      value =
          "select count(*) from DbCriteria where code like upper(concat(:term,'%')) and match(fullText, concat('+[', :domain, '_rank1]')) > 0")
  Long findDomainCountOnCode(@Param("term") String term, @Param("domain") String domain);

  @Query(
      value =
          "select count(*) from DbCriteria where code like upper(concat(:term,'%')) and standard = :standard and match(fullText, concat('+[', :domain, '_rank1]')) > 0")
  Long findDomainCountOnCodeAndStandard(
      @Param("term") String term,
      @Param("domain") String domain,
      @Param("standard") Boolean standard);

  @Query(
      value =
          "select count from cb_criteria c "
              + "join(select substring_index(path,'.',1) as survey_version_concept_id, count(*) as count "
              + "       from cb_criteria "
              + "      where domain_id = 'SURVEY' "
              + "        and subtype = 'QUESTION' "
              + "        and concept_id in ( select concept_id "
              + "                              from cb_criteria "
              + "                             where domain_id = 'SURVEY' "
              + "                               and match(full_text) against(concat(:term, '+[survey_rank1]') in boolean mode)) "
              + "   group by survey_version_concept_id) a "
              + "on c.id = a.survey_version_concept_id "
              + "where name = :surveyName",
      nativeQuery = true)
  Long findSurveyCount(@Param("surveyName") String surveyName, @Param("term") String term);

  @Query(
      value =
          "select surveyVersionConceptId, displayName, itemCount from( "
              + "select distinct csv.survey_version_concept_id as surveyVersionConceptId, csv.display_name as displayName, csa.item_count as itemCount, csv.display_order "
              + "from cb_survey_version csv "
              + "join cb_survey_attribute csa on csv.survey_version_concept_id = csa.survey_version_concept_id "
              + "where csv.survey_concept_id = :surveyConceptId "
              + "and csa.question_concept_id = :questionConceptId "
              + "and csa.answer_concept_id = :answerConceptId "
              + "order by csv.display_order) innerSql",
      nativeQuery = true)
  List<DbSurveyVersion> findSurveyVersionByQuestionConceptIdAndAnswerConceptId(
      @Param("surveyConceptId") Long surveyConceptId,
      @Param("questionConceptId") Long questionConceptId,
      @Param("answerConceptId") Long answerConceptId);

  @Query(value = "SELECT * FROM INFORMATION_SCHEMA.INNODB_FT_DEFAULT_STOPWORD", nativeQuery = true)
  List<String> findMySQLStopWords();

  List<DbCriteria> findByConceptIdIn(List<String> conceptIds);
}
