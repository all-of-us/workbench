package org.pmiops.workbench.cohortbuilder;

import static org.pmiops.workbench.model.FilterColumns.ETHNICITY;
import static org.pmiops.workbench.model.FilterColumns.GENDER;
import static org.pmiops.workbench.model.FilterColumns.RACE;
import static org.pmiops.workbench.model.FilterColumns.SEXATBIRTH;

import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableResult;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;
import javax.inject.Provider;
import org.jetbrains.annotations.NotNull;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.cdr.cache.MySQLStopWords;
import org.pmiops.workbench.cdr.dao.CBCriteriaAttributeDao;
import org.pmiops.workbench.cdr.dao.CBCriteriaDao;
import org.pmiops.workbench.cdr.dao.CBDataFilterDao;
import org.pmiops.workbench.cdr.dao.CriteriaMenuDao;
import org.pmiops.workbench.cdr.dao.DomainInfoDao;
import org.pmiops.workbench.cdr.dao.PersonDao;
import org.pmiops.workbench.cdr.dao.SurveyModuleDao;
import org.pmiops.workbench.cdr.model.DbCriteria;
import org.pmiops.workbench.cdr.model.DbCriteriaAttribute;
import org.pmiops.workbench.cohortbuilder.mapper.CohortBuilderMapper;
import org.pmiops.workbench.db.model.DbConceptSetConceptId;
import org.pmiops.workbench.model.AgeType;
import org.pmiops.workbench.model.AgeTypeCount;
import org.pmiops.workbench.model.ConceptIdName;
import org.pmiops.workbench.model.Criteria;
import org.pmiops.workbench.model.CriteriaAttribute;
import org.pmiops.workbench.model.CriteriaListWithCountResponse;
import org.pmiops.workbench.model.CriteriaMenu;
import org.pmiops.workbench.model.DataFilter;
import org.pmiops.workbench.model.DemoChartInfo;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.DomainInfo;
import org.pmiops.workbench.model.FilterColumns;
import org.pmiops.workbench.model.GenderOrSexType;
import org.pmiops.workbench.model.ParticipantDemographics;
import org.pmiops.workbench.model.SearchRequest;
import org.pmiops.workbench.model.SurveyModule;
import org.pmiops.workbench.model.SurveyVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class CohortBuilderServiceImpl implements CohortBuilderService {

  private static final Integer DEFAULT_TREE_SEARCH_LIMIT = 100;
  private static final Integer DEFAULT_CRITERIA_SEARCH_LIMIT = 250;
  private static final ImmutableList<String> MYSQL_FULL_TEXT_CHARS =
      ImmutableList.of("\"", "+", "-", "*", "(", ")");

  private final BigQueryService bigQueryService;
  private final CohortQueryBuilder cohortQueryBuilder;
  private final CBCriteriaAttributeDao cbCriteriaAttributeDao;
  private final CBCriteriaDao cbCriteriaDao;
  private final CriteriaMenuDao criteriaMenuDao;
  private final CBDataFilterDao cbDataFilterDao;
  private final DomainInfoDao domainInfoDao;
  private final PersonDao personDao;
  private final SurveyModuleDao surveyModuleDao;
  private final CohortBuilderMapper cohortBuilderMapper;
  private final Provider<MySQLStopWords> mySQLStopWordsProvider;

  @Autowired
  public CohortBuilderServiceImpl(
      BigQueryService bigQueryService,
      CohortQueryBuilder cohortQueryBuilder,
      CBCriteriaAttributeDao cbCriteriaAttributeDao,
      CBCriteriaDao cbCriteriaDao,
      CriteriaMenuDao criteriaMenuDao,
      CBDataFilterDao cbDataFilterDao,
      DomainInfoDao domainInfoDao,
      PersonDao personDao,
      SurveyModuleDao surveyModuleDao,
      CohortBuilderMapper cohortBuilderMapper,
      Provider<MySQLStopWords> mySQLStopWordsProvider) {
    this.bigQueryService = bigQueryService;
    this.cohortQueryBuilder = cohortQueryBuilder;
    this.cbCriteriaAttributeDao = cbCriteriaAttributeDao;
    this.cbCriteriaDao = cbCriteriaDao;
    this.criteriaMenuDao = criteriaMenuDao;
    this.cbDataFilterDao = cbDataFilterDao;
    this.domainInfoDao = domainInfoDao;
    this.personDao = personDao;
    this.surveyModuleDao = surveyModuleDao;
    this.cohortBuilderMapper = cohortBuilderMapper;
    this.mySQLStopWordsProvider = mySQLStopWordsProvider;
  }

  @Override
  public ConceptIds classifyConceptIds(Set<Long> conceptIds) {
    ImmutableList.Builder<Long> standardConceptIds = ImmutableList.builder();
    ImmutableList.Builder<Long> sourceConceptIds = ImmutableList.builder();
    cbCriteriaDao
        .findByConceptIdIn(conceptIds.stream().map(String::valueOf).collect(Collectors.toList()))
        .forEach(
            c -> {
              if (c.getStandard()) {
                standardConceptIds.add(Long.valueOf(c.getConceptId()));
              } else {
                sourceConceptIds.add(Long.valueOf(c.getConceptId()));
              }
            });
    return new ConceptIds(standardConceptIds.build(), sourceConceptIds.build());
  }

  @Override
  public List<Criteria> findCriteriaByDomainIdAndConceptIds(
      String domainId, Collection<DbConceptSetConceptId> dbConceptSetConceptIds) {
    List<DbCriteria> criteriaList = new ArrayList<>();
    Map<Boolean, List<DbConceptSetConceptId>> partitionSourceAndStandard =
        dbConceptSetConceptIds.stream()
            .collect(Collectors.partitioningBy(DbConceptSetConceptId::getStandard));
    List<DbConceptSetConceptId> standard = partitionSourceAndStandard.get(true);
    List<DbConceptSetConceptId> source = partitionSourceAndStandard.get(false);
    if (!standard.isEmpty()) {
      criteriaList.addAll(
          cbCriteriaDao.findCriteriaByDomainIdAndStandardAndConceptIds(
              domainId,
              true,
              standard.stream()
                  .map(c -> c.getConceptId().toString())
                  .collect(Collectors.toList())));
    }
    if (!source.isEmpty()) {
      criteriaList.addAll(
          cbCriteriaDao.findCriteriaByDomainIdAndStandardAndConceptIds(
              domainId,
              false,
              source.stream().map(c -> c.getConceptId().toString()).collect(Collectors.toList())));
    }
    return criteriaList.stream()
        .map(cohortBuilderMapper::dbModelToClient)
        .sorted(Ordering.from(String.CASE_INSENSITIVE_ORDER).onResultOf(Criteria::getName))
        .collect(Collectors.toList());
  }

  @Override
  public List<Criteria> findCriteriaByDomainIdAndConceptIds(
      String domainId, Collection<Long> sourceConceptIds, Collection<Long> standardConceptIds) {
    List<Criteria> criteriaList = new ArrayList<>();
    List<String> sourceIds =
        sourceConceptIds.stream().map(Object::toString).collect(Collectors.toList());
    List<String> standardIds =
        standardConceptIds.stream().map(Object::toString).collect(Collectors.toList());
    if (!sourceIds.isEmpty()) {
      criteriaList.addAll(
          cbCriteriaDao.findCriteriaByDomainIdAndStandardAndConceptIds(domainId, false, sourceIds)
              .stream()
              .map(cohortBuilderMapper::dbModelToClient)
              .collect(Collectors.toList()));
    }
    if (!standardConceptIds.isEmpty()) {
      criteriaList.addAll(
          cbCriteriaDao.findCriteriaByDomainIdAndStandardAndConceptIds(domainId, true, standardIds)
              .stream()
              .map(cohortBuilderMapper::dbModelToClient)
              .collect(Collectors.toList()));
    }
    return criteriaList;
  }

  @Override
  public Long countParticipants(SearchRequest request) {
    QueryJobConfiguration qjc =
        bigQueryService.filterBigQueryConfig(
            cohortQueryBuilder.buildParticipantCounterQuery(new ParticipantCriteria(request)));
    TableResult result = bigQueryService.executeQuery(qjc);
    Map<String, Integer> rm = bigQueryService.getResultMapper(result);
    List<FieldValue> row = result.iterateAll().iterator().next();
    return bigQueryService.getLong(row, rm.get("count"));
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
        PageRequest.of(0, Optional.ofNullable(limit).orElse(DEFAULT_TREE_SEARCH_LIMIT));
    List<DbCriteria> criteriaList =
        cbCriteriaDao.findCriteriaByDomainAndTypeAndStandardAndFullText(
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
  public CriteriaListWithCountResponse findCriteriaByDomainAndSearchTerm(
      String domain, String term, String surveyName, Integer limit) {
    PageRequest pageRequest =
        PageRequest.of(0, Optional.ofNullable(limit).orElse(DEFAULT_CRITERIA_SEARCH_LIMIT));
    if (term == null || term.trim().isEmpty()) {
      if (Domain.SURVEY.equals(Domain.fromValue(domain))) {
        Long id = cbCriteriaDao.findIdByDomainAndName(domain, surveyName);
        Page<DbCriteria> dbCriteriaPage =
            cbCriteriaDao.findSurveyQuestionCriteriaByDomainAndIdAndFullText(
                domain, id, pageRequest);
        return new CriteriaListWithCountResponse()
            .items(
                dbCriteriaPage.getContent().stream()
                    .map(cohortBuilderMapper::dbModelToClient)
                    .collect(Collectors.toList()))
            .totalCount(dbCriteriaPage.getTotalElements());
      }
      Page<DbCriteria> dbCriteriaPage = cbCriteriaDao.findCriteriaTopCounts(domain, pageRequest);
      return new CriteriaListWithCountResponse()
          .items(
              dbCriteriaPage.getContent().stream()
                  .map(cohortBuilderMapper::dbModelToClient)
                  .collect(Collectors.toList()))
          .totalCount(dbCriteriaPage.getTotalElements());
    }

    String modifiedSearchTerm = modifyTermMatch(term);
    if (modifiedSearchTerm.isEmpty()) {
      return new CriteriaListWithCountResponse().totalCount(0L);
    }
    if (Domain.SURVEY.equals(Domain.fromValue(domain))) {
      Page<DbCriteria> dbCriteriaPage;
      if (surveyName.equals("All")) {
        dbCriteriaPage =
            cbCriteriaDao.findSurveyQuestionCriteriaByDomainAndFullText(
                domain, modifiedSearchTerm, pageRequest);
      } else {
        Long id = cbCriteriaDao.findIdByDomainAndName(domain, surveyName);
        dbCriteriaPage =
            cbCriteriaDao.findSurveyQuestionCriteriaByDomainAndIdAndFullText(
                domain, id, modifiedSearchTerm, pageRequest);
      }
      return new CriteriaListWithCountResponse()
          .items(
              dbCriteriaPage.getContent().stream()
                  .map(cohortBuilderMapper::dbModelToClient)
                  .collect(Collectors.toList()))
          .totalCount(dbCriteriaPage.getTotalElements());
    }

    Page<DbCriteria> dbCriteriaPage =
        cbCriteriaDao.findCriteriaByDomainAndTypeAndCode(
            domain, term.replaceAll("[()+\"*-]", ""), pageRequest);
    if (dbCriteriaPage.getContent().isEmpty() && !term.contains(".")) {
      dbCriteriaPage =
          cbCriteriaDao.findCriteriaByDomainAndFullText(domain, modifiedSearchTerm, pageRequest);
    }
    return new CriteriaListWithCountResponse()
        .items(
            dbCriteriaPage.getContent().stream()
                .map(cohortBuilderMapper::dbModelToClient)
                .collect(Collectors.toList()))
        .totalCount(dbCriteriaPage.getTotalElements());
  }

  @Override
  public List<CriteriaMenu> findCriteriaMenuByParentId(long parentId) {
    return criteriaMenuDao.findByParentIdOrderBySortOrderAsc(parentId).stream()
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
  public List<DemoChartInfo> findDemoChartInfo(
      GenderOrSexType genderOrSexType, AgeType ageType, SearchRequest request) {
    QueryJobConfiguration qjc =
        bigQueryService.filterBigQueryConfig(
            cohortQueryBuilder.buildDemoChartInfoCounterQuery(
                new ParticipantCriteria(request, genderOrSexType, ageType)));
    TableResult result = bigQueryService.executeQuery(qjc);
    Map<String, Integer> rm = bigQueryService.getResultMapper(result);

    List<DemoChartInfo> demoChartInfos = new ArrayList<>();
    for (List<FieldValue> row : result.iterateAll()) {
      demoChartInfos.add(
          new DemoChartInfo()
              .name(bigQueryService.getString(row, rm.get("name")))
              .race(bigQueryService.getString(row, rm.get("race")))
              .ageRange(bigQueryService.getString(row, rm.get("ageRange")))
              .count(bigQueryService.getLong(row, rm.get("count"))));
    }
    return demoChartInfos;
  }

  @Override
  public Long findDomainCount(String domain, String term) {
    Domain domainToCount = Domain.valueOf(domain);
    if (domainToCount.equals(Domain.PHYSICAL_MEASUREMENT)) {
      return cbCriteriaDao.findPhysicalMeasurementCount(modifyTermMatch(term));
    }
    Long count = cbCriteriaDao.findDomainCountOnCode(term, domain);
    return count == 0 ? cbCriteriaDao.findDomainCount(modifyTermMatch(term), domain) : count;
  }

  @Override
  public List<DomainInfo> findDomainInfos() {
    return domainInfoDao.findByOrderByDomainId().stream()
        .filter(dbDomainInfo -> dbDomainInfo.getAllConceptCount() > 0)
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
    List<DbCriteria> criteriaList = cbCriteriaDao.findAllDemographics();
    return new ParticipantDemographics()
        .genderList(buildConceptIdNameList(criteriaList, GENDER))
        .raceList(buildConceptIdNameList(criteriaList, RACE))
        .ethnicityList(buildConceptIdNameList(criteriaList, ETHNICITY))
        .sexAtBirthList(buildConceptIdNameList(criteriaList, SEXATBIRTH));
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

  @Override
  public Map<Long, String> findAllDemographicsMap() {
    return cbCriteriaDao.findAllDemographics().stream()
        .collect(
            Collectors.toMap(
                DbCriteria::getLongConceptId,
                DbCriteria::getName,
                (oldValue, newValue) -> oldValue));
  }

  @Override
  public List<String> findSortedConceptIdsByDomainIdAndType(
      String domainId, String sortColumn, String sortName) {
    Sort sort =
        sortName.equalsIgnoreCase(Sort.Direction.ASC.toString())
            ? Sort.by(Sort.Direction.ASC, "name")
            : Sort.by(Sort.Direction.DESC, "name");
    List<DbCriteria> criteriaList =
        cbCriteriaDao.findByDomainIdAndType(Domain.PERSON.toString(), sortColumn, sort);
    return criteriaList.stream()
        .map(
            c -> new ConceptIdName().conceptId(new Long(c.getConceptId())).conceptName(c.getName()))
        .sorted(Comparator.comparing(ConceptIdName::getConceptName))
        .map(c -> c.getConceptId().toString())
        .collect(Collectors.toList());
  }

  @Override
  public Long findSurveyCount(String name, String term) {
    return cbCriteriaDao.findSurveyCount(name, modifyTermMatch(term));
  }

  @Override
  public List<SurveyModule> findSurveyModules() {
    return surveyModuleDao.findByOrderByOrderNumberAsc().stream()
        .map(cohortBuilderMapper::dbModelToClient)
        .collect(Collectors.toList());
  }

  @Override
  public List<SurveyVersion> findSurveyVersionByQuestionConceptId(
      Long surveyConceptId, Long questionConceptId) {
    return findSurveyVersionByQuestionConceptIdAndAnswerConceptId(
        surveyConceptId, questionConceptId, 0L);
  }

  @Override
  public List<SurveyVersion> findSurveyVersionByQuestionConceptIdAndAnswerConceptId(
      Long surveyConceptId, Long questionConceptId, Long answerConceptId) {
    return cbCriteriaDao
        .findSurveyVersionByQuestionConceptIdAndAnswerConceptId(
            surveyConceptId, questionConceptId, answerConceptId)
        .stream()
        .map(cohortBuilderMapper::dbModelToClient)
        .collect(Collectors.toList());
  }

  private String modifyTermMatch(String term) {
    term = removeStopWords(term);
    if (MYSQL_FULL_TEXT_CHARS.stream().anyMatch(term::contains)) {
      return Arrays.stream(term.split("\\s+"))
          .map(
              s -> {
                if (s.startsWith("(")
                    || (!s.startsWith("+") && !s.startsWith("-") && !s.endsWith(")"))) {
                  return "+" + s;
                }
                return s;
              })
          .collect(Collectors.joining(" "));
    }

    String[] keywords = term.split("\\W+");

    return IntStream.range(0, keywords.length)
        .filter(i -> keywords[i].length() >= 2)
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
  private String removeStopWords(String term) {
    List<String> stopWords = mySQLStopWordsProvider.get().getStopWords();
    term =
        Arrays.stream(term.split("\\s+"))
            .filter(w -> !stopWords.contains(w))
            .collect(Collectors.joining(" "));
    return term;
  }

  @NotNull
  private List<ConceptIdName> buildConceptIdNameList(
      List<DbCriteria> criteriaList, FilterColumns columnName) {
    return criteriaList.stream()
        .filter(c -> columnName.toString().startsWith(c.getType()))
        .map(
            c -> new ConceptIdName().conceptId(new Long(c.getConceptId())).conceptName(c.getName()))
        .collect(Collectors.toList());
  }
}
