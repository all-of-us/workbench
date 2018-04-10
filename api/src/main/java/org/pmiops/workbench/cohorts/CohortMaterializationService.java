package org.pmiops.workbench.cohorts;

import com.google.cloud.bigquery.BigQueryException;
import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryResult;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.inject.Provider;
import javax.servlet.http.HttpServletResponse;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.cohortbuilder.FieldSetQueryBuilder;
import org.pmiops.workbench.cohortbuilder.ParticipantCounter;
import org.pmiops.workbench.cohortbuilder.ParticipantCriteria;
import org.pmiops.workbench.cohortbuilder.QueryConfiguration;
import org.pmiops.workbench.cohortbuilder.TableQueryAndConfig;
import org.pmiops.workbench.config.CdrBigQuerySchemaConfig;
import org.pmiops.workbench.config.CdrBigQuerySchemaConfig.ColumnConfig;
import org.pmiops.workbench.config.CdrBigQuerySchemaConfig.TableConfig;
import org.pmiops.workbench.db.dao.ParticipantCohortStatusDao;
import org.pmiops.workbench.db.model.CohortReview;
import org.pmiops.workbench.db.model.ParticipantIdAndCohortStatus;
import org.pmiops.workbench.db.model.ParticipantIdAndCohortStatus.Key;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.exceptions.ServerUnavailableException;
import org.pmiops.workbench.model.CohortStatus;
import org.pmiops.workbench.model.FieldSet;
import org.pmiops.workbench.model.MaterializeCohortRequest;
import org.pmiops.workbench.model.MaterializeCohortResponse;
import org.pmiops.workbench.model.SearchRequest;
import org.pmiops.workbench.model.TableQuery;
import org.pmiops.workbench.utils.PaginationToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CohortMaterializationService {

  @VisibleForTesting
  static final String PERSON_ID = "person_id";
  @VisibleForTesting
  static final String PERSON_TABLE = "person";

  private static final List<CohortStatus> ALL_STATUSES = Arrays.asList(CohortStatus.values());

  private final BigQueryService bigQueryService;
  private final ParticipantCounter participantCounter;
  private final FieldSetQueryBuilder fieldSetQueryBuilder;
  private final ParticipantCohortStatusDao participantCohortStatusDao;
  private final Provider<CdrBigQuerySchemaConfig> cdrSchemaConfigProvider;

  @Autowired
  public CohortMaterializationService(BigQueryService bigQueryService,
      ParticipantCounter participantCounter,
      FieldSetQueryBuilder fieldSetQueryBuilder,
      ParticipantCohortStatusDao participantCohortStatusDao,
      Provider<CdrBigQuerySchemaConfig> cdrSchemaConfigProvider) {
    this.bigQueryService = bigQueryService;
    this.participantCounter = participantCounter;
    this.fieldSetQueryBuilder = fieldSetQueryBuilder;
    this.participantCohortStatusDao = participantCohortStatusDao;
    this.cdrSchemaConfigProvider = cdrSchemaConfigProvider;
  }

  private Set<Long> getParticipantIdsWithStatus(@Nullable CohortReview cohortReview, List<CohortStatus> statusFilter) {
    if (cohortReview == null) {
      return ImmutableSet.of();
    }
    Set<Long> participantIds = participantCohortStatusDao.findByParticipantKey_CohortReviewIdAndStatusIn(
        cohortReview.getCohortReviewId(), statusFilter)
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

  private TableQueryAndConfig getTableQueryAndConfig(FieldSet fieldSet) {
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
    return new TableQueryAndConfig(tableQuery, cdrSchemaConfig);
  }

  public MaterializeCohortResponse materializeCohort(@Nullable CohortReview cohortReview,
      SearchRequest searchRequest, MaterializeCohortRequest request) {
    long offset = 0L;
    FieldSet fieldSet = request.getFieldSet();
    List<CohortStatus> statusFilter = request.getStatusFilter();
    String paginationToken = request.getPageToken();
    int pageSize = request.getPageSize();
    // TODO: add CDR version ID here
    Object[] paginationParameters = new Object[] { searchRequest, statusFilter };
    if (paginationToken != null) {
      PaginationToken token = PaginationToken.fromBase64(paginationToken);
      if (token.matchesParameters(paginationParameters)) {
        offset = token.getOffset();
      } else {
        throw new BadRequestException(
            String.format("Use of pagination token %s with new parameter values", paginationToken));
      }
    }
    int limit = pageSize + 1;
    if (statusFilter == null) {
      statusFilter = ALL_STATUSES;
    }

    ParticipantCriteria criteria;
    MaterializeCohortResponse response = new MaterializeCohortResponse();
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
      criteria = new ParticipantCriteria(searchRequest, participantIdsToExclude);
    } else {
      Set<Long> participantIds = getParticipantIdsWithStatus(cohortReview, statusFilter);
      if (participantIds.isEmpty()) {
        // There is no cohort review, or no participants matching the status filter;
        // return an empty response.
        return response;
      }
      criteria = new ParticipantCriteria(participantIds);
    }
    TableQueryAndConfig tableQueryAndConfig = getTableQueryAndConfig(fieldSet);

    QueryConfiguration queryConfiguration = fieldSetQueryBuilder.buildQuery(criteria,
        tableQueryAndConfig, limit, offset);
    QueryResult result;
    QueryJobConfiguration jobConfiguration = queryConfiguration.getQueryJobConfiguration();
    try {
      result = bigQueryService.executeQuery(bigQueryService.filterBigQueryConfig(jobConfiguration));
    } catch (BigQueryException e) {
      if (e.getCode() == HttpServletResponse.SC_SERVICE_UNAVAILABLE) {
        throw new ServerUnavailableException("BigQuery was temporarily unavailable, try again later", e);
      } else if (e.getCode() == HttpServletResponse.SC_FORBIDDEN) {
        throw new ForbiddenException("Access to the CDR is denied", e);
      } else {
        throw new ServerErrorException(
            String.format("An unexpected error occurred materializing the cohort with "
                + "query = (%s), params = (%s)", jobConfiguration.getQuery(),
                jobConfiguration.getNamedParameters()), e);
      }

    }
    Map<String, Integer> rm = bigQueryService.getResultMapper(result);
    int numResults = 0;
    boolean hasMoreResults = false;
    ArrayList<Object> results = new ArrayList<>();
    for (List<FieldValue> row : result.iterateAll()) {
      if (numResults == pageSize) {
        hasMoreResults = true;
        break;
      }
      Map<String, Object> resultMap = fieldSetQueryBuilder.extractResults(tableQueryAndConfig,
          queryConfiguration.getSelectColumns(), row);
      results.add(resultMap);
      numResults++;
    }
    response.setResults(results);
    if (hasMoreResults) {
      // TODO: consider pagination based on cursor / values rather than offset
      PaginationToken token = PaginationToken.of(offset + pageSize, paginationParameters);
      response.setNextPageToken(token.toBase64());
    }
    return response;
  }
}
