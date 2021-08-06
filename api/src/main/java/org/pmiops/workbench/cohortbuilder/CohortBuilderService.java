package org.pmiops.workbench.cohortbuilder;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.pmiops.workbench.db.model.DbConceptSetConceptId;
import org.pmiops.workbench.model.AgeType;
import org.pmiops.workbench.model.AgeTypeCount;
import org.pmiops.workbench.model.Criteria;
import org.pmiops.workbench.model.CriteriaAttribute;
import org.pmiops.workbench.model.CriteriaListWithCountResponse;
import org.pmiops.workbench.model.CriteriaMenu;
import org.pmiops.workbench.model.DataFilter;
import org.pmiops.workbench.model.DemoChartInfo;
import org.pmiops.workbench.model.DomainCard;
import org.pmiops.workbench.model.DomainInfo;
import org.pmiops.workbench.model.GenderOrSexType;
import org.pmiops.workbench.model.ParticipantDemographics;
import org.pmiops.workbench.model.SearchRequest;
import org.pmiops.workbench.model.SurveyModule;
import org.pmiops.workbench.model.SurveyVersion;

public interface CohortBuilderService {

  class ConceptIds {

    private final List<Long> standardConceptIds;
    private final List<Long> sourceConceptIds;

    public ConceptIds(List<Long> standardConceptIds, List<Long> sourceConceptIds) {
      this.standardConceptIds = standardConceptIds;
      this.sourceConceptIds = sourceConceptIds;
    }

    public List<Long> getStandardConceptIds() {
      return standardConceptIds;
    }

    public List<Long> getSourceConceptIds() {
      return sourceConceptIds;
    }
  }

  ConceptIds classifyConceptIds(Set<Long> conceptIds);

  List<Criteria> findCriteriaByDomainIdAndConceptIds(
      String domainId, Collection<DbConceptSetConceptId> dbConceptSetConceptIds);

  List<Criteria> findCriteriaByDomainIdAndConceptIds(
      String domainId, Collection<Long> sourceConceptIds, Collection<Long> standardConceptIds);

  Long countParticipants(SearchRequest request);

  List<AgeTypeCount> findAgeTypeCounts();

  List<CriteriaAttribute> findCriteriaAttributeByConceptId(Long conceptId);

  List<Criteria> findCriteriaAutoComplete(
      String domain, String term, String type, Boolean standard, Integer limit);

  List<Criteria> findCriteriaBy(String domain, String type, Boolean standard, Long parentId);

  CriteriaListWithCountResponse findCriteriaByDomainAndSearchTerm(
      String domain, String term, String surveyName, Integer limit);

  List<CriteriaMenu> findCriteriaMenuByParentId_old(long parentId);

  List<CriteriaMenu> findCriteriaMenuByParentId(long parentId);

  List<DataFilter> findDataFilters();

  List<DemoChartInfo> findDemoChartInfo(
      GenderOrSexType genderOrSexType, AgeType ageType, SearchRequest request);

  Long findDomainCount(String domain, String term);

  List<DomainInfo> findDomainInfos();

  List<DomainCard> findDomainCards();

  List<Criteria> findDrugBrandOrIngredientByValue(String value, Integer limit);

  List<Criteria> findDrugIngredientByConceptId(Long conceptId);

  ParticipantDemographics findParticipantDemographics();

  List<Criteria> findStandardCriteriaByDomainAndConceptId(String domain, Long conceptId);

  /**
   * Build a map that contains all gender/race/ethnicity/sex_at_birth names with the concept id as
   * the key.
   */
  Map<Long, String> findAllDemographicsMap();

  List<String> findSortedConceptIdsByDomainIdAndType(
      String domainId, String sortColumn, String sortName);

  Long findSurveyCount(String name, String term);

  List<SurveyModule> findSurveyModules();

  List<SurveyVersion> findSurveyVersionByQuestionConceptId(
      Long surveyConceptId, Long questionConceptId);

  List<SurveyVersion> findSurveyVersionByQuestionConceptIdAndAnswerConceptId(
      Long surveyConceptId, Long questionConceptId, Long answerConceptId);
}
