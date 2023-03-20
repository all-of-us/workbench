package org.pmiops.workbench.cdr.dao;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.List;
import org.pmiops.workbench.cdr.model.DbCardCount;
import org.pmiops.workbench.cdr.model.DbCriteria;
import org.pmiops.workbench.cdr.model.DbSurveyVersion;
import org.pmiops.workbench.cohortbuilder.SearchTerm;
import org.pmiops.workbench.model.Domain;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

/**
 * Some of our trees are polyhierarchical(SNOMED and DRUG). Since a node may exist multiple times in
 * this scenario with the same concept_id we only want to return it once from a user search. The
 * query to rank/min this to a single row was expensive. Instead, when building the cb_criteria
 * table(CDR indexing build) we add domain_rank1 to the full_text column for the first node that
 * matches in the tree for a specific concept_id. This allows us to use the full text index and
 * makes the query much faster.
 */
public interface CBCriteriaDao extends CrudRepository<DbCriteria, Long>, CustomCBCriteriaDao {

  /** Consolidate all Survey Question Dao calls into one default method. */
  default Page<DbCriteria> findSurveyQuestions(
      String surveyName, SearchTerm searchTerm, Pageable pageRequest) {
    Long surveyId = findSurveyId(surveyName);
    boolean hasSurveyId = surveyId != null;
    if (searchTerm.hasModifiedTermOnly()) {
      return hasSurveyId
          ? findSurveyQuestionByPathAndTerm(surveyId, searchTerm.getModifiedTerm(), pageRequest)
          : findSurveyQuestionByTerm(searchTerm.getModifiedTerm(), pageRequest);
    } else if (searchTerm.hasEndsWithOnly()) {
      return hasSurveyId
          ? findSurveyQuestionByPathAndNameEndsWith(
              surveyId, searchTerm.getEndsWithTerms(), pageRequest)
          : findSurveyQuestionByNameEndsWith(searchTerm.getEndsWithTerms(), pageRequest);
    } else if (searchTerm.hasEndsWithTermsAndModifiedTerm()) {
      return hasSurveyId
          ? findSurveyQuestionByPathAndTermAndNameEndsWith(
              surveyId, searchTerm.getModifiedTerm(), searchTerm.getEndsWithTerms(), pageRequest)
          : findSurveyQuestionByTermAndNameEndsWith(
              searchTerm.getModifiedTerm(), searchTerm.getEndsWithTerms(), pageRequest);
    }
    return Page.empty();
  }

  /** Consolidate all AutoComplete Dao calls into one default method. */
  default List<DbCriteria> findCriteriaAutoComplete(
      String domain,
      List<String> types,
      Boolean standard,
      List<Boolean> hierarchies,
      SearchTerm searchTerm,
      Pageable pageRequest) {
    Boolean isSurvey = Domain.SURVEY.toString().equals(domain);
    if (searchTerm.hasModifiedTermOnly()) {
      return isSurvey
          ? findSurveyQuestionByTerm(searchTerm.getModifiedTerm(), pageRequest).getContent()
          : findCriteriaByDomainAndTypeAndStandardAndFullText(
              domain, types, standard, hierarchies, searchTerm.getModifiedTerm(), pageRequest);
    } else if (searchTerm.hasEndsWithOnly()) {
      return isSurvey
          ? findSurveyQuestionByNameEndsWith(searchTerm.getEndsWithTerms(), pageRequest)
              .getContent()
          : findCriteriaByDomainAndTypeAndStandardAndNameEndsWith(
              domain, types, standard, hierarchies, searchTerm.getEndsWithTerms(), pageRequest);
    } else if (searchTerm.hasEndsWithTermsAndModifiedTerm()) {
      return isSurvey
          ? findSurveyQuestionByTermAndNameEndsWith(
                  searchTerm.getModifiedTerm(), searchTerm.getEndsWithTerms(), pageRequest)
              .getContent()
          : findCriteriaByDomainAndTypeAndStandardAndTermAndNameEndsWith(
              domain,
              types,
              standard,
              hierarchies,
              searchTerm.getModifiedTerm(),
              searchTerm.getEndsWithTerms(),
              pageRequest);
    }
    return ImmutableList.of();
  }

  default Page<DbCriteria> findCriteriaByDomain(
      String domain, SearchTerm searchTerm, Boolean standard, String type, Pageable pageRequest) {
    if (searchTerm.hasModifiedTermOnly()) {
      return findCriteriaByDomainAndFullTextAndStandardAndNotType(
          domain, searchTerm.getModifiedTerm(), standard, type, pageRequest);
    } else if (searchTerm.hasEndsWithOnly()) {
      return findCriteriaByDomainAndNameEndsWithAndStandardAndNotType(
          domain, searchTerm.getEndsWithTerms(), standard, type, pageRequest);
    } else if (searchTerm.hasEndsWithTermsAndModifiedTerm()) {
      return findCriteriaByDomainAndNameEndsWithAndTermAndStandardAndNotType(
          domain,
          searchTerm.getModifiedTerm(),
          searchTerm.getEndsWithTerms(),
          standard,
          type,
          pageRequest);
    }
    return Page.empty();
  }

