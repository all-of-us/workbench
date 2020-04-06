package org.pmiops.workbench.cohortbuilder;

import static org.pmiops.workbench.model.FilterColumns.ETHNICITY;
import static org.pmiops.workbench.model.FilterColumns.GENDER;
import static org.pmiops.workbench.model.FilterColumns.RACE;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;
import org.jetbrains.annotations.NotNull;
import org.pmiops.workbench.cdr.dao.CBCriteriaAttributeDao;
import org.pmiops.workbench.cdr.dao.CBCriteriaDao;
import org.pmiops.workbench.cdr.dao.CBDataFilterDao;
import org.pmiops.workbench.cdr.dao.PersonDao;
import org.pmiops.workbench.cdr.model.DbCriteria;
import org.pmiops.workbench.cdr.model.DbCriteriaAttribute;
import org.pmiops.workbench.cohortbuilder.mappers.CohortBuilderMapper;
import org.pmiops.workbench.model.AgeTypeCount;
import org.pmiops.workbench.model.ConceptIdName;
import org.pmiops.workbench.model.Criteria;
import org.pmiops.workbench.model.CriteriaAttribute;
import org.pmiops.workbench.model.DataFilter;
import org.pmiops.workbench.model.FilterColumns;
import org.pmiops.workbench.model.ParticipantDemographics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class CohortBuilderServiceImpl implements CohortBuilderService {

  private static final Integer DEFAULT_TREE_SEARCH_LIMIT = 100;
  private static final Integer DEFAULT_CRITERIA_SEARCH_LIMIT = 250;

  private CBCriteriaAttributeDao cbCriteriaAttributeDao;
  private CBCriteriaDao cbCriteriaDao;
  private CBDataFilterDao cbDataFilterDao;
  private PersonDao personDao;
  private CohortBuilderMapper cohortBuilderMapper;

  @Autowired
  public CohortBuilderServiceImpl(
      CBCriteriaAttributeDao cbCriteriaAttributeDao,
      CBCriteriaDao cbCriteriaDao,
      CBDataFilterDao cbDataFilterDao,
      PersonDao personDao,
      CohortBuilderMapper cohortBuilderMapper) {
    this.cbCriteriaAttributeDao = cbCriteriaAttributeDao;
    this.cbCriteriaDao = cbCriteriaDao;
    this.cbDataFilterDao = cbDataFilterDao;
    this.personDao = personDao;
    this.cohortBuilderMapper = cohortBuilderMapper;
  }

  @Override
  public List<AgeTypeCount> findAgeTypeCounts() {
    return personDao.findAgeTypeCounts().stream()
        .map(cohortBuilderMapper::dbModelToClient)
        .collect(Collectors.toList());
  }

  @Override
  public List<CriteriaAttribute> findCriteriaAttributeByConceptId(Long conceptId) {
    List<DbCriteriaAttribute> attributeList =
        cbCriteriaAttributeDao.findCriteriaAttributeByConceptId(conceptId);
    return attributeList.stream()
        .map(cohortBuilderMapper::dbModelToClient)
        .collect(Collectors.toList());
  }

  @Override
  public List<Criteria> findCriteriaAutoComplete(
      String domain, String term, String type, Boolean standard, Integer limit) {
    PageRequest pageRequest =
        new PageRequest(0, Optional.ofNullable(limit).orElse(DEFAULT_TREE_SEARCH_LIMIT));
    List<DbCriteria> criteriaList =
        cbCriteriaDao.findCriteriaByDomainAndTypeAndStandardAndSynonyms(
            domain, type, standard, modifyTermMatch(term), pageRequest);
    if (criteriaList.isEmpty()) {
      criteriaList =
          cbCriteriaDao.findCriteriaByDomainAndTypeAndStandardAndCode(
              domain, type, standard, term, pageRequest);
    }
    return criteriaList.stream()
        .map(cohortBuilderMapper::dbModelToClient)
        .collect(Collectors.toList());
  }

  @Override
  public List<Criteria> findCriteriaBy(
      String domain, String type, Boolean standard, Long parentId) {
    List<DbCriteria> criteriaList;
    if (parentId != null) {
      criteriaList =
          cbCriteriaDao.findCriteriaByDomainIdAndTypeAndParentIdOrderByIdAsc(
              domain, type, standard, parentId);
    } else {
      criteriaList = cbCriteriaDao.findCriteriaByDomainAndTypeOrderByIdAsc(domain, type);
    }
    return criteriaList.stream()
        .map(cohortBuilderMapper::dbModelToClient)
        .collect(Collectors.toList());
  }

  @Override
  public List<Criteria> findCriteriaByDomainAndSearchTerm(
      String domain, String term, Integer limit) {
    List<DbCriteria> criteriaList;
    PageRequest pageRequest =
        new PageRequest(0, Optional.ofNullable(limit).orElse(DEFAULT_CRITERIA_SEARCH_LIMIT));
    List<DbCriteria> exactMatchByCode = cbCriteriaDao.findExactMatchByCode(domain, term);
    boolean isStandard = exactMatchByCode.isEmpty() || exactMatchByCode.get(0).getStandard();

    if (!isStandard) {
      Map<Boolean, List<DbCriteria>> groups =
          cbCriteriaDao
              .findCriteriaByDomainAndTypeAndCode(
                  domain, exactMatchByCode.get(0).getType(), isStandard, term, pageRequest)
              .stream()
              .collect(Collectors.partitioningBy(c -> c.getCode().equals(term)));
      criteriaList = groups.get(true);
      criteriaList.addAll(groups.get(false));
    } else {
      criteriaList =
          cbCriteriaDao.findCriteriaByDomainAndCode(domain, isStandard, term, pageRequest);
      if (criteriaList.isEmpty() && !term.contains(".")) {
        criteriaList =
            cbCriteriaDao.findCriteriaByDomainAndSynonyms(
                domain, isStandard, modifyTermMatch(term), pageRequest);
      }
      if (criteriaList.isEmpty() && !term.contains(".")) {
        criteriaList =
            cbCriteriaDao.findCriteriaByDomainAndSynonyms(
                domain, !isStandard, modifyTermMatch(term), pageRequest);
      }
    }
    return criteriaList.stream()
        .map(cohortBuilderMapper::dbModelToClient)
        .collect(Collectors.toList());
  }

  @Override
  public List<DataFilter> findDataFilters() {
    return StreamSupport.stream(cbDataFilterDao.findAll().spliterator(), false)
        .map(cohortBuilderMapper::dbModelToClient)
        .collect(Collectors.toList());
  }

  @Override
  public List<Criteria> findDrugBrandOrIngredientByValue(String value, Integer limit) {
    List<DbCriteria> criteriaList =
        cbCriteriaDao.findDrugBrandOrIngredientByValue(
            value, Optional.ofNullable(limit).orElse(DEFAULT_TREE_SEARCH_LIMIT));
    return criteriaList.stream()
        .map(cohortBuilderMapper::dbModelToClient)
        .collect(Collectors.toList());
  }

  @Override
  public List<Criteria> findDrugIngredientByConceptId(Long conceptId) {
    List<DbCriteria> criteriaList = cbCriteriaDao.findDrugIngredientByConceptId(conceptId);
    return criteriaList.stream()
        .map(cohortBuilderMapper::dbModelToClient)
        .collect(Collectors.toList());
  }

  @Override
  public ParticipantDemographics findParticipantDemographics() {
    List<DbCriteria> criteriaList = cbCriteriaDao.findGenderRaceEthnicity();
    return new ParticipantDemographics()
        .genderList(buildConceptIdNameList(criteriaList, GENDER))
        .raceList(buildConceptIdNameList(criteriaList, RACE))
        .ethnicityList(buildConceptIdNameList(criteriaList, ETHNICITY));
  }

  @Override
  public List<Criteria> findStandardCriteriaByDomainAndConceptId(String domain, Long conceptId) {
    // These look ups can be done as one dao call but to make this code testable with the mysql
    // fulltext search match function and H2 in memory database, it's split into 2 separate calls
    // Each call is sub second, so having 2 calls and being testable is better than having one call
    // and it being non-testable.
    List<String> conceptIds =
        cbCriteriaDao.findConceptId2ByConceptId1(conceptId).stream()
            .map(String::valueOf)
            .collect(Collectors.toList());
    List<DbCriteria> criteriaList = new ArrayList<>();
    if (!conceptIds.isEmpty()) {
      criteriaList =
          cbCriteriaDao.findStandardCriteriaByDomainAndConceptId(domain, true, conceptIds);
    }
    return criteriaList.stream()
        .map(cohortBuilderMapper::dbModelToClient)
        .collect(Collectors.toList());
  }

  private String modifyTermMatch(String term) {
    String[] keywords = term.split("\\W+");
    if (keywords.length == 1 && keywords[0].length() <= 3) {
      return "+\"" + keywords[0];
    }

    return IntStream.range(0, keywords.length)
        .filter(i -> keywords[i].length() > 2)
        .mapToObj(
            i -> {
              if ((i + 1) != keywords.length) {
                return "+\"" + keywords[i] + "\"";
              }
              return "+" + keywords[i] + "*";
            })
        .collect(Collectors.joining());
  }

  @NotNull
  private List<ConceptIdName> buildConceptIdNameList(
      List<DbCriteria> criteriaList, FilterColumns columnName) {
    return criteriaList.stream()
        .filter(c -> c.getType().equals(columnName.toString()))
        .map(
            c -> new ConceptIdName().conceptId(new Long(c.getConceptId())).conceptName(c.getName()))
        .collect(Collectors.toList());
  }
}
