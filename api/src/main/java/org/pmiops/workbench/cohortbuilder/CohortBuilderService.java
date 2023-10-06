package org.pmiops.workbench.cohortbuilder;

import com.google.common.collect.Table;
import java.util.Collection;
import java.util.List;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.pmiops.workbench.db.model.DbConceptSetConceptId;
import org.pmiops.workbench.model.AgeTypeCount;
import org.pmiops.workbench.model.CardCount;
import org.pmiops.workbench.model.CohortDefinition;
import org.pmiops.workbench.model.Criteria;
import org.pmiops.workbench.model.CriteriaAttribute;
import org.pmiops.workbench.model.CriteriaListWithCountResponse;
import org.pmiops.workbench.model.CriteriaMenu;
import org.pmiops.workbench.model.CriteriaSearchRequest;
import org.pmiops.workbench.model.CriteriaType;
import org.pmiops.workbench.model.DataFilter;
import org.pmiops.workbench.model.DomainCard;
import org.pmiops.workbench.model.ParticipantDemographics;
import org.pmiops.workbench.model.SurveyModule;
import org.pmiops.workbench.model.SurveyVersion;
import org.pmiops.workbench.model.Variant;
import org.pmiops.workbench.model.VariantFilterRequest;
import org.pmiops.workbench.model.VariantFilterResponse;

public interface CohortBuilderService {

  List<Criteria> findCriteriaByDomainIdAndConceptIds(
      String domainId, Collection<DbConceptSetConceptId> dbConceptSetConceptIds);

  List<Criteria> findCriteriaByDomainIdAndConceptIds(
      String domainId, Collection<Long> sourceConceptIds, Collection<Long> standardConceptIds);

  Long countParticipants(CohortDefinition cohortDefinition);

  List<AgeTypeCount> findAgeTypeCounts();

  List<CriteriaAttribute> findCriteriaAttributeByConceptId(Long conceptId);

  List<Criteria> findCriteriaAutoComplete(CriteriaSearchRequest criteriaSearchRequest);

  List<Criteria> findCriteriaBy(String domain, String type, Boolean standard, Long parentId);

  CriteriaListWithCountResponse findCriteriaByDomain(CriteriaSearchRequest request);

  List<CriteriaMenu> findCriteriaMenuByParentId(long parentId);

  List<DataFilter> findDataFilters();

  List<CardCount> findUniversalDomainCounts(String term);

  List<CardCount> findDomainCounts(String term);

  List<DomainCard> findDomainCards();

  List<Criteria> findDrugBrandOrIngredientByValue(String value, Integer limit);

  List<Criteria> findDrugIngredientByConceptId(Long conceptId);

  ParticipantDemographics findParticipantDemographics();

  List<Criteria> findStandardCriteriaByDomainAndConceptId(String domain, Long conceptId);

  /**
   * Build a map that contains all gender/race/ethnicity/sex_at_birth names with the concept id as
   * the key.
   */
  Table<Long, CriteriaType, String> findAllDemographicsMap();

  List<String> findSortedConceptIdsByDomainIdAndType(
      String domainId, String sortColumn, String sortName);

  List<SurveyModule> findSurveyModules();

  List<SurveyVersion> findSurveyVersionByQuestionConceptId(Long questionConceptId);

  List<SurveyVersion> findSurveyVersionByQuestionConceptIdAndAnswerConceptId(
      Long questionConceptId, Long answerConceptId);

  List<Criteria> findVersionedSurveys();

  List<Criteria> findCriteriaByConceptIdsOrConceptCodes(List<String> conceptKeys);

  List<Long> findSurveyQuestionIds(List<Long> surveyConceptIds);

  /**
   * Find variant filters by specified search term. The list of variant filters can change depending
   * on the search results.
   */
  VariantFilterResponse findVariantFilters(VariantFilterRequest filters);

  /**
   * Find variants by specified search term. Supported searches include variant id, gene name,
   * chromosome position and rs number. This method returns an ImmutableTriple that includes next
   * pageToken, total query count and list of variants(paginated).
   */
  ImmutableTriple<String, Integer, List<Variant>> findVariants(VariantFilterRequest filters);
}