  default List<DbCardCount> findDomainCounts(
      SearchTerm searchTerm, Boolean standard, List<String> domainNames) {
    if (searchTerm.hasModifiedTermOnly()) {
      return findDomainCountsByTermAndStandardAndDomains(
          searchTerm.getModifiedTerm(), standard, domainNames);
    } else if (searchTerm.hasEndsWithOnly()) {
      return findDomainCountsByNameEndsWithAndStandardAndDomains(
          searchTerm.getEndsWithTerms(), standard, domainNames);
    } else if (searchTerm.hasEndsWithTermsAndModifiedTerm()) {
      return findDomainCountsByTermAndNameEndsWithAndStandardAndDomains(
          searchTerm.getModifiedTerm(), searchTerm.getEndsWithTerms(), standard, domainNames);
    }
    return ImmutableList.of();
  }

  default List<DbCardCount> findSurveyCounts(SearchTerm searchTerm) {
    if (searchTerm.hasModifiedTermOnly()) {
      return findSurveyCountsByTerm(searchTerm.getModifiedTerm());
    } else if (searchTerm.hasEndsWithOnly()) {
      return findSurveyCountsByNameEndsWith(searchTerm.getEndsWithTerms());
    } else if (searchTerm.hasEndsWithTermsAndModifiedTerm()) {
      return findSurveyCountsByTermAndNameEndsWith(
          searchTerm.getModifiedTerm(), searchTerm.getEndsWithTerms());
    }
    return ImmutableList.of();
  }

  @Query(
      value =
          "select c "
              + "from DbCriteria c "
              + "where c.standard = :standard "
              + "and c.conceptId in (:conceptIds) "
              + "and match(c.fullText, concat('+[', :domainId, '_rank1]')) > 0")
  List<DbCriteria> findCriteriaByDomainIdAndStandardAndConceptIds(
      @Param("domainId") String domainId,
      @Param("standard") Boolean standard,
      @Param("conceptIds") Collection<String> conceptIds);

  @Query(
      value =
          "select concept_id_2 "
              + "from cb_criteria_relationship "
              + "where concept_id_1 = :conceptId",
      nativeQuery = true)
  List<Integer> findConceptId2ByConceptId1(@Param("conceptId") Long conceptId);

  @Query(
      value =
          "select cr "
              + "from DbCriteria cr "
              + "where cr.conceptId in (:conceptIds) "
              + "and cr.standard = :standard "
              + "and match(cr.fullText, :domain) > 0")
  List<DbCriteria> findStandardCriteriaByDomainAndConceptId(
      @Param("domain") String domain,
      @Param("standard") Boolean isStandard,
      @Param("conceptIds") List<String> conceptIds);

  @Query(
      value =
          "select * "
              + "from cb_criteria "
              + "where domain_id in (:domains) "
              + "and type=:type "
              + "and is_standard=:standard "
              + "and parent_id=:parentId "
              + "order by id asc",
      nativeQuery = true)
  List<DbCriteria> findCriteriaByDomainIdAndTypeAndParentIdOrderByIdAsc(
      @Param("domains") Collection<String> domains,
      @Param("type") String type,
      @Param("standard") Boolean isStandard,
      @Param("parentId") Long parentId);

  /**
   * This call should only be used to read the following hierarchies: Visits, Physical Measurements,
   * Race, Ethnicity, Gender and Sex at Birth.
   */
  @Query(
      value =
          "select c "
              + "from DbCriteria c "
              + "where c.domainId=:domain "
              + "and c.type=:type "
              + "order by c.id asc")
  List<DbCriteria> findCriteriaByDomainAndTypeOrderByIdAsc(
      @Param("domain") String domain, @Param("type") String type);

  @Query(
      value =
          "select c "
              + "from DbCriteria c "
              + "where c.standard=:standard "
              + "and c.code like upper(concat(:term,'%')) "
              + "and match(c.fullText, concat('+[', :domain, '_rank1]')) > 0 "
              + "and c.type != :type "
              + "order by c.count desc")
  Page<DbCriteria> findCriteriaByDomainAndCodeAndStandardAndNotType(
      @Param("domain") String domain,
      @Param("term") String term,
      @Param("standard") Boolean standard,
      @Param("type") String type,
      Pageable page);

