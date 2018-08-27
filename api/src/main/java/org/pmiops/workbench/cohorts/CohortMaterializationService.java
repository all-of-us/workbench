package org.pmiops.workbench.cohorts;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
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
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.inject.Provider;
import org.pmiops.workbench.cdr.dao.ConceptDao;
import org.pmiops.workbench.cdr.dao.ConceptService;
import org.pmiops.workbench.cdr.model.Concept;
import org.pmiops.workbench.cohortbuilder.FieldSetQueryBuilder;
import org.pmiops.workbench.cohortbuilder.ParticipantCriteria;
import org.pmiops.workbench.cohortbuilder.TableQueryAndConfig;
import org.pmiops.workbench.cohortreview.AnnotationQueryBuilder;
import org.pmiops.workbench.config.CdrBigQuerySchemaConfig;
import org.pmiops.workbench.config.CdrBigQuerySchemaConfig.ColumnConfig;
import org.pmiops.workbench.config.CdrBigQuerySchemaConfig.TableConfig;
import org.pmiops.workbench.db.dao.ParticipantCohortStatusDao;
import org.pmiops.workbench.db.model.CohortReview;
import org.pmiops.workbench.db.model.ParticipantIdAndCohortStatus;
import org.pmiops.workbench.db.model.ParticipantIdAndCohortStatus.Key;
import org.pmiops.workbench.db.model.StorageEnums;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.model.CohortStatus;
import org.pmiops.workbench.model.ColumnFilter;
import org.pmiops.workbench.model.FieldSet;
import org.pmiops.workbench.model.MaterializeCohortRequest;
import org.pmiops.workbench.model.MaterializeCohortResponse;
import org.pmiops.workbench.model.ResultFilters;
import org.pmiops.workbench.model.SearchRequest;
import org.pmiops.workbench.model.TableQuery;
import org.pmiops.workbench.utils.PaginationToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CohortMaterializationService {

  private static final String DOMAIN_CONCEPT_STANDARD = "standard";
  private static final String DOMAIN_CONCEPT_SOURCE = "source";

  @VisibleForTesting
  static final String PERSON_ID = "person_id";
  @VisibleForTesting
  static final String PERSON_TABLE = "person";

  private static final List<CohortStatus> NOT_EXCLUDED =
      Arrays.asList(CohortStatus.INCLUDED, CohortStatus.NEEDS_FURTHER_REVIEW,
          CohortStatus.NOT_REVIEWED);

  private final FieldSetQueryBuilder fieldSetQueryBuilder;
  private final AnnotationQueryBuilder annotationQueryBuilder;
  private final ParticipantCohortStatusDao participantCohortStatusDao;
  private final Provider<CdrBigQuerySchemaConfig> cdrSchemaConfigProvider;
  private final ConceptDao conceptDao;

  @Autowired
  public CohortMaterializationService(
      FieldSetQueryBuilder fieldSetQueryBuilder,
      AnnotationQueryBuilder annotationQueryBuilder,
      ParticipantCohortStatusDao participantCohortStatusDao,
      Provider<CdrBigQuerySchemaConfig> cdrSchemaConfigProvider,
      ConceptDao conceptDao) {
    this.fieldSetQueryBuilder = fieldSetQueryBuilder;
    this.annotationQueryBuilder = annotationQueryBuilder;
    this.participantCohortStatusDao = participantCohortStatusDao;
    this.cdrSchemaConfigProvider = cdrSchemaConfigProvider;
    this.conceptDao = conceptDao;
  }

  private Set<Long> getParticipantIdsWithStatus(@Nullable CohortReview cohortReview, List<CohortStatus> statusFilter) {
    if (cohortReview == null) {
      return ImmutableSet.of();
    }
    List<Short> dbStatusFilter = statusFilter.stream()
        .map(StorageEnums::cohortStatusToStorage)
        .collect(Collectors.toList());
    Set<Long> participantIds = participantCohortStatusDao.findByParticipantKey_CohortReviewIdAndStatusIn(
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

  private TableQueryAndConfig getTableQueryAndConfig(FieldSet fieldSet,
      @Nullable Set<Long> conceptIds) {
    TableQuery tableQuery;
    if (fieldSet == null) {
      tableQuery = new TableQuery();
      tableQuery.setTableName(PERSON_TABLE);
      tableQuery.setColumns(ImmutableList.of(PERSON_ID));
    } else {
      tableQuery = fieldSet.getTableQuery();
      if (tableQuery == null) {
        // TODO: support other kinds of field sets besides tableQuery
        throw new BadRequestException("tableQuery must be specified in field sets");
      }
      String tableName = tableQuery.getTableName();
      if (Strings.isNullOrEmpty(tableName)) {
        throw new BadRequestException("Table name must be specified in field sets");
      }
    }
    CdrBigQuerySchemaConfig cdrSchemaConfig = cdrSchemaConfigProvider.get();
    TableConfig tableConfig = cdrSchemaConfig.cohortTables.get(tableQuery.getTableName());
    if (tableConfig == null) {
      throw new BadRequestException("Table " + tableQuery.getTableName() + " is not a valid "
          + "cohort table; valid tables are: " +
          cdrSchemaConfig.cohortTables.keySet().stream().sorted().collect(Collectors.joining(",")));
    }
    Map<String, ColumnConfig> columnMap = Maps.uniqueIndex(tableConfig.columns,
        columnConfig -> columnConfig.name);

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


  private ParticipantCriteria getParticipantCriteria(List<CohortStatus> statusFilter,
      @Nullable CohortReview cohortReview, SearchRequest searchRequest) {
    if (statusFilter.contains(CohortStatus.NOT_REVIEWED)) {
      Set<Long> participantIdsToExclude;
      if (statusFilter.size() < CohortStatus.values().length) {
        // Find the participant IDs that have statuses which *aren't* in the filter.
        Set<CohortStatus> statusesToExclude =
            Sets.difference(ImmutableSet.copyOf(CohortStatus.values()), ImmutableSet.copyOf(statusFilter));
        participantIdsToExclude = getParticipantIdsWithStatus(cohortReview, ImmutableList.copyOf(statusesToExclude));
      } else {
        participantIdsToExclude = ImmutableSet.of();
      }
      return new ParticipantCriteria(searchRequest, participantIdsToExclude);
    } else {
      Set<Long> participantIds = getParticipantIdsWithStatus(cohortReview, statusFilter);
      return new ParticipantCriteria(participantIds);
    }
  }

  private void addFilterOnConcepts(TableQuery tableQuery, Set<Long> conceptIds,
      TableConfig tableConfig) {
    String standardConceptColumn = null;
    String sourceConceptColumn = null;
    for (ColumnConfig columnConfig : tableConfig.columns) {
      if (DOMAIN_CONCEPT_STANDARD.equals(columnConfig.domainConcept)) {
        standardConceptColumn = columnConfig.name;
        if (sourceConceptColumn != null) {
          break;
        }
      } else if (DOMAIN_CONCEPT_SOURCE.equals(columnConfig.domainConcept)) {
        sourceConceptColumn = columnConfig.name;
        if (standardConceptColumn != null) {
          break;
        }
      }
    }
    if (standardConceptColumn == null || sourceConceptColumn == null) {
      throw new ServerErrorException("Couldn't find standard and source concept columns for " +
          tableQuery.getTableName());
    }

    Iterable<Concept> concepts = conceptDao.findAll(conceptIds);
    List<Long> standardConceptIds = Lists.newArrayList();
    List<Long> sourceConceptIds = Lists.newArrayList();
    for (Concept concept : concepts) {
      if (concept.getStandardConcept() != null &&
          concept.getStandardConcept().equals(ConceptService.STANDARD_CONCEPT_CODE)) {
        standardConceptIds.add(concept.getConceptId());
      } else {
        // We may need to handle classification / concept hierarchy here eventually...
        sourceConceptIds.add(concept.getConceptId());
      }
    }
    ResultFilters conceptFilters = null;
    if (!standardConceptIds.isEmpty()) {
      ColumnFilter standardConceptFilter =
          new ColumnFilter().columnName(standardConceptColumn)
              .valueNumbers(standardConceptIds.stream().map(id -> new BigDecimal(id))
                  .collect(Collectors.toList()));
      conceptFilters = new ResultFilters().columnFilter(standardConceptFilter);
    }
    if (!sourceConceptIds.isEmpty()) {
      ColumnFilter sourceConceptFilter =
          new ColumnFilter().columnName(sourceConceptColumn)
              .valueNumbers(sourceConceptIds.stream().map(id -> new BigDecimal(id))
                  .collect(Collectors.toList()));
      ResultFilters sourceResultFilters = new ResultFilters().columnFilter(sourceConceptFilter);
      if (conceptFilters == null) {
        conceptFilters = sourceResultFilters;
      } else {
        // If both source and standard concepts are present, match either.
        conceptFilters = new ResultFilters().anyOf(ImmutableList.of(conceptFilters, sourceResultFilters));
      }
    }
    if (conceptFilters != null) {
      if (tableQuery.getFilters() == null) {
        tableQuery.setFilters(conceptFilters);
      } else {
        // If both concept filters and other filters are requested, require results to match both.
        tableQuery.setFilters(new ResultFilters().allOf(
            ImmutableList.of(tableQuery.getFilters(), conceptFilters)));
      }
    }
  }


  /**
   * Materializes a cohort.
   * @param cohortReview {@link CohortReview} representing a manual review of participants in the cohort.
   * @param cohortSpec JSON representing the cohort criteria.
   * @param conceptIds an optional set of IDs for concepts used to filter results by
   * (in addition to the filtering specified in {@param cohortSpec})
   * @param request {@link MaterializeCohortRequest} representing the request options
   * @return {@link MaterializeCohortResponse} containing the results of cohort materialization
   */
  public MaterializeCohortResponse materializeCohort(@Nullable CohortReview cohortReview,
      String cohortSpec, @Nullable Set<Long> conceptIds, MaterializeCohortRequest request) {
    SearchRequest searchRequest;
    try {
      searchRequest = new Gson().fromJson(cohortSpec, SearchRequest.class);
    } catch (JsonSyntaxException e) {
      throw new BadRequestException("Invalid cohort spec");
    }
    return materializeCohort(cohortReview, searchRequest, conceptIds,
        Objects.hash(cohortSpec, conceptIds), request);
  }

  /**
   * Materializes a cohort.
   * @param cohortReview {@link CohortReview} representing a manual review of participants in the cohort.
   * @param searchRequest {@link SearchRequest} representing the cohort criteria
   * @param conceptIds an optional set of IDs for concepts used to filter results by
   *    * (in addition to the filtering specified in {@param searchRequest})
   * @param requestHash a number representing a stable hash of the request; used to enforce that pagination
   *   tokens are used appropriately
   * @param request {@link MaterializeCohortRequest} representing the request
   * @return {@link MaterializeCohortResponse} containing the results of cohort materialization
   */
  @VisibleForTesting
  MaterializeCohortResponse materializeCohort(@Nullable CohortReview cohortReview,
      SearchRequest searchRequest, @Nullable Set<Long> conceptIds,
      int requestHash, MaterializeCohortRequest request) {
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
    Object[] paginationParameters = new Object[] { requestHash, String.valueOf(statusFilter) };

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
      ParticipantCriteria criteria = getParticipantCriteria(statusFilter, cohortReview,
          searchRequest);
      if (criteria.getParticipantIdsToInclude() != null
          && criteria.getParticipantIdsToInclude().isEmpty()) {
        // There is no cohort review, or no participants matching the status filter;
        // return an empty response.
        return response;
      }
      results = fieldSetQueryBuilder.materializeTableQuery(getTableQueryAndConfig(fieldSet,
          conceptIds), criteria, limit, offset);
    } else if (fieldSet.getAnnotationQuery() != null) {
      if (cohortReview == null) {
        // There is no cohort review, so there are no annotations; return empty results.
        return response;
      }
      results = annotationQueryBuilder.materializeAnnotationQuery(cohortReview, statusFilter,
          fieldSet.getAnnotationQuery(), limit, offset);
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
}
