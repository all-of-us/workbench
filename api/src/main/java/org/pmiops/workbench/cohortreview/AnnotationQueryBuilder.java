package org.pmiops.workbench.cohortreview;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;
import org.pmiops.workbench.db.dao.CohortAnnotationDefinitionDao;
import org.pmiops.workbench.db.model.CohortAnnotationDefinition;
import org.pmiops.workbench.db.model.CohortReview;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.model.AnnotationQuery;
import org.pmiops.workbench.model.AnnotationType;
import org.pmiops.workbench.model.CohortStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

@Service
// TODO(RW-499): use a library to construct the SQL below, rather than concatenating strings
public class AnnotationQueryBuilder {

  public static final String PERSON_ID_COLUMN = "person_id";
  public static final String REVIEW_STATUS_COLUMN = "review_status";

  // These column names are reserved and can't be used for annotation definition column names.
  public static final ImmutableSet<String> RESERVED_COLUMNS = ImmutableSet.of(PERSON_ID_COLUMN,
      REVIEW_STATUS_COLUMN);
  private static final ImmutableMap<AnnotationType, String> ANNOTATION_COLUMN_MAP =
      ImmutableMap.of(
          AnnotationType.BOOLEAN, "annotation_value_boolean",
          AnnotationType.DATE, "annotation_value_date",
          AnnotationType.INTEGER, "annotation_value_integer",
          AnnotationType.STRING, "annotation_value_string");

  public static final String DESCENDING_PREFIX = "DESCENDING(";

  private static final String DATE_FORMAT_PATTERN = "yyyy-MM-dd";
  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(DATE_FORMAT_PATTERN);

  static {
    DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
  }

  private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
  private final CohortAnnotationDefinitionDao cohortAnnotationDefinitionDao;

  @Autowired
  AnnotationQueryBuilder(NamedParameterJdbcTemplate namedParameterJdbcTemplate,
      CohortAnnotationDefinitionDao cohortAnnotationDefinitionDao) {
    this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    this.cohortAnnotationDefinitionDao = cohortAnnotationDefinitionDao;
  }

  private static final String ANNOTATION_JOIN_SQL =
      " LEFT OUTER JOIN participant_cohort_annotations a%d "
      + "ON a%d.participant_id = pcs.participant_id "
      + "AND a%d.cohort_annotation_definition_id = :def_%d "
      + "AND a%d.cohort_review_id = %d";

  private static final String ANNOTATION_VALUE_JOIN_SQL =
      " LEFT OUTER JOIN cohort_annotation_enum_value ae%d "
      + "ON ae%d.cohort_annotation_enum_value_id = a%d.cohort_annotation_enum_value_id";

  private static final ImmutableSet<CohortStatus> REVIEWED_STATUSES =
      ImmutableSet.of(CohortStatus.INCLUDED, CohortStatus.EXCLUDED, CohortStatus.NEEDS_FURTHER_REVIEW);

  private Map<String, CohortAnnotationDefinition> getAnnotationDefinitions(CohortReview cohortReview) {
    return Maps.uniqueIndex(
            cohortAnnotationDefinitionDao.findByCohortId(cohortReview.getCohortId()),
            CohortAnnotationDefinition::getColumnName);
  }

  private String getSelectAndFromSql(CohortReview cohortReview, List<String> columns,
      Map<String, CohortAnnotationDefinition> annotationDefinitions, Map<String, String> columnAliasMap,
      ImmutableMap.Builder<String, Object> parameters) {
    StringBuilder selectBuilder = new StringBuilder("SELECT ");
    StringBuilder fromBuilder = new StringBuilder("\nFROM participant_cohort_status pcs");
    int annotationCount = 0;
    boolean firstColumn = true;
    for (String column : columns) {
      if (firstColumn) {
        firstColumn = false;
      } else {
        selectBuilder.append(", ");
      }
      if (column.equals(PERSON_ID_COLUMN)) {
        selectBuilder.append("pcs.participant_id person_id");
      } else if (column.equals(REVIEW_STATUS_COLUMN)) {
        selectBuilder.append("pcs.status review_status");
      } else {
        if (annotationDefinitions == null) {
          annotationDefinitions = getAnnotationDefinitions(cohortReview);
        }
        CohortAnnotationDefinition definition = annotationDefinitions.get(column);
        if (definition == null) {
          throw new BadRequestException("Invalid annotation name: " + column);
        }
        annotationCount++;
        fromBuilder.append(
            String.format(ANNOTATION_JOIN_SQL, annotationCount, annotationCount, annotationCount,
                annotationCount, annotationCount,
                cohortReview.getCohortReviewId()));
        parameters.put("def_" + annotationCount, definition.getCohortAnnotationDefinitionId());
        String sourceColumn;
        if (definition.getAnnotationType().equals(AnnotationType.ENUM)) {
          sourceColumn = String.format("ae%d.name", annotationCount);
          fromBuilder.append(
              String.format(ANNOTATION_VALUE_JOIN_SQL, annotationCount, annotationCount, annotationCount));
        } else {

          String columnName = ANNOTATION_COLUMN_MAP.get(definition.getAnnotationType());
          if (columnName == null) {
            throw new ServerErrorException("Invalid annotation type: " + definition.getAnnotationType());
          }
          sourceColumn = String.format("a%d.%s", annotationCount, columnName);
        }
        String columnAliasName = String.format("av%d", annotationCount);
        selectBuilder.append(String.format("%s %s", sourceColumn, columnAliasName));
        columnAliasMap.put(column, columnAliasName);
      }
    }
    return selectBuilder.toString() + fromBuilder.toString();
  }