  @Query(
      value =
          "select c "
              + "from DbCriteria c "
              + "where c.standard=:standard "
              + "and match(c.fullText, concat(:term, '+[', :domain, '_rank1]')) > 0 "
              + "and c.type != :type "
              + "order by c.count desc, c.name asc")
  Page<DbCriteria> findCriteriaByDomainAndFullTextAndStandardAndNotType(
      @Param("domain") String domain,
      @Param("term") String term,
      @Param("standard") Boolean standard,
      @Param("type") String type,
      Pageable page);

  @Query(
      value =
          "select c "
              + "from DbCriteria c "
              + "where c.selectable = 1 "
              + "and c.standard=:standard "
              + "and match(c.fullText, concat('+[', :domain, '_rank1]')) > 0 "
              + "order by c.count desc, c.name asc")
  Page<DbCriteria> findCriteriaTopCountsByStandard(
      @Param("domain") String domain, @Param("standard") Boolean standard, Pageable page);

  @Query(
      value =
          "select c1 "
              + "from DbCriteria c1 "
              + "where c1.domainId = 'SURVEY' "
              + "and c1.subtype = 'QUESTION' "
              + "and c1.fullText like '%[survey_rank1]%' "
              + "and c1.conceptId in ( select c.conceptId "
              + "                      from DbCriteria c "
              + "                     where c.domainId = 'SURVEY' "
              + "                       and match(c.fullText, concat(:term, '+[survey_rank1]')) > 0 "
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
              + "                       and match(c.fullText, concat(:term, '+[survey_rank1]')) > 0) "
              + "order by c1.count desc")
  Page<DbCriteria> findSurveyQuestionByTerm(@Param("term") String term, Pageable page);

  @Query(
      value =
          "select c1 "
              + "from DbCriteria c1 "
              + "where c1.domainId = 'SURVEY' "
              + "and c1.subtype = 'QUESTION' "
              + "and c1.id in ( select c.id "
              + "                 from DbCriteria c "
              + "                where c.domainId = 'SURVEY' "
              + "                  and match(c.path, :id) > 0) "
              + "order by c1.count desc")
  Page<DbCriteria> findSurveyQuestionByPath(@Param("id") Long id, Pageable page);

  @Query(
      value =
          "select c "
              + "from DbCriteria c "
              + "where c.type in (:types) "
              + "and c.standard=:standard "
              + "and c.hierarchy in (:hierarchies) "
              + "and c.code like upper(concat(:term,'%')) "
              + "and match(c.fullText, concat('+[', :domain, '_rank1]')) > 0 "
              + "order by c.count desc")
  List<DbCriteria> findCriteriaByDomainAndTypeAndStandardAndCode(
      @Param("domain") String domain,
      @Param("types") List<String> types,
      @Param("standard") Boolean standard,
      @Param("hierarchies") List<Boolean> hierarchies,
      @Param("term") String term,
      Pageable page);

  @Query(
      value =
          "select c "
              + "from DbCriteria c "
              + "where c.type in (:types) "
              + "and c.standard = :standard "
              + "and c.hierarchy in (:hierarchies) "
              + "and match(c.fullText, concat(:term, '+[', :domain, '_rank1]')) > 0 "
              + "order by c.count desc, name asc")
  List<DbCriteria> findCriteriaByDomainAndTypeAndStandardAndFullText(
      @Param("domain") String domain,
      @Param("types") List<String> types,
      @Param("standard") Boolean standard,
      @Param("hierarchies") List<Boolean> hierarchies,
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
          "select * "
              + "from cb_criteria "
              + "where domain_id = 'PERSON' "
              + "and type in ('GENDER', 'RACE', 'ETHNICITY', 'SEX') "
              + "order by name asc",
      nativeQuery = true)
  List<DbCriteria> findAllDemographics();

  @Query(
      value =
          "select c "
              + "from DbCriteria c "
              + "where c.type=:type "
              + "and match(c.fullText, concat('+[', :domainId, '_rank1]')) > 0")
  List<DbCriteria> findByDomainIdAndType(
      @Param("domainId") String domainId, @Param("type") String type, Sort sort);

  @Query(value = "select c.id from DbCriteria c where c.domainId = 'SURVEY' and c.name = :name")
  Long findSurveyId(@Param("name") String name);

