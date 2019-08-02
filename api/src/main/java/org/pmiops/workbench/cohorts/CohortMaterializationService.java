package org.pmiops.workbench.cohorts;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.inject.Provider;
import org.pmiops.workbench.cdr.CdrVersionContext;
import org.pmiops.workbench.cdr.dao.ConceptService;
import org.pmiops.workbench.cdr.dao.ConceptService.ConceptIds;
import org.pmiops.workbench.cohortbuilder.FieldSetQueryBuilder;
import org.pmiops.workbench.cohortbuilder.ParticipantCriteria;
import org.pmiops.workbench.cohortbuilder.TableQueryAndConfig;
import org.pmiops.workbench.cohortreview.AnnotationQueryBuilder;
import org.pmiops.workbench.config.CdrBigQuerySchemaConfig;
import org.pmiops.workbench.config.CdrBigQuerySchemaConfig.ColumnConfig;
import org.pmiops.workbench.config.CdrBigQuerySchemaConfig.TableConfig;
import org.pmiops.workbench.config.CdrBigQuerySchemaConfigService;
import org.pmiops.workbench.config.CdrBigQuerySchemaConfigService.ConceptColumns;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.ParticipantCohortStatusDao;
import org.pmiops.workbench.db.model.CdrVersion;
import org.pmiops.workbench.db.model.CohortReview;
import org.pmiops.workbench.db.model.ParticipantIdAndCohortStatus;
import org.pmiops.workbench.db.model.ParticipantIdAndCohortStatus.Key;
import org.pmiops.workbench.db.model.StorageEnums;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.model.CdrQuery;
import org.pmiops.workbench.model.CohortAnnotationsRequest;
import org.pmiops.workbench.model.CohortAnnotationsResponse;
import org.pmiops.workbench.model.CohortStatus;
import org.pmiops.workbench.model.ColumnFilter;
import org.pmiops.workbench.model.DataTableSpecification;
import org.pmiops.workbench.model.FieldSet;
import org.pmiops.workbench.model.MaterializeCohortRequest;
import org.pmiops.workbench.model.MaterializeCohortResponse;
import org.pmiops.workbench.model.Operator;
import org.pmiops.workbench.model.ResultFilters;
import org.pmiops.workbench.model.SearchRequest;
import org.pmiops.workbench.model.TableQuery;
import org.pmiops.workbench.utils.PaginationToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CohortMaterializationService {

  // Transforms a name, query parameter value pair into a map that will be converted into a JSON
  // dictionary representing the named query parameter value, to be used as a part of a BigQuery
  // job configuration when executing SQL queries on the client.
  // See https://cloud.google.com/bigquery/docs/parameterized-queries for JSON configuration of
  // query parameters
  private static final Function<Map.Entry<String, QueryParameterValue>, Map<String, Object>>
      TO_QUERY_PARAMETER_MAP =
          (entry) -> {
            QueryParameterValue value = entry.getValue();
            ImmutableMap.Builder<String, Object> builder =
                ImmutableMap.<String, Object>builder().put("name", entry.getKey());
            ImmutableMap.Builder<String, Object> parameterTypeMap = ImmutableMap.builder();
            parameterTypeMap.put("type", value.getType().toString());
            ImmutableMap.Builder<String, Object> parameterValueMap = ImmutableMap.builder();
            if (value.getArrayType() == null) {
              parameterValueMap.put("value", value.getValue());
            } else {
              ImmutableMap.Builder<String, Object> arrayTypeMap = ImmutableMap.builder();
              arrayTypeMap.put("type", value.getArrayType().toString());
              parameterTypeMap.put("arrayType", arrayTypeMap.build());
              ImmutableList.Builder<Map<String, Object>> values =
                  ImmutableList.<Map<String, Object>>builder();
              for (QueryParameterValue arrayValue : value.getArrayValues()) {
                ImmutableMap.Builder<String, Object> valueMap =
                    ImmutableMap.<String, Object>builder();
                valueMap.put("value", arrayValue.getValue());
                values.add(valueMap.build());
              }
              parameterValueMap.put("arrayValues", values.build().<Map<String, Object>>toArray());
            }
            builder.put("parameterType", parameterTypeMap.build());
            builder.put("parameterValue", parameterValueMap.build());
            return builder.build();
          };

  @VisibleForTesting static final String PERSON_ID = "person_id";
  @VisibleForTesting static final String PERSON_TABLE = "person";

  private static final List<CohortStatus> NOT_EXCLUDED =
      Arrays.asList(
          CohortStatus.INCLUDED, CohortStatus.NEEDS_FURTHER_REVIEW, CohortStatus.NOT_REVIEWED);

  private final FieldSetQueryBuilder fieldSetQueryBuilder;
  private final AnnotationQueryBuilder annotationQueryBuilder;
  private final ParticipantCohortStatusDao participantCohortStatusDao;
  private final CdrBigQuerySchemaConfigService cdrBigQuerySchemaConfigService;
  private final ConceptService conceptService;
  private Provider<WorkbenchConfig> configProvider;

  @Autowired
  public CohortMaterializationService(
      FieldSetQueryBuilder fieldSetQueryBuilder,
      AnnotationQueryBuilder annotationQueryBuilder,
      ParticipantCohortStatusDao participantCohortStatusDao,
      CdrBigQuerySchemaConfigService cdrBigQuerySchemaConfigService,
      ConceptService conceptService,
      Provider<WorkbenchConfig> configProvider) {
    this.fieldSetQueryBuilder = fieldSetQueryBuilder;
    this.annotationQueryBuilder = annotationQueryBuilder;
    this.participantCohortStatusDao = participantCohortStatusDao;
    this.cdrBigQuerySchemaConfigService = cdrBigQuerySchemaConfigService;
    this.conceptService = conceptService;
    this.configProvider = configProvider;
  }

  private Set<Long> getParticipantIdsWithStatus(
      @Nullable CohortReview cohortReview, List<CohortStatus> statusFilter) {
    if (cohortReview == null) {
      return ImmutableSet.of();
    }
    List<Short> dbStatusFilter =
        statusFilter.stream().map(StorageEnums::cohortStatusToStorage).collect(Collectors.toList());
    Set<Long> participantIds =
        participantCohortStatusDao
            .findByParticipantKey_CohortReviewIdAndStatusIn(
                cohortReview.getCohortReviewId(), dbStatusFilter)
            .stream()
            .map(ParticipantIdAndCohortStatus::getParticipantKey)
            .map(Key::getParticipantId)
            .collect(Collectors.toSet());
    return participantIds;
  }

  private ColumnConfig findPrimaryKey(TableConfig tableConfig) {
    for (ColumnConfig columnConfig : tableConfig.columns) {
      if (columnConfig.primaryKey != null && columnConfig.primaryKey) {
        return columnConfig;
      }
    }
    throw new IllegalStateException("Table lacks primary key!");
  }

  private TableQueryAndConfig getTableQueryAndConfig(
      @Nullable TableQuery tableQuery, @Nullable Set<Long> conceptIds) {
    if (tableQuery == null) {
      tableQuery = new TableQuery();
      tableQuery.setTableName(PERSON_TABLE);
      tableQuery.setColumns(ImmutableList.of(PERSON_ID));
    } else {
      String tableName = tableQuery.getTableName();
      if (Strings.isNullOrEmpty(tableName)) {
        throw new BadRequestException("Table name must be specified in field sets");
      }
    }
    CdrBigQuerySchemaConfig cdrSchemaConfig = cdrBigQuerySchemaConfigService.getConfig();
    TableConfig tableConfig = cdrSchemaConfig.cohortTables.get(tableQuery.getTableName());
    if (tableConfig == null) {
      throw new BadRequestException(
          "Table "
              + tableQuery.getTableName()
              + " is not a valid "
              + "cohort table; valid tables are: "
              + cdrSchemaConfig.cohortTables.keySet().stream()
                  .sorted()
                  .collect(Collectors.joining(",")));
    }
    Map<String, ColumnConfig> columnMap =
        Maps.uniqueIndex(tableConfig.columns, columnConfig -> columnConfig.name);

    List<String> columnNames = tableQuery.getColumns();
    if (columnNames == null || columnNames.isEmpty()) {
      // By default, return all columns on the table in question in our configuration.
      tableQuery.setColumns(columnMap.keySet().stream().collect(Collectors.toList()));
    }
    List<String> orderBy = tableQuery.getOrderBy();
    if (orderBy == null || orderBy.isEmpty()) {
      ColumnConfig primaryKey = findPrimaryKey(tableConfig);
      if (PERSON_ID.equals(primaryKey.name)) {
        tableQuery.setOrderBy(ImmutableList.of(PERSON_ID));
      } else {
        // TODO: consider having per-table default sort order based on e.g. timestamp
        tableQuery.setOrderBy(ImmutableList.of(PERSON_ID, primaryKey.name));
      }
    }
    if (conceptIds != null) {
      addFilterOnConcepts(tableQuery, conceptIds, tableConfig);
    }
    return new TableQueryAndConfig(tableQuery, cdrSchemaConfig);
  }

  private ParticipantCriteria getParticipantCriteria(
      List<CohortStatus> statusFilter,
      @Nullable CohortReview cohortReview,
      SearchRequest searchRequest) {
    if (statusFilter.contains(CohortStatus.NOT_REVIEWED)) {
      Set<Long> participantIdsToExclude;
      if (statusFilter.size() < CohortStatus.values().length) {
        // Find the participant IDs that have statuses which *aren't* in the filter.
        Set<CohortStatus> statusesToExclude =
            Sets.difference(
                ImmutableSet.copyOf(CohortStatus.values()), ImmutableSet.copyOf(statusFilter));
        participantIdsToExclude =
            getParticipantIdsWithStatus(cohortReview, ImmutableList.copyOf(statusesToExclude));
      } else {
        participantIdsToExclude = ImmutableSet.of();
      }
      return new ParticipantCriteria(searchRequest, participantIdsToExclude);
    } else {
      Set<Long> participantIds = getParticipantIdsWithStatus(cohortReview, statusFilter);
      return new ParticipantCriteria(participantIds);
    }
  }

  private void addFilterOnConcepts(
      TableQuery tableQuery, Set<Long> conceptIds, TableConfig tableConfig) {
    ConceptColumns conceptColumns =
        cdrBigQuerySchemaConfigService.getConceptColumns(tableConfig, tableQuery.getTableName());
    ConceptIds classifiedConceptIds = conceptService.classifyConceptIds(conceptIds);

    if (classifiedConceptIds.getSourceConceptIds().isEmpty()
        && classifiedConceptIds.getStandardConceptIds().isEmpty()) {
      throw new BadRequestException("Concept set contains no valid concepts");
    }
    ResultFilters conceptFilters = null;
    if (!classifiedConceptIds.getStandardConceptIds().isEmpty()) {
      ColumnFilter standardConceptFilter =
          new ColumnFilter()
              .columnName(conceptColumns.getStandardConceptColumn().name)
              .operator(Operator.IN)
              .valueNumbers(
                  classifiedConceptIds.getStandardConceptIds().stream()
                      .map(id -> new BigDecimal(id))
                      .collect(Collectors.toList()));
      conceptFilters = new ResultFilters().columnFilter(standardConceptFilter);
    }
    if (!classifiedConceptIds.getSourceConceptIds().isEmpty()) {
      ColumnFilter sourceConceptFilter =
          new ColumnFilter()
              .columnName(conceptColumns.getSourceConceptColumn().name)
              .operator(Operator.IN)
              .valueNumbers(
                  classifiedConceptIds.getSourceConceptIds().stream()
                      .map(id -> new BigDecimal(id))
                      .collect(Collectors.toList()));
      ResultFilters sourceResultFilters = new ResultFilters().columnFilter(sourceConceptFilter);
      if (conceptFilters == null) {
        conceptFilters = sourceResultFilters;
      } else {
        // If both source and standard concepts are present, match either.
        conceptFilters =
            new ResultFilters().anyOf(ImmutableList.of(conceptFilters, sourceResultFilters));
      }
    }
    if (conceptFilters != null) {
      if (tableQuery.getFilters() == null) {
        tableQuery.setFilters(conceptFilters);
      } else {
        // If both concept filters and other filters are requested, require results to match both.
        tableQuery.setFilters(
            new ResultFilters().allOf(ImmutableList.of(tableQuery.getFilters(), conceptFilters)));
      }
    }
  }

  @VisibleForTesting
  CdrQuery getCdrQuery(
      SearchRequest searchRequest,
      DataTableSpecification dataTableSpecification,
      @Nullable CohortReview cohortReview,
      @Nullable Set<Long> conceptIds) {
    CdrVersion cdrVersion = CdrVersionContext.getCdrVersion();
    CdrQuery cdrQuery =
        new CdrQuery()
            .bigqueryDataset(cdrVersion.getBigqueryDataset())
            .bigqueryProject(cdrVersion.getBigqueryProject());
    List<CohortStatus> statusFilter = dataTableSpecification.getStatusFilter();
    if (statusFilter == null) {
      statusFilter = NOT_EXCLUDED;
    }
    ParticipantCriteria criteria =
        getParticipantCriteria(statusFilter, cohortReview, searchRequest);
    TableQueryAndConfig tableQueryAndConfig =
        getTableQueryAndConfig(dataTableSpecification.getTableQuery(), conceptIds);
    cdrQuery.setColumns(tableQueryAndConfig.getTableQuery().getColumns());
    if (criteria.getParticipantIdsToInclude() != null
        && criteria.getParticipantIdsToInclude().isEmpty()) {
      // There is no cohort review, or no participants matching the status filter;
      // return a query with no SQL, indicating there should be no results.
      return cdrQuery;
    }
    QueryJobConfiguration jobConfiguration =
        fieldSetQueryBuilder.getQueryJobConfiguration(
            criteria, tableQueryAndConfig, dataTableSpecification.getMaxResults());
    cdrQuery.setSql(jobConfiguration.getQuery());
    ImmutableMap.Builder<String, Object> configurationMap = ImmutableMap.builder();
    ImmutableMap.Builder<String, Object> queryConfigurationMap = ImmutableMap.builder();
    queryConfigurationMap.put(
        "queryParameters",
        jobConfiguration.getNamedParameters().entrySet().stream()
            .map(TO_QUERY_PARAMETER_MAP)
            .<Map<String, Object>>toArray());
    configurationMap.put("query", queryConfigurationMap.build());
    cdrQuery.setConfiguration(configurationMap.build());
    return cdrQuery;
  }

  public CdrQuery getCdrQuery(
      String cohortSpec,
      DataTableSpecification dataTableSpecification,
      @Nullable CohortReview cohortReview,
      @Nullable Set<Long> conceptIds) {
    SearchRequest searchRequest;
    try {
      searchRequest = new Gson().fromJson(cohortSpec, SearchRequest.class);
    } catch (JsonSyntaxException e) {
      throw new BadRequestException("Invalid cohort spec");
    }
    return getCdrQuery(searchRequest, dataTableSpecification, cohortReview, conceptIds);
  }

  /**
   * Materializes a cohort.
   *
   * @param cohortReview {@link CohortReview} representing a manual review of participants in the
   *     cohort.
   * @param cohortSpec JSON representing the cohort criteria.
   * @param conceptIds an optional set of IDs for concepts used to filter results by (in addition to
   *     the filtering specified in {@param cohortSpec})
   * @param request {@link MaterializeCohortRequest} representing the request options
   * @return {@link MaterializeCohortResponse} containing the results of cohort materialization
   */
  public MaterializeCohortResponse materializeCohort(
      @Nullable CohortReview cohortReview,
      String cohortSpec,
      @Nullable Set<Long> conceptIds,
      MaterializeCohortRequest request) {
    SearchRequest searchRequest;
    try {
      searchRequest = new Gson().fromJson(cohortSpec, SearchRequest.class);
    } catch (JsonSyntaxException e) {
      throw new BadRequestException("Invalid cohort spec");
    }
    return materializeCohort(
        cohortReview, searchRequest, conceptIds, Objects.hash(cohortSpec, conceptIds), request);
  }

  /**
   * Materializes a cohort.
   *
   * @param cohortReview {@link CohortReview} representing a manual review of participants in the
   *     cohort.
   * @param searchRequest {@link SearchRequest} representing the cohort criteria
   * @param conceptIds an optional set of IDs for concepts used to filter results by * (in addition
   *     to the filtering specified in {@param searchRequest})
   * @param requestHash a number representing a stable hash of the request; used to enforce that
   *     pagination tokens are used appropriately
   * @param request {@link MaterializeCohortRequest} representing the request
   * @return {@link MaterializeCohortResponse} containing the results of cohort materialization
   */
  @VisibleForTesting
  MaterializeCohortResponse materializeCohort(
      @Nullable CohortReview cohortReview,
      SearchRequest searchRequest,
      @Nullable Set<Long> conceptIds,
      int requestHash,
      MaterializeCohortRequest request) {
    long offset = 0L;
    FieldSet fieldSet = request.getFieldSet();
    List<CohortStatus> statusFilter = request.getStatusFilter();
    String paginationToken = request.getPageToken();
    int pageSize = request.getPageSize();
    // TODO: add CDR version ID here
    // We require the client to specify requestHash here instead of hashing searchRequest itself,
    // and use String.valueOf(statusFilter) instead of just statusFilter;
    // both searchRequest and statusFilter contain enums, which do not have stable has codes across
    // JVMs (see [RW-1149]).
    Object[] paginationParameters = new Object[] {requestHash, String.valueOf(statusFilter)};

    if (paginationToken != null) {
      PaginationToken token = PaginationToken.fromBase64(paginationToken);
      if (token.matchesParameters(paginationParameters)) {
        offset = token.getOffset();
      } else {
        throw new BadRequestException(
            String.format("Use of pagination token %s with new parameter values", paginationToken));
      }
    }
    // Grab the next pagination token here, because statusFilter can be modified below.
    // TODO: consider pagination based on cursor / values rather than offset
    String nextToken = PaginationToken.of(offset + pageSize, paginationParameters).toBase64();

    int limit = pageSize + 1;

    MaterializeCohortResponse response = new MaterializeCohortResponse();
    Iterable<Map<String, Object>> results;
    if (statusFilter == null) {
      statusFilter = NOT_EXCLUDED;
    }
    if (fieldSet == null || fieldSet.getTableQuery() != null) {
      ParticipantCriteria criteria =
          getParticipantCriteria(statusFilter, cohortReview, searchRequest);
      if (criteria.getParticipantIdsToInclude() != null
          && criteria.getParticipantIdsToInclude().isEmpty()) {
        // There is no cohort review, or no participants matching the status filter;
        // return an empty response.
        return response;
      }
      TableQuery tableQuery = fieldSet == null ? null : fieldSet.getTableQuery();
      results =
          fieldSetQueryBuilder.materializeTableQuery(
              getTableQueryAndConfig(tableQuery, conceptIds), criteria, limit, offset);
    } else if (fieldSet.getAnnotationQuery() != null) {
      if (cohortReview == null) {
        // There is no cohort review, so there are no annotations; return empty results.
        return response;
      }
      results =
          annotationQueryBuilder
              .materializeAnnotationQuery(
                  cohortReview, statusFilter, fieldSet.getAnnotationQuery(), limit, offset)
              .getResults();
    } else {
      throw new BadRequestException("Must specify tableQuery or annotationQuery");
    }
    int numResults = 0;
    boolean hasMoreResults = false;
    ArrayList<Object> responseResults = new ArrayList<>();
    for (Map<String, Object> resultMap : results) {
      if (numResults == pageSize) {
        hasMoreResults = true;
        break;
      }
      responseResults.add(resultMap);
      numResults++;
    }
    response.setResults(responseResults);
    if (hasMoreResults) {
      response.setNextPageToken(nextToken);
    }
    return response;
  }

  public CohortAnnotationsResponse getAnnotations(
      CohortReview cohortReview, CohortAnnotationsRequest request) {
    List<CohortStatus> statusFilter = request.getStatusFilter();
    if (statusFilter == null) {
      statusFilter = NOT_EXCLUDED;
    }
    AnnotationQueryBuilder.AnnotationResults results =
        annotationQueryBuilder.materializeAnnotationQuery(
            cohortReview, statusFilter, request.getAnnotationQuery(), null, 0L);
    return new CohortAnnotationsResponse()
        .results(ImmutableList.copyOf(results.getResults()))
        .columns(results.getColumns());
  }
}