  private String getWhereSql(CohortReview cohortReview, List<CohortStatus> statusFilter,
      ImmutableMap.Builder<String, Object> parameters) {
    StringBuilder whereBuilder = new StringBuilder(" WHERE pcs.cohort_review_id = :cohort_review_id");
    parameters.put("cohort_review_id", cohortReview.getCohortReviewId());
    if (!statusFilter.containsAll(REVIEWED_STATUSES)) {
      whereBuilder.append(" AND pcs.status IN (:statuses)");
      parameters.put("statuses", statusFilter.stream().map(CohortStatus::ordinal).collect(Collectors.toList()));
    }
    return whereBuilder.toString();
  }

  private String getOrderBySql(List<String> orderBy, Map<String, String> columnAliasMap) {
    StringBuilder orderByBuilder = new StringBuilder("\nORDER BY ");
    boolean firstOrderByColumn = true;
    for (String orderByColumn : orderBy) {
      if (firstOrderByColumn) {
        firstOrderByColumn = false;
      } else {
        orderByBuilder.append(", ");
      }
      boolean descending = false;
      if (orderByColumn.startsWith(DESCENDING_PREFIX)) {
        orderByColumn = orderByColumn.substring(DESCENDING_PREFIX.length(), orderByColumn.length() - 1);
        descending = true;
      }

      if (orderByColumn.equals(PERSON_ID_COLUMN) || orderByColumn.equals(REVIEW_STATUS_COLUMN)) {
        orderByBuilder.append(orderByColumn);
      } else {
        String columnAlias = columnAliasMap.get(orderByColumn);
        if (columnAlias == null) {
          throw new BadRequestException("Column " + orderByColumn + " in orderBy must be present in columns");
        }
        orderByBuilder.append(columnAlias);
      }
      if (descending) {
        orderByBuilder.append(" DESC");
      }
    }
    return orderByBuilder.toString();
  }

  private String getLimitAndOffsetSql(int limit, long offset, ImmutableMap.Builder<String, Object> params) {
    StringBuilder endSqlBuilder = new StringBuilder("\nLIMIT :limit");
    params.put("limit", limit);
    if (offset != 0L) {
      // TODO: consider pagination based on values rather than offsets
      endSqlBuilder.append(" OFFSET :offset");
      params.put("offset", offset);
    }
    return endSqlBuilder.toString();
  }

  private String getSql(CohortReview cohortReview, List<CohortStatus> statusFilter,
      AnnotationQuery annotationQuery, int limit, long offset,
      Map<String, CohortAnnotationDefinition> annotationDefinitions,
      ImmutableMap.Builder<String, Object> parameters) {
    Map<String, String> columnAliasMap = Maps.newHashMap();
    String selectAndFromSql = getSelectAndFromSql(cohortReview, annotationQuery.getColumns(),
        annotationDefinitions, columnAliasMap, parameters);
    String whereSql = getWhereSql(cohortReview, statusFilter, parameters);
    String orderBySql = getOrderBySql(annotationQuery.getOrderBy(), columnAliasMap);
    String limitAndOffsetSql = getLimitAndOffsetSql(limit, offset, parameters);
        StringBuilder sqlBuilder = new StringBuilder(selectAndFromSql);
    sqlBuilder.append(whereSql);
    sqlBuilder.append(orderBySql);
    sqlBuilder.append(limitAndOffsetSql);
    return sqlBuilder.toString();
  }

  public Iterable<Map<String, Object>> materializeAnnotationQuery(CohortReview cohortReview,
      List<CohortStatus> statusFilter,
      AnnotationQuery annotationQuery, int limit, long offset) {
    Map<String, CohortAnnotationDefinition> annotationDefinitions = null;
    List<String> columns = annotationQuery.getColumns();
    if (columns == null || columns.isEmpty()) {
      // By default get person_id, review_status, and all the annotation definitions.
      columns = new ArrayList<>();
      columns.add(PERSON_ID_COLUMN);
      columns.add(REVIEW_STATUS_COLUMN);
      annotationDefinitions = getAnnotationDefinitions(cohortReview);
      columns.addAll(annotationDefinitions.values().stream()
          .map(CohortAnnotationDefinition::getColumnName).collect(Collectors.toList()));
      annotationQuery.setColumns(columns);
    }
    List<String> orderBy = annotationQuery.getOrderBy();
    if (orderBy == null || orderBy.isEmpty()) {
      annotationQuery.setOrderBy(ImmutableList.of(PERSON_ID_COLUMN));
    }
    ImmutableMap.Builder<String, Object> parameters = ImmutableMap.builder();
    String sql = getSql(cohortReview, statusFilter, annotationQuery, limit, offset,
        annotationDefinitions, parameters);
    return namedParameterJdbcTemplate.query(sql, parameters.build(),
        new RowMapper<Map<String, Object>>() {
      @Override
      public Map<String, Object> mapRow(ResultSet rs, int rowNum) throws SQLException {
        ImmutableMap.Builder<String, Object> result = ImmutableMap.builder();
        List<String> columns = annotationQuery.getColumns();
        for (int i = 0; i < columns.size(); i++) {
          Object obj = rs.getObject(i + 1);
          if (obj != null) {
            String column = columns.get(i);
            if (column.equals(REVIEW_STATUS_COLUMN)) {
              result.put(column, CohortStatus.values()[(Integer) obj].name());
            } else if (obj instanceof java.sql.Date) {
              result.put(column, DATE_FORMAT.format((java.sql.Date) obj));
            } else {
              result.put(column, obj);
            }
          }
        }
        return result.build();
      }
    });
  }
}
