package org.pmiops.workbench.cohortbuilder;

import java.util.List;
import org.pmiops.workbench.model.AgeTypeCount;
import org.pmiops.workbench.model.Criteria;
import org.pmiops.workbench.model.CriteriaAttribute;
import org.pmiops.workbench.model.DataFilter;
import org.pmiops.workbench.model.ParticipantDemographics;

public interface CohortBuilderService {

  List<AgeTypeCount> findAgeTypeCounts();

  List<CriteriaAttribute> findCriteriaAttributeByConceptId(Long conceptId);

  List<Criteria> findCriteriaAutoComplete(
      String domain, String term, String type, Boolean standard, Integer limit);

  List<Criteria> findCriteriaBy(String domain, String type, Boolean standard, Long parentId);

  List<Criteria> findCriteriaByDomainAndSearchTerm(String domain, String term, Integer limit);

  List<DataFilter> findDataFilters();

  List<Criteria> findDrugBrandOrIngredientByValue(String value, Integer limit);

  List<Criteria> findDrugIngredientByConceptId(Long conceptId);

  ParticipantDemographics findParticipantDemographics();

  List<Criteria> findStandardCriteriaByDomainAndConceptId(String domain, Long conceptId);
}
