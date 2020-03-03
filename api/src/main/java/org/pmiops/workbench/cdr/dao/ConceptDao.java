package org.pmiops.workbench.cdr.dao;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;
import org.pmiops.workbench.cdr.model.DbConcept;
import org.pmiops.workbench.db.model.CommonStorageEnums;
import org.pmiops.workbench.model.Domain;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ConceptDao extends CrudRepository<DbConcept, Long> {

  /**
   * Return standard or all concepts in each vocabulary for the specified domain matching the
   * specified expression, matching concept name, synonym, ID, or code.
   *
   * @param keyword SQL MATCH expression to match concept name or synonym
   * @param conceptTypes can be 'S', 'C' or ''
   * @param domainId domain ID to use when filtering concepts
   * @return per-vocabulary concept counts
   */
  @Query(
      value =
          "select distinct c from DbConcept c "
              + "where (c.countValue > 0 or c.sourceCountValue > 0) "
              + "and matchConcept(c.conceptName, c.conceptCode, c.vocabularyId, c.synonymsStr, ?1) > 0 "
              + "and c.standardConcept IN (?2) "
              + "and c.domainId = ?3")
  Page<DbConcept> findConcepts(
      String keyword, ImmutableList<String> conceptTypes, String domainId, Pageable page);

  /**
   * Return standard or all concepts in each vocabulary for the specified domain matching the
   * specified expression, matching concept name, synonym, ID, or code.
   *
   * @param conceptTypes can be 'S', 'C' or ''
   * @param domainId domain ID to use when filtering concepts
   * @return per-vocabulary concept counts
   */
  @Query(
      value =
          "select distinct c from DbConcept c "
              + "where (c.countValue > 0 or c.sourceCountValue > 0) "
              + "and c.standardConcept IN (?1) "
              + "and c.domainId = ?2")
  Page<DbConcept> findConcepts(ImmutableList<String> conceptTypes, String domainId, Pageable page);

  /**
   * Return PM concepts in each vocabulary for the specified domain matching the specified
   * expression, matching concept name, synonym, ID, or code.
   *
   * @param keyword SQL MATCH expression to match concept name or synonym
   * @param conceptTypes can be 'S', 'C' or ''
   * @return per-vocabulary concept counts
   */
  @Query(
      value =
          "select new DbConcept(c.conceptId, c.conceptName, c.standardConcept, c.conceptCode, c.conceptClassId, c.vocabularyId, "
              + "'Physical Measurement' as domainId, c.countValue, c.sourceCountValue, c.prevalence, c.synonymsStr) "
              + "from DbConcept c "
              + "where (c.countValue > 0 or c.sourceCountValue > 0) "
              + "and matchConcept(c.conceptName, c.conceptCode, c.vocabularyId, c.synonymsStr, ?1) > 0 "
              + "and c.standardConcept IN (?2) "
              + "and c.domainId = 'Measurement' "
              + "and c.vocabularyId = 'PPI' "
              + "and c.conceptClassId = 'Clinical Observation'")
  Page<DbConcept> findPhysicalMeasurementConcepts(
      String keyword, ImmutableList<String> conceptTypes, Pageable page);

  /**
   * Return PM concepts in each vocabulary for the specified domain matching the specified
   * expression, matching concept name, synonym, ID, or code.
   *
   * @param conceptTypes can be 'S', 'C' or ''
   * @return per-vocabulary concept counts
   */
  @Query(
      value =
          "select new DbConcept(c.conceptId, c.conceptName, c.standardConcept, c.conceptCode, c.conceptClassId, c.vocabularyId, "
              + "'Physical Measurement' as domainId, c.countValue, c.sourceCountValue, c.prevalence, c.synonymsStr) "
              + "from DbConcept c "
              + "where (c.countValue > 0 or c.sourceCountValue > 0) "
              + "and c.standardConcept IN (?1) "
              + "and c.domainId = 'Measurement' "
              + "and c.vocabularyId = 'PPI' "
              + "and c.conceptClassId = 'Clinical Observation'")
  Page<DbConcept> findPhysicalMeasurementConcepts(
      ImmutableList<String> conceptTypes, Pageable page);

  /**
   * Return any concepts in each vocabulary for the specified domain matching the specified
   * expression, matching concept name, synonym, ID, or code.
   */
  default Page<DbConcept> findConcepts(
      String keyword, ImmutableList<String> conceptTypes, Domain domainId, Pageable pageable) {
    if (Domain.PHYSICALMEASUREMENT.equals(domainId)) {
      return StringUtils.isBlank(keyword)
          ? findPhysicalMeasurementConcepts(conceptTypes, pageable)
          : findPhysicalMeasurementConcepts(keyword, conceptTypes, pageable);
    }
    String toDomainId = CommonStorageEnums.domainToDomainId(domainId);
    return StringUtils.isBlank(keyword)
        ? findConcepts(conceptTypes, toDomainId, pageable)
        : findConcepts(keyword, conceptTypes, toDomainId, pageable);
  }

  @Query(
      value =
          "select count(*) "
              + "from "
              + "  (select case when @curType = concept_name then @curRow \\:= @curRow + 1 else @curRow \\:= 1 end as rank, "
              + "   id, @curType \\:= concept_name as concept_name, concept_id, domain_id, vocabulary_id, concept_class_id, standard_concept, concept_code, count_value, prevalence, source_count_value, synonyms "
              + "    from "
              + "      (select cr.id, c.* "
              + "        from cb_criteria cr "
              + "        join concept c on c.concept_id = cr.concept_id "
              + "        where cr.domain_id = 'SURVEY' "
              + "          and cr.type = 'PPI' "
              + "          and cr.subtype = 'ANSWER' "
              + "          and match(c.concept_name, c.concept_code, c.vocabulary_id, c.synonyms) against (:term in boolean mode)"
              + "      ) a, "
              + "      (select @curRow \\:= 0, @curType \\:= '') r "
              + "    order by concept_name, id "
              + "  ) as x "
              + "where rank = 1",
      nativeQuery = true)
  long countSurveys(@Param("term") String term);

  @Query(
      value =
          "select count(*) "
              + "from "
              + "  (select case when @curType = concept_name then @curRow \\:= @curRow + 1 else @curRow \\:= 1 end as rank, "
              + "   id, @curType \\:= concept_name as concept_name, concept_id, domain_id, vocabulary_id, concept_class_id, standard_concept, concept_code, count_value, prevalence, source_count_value, synonyms "
              + "    from "
              + "      (select cr.id, c.* "
              + "        from cb_criteria cr "
              + "        join concept c on c.concept_id = cr.concept_id "
              + "        where cr.domain_id = 'SURVEY' "
              + "          and cr.type = 'PPI' "
              + "          and cr.subtype = 'ANSWER' "
              + "          and cr.path like CONCAT( "
              + "            (select dc.id "
              + "              from cb_criteria dc "
              + "              where dc.domain_id = 'SURVEY' "
              + "                and dc.type = 'PPI' "
              + "                and dc.name = :surveyName), '.%' "
              + "          ) "
              + "          and match(c.concept_name, c.concept_code, c.vocabulary_id, c.synonyms) against (:term in boolean mode)"
              + "      ) a, "
              + "      (select @curRow \\:= 0, @curType \\:= '') r "
              + "    order by concept_name, id "
              + "  ) as x "
              + "where rank = 1",
      nativeQuery = true)
  long countSurveyByTermAndName(@Param("term") String term, @Param("surveyName") String surveyName);

  @Query(
      value =
          "select concept_id, concept_name, domain_id, vocabulary_id, concept_class_id, standard_concept, concept_code, count_value, prevalence, source_count_value, synonyms "
              + "from "
              + "  (select case when @curType = concept_name then @curRow \\:= @curRow + 1 else @curRow \\:= 1 end as rank, "
              + "   id, @curType \\:= concept_name as concept_name, concept_id, domain_id, vocabulary_id, concept_class_id, standard_concept, concept_code, count_value, prevalence, source_count_value, synonyms "
              + "    from "
              + "      (select cr.id, c.* "
              + "        from cb_criteria cr "
              + "        join concept c on c.concept_id = cr.concept_id "
              + "        where cr.domain_id = 'SURVEY' "
              + "          and cr.type = 'PPI' "
              + "          and cr.subtype = 'ANSWER' "
              + "          and match(c.concept_name, c.concept_code, c.vocabulary_id, c.synonyms) against (:term in boolean mode)"
              + "      ) a, "
              + "      (select @curRow \\:= 0, @curType \\:= '') r "
              + "    order by concept_name, id "
              + "  ) as x "
              + "where rank = 1 "
              + "order by id "
              + "limit :limit offset :offset",
      nativeQuery = true)
  List<DbConcept> findSurveys(
      @Param("term") String term, @Param("limit") int limit, @Param("offset") int offset);

  @Query(
      value =
          "select concept_id, concept_name, domain_id, vocabulary_id, concept_class_id, standard_concept, concept_code, count_value, prevalence, source_count_value, synonyms "
              + "from "
              + "  (select case when @curType = concept_name then @curRow \\:= @curRow + 1 else @curRow \\:= 1 end as rank, "
              + "   id, @curType \\:= concept_name as concept_name, concept_id, domain_id, vocabulary_id, concept_class_id, standard_concept, concept_code, count_value, prevalence, source_count_value, synonyms "
              + "    from "
              + "      (select cr.id, c.* "
              + "        from cb_criteria cr "
              + "        join concept c on c.concept_id = cr.concept_id "
              + "        where cr.domain_id = 'SURVEY' "
              + "          and cr.type = 'PPI' "
              + "          and cr.subtype = 'ANSWER' "
              + "      ) a, "
              + "      (select @curRow \\:= 0, @curType \\:= '') r "
              + "    order by concept_name, id "
              + "  ) as x "
              + "where rank = 1 "
              + "order by id "
              + "limit :limit offset :offset",
      nativeQuery = true)
  List<DbConcept> findSurveys(@Param("limit") int limit, @Param("offset") int offset);

  @Query(
      value =
          "select count(*) "
              + "from "
              + "  (select case when @curType = concept_name then @curRow \\:= @curRow + 1 else @curRow \\:= 1 end as rank, "
              + "   id, @curType \\:= concept_name as concept_name, concept_id, domain_id, vocabulary_id, concept_class_id, standard_concept, concept_code, count_value, prevalence, source_count_value, synonyms "
              + "    from "
              + "      (select cr.id, c.* "
              + "        from cb_criteria cr "
              + "        join concept c on c.concept_id = cr.concept_id "
              + "        where cr.domain_id = 'SURVEY' "
              + "          and cr.type = 'PPI' "
              + "          and cr.subtype = 'ANSWER' "
              + "      ) a, "
              + "      (select @curRow \\:= 0, @curType \\:= '') r "
              + "    order by concept_name, id "
              + "  ) as x "
              + "where rank = 1",
      nativeQuery = true)
  long countSurveys();

  @Query(
      value =
          "select concept_id, concept_name, domain_id, vocabulary_id, concept_class_id, standard_concept, concept_code, count_value, prevalence, source_count_value, synonyms "
              + "from "
              + "  (select case when @curType = concept_name then @curRow \\:= @curRow + 1 else @curRow \\:= 1 end as rank, "
              + "   id, @curType \\:= concept_name as concept_name, concept_id, domain_id, vocabulary_id, concept_class_id, standard_concept, concept_code, count_value, prevalence, source_count_value, synonyms "
              + "    from "
              + "      (select cr.id, c.* "
              + "        from cb_criteria cr "
              + "        join concept c on c.concept_id = cr.concept_id "
              + "        where cr.domain_id = 'SURVEY' "
              + "          and cr.type = 'PPI' "
              + "          and cr.subtype = 'ANSWER' "
              + "          and cr.path like CONCAT( "
              + "            (select dc.id "
              + "              from cb_criteria dc "
              + "              where dc.domain_id = 'SURVEY' "
              + "                and dc.type = 'PPI' "
              + "                and dc.name = :surveyName), '.%' "
              + "          ) "
              + "          and match(c.concept_name, c.concept_code, c.vocabulary_id, c.synonyms) against (:term in boolean mode)"
              + "      ) a, "
              + "      (select @curRow \\:= 0, @curType \\:= '') r "
              + "    order by concept_name, id "
              + "  ) as x "
              + "where rank = 1 "
              + "order by id "
              + "limit :limit offset :offset",
      nativeQuery = true)
  List<DbConcept> findSurveysByTermAndName(
      @Param("term") String term,
      @Param("surveyName") String surveyName,
      @Param("limit") int limit,
      @Param("offset") int offset);

  @Query(
      value =
          "select count(*) "
              + "from "
              + "  (select case when @curType = concept_name then @curRow \\:= @curRow + 1 else @curRow \\:= 1 end as rank, "
              + "   id, @curType \\:= concept_name as concept_name, concept_id, domain_id, vocabulary_id, concept_class_id, standard_concept, concept_code, count_value, prevalence, source_count_value, synonyms "
              + "    from "
              + "      (select cr.id, c.* "
              + "        from cb_criteria cr "
              + "        join concept c on c.concept_id = cr.concept_id "
              + "        where cr.domain_id = 'SURVEY' "
              + "          and cr.type = 'PPI' "
              + "          and cr.subtype = 'ANSWER' "
              + "          and cr.path like CONCAT( "
              + "            (select dc.id "
              + "              from cb_criteria dc "
              + "              where dc.domain_id = 'SURVEY' "
              + "                and dc.type = 'PPI' "
              + "                and dc.name = :surveyName), '.%' "
              + "          ) "
              + "      ) a, "
              + "      (select @curRow \\:= 0, @curType \\:= '') r "
              + "    order by concept_name, id "
              + "  ) as x "
              + "where rank = 1",
      nativeQuery = true)
  long countSurveyByName(@Param("surveyName") String surveyName);

  @Query(
      value =
          "select concept_id, concept_name, domain_id, vocabulary_id, concept_class_id, standard_concept, concept_code, count_value, prevalence, source_count_value, synonyms "
              + "from "
              + "  (select case when @curType = concept_name then @curRow \\:= @curRow + 1 else @curRow \\:= 1 end as rank, "
              + "   id, @curType \\:= concept_name as concept_name, concept_id, domain_id, vocabulary_id, concept_class_id, standard_concept, concept_code, count_value, prevalence, source_count_value, synonyms "
              + "    from "
              + "      (select cr.id, c.* "
              + "        from cb_criteria cr "
              + "        join concept c on c.concept_id = cr.concept_id "
              + "        where cr.domain_id = 'SURVEY' "
              + "          and cr.type = 'PPI' "
              + "          and cr.subtype = 'ANSWER' "
              + "          and cr.path like CONCAT( "
              + "            (select dc.id "
              + "              from cb_criteria dc "
              + "              where dc.domain_id = 'SURVEY' "
              + "                and dc.type = 'PPI' "
              + "                and dc.name = :surveyName), '.%' "
              + "          ) "
              + "      ) a, "
              + "      (select @curRow \\:= 0, @curType \\:= '') r "
              + "    order by concept_name, id "
              + "  ) as x "
              + "where rank = 1 "
              + "order by id "
              + "limit :limit offset :offset",
      nativeQuery = true)
  List<DbConcept> findSurveysByName(
      @Param("surveyName") String surveyName,
      @Param("limit") int limit,
      @Param("offset") int offset);

  /**
   * Find surveys by specified term or surveyName. If both term and surveyName are blank return all
   * surveys.
   *
   * @param term
   * @param surveyName
   * @param pageable
   * @return
   */
  default List<DbConcept> findSurveys(String term, String surveyName, Pageable pageable) {
    int limit = pageable.getPageSize();
    int offset = pageable.getPageNumber() * limit;
    if (StringUtils.isBlank(term) && StringUtils.isBlank(surveyName)) {
      return findSurveys(limit, offset);
    }
    if (StringUtils.isBlank(term)) {
      return findSurveysByName(surveyName, limit, offset);
    }
    if (StringUtils.isBlank(surveyName)) {
      return findSurveys(term, limit, offset);
    }
    return findSurveysByTermAndName(term, surveyName, limit, offset);
  }

  /**
   * Count surveys by specified term or surveyName. If both term and surveyName are blank count all
   * surveys.
   *
   * @param term
   * @param surveyName
   * @return
   */
  default long countSurveys(String term, String surveyName) {
    if (StringUtils.isBlank(term) && StringUtils.isBlank(surveyName)) {
      return countSurveys();
    }
    if (StringUtils.isBlank(term)) {
      return countSurveyByName(surveyName);
    }
    if (StringUtils.isBlank(surveyName)) {
      return countSurveys(term);
    }
    return countSurveyByTermAndName(term, surveyName);
  }
}
