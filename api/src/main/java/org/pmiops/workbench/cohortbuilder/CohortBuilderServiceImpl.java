package org.pmiops.workbench.cohortbuilder;

import static org.pmiops.workbench.model.FilterColumns.ETHNICITY;
import static org.pmiops.workbench.model.FilterColumns.GENDER;
import static org.pmiops.workbench.model.FilterColumns.RACE;
import static org.pmiops.workbench.model.FilterColumns.SEXATBIRTH;

import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableResult;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;
import com.google.common.collect.Table;
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
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.inject.Provider;
import org.jetbrains.annotations.NotNull;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.cdr.cache.MySQLStopWords;
import org.pmiops.workbench.cdr.dao.CBCriteriaAttributeDao;
import org.pmiops.workbench.cdr.dao.CBCriteriaDao;
import org.pmiops.workbench.cdr.dao.CBDataFilterDao;
import org.pmiops.workbench.cdr.dao.CriteriaMenuDao;
import org.pmiops.workbench.cdr.dao.DomainCardDao;
import org.pmiops.workbench.cdr.dao.PersonDao;
import org.pmiops.workbench.cdr.dao.SurveyModuleDao;
import org.pmiops.workbench.cdr.model.DbCardCount;
import org.pmiops.workbench.cdr.model.DbCriteria;
import org.pmiops.workbench.cdr.model.DbCriteriaAttribute;
import org.pmiops.workbench.cohortbuilder.mapper.CohortBuilderMapper;
import org.pmiops.workbench.db.model.DbConceptSetConceptId;
import org.pmiops.workbench.model.AgeType;
import org.pmiops.workbench.model.AgeTypeCount;
import org.pmiops.workbench.model.CardCount;
import org.pmiops.workbench.model.ConceptIdName;
import org.pmiops.workbench.model.Criteria;
import org.pmiops.workbench.model.CriteriaAttribute;
import org.pmiops.workbench.model.CriteriaListWithCountResponse;
import org.pmiops.workbench.model.CriteriaMenu;
import org.pmiops.workbench.model.CriteriaType;
import org.pmiops.workbench.model.DataFilter;
import org.pmiops.workbench.model.DemoChartInfo;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.DomainCard;
import org.pmiops.workbench.model.EthnicityInfo;
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
      ImmutableList.of("\"", "+", "-", "*");
  private static final List<String> CONDITION_PROCEDURE_SOURCE_DOMAINS =
      Stream.of(
              Domain.CONDITION.toString(),
              Domain.PROCEDURE.toString(),
              Domain.OBSERVATION.toString(),
              Domain.MEASUREMENT.toString(),
              Domain.DRUG.toString())
          .collect(Collectors.toList());
  private static final StringBuilder SOURCE_FULL_TEXT =
      new StringBuilder()
          .append(String.format("[%s_rank1]", Domain.CONDITION.toString().toLowerCase()))
          .append(String.format("[%s_rank1]", Domain.PROCEDURE.toString().toLowerCase()))
          .append(String.format("[%s_rank1]", Domain.OBSERVATION.toString().toLowerCase()))
          .append(String.format("[%s_rank1]", Domain.MEASUREMENT.toString().toLowerCase()))
          .append(String.format("[%s_rank1]", Domain.DRUG.toString().toLowerCase()));

  private final BigQueryService bigQueryService;
  private final CohortQueryBuilder cohortQueryBuilder;
  private final CBCriteriaAttributeDao cbCriteriaAttributeDao;
  private final CBCriteriaDao cbCriteriaDao;
  private final CriteriaMenuDao criteriaMenuDao;
  private final CBDataFilterDao cbDataFilterDao;
  private final DomainCardDao domainCardDao;
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
      DomainCardDao domainCardDao,
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
    this.domainCardDao = domainCardDao;
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
          cbCriteriaDao
              .findCriteriaByDomainIdAndStandardAndConceptIds(domainId, false, sourceIds)
              .stream()
              .map(cohortBuilderMapper::dbModelToClient)
              .collect(Collectors.toList()));
    }
    if (!standardConceptIds.isEmpty()) {
      criteriaList.addAll(
          cbCriteriaDao
              .findCriteriaByDomainIdAndStandardAndConceptIds(domainId, true, standardIds)
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
      // Not everything in the condition source hierarchy(ICD9CM/ICD10CM) is a condition
      // Or in the procedure source hierarchy(ICD9Proc/ICDPCS/CPT4) is a procedure
      // Please see - https://precisionmedicineinitiative.atlassian.net/browse/RW-7658
      List<String> domains =
          isConditionProcedureSourceHierarchy(domain, standard)
              ? CONDITION_PROCEDURE_SOURCE_DOMAINS
              : Stream.of(domain).collect(Collectors.toList());
      criteriaList =
          cbCriteriaDao.findCriteriaByDomainIdAndTypeAndParentIdOrderByIdAsc(
              domains, type, standard, parentId);
    } else {
      // read the visits, PM, Race, Ethnicity, Gender, Sex at Birth hierarchies
      criteriaList = cbCriteriaDao.findCriteriaByDomainAndTypeOrderByIdAsc(domain, type);
    }
    return criteriaList.stream()
        .map(cohortBuilderMapper::dbModelToClient)
        .collect(Collectors.toList());
  }

  @Override
  public CriteriaListWithCountResponse findCriteriaByDomain(
      String domain, String term, String surveyName, Boolean standard, Integer limit) {
    PageRequest pageRequest =
        PageRequest.of(0, Optional.ofNullable(limit).orElse(DEFAULT_CRITERIA_SEARCH_LIMIT));

    // if search term is empty find the top counts for the domain
    if (isTopCountsSearch(term)) {
      return getTopCountsSearchWithStandard(domain, surveyName, standard, pageRequest);
    }

    String modifiedSearchTerm = modifyTermMatch(term);
    // if the modified search term is empty return an empty result
    if (modifiedSearchTerm.isEmpty()) {
      return new CriteriaListWithCountResponse().totalCount(0L);
    }

    // if domain type is survey then search survey by term.
    if (isSurveyDomain(domain)) {
      return findSurveyCriteriaBySearchTerm(surveyName, pageRequest, modifiedSearchTerm);
    }

    // find a match on concept code
    Page<DbCriteria> dbCriteriaPage =
        cbCriteriaDao.findCriteriaByDomainAndTypeAndCodeAndStandard(
            domain, term.replaceAll("[()+\"*-]", ""), standard, pageRequest);

    // if no match is found on concept code then find match on full text index by term
    if (dbCriteriaPage.getContent().isEmpty() && !term.contains(".")) {
      dbCriteriaPage =
          cbCriteriaDao.findCriteriaByDomainAndFullTextAndStandard(
              domain, modifiedSearchTerm, standard, pageRequest);
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
    return criteriaMenuDao.findByParentIdOrderByIdAscSortOrderAsc(parentId).stream()
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
  public List<EthnicityInfo> findEthnicityInfo(SearchRequest request) {
    QueryJobConfiguration qjc =
        bigQueryService.filterBigQueryConfig(
            cohortQueryBuilder.buildEthnicityInfoCounterQuery(new ParticipantCriteria(request)));
    TableResult result = bigQueryService.executeQuery(qjc);
    Map<String, Integer> rm = bigQueryService.getResultMapper(result);

    List<EthnicityInfo> ethnicityInfos = new ArrayList<>();
    for (List<FieldValue> row : result.iterateAll()) {
      ethnicityInfos.add(
          new EthnicityInfo()
              .ethnicity(bigQueryService.getString(row, rm.get("ethnicity")))
              .count(bigQueryService.getLong(row, rm.get("count"))));
    }
    return ethnicityInfos;
  }

  @Override
  public List<CardCount> findUniversalDomainCounts(String term) {
    List<CardCount> cardCounts =
        findDomainCounts(
            term,
            true,
            ImmutableList.of(
                Domain.CONDITION,
                Domain.DRUG,
                Domain.MEASUREMENT,
                Domain.OBSERVATION,
                Domain.PROCEDURE,
                Domain.DEVICE,
                Domain.VISIT));
    cardCounts.addAll(findDomainCounts(term, false, ImmutableList.of(Domain.PHYSICAL_MEASUREMENT)));
    Long sum = findSurveyCounts(term).stream().map(CardCount::getCount).reduce(0L, Long::sum);
    if (sum > 0) {
      cardCounts.add(new CardCount().domain(Domain.SURVEY).name("Survey").count(sum));
    }
    return cardCounts;
  }

  @Override
  public List<CardCount> findDomainCounts(String term) {
    List<CardCount> cardCounts =
        findDomainCounts(
            term,
            true,
            ImmutableList.of(
                Domain.CONDITION,
                Domain.DRUG,
                Domain.MEASUREMENT,
                Domain.OBSERVATION,
                Domain.PROCEDURE,
                Domain.DEVICE));
    cardCounts.addAll(
        findDomainCounts(term, false, ImmutableList.of(Domain.PHYSICAL_MEASUREMENT_CSS)));
    cardCounts.addAll(findSurveyCounts(term));
    return cardCounts;
  }

  private List<CardCount> findDomainCounts(String term, Boolean standard, List<Domain> domains) {
    List<String> strDomains =
        domains.stream().map(d1 -> d1.toString()).collect(Collectors.toList());
    List<DbCardCount> cardCounts =
        cbCriteriaDao.findDomainCountsByCode(term, standard, strDomains).stream()
            .filter(cardCount -> cardCount.getCount() > 0)
            .collect(Collectors.toList());
    // filter strDomains to remove domains that have a cardCount by domain
    strDomains.removeAll(
        cardCounts.stream().map(c -> c.getDomainId()).collect(Collectors.toList()));
    // modify search term and call
    cardCounts.addAll(cbCriteriaDao.findDomainCounts(modifyTermMatch(term), standard, strDomains));

    return cardCounts.stream()
        .filter(cardCount -> cardCount.getCount() > 0)
        .map(cohortBuilderMapper::dbModelToClient)
        .collect(Collectors.toList());
  }

  private List<CardCount> findSurveyCounts(String term) {
    return cbCriteriaDao.findSurveyCounts(modifyTermMatch(term)).stream()
        .filter(cardCount -> cardCount.getCount() > 0)
        .map(cohortBuilderMapper::dbModelToClient)
        .collect(Collectors.toList());
  }

  @Override
  public List<DomainCard> findDomainCards() {
    return domainCardDao.findByOrderById().stream()
        .filter(dbDomainCard -> dbDomainCard.getConceptCount() > 0)
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
      // Not everything in the condition source hierarchy(ICD9CM/ICD10CM) is a condition
      // Or in the procedure source hierarchy(ICD9Proc/ICDPCS/CPT4) is a procedure
      // Please see - https://precisionmedicineinitiative.atlassian.net/browse/RW-7658
      domain =
          isConditionProcedureSourceDomain(domain)
              ? SOURCE_FULL_TEXT.toString()
              : String.format("[%s_rank1]", domain.toLowerCase());
      criteriaList =
          cbCriteriaDao.findStandardCriteriaByDomainAndConceptId(domain, true, conceptIds);
    }
    return criteriaList.stream()
        .map(cohortBuilderMapper::dbModelToClient)
        .collect(Collectors.toList());
  }

  @Override
  public synchronized Table<Long, CriteriaType, String> findAllDemographicsMap() {
    Table<Long, CriteriaType, String> demoTable = HashBasedTable.create();
    for (DbCriteria dbCriteria : cbCriteriaDao.findAllDemographics()) {
      demoTable.put(
          dbCriteria.getLongConceptId(),
          CriteriaType.valueOf(dbCriteria.getType()),
          dbCriteria.getName());
    }
    return demoTable;
  }

  @Override
  public List<String> findSortedConceptIdsByDomainIdAndType(
      String domainId, String sortColumn, String sortName) {
    Sort sort =
        sortName.equalsIgnoreCase(Sort.Direction.ASC.toString())
            ? Sort.by(Sort.Direction.ASC, "name")
            : Sort.by(Sort.Direction.DESC, "name");
    List<DbCriteria> criteriaList = cbCriteriaDao.findByDomainIdAndType(domainId, sortColumn, sort);
    return criteriaList.stream()
        .map(
            c -> new ConceptIdName().conceptId(new Long(c.getConceptId())).conceptName(c.getName()))
        .sorted(Comparator.comparing(ConceptIdName::getConceptName))
        .map(c -> c.getConceptId().toString())
        .collect(Collectors.toList());
  }

  @Override
  public List<SurveyModule> findSurveyModules() {
    return surveyModuleDao.findByOrderByOrderNumberAsc().stream()
        .map(cohortBuilderMapper::dbModelToClient)
        .collect(Collectors.toList());
  }

  @Override
  public List<SurveyVersion> findSurveyVersionByQuestionConceptId(Long questionConceptId) {
    return findSurveyVersionByQuestionConceptIdAndAnswerConceptId(questionConceptId, 0L);
  }

  @Override
  public List<SurveyVersion> findSurveyVersionByQuestionConceptIdAndAnswerConceptId(
      Long questionConceptId, Long answerConceptId) {
    return cbCriteriaDao
        .findSurveyVersionByQuestionConceptIdAndAnswerConceptId(questionConceptId, answerConceptId)
        .stream()
        .map(cohortBuilderMapper::dbModelToClient)
        .collect(Collectors.toList());
  }

  @Override
  public List<Criteria> findVersionedSurveys() {
    return cbCriteriaDao.findVersionedSurveys().stream()
        .map(cohortBuilderMapper::dbModelToClient)
        .collect(Collectors.toList());
  }

  private CriteriaListWithCountResponse getTopCountsSearchWithStandard(
      String domain, String surveyName, Boolean standard, PageRequest pageRequest) {
    Page<DbCriteria> dbCriteriaPage;
    if (isSurveyDomain(domain)) {
      Long id = cbCriteriaDao.findSurveyId(surveyName);
      dbCriteriaPage = cbCriteriaDao.findSurveyQuestionByPath(id, pageRequest);
    } else {
      dbCriteriaPage = cbCriteriaDao.findCriteriaTopCountsByStandard(domain, standard, pageRequest);
    }
    return new CriteriaListWithCountResponse()
        .items(
            dbCriteriaPage.getContent().stream()
                .map(cohortBuilderMapper::dbModelToClient)
                .collect(Collectors.toList()))
        .totalCount(dbCriteriaPage.getTotalElements());
  }

  private CriteriaListWithCountResponse findSurveyCriteriaBySearchTerm(
      String surveyName, PageRequest pageRequest, String modifiedSearchTerm) {
    Page<DbCriteria> dbCriteriaPage;
    if (surveyName.equals("All")) {
      dbCriteriaPage = cbCriteriaDao.findSurveyQuestionByTerm(modifiedSearchTerm, pageRequest);
    } else {
      Long id = cbCriteriaDao.findSurveyId(surveyName);
      dbCriteriaPage =
          cbCriteriaDao.findSurveyQuestionByPathAndTerm(id, modifiedSearchTerm, pageRequest);
    }
    return new CriteriaListWithCountResponse()
        .items(
            dbCriteriaPage.getContent().stream()
                .map(cohortBuilderMapper::dbModelToClient)
                .collect(Collectors.toList()))
        .totalCount(dbCriteriaPage.getTotalElements());
  }

  private boolean isTopCountsSearch(String term) {
    return term == null || term.trim().isEmpty();
  }

  private boolean isSurveyDomain(String domain) {
    return Domain.SURVEY.equals(Domain.fromValue(domain));
  }

  protected String modifyTermMatch(String term) {
    term = removeStopWords(term);
    if (MYSQL_FULL_TEXT_CHARS.stream().anyMatch(term::contains)) {
      // doesn't start with special char so find exact match
      if (term.matches("^[a-zA-Z0-9].*")) {
        return "+\"" + term + "\"";
      }
      // starts with a special char so don't mutate.
      return term;
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

  private boolean isConditionProcedureSourceHierarchy(String domain, Boolean standard) {
    return (isConditionProcedureSourceDomain(domain)) && !standard;
  }

  private boolean isConditionProcedureSourceDomain(String domain) {
    return Domain.CONDITION.equals(Domain.valueOf(domain))
        || Domain.PROCEDURE.equals(Domain.valueOf(domain));
  }
}
