package org.pmiops.workbench.cohortbuilder;

import static org.pmiops.workbench.model.FilterColumns.ETHNICITY;
import static org.pmiops.workbench.model.FilterColumns.GENDER;
import static org.pmiops.workbench.model.FilterColumns.RACE;
import static org.pmiops.workbench.model.FilterColumns.SEXATBIRTH;

import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.TableResult;
import com.google.common.base.Strings;
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
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.inject.Provider;
import org.apache.commons.lang3.tuple.ImmutableTriple;
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
import org.pmiops.workbench.model.AgeTypeCount;
import org.pmiops.workbench.model.CardCount;
import org.pmiops.workbench.model.CohortDefinition;
import org.pmiops.workbench.model.ConceptIdName;
import org.pmiops.workbench.model.Criteria;
import org.pmiops.workbench.model.CriteriaAttribute;
import org.pmiops.workbench.model.CriteriaListWithCountResponse;
import org.pmiops.workbench.model.CriteriaMenu;
import org.pmiops.workbench.model.CriteriaSearchRequest;
import org.pmiops.workbench.model.CriteriaType;
import org.pmiops.workbench.model.DataFilter;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.DomainCard;
import org.pmiops.workbench.model.FilterColumns;
import org.pmiops.workbench.model.ParticipantDemographics;
import org.pmiops.workbench.model.SurveyModule;
import org.pmiops.workbench.model.SurveyVersion;
import org.pmiops.workbench.model.Variant;
import org.pmiops.workbench.model.VariantFilterRequest;
import org.pmiops.workbench.utils.FieldValues;
import org.pmiops.workbench.utils.PaginationToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class CohortBuilderServiceImpl implements CohortBuilderService {

  private static final Integer DEFAULT_TREE_SEARCH_LIMIT = 100;
  private static final Integer DEFAULT_CRITERIA_SEARCH_LIMIT = 250;
  private static final int DEFAULT_VARIANT_PAGE_SIZE = 25;
  private static final int MAX_VARIANT_PAGE_SIZE = 100;
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
  public Long countParticipants(CohortDefinition cohortDefinition) {
    TableResult result =
        bigQueryService.filterBigQueryConfigAndExecuteQuery(
            cohortQueryBuilder.buildParticipantCounterQuery(
                new ParticipantCriteria(cohortDefinition)));
    FieldValueList row = result.iterateAll().iterator().next();
    return row.get("count").getLongValue();
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
  public List<Criteria> findCriteriaAutoComplete(CriteriaSearchRequest criteriaSearchRequest) {
    PageRequest pageRequest = PageRequest.of(0, DEFAULT_TREE_SEARCH_LIMIT);

    SearchTerm searchTerm =
        new SearchTerm(
            criteriaSearchRequest.getTerm(), mySQLStopWordsProvider.get().getStopWords());

    if (Domain.SURVEY.equals(Domain.fromValue(criteriaSearchRequest.getDomain()))) {
      return findSurveyCriteriaAutoComplete(
          criteriaSearchRequest.getSurveyName(), searchTerm, pageRequest);

    } else {
      return findDomainCriteriaAutoComplete(criteriaSearchRequest, searchTerm, pageRequest);
    }
  }

  private List<Criteria> findDomainCriteriaAutoComplete(
      CriteriaSearchRequest criteriaSearchRequest, SearchTerm searchTerm, PageRequest pageRequest) {
    List<DbCriteria> criteriaList =
        cbCriteriaDao.findCriteriaAutoComplete(
            criteriaSearchRequest.getDomain(),
            ImmutableList.of(criteriaSearchRequest.getType()),
            criteriaSearchRequest.getStandard(),
            ImmutableList.of(true),
            searchTerm,
            pageRequest);

    // find by code if auto complete return nothing.
    if (criteriaList.isEmpty()) {
      criteriaList =
          cbCriteriaDao.findCriteriaByDomainAndTypeAndStandardAndCode(
              criteriaSearchRequest.getDomain(),
              ImmutableList.of(criteriaSearchRequest.getType()),
              criteriaSearchRequest.getStandard(),
              ImmutableList.of(true),
              searchTerm.getCodeTerm(),
              pageRequest);
    }
    return criteriaList.stream()
        .map(cohortBuilderMapper::dbModelToClient)
        .collect(Collectors.toList());
  }

  private List<Criteria> findSurveyCriteriaAutoComplete(
      String surveyName, SearchTerm searchTerm, PageRequest pageRequest) {

    Page<DbCriteria> dbCriteriaPage =
        cbCriteriaDao.findSurveyQuestions(surveyName, searchTerm, pageRequest);

    if (dbCriteriaPage == null) {
      return ImmutableList.of();
    }
    return dbCriteriaPage.getContent().stream()
        .map(cohortBuilderMapper::dbModelToClient)
        .collect(Collectors.toList());
  }

  @Override
  public List<Criteria> findCriteriaBy(
      String domain, String type, Boolean standard, Long parentId) {
    List<DbCriteria> criteriaList;
    if (parentId != null) {
      // Not everything in the condition source hierarchy(ICD9CM/ICD10CM) is a condition
      // Or in the procedure source hierarchy(ICD9Proc/ICD10PCS/CPT4) is a procedure
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
  public CriteriaListWithCountResponse findCriteriaByDomain(CriteriaSearchRequest request) {
    boolean removeDrugBrand = request.getRemoveDrugBrand();
    PageRequest pageRequest = PageRequest.of(0, DEFAULT_CRITERIA_SEARCH_LIMIT);

    // if search term is empty find the top counts for the domain
    if (isTopCountsSearch(request.getTerm())) {
      return getTopCountsSearchWithStandard(request, pageRequest);
    }

    SearchTerm searchTerm =
        new SearchTerm(request.getTerm(), mySQLStopWordsProvider.get().getStopWords());

    // check survey domain before checking for match on concept code
    if (isSurveyDomain(request.getDomain())) {
      return findSurveyCriteriaBySearchTermV2(request.getSurveyName(), searchTerm, pageRequest);
    }

    // if we need to remove brand names(only applies to drug) use brand type otherwise use none
    // for  other domains
    String type = removeDrugBrand ? CriteriaType.BRAND.toString() : CriteriaType.NONE.toString();

    // find a match on concept code
    Page<DbCriteria> dbCriteriaPage =
        cbCriteriaDao.findCriteriaByDomainAndCodeAndStandardAndNotType(
            request.getDomain(),
            searchTerm.getCodeTerm(),
            request.getStandard(),
            type,
            pageRequest);

    // if the modified search term is empty and endsWithTerms is empty return an empty result
    // this needs ot be here since word length of <3 are filtered and there are 2-concept codes
    // of length 2 with rank1 - for procedures - type=(ICD9Proc abd ICD10PCS)
    if (searchTerm.hasNoTerms() && dbCriteriaPage.getContent().isEmpty()) {
      return new CriteriaListWithCountResponse().totalCount(0L);
    }

    // if no match is found on concept code then find match on full text index by term
    if (dbCriteriaPage.getContent().isEmpty() && !request.getTerm().contains(".")) {
      dbCriteriaPage =
          cbCriteriaDao.findCriteriaByDomain(
              request.getDomain(), searchTerm, request.getStandard(), type, pageRequest);
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
  public List<CardCount> findUniversalDomainCounts(String term) {
    SearchTerm searchTerm = new SearchTerm(term, mySQLStopWordsProvider.get().getStopWords());

    List<CardCount> cardCounts =
        findDomainCountsV2(
            searchTerm,
            true,
            ImmutableList.of(
                Domain.CONDITION,
                Domain.DRUG,
                Domain.MEASUREMENT,
                Domain.OBSERVATION,
                Domain.PROCEDURE,
                Domain.DEVICE,
                Domain.VISIT));
    cardCounts.addAll(
        findDomainCountsV2(searchTerm, false, ImmutableList.of(Domain.PHYSICAL_MEASUREMENT)));
    Long sum =
        findSurveyCountsV2(searchTerm).stream().map(CardCount::getCount).reduce(0L, Long::sum);
    if (sum > 0) {
      cardCounts.add(new CardCount().domain(Domain.SURVEY).name("Survey").count(sum));
    }
    return cardCounts;
  }

  @Override
  public List<CardCount> findDomainCounts(String term) {
    SearchTerm searchTerm = new SearchTerm(term, mySQLStopWordsProvider.get().getStopWords());

    List<CardCount> cardCounts =
        findDomainCountsV2(
            searchTerm,
            true,
            ImmutableList.of(
                Domain.CONDITION,
                Domain.DRUG,
                Domain.MEASUREMENT,
                Domain.OBSERVATION,
                Domain.PROCEDURE,
                Domain.DEVICE));
    cardCounts.addAll(
        findDomainCountsV2(searchTerm, false, ImmutableList.of(Domain.PHYSICAL_MEASUREMENT_CSS)));
    cardCounts.addAll(findSurveyCountsV2(searchTerm));
    return cardCounts;
  }

  private List<CardCount> findDomainCountsV2(
      SearchTerm searchTerm, Boolean standard, List<Domain> domains) {
    List<String> domainNames = domains.stream().map(Domain::toString).collect(Collectors.toList());
    List<DbCardCount> cardCounts =
        cbCriteriaDao
            .findDomainCountsByCode(searchTerm.getCodeTerm(), standard, domainNames)
            .stream()
            .filter(cardCount -> cardCount.getCount() > 0)
            .collect(Collectors.toList());

    // filter strDomains to remove domains that have a cardCount by domain
    domainNames.removeAll(
        cardCounts.stream().map(DbCardCount::getDomainId).collect(Collectors.toList()));

    cardCounts.addAll(cbCriteriaDao.findDomainCounts(searchTerm, standard, domainNames));

    return cardCounts.stream()
        .filter(cardCount -> cardCount.getCount() > 0)
        .map(cohortBuilderMapper::dbModelToClient)
        .collect(Collectors.toList());
  }

  private List<CardCount> findSurveyCountsV2(SearchTerm searchTerm) {
    return cbCriteriaDao.findSurveyCounts(searchTerm).stream()
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
  public List<Criteria> findDrugBrandOrIngredientByValue(String term, Integer limit) {
    PageRequest pageRequest =
        PageRequest.of(0, Optional.ofNullable(limit).orElse(DEFAULT_TREE_SEARCH_LIMIT));

    SearchTerm searchTerm = new SearchTerm(term, mySQLStopWordsProvider.get().getStopWords());

    String domain = Domain.DRUG.toString();
    List<String> types =
        ImmutableList.of(
            CriteriaType.ATC.toString(),
            CriteriaType.BRAND.toString(),
            CriteriaType.RXNORM.toString());
    Boolean standard = true;
    List<Boolean> hierarchies = ImmutableList.of(true, false);
    List<DbCriteria> criteriaList =
        cbCriteriaDao.findCriteriaAutoComplete(
            domain, types, standard, hierarchies, searchTerm, pageRequest);

    // find by code if auto complete return nothing.
    if (criteriaList.isEmpty()) {
      criteriaList =
          cbCriteriaDao.findCriteriaByDomainAndTypeAndStandardAndCode(
              domain, types, standard, hierarchies, searchTerm.getCodeTerm(), pageRequest);
    }
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
    // These look-ups can be done as one dao call but to make this code testable with the mysql
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
      // Or in the procedure source hierarchy(ICD9Proc/ICD10PCS/CPT4) is a procedure
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
            c ->
                new ConceptIdName()
                    .conceptId(Long.valueOf(c.getConceptId()))
                    .conceptName(c.getName()))
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

  @Override
  public List<Long> findSurveyQuestionIds(List<Long> surveyConceptIds) {
    return cbCriteriaDao.findSurveyQuestionIds(surveyConceptIds);
  }

  @Override
  public ImmutableTriple<String, Integer, List<Variant>> findVariants(
      VariantFilterRequest filters) {
    Integer pageSize = filters.getPageSize();
    String pageToken = filters.getPageToken();
    TableResult result =
        bigQueryService.filterBigQueryConfigAndExecuteQuery(
            VariantQueryBuilder.buildCountQuery(filters));
    FieldValueList row = result.iterateAll().iterator().next();
    int count = Integer.parseInt(row.get("count").getStringValue());

    int limit = DEFAULT_VARIANT_PAGE_SIZE;
    if (pageSize != null && pageSize > 0) {
      limit = Math.min(pageSize, MAX_VARIANT_PAGE_SIZE);
    }

    int offset = 0;
    if (!Strings.isNullOrEmpty(pageToken)) {
      PaginationToken token = PaginationToken.fromBase64(pageToken);
      offset = (int) token.getOffset();
    }

    result =
        bigQueryService.filterBigQueryConfigAndExecuteQuery(
            VariantQueryBuilder.buildQuery(filters, limit, offset));
    List<Variant> variants =
        StreamSupport.stream(result.iterateAll().spliterator(), false)
            .map(this::fieldValueListToVariant)
            .collect(ImmutableList.toImmutableList());

    String nextPageToken = null;
    if ((offset + limit) < count) {
      nextPageToken = PaginationToken.of(offset + limit).toBase64();
    }

    return ImmutableTriple.of(nextPageToken, count, variants);
  }

  @Override
  public List<Criteria> findCriteriaByConceptIdsOrConceptCodes(List<String> conceptKeys) {
    List<String> searchDomains =
        ImmutableList.of(
            Domain.CONDITION.toString(),
            Domain.PROCEDURE.toString(),
            Domain.DRUG.toString(),
            Domain.OBSERVATION.toString(),
            Domain.VISIT.toString(),
            Domain.DEVICE.toString(),
            Domain.MEASUREMENT.toString(),
            Domain.PHYSICAL_MEASUREMENT_CSS.toString());
    List<DbCriteria> dbCriteria;
    dbCriteria = cbCriteriaDao.findByConceptIdIn(conceptKeys, searchDomains);

    if (dbCriteria == null || dbCriteria.isEmpty()) {
      dbCriteria = cbCriteriaDao.findByCodeIn(conceptKeys, searchDomains);
    }

    return dbCriteria.stream()
        .map(cohortBuilderMapper::dbModelToClient)
        .collect(Collectors.toList());
  }

  private Variant fieldValueListToVariant(FieldValueList row) {
    Variant variant = new Variant();
    FieldValues.getString(row, "vid").ifPresent(variant::setVid);
    FieldValues.getString(row, "genes").ifPresent(variant::setGene);
    FieldValues.getString(row, "cons_str").ifPresent(variant::setConsequence);
    FieldValues.getString(row, "protein_change").ifPresent(variant::setProteinChange);
    FieldValues.getString(row, "clinical_significance_string")
        .ifPresent(variant::setClinVarSignificance);
    FieldValues.getLong(row, "allele_count").ifPresent(variant::setAlleleCount);
    FieldValues.getLong(row, "allele_number").ifPresent(variant::setAlleleNumber);
    FieldValues.getDouble(row, "allele_frequency").ifPresent(variant::setAlleleFrequency);
    FieldValues.getLong(row, "participant_count").ifPresent(variant::setParticipantCount);
    return variant;
  }

  private CriteriaListWithCountResponse getTopCountsSearchWithStandard(
      CriteriaSearchRequest request, PageRequest pageRequest) {
    Page<DbCriteria> dbCriteriaPage;
    if (isSurveyDomain(request.getDomain())) {
      Long id = cbCriteriaDao.findSurveyId(request.getSurveyName());
      dbCriteriaPage = cbCriteriaDao.findSurveyQuestionByPath(id, pageRequest);
    } else {
      dbCriteriaPage =
          cbCriteriaDao.findCriteriaTopCountsByStandard(
              request.getDomain(), request.getStandard(), pageRequest);
    }
    return new CriteriaListWithCountResponse()
        .items(
            dbCriteriaPage.getContent().stream()
                .map(cohortBuilderMapper::dbModelToClient)
                .collect(Collectors.toList()))
        .totalCount(dbCriteriaPage.getTotalElements());
  }

  private CriteriaListWithCountResponse findSurveyCriteriaBySearchTermV2(
      String surveyName, SearchTerm searchTerm, PageRequest pageRequest) {
    Page<DbCriteria> dbCriteriaPage =
        cbCriteriaDao.findSurveyQuestions(surveyName, searchTerm, pageRequest);

    if (dbCriteriaPage == null) {
      return new CriteriaListWithCountResponse();
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
            c ->
                new ConceptIdName()
                    .conceptId(Long.valueOf(c.getConceptId()))
                    .conceptName(c.getName()))
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