  @Query(
      value =
          "select upper(substring_index(substring_index(full_text, '[', -1), '_rank1', 1)) as domainId, "
              + " upper(substring_index(substring_index(full_text, '[', -1), '_rank1', 1)) as name, "
              + " count(*) as count "
              + "from cb_criteria "
              + "where match(full_text) against(:term in boolean mode) "
              + "and full_text like '%_rank1%' "
              + "and is_standard = :standard "
              + "and domain_id in (:domains) "
              + "group by 1 "
              + "order by count desc",
      nativeQuery = true)
  List<DbCardCount> findDomainCountsByTermAndStandardAndDomains(
      @Param("term") String term,
      @Param("standard") Boolean standard,
      @Param("domains") List<String> domains);

  @Query(
      value =
          "select upper(substring_index(substring_index(full_text, '[', -1), '_rank1', 1)) as domainId, "
              + " upper(substring_index(substring_index(full_text, '[', -1), '_rank1', 1)) as name, "
              + " count(*) as count "
              + "from cb_criteria "
              + "where domain_id in (:domains) "
              + "and is_standard = :standard "
              + "and code like upper(concat(:term,'%')) "
              + "and full_text like '%_rank1%' "
              + "group by 1 "
              + "order by count desc",
      nativeQuery = true)
  List<DbCardCount> findDomainCountsByCode(
      @Param("term") String term,
      @Param("standard") Boolean standard,
      @Param("domains") List<String> domains);

  @Query(
      value =
          "select 'SURVEY' as domainId, name, count "
              + "from cb_criteria c "
              + "join( "
              + "select substring_index(path,'.',1) as survey_version_concept_id, count(*) as count "
              + "from cb_criteria "
              + "where domain_id = 'SURVEY' "
              + "and subtype = 'QUESTION' "
              + "and concept_id in ( select concept_id "
              + "from cb_criteria "
              + "where domain_id = 'SURVEY' "
              + "and match(full_text) against(concat(:term, '+[survey_rank1]') in boolean mode)) "
              + "group by survey_version_concept_id"
              + ") a on c.id = a.survey_version_concept_id "
              + "order by count desc",
      nativeQuery = true)
  List<DbCardCount> findSurveyCountsByTerm(@Param("term") String term);

  @Query(
      value =
          "select surveyVersionConceptId, displayName, itemCount from( "
              + "select distinct csv.survey_version_concept_id as surveyVersionConceptId, csv.display_name as displayName, csa.item_count as itemCount, csv.display_order "
              + "from cb_survey_version csv "
              + "join cb_survey_attribute csa on csv.survey_version_concept_id = csa.survey_version_concept_id "
              + "where csa.question_concept_id = :questionConceptId "
              + "and csa.answer_concept_id = :answerConceptId "
              + "order by csv.display_order) innerSql",
      nativeQuery = true)
  List<DbSurveyVersion> findSurveyVersionByQuestionConceptIdAndAnswerConceptId(
      @Param("questionConceptId") Long questionConceptId,
      @Param("answerConceptId") Long answerConceptId);

  @Query(
      value =
          "select distinct cr.* "
              + "from cb_criteria cr "
              + "join cb_survey_version sv on cr.concept_id = sv.survey_concept_id "
              + "where domain_id = 'SURVEY'",
      nativeQuery = true)
  List<DbCriteria> findVersionedSurveys();

  @Query(
      value =
          "select distinct concept_id "
              + "from cb_criteria c "
              + "join ( "
              + "      select cast(id as char) as id "
              + "      from cb_criteria "
              + "      where concept_id in (1740639) "
              + "      and domain_id = 'SURVEY' "
              + "      ) a on (c.path like CONCAT('%', a.id, '.%')) "
              + "where domain_id = 'SURVEY' "
              + "and type = 'PPI' "
              + "and subtype = 'QUESTION' "
              + "and concept_id in (:conceptIds)",
      nativeQuery = true)
  List<Long> findPFHHSurveyQuestionIds(@Param("conceptIds") List<Long> conceptIds);

  @Query(
      value =
          "select distinct cast(value as signed) "
              + "from cb_criteria c "
              + "join ( "
              + "      select cast(id as char) as id "
              + "      from cb_criteria "
              + "      where concept_id in (1740639) "
              + "      and domain_id = 'SURVEY' "
              + "      ) a on (c.path like CONCAT('%', a.id, '.%')) "
              + "where domain_id = 'SURVEY' "
              + "and type = 'PPI' "
              + "and subtype = 'ANSWER' "
              + "and concept_id in (:conceptIds)",
      nativeQuery = true)
  List<Long> findPFHHSurveyAnswerIds(@Param("conceptIds") List<Long> conceptIds);

  @Query(value = "SELECT * FROM INFORMATION_SCHEMA.INNODB_FT_DEFAULT_STOPWORD", nativeQuery = true)
  List<String> findMySQLStopWords();

  List<DbCriteria> findByConceptIdIn(List<String> conceptIds);

  List<DbCriteria> findByCodeIn(List<String> conceptCodes);
}
