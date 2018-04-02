package org.pmiops.workbench.cohortbuilder;

import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.pmiops.workbench.config.CdrBigQuerySchemaConfig.ColumnConfig;
import org.pmiops.workbench.config.CdrBigQuerySchemaConfig.ColumnType;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.model.ColumnFilter;
import org.pmiops.workbench.model.FieldSet;
import org.pmiops.workbench.model.Operator;
import org.pmiops.workbench.model.TableQuery;
import org.pmiops.workbench.utils.OperatorUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Builds queries based of a {@link FieldSet} and {@link ParticipantCriteria}.
 */
// TODO: figure out how to return nicer error messages to users for bad queries
@Service
public class FieldSetQueryBuilder {

  private static final String DATE_FORMAT_PATTERN = "yyyy-MM-dd";
  private static final String DATE_TIME_FORMAT_PATTERN = "yyyy-MM-dd HH:mm:ss zzz";

  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(DATE_FORMAT_PATTERN);
  private static final DateTimeFormatter DATE_TIME_FORMAT =
      DateTimeFormat.forPattern(DATE_TIME_FORMAT_PATTERN).withZoneUTC();

  private final ParticipantCounter participantCounter;

  @Autowired
  public FieldSetQueryBuilder(ParticipantCounter participantCounter) {
    this.participantCounter = participantCounter;
  }

  private void handleComparison(ColumnFilter columnFilter, ColumnConfig columnConfig,
      StringBuilder sqlBuilder, Map<String, QueryParameterValue> paramMap) {
    String paramName = "p" + paramMap.size();
    Operator operator = columnFilter.getOperator();
    if (columnFilter.getValueNumbers() != null || columnFilter.getValues() != null) {
      throw new BadRequestException("Can't use valueNumbers or values with operator " + operator);
    }
    if (!((columnFilter.getValue() != null)
        ^ (columnFilter.getValueDate() != null)
        ^ (columnFilter.getValueNumber() != null)
        ^ (columnFilter.getValueNull() != null && columnFilter.getValueNull()))) {
      throw new BadRequestException("Exactly one of value, valueDate, valueNumber, and valueNull "
          + "must be specified for filter on column " + columnConfig.name);
    }
    if (operator.equals(Operator.LIKE) && columnFilter.getValue() == null) {
      throw new BadRequestException("LIKE operator only support with value");
    }
    if (columnFilter.getValue() != null) {
      if (!columnConfig.type.equals(ColumnType.STRING)) {
        throw new BadRequestException("Can't use value with column " + columnConfig.name
            + " of type " + columnConfig.type);
      }
      paramMap.put(paramName, QueryParameterValue.string(columnFilter.getValue()));
    } else if (columnFilter.getValueDate() != null) {
      if (columnConfig.type.equals(ColumnType.DATE)) {
        try {
          DATE_FORMAT.parse(columnFilter.getValueDate());
          paramMap.put(paramName, QueryParameterValue.date(columnFilter.getValueDate()));
        } catch (ParseException e) {
          throw new BadRequestException("Couldn't parse date value " + columnFilter.getValueDate()
              + "; expected format: " + DATE_FORMAT_PATTERN);
        }
      } else if (columnConfig.type.equals(ColumnType.TIMESTAMP)) {
        try {
          long timestamp = DATE_TIME_FORMAT.parseDateTime(columnFilter.getValueDate()).getMillis() * 1000;
          paramMap.put(paramName, QueryParameterValue.timestamp(timestamp));
        } catch (IllegalArgumentException e) {
          throw new BadRequestException("Couldn't parse timestamp value " +
              columnFilter.getValueDate() + "; expected format: " + DATE_TIME_FORMAT_PATTERN);
        }
      } else {
        throw new BadRequestException("Can't use valueDate with column " + columnConfig.name
            + " of type " + columnConfig.type);
      }
    } else if (columnFilter.getValueNumber() != null) {
      if (columnConfig.type.equals(ColumnType.FLOAT)) {
        paramMap.put(paramName, QueryParameterValue.float64(columnFilter.getValueNumber().doubleValue()));
      } else if (columnConfig.type.equals(ColumnType.INTEGER)) {
        paramMap.put(paramName, QueryParameterValue.int64(columnFilter.getValueNumber().longValue()));
      } else {
        throw new BadRequestException("Can't use valueNumber with column " + columnConfig.name
            + " of type " + columnConfig.type);
      }
    } else if (columnFilter.getValueNull() != null && columnFilter.getValueNull()) {
      if (operator != Operator.EQUAL) {
        throw new BadRequestException("Unsupported operator for valueNull: " + operator);
      }
      sqlBuilder.append(columnFilter.getColumnName());
      sqlBuilder.append(" is null\n");
      return;
    }
    sqlBuilder.append(columnFilter.getColumnName());
    sqlBuilder.append(' ');
    sqlBuilder.append(OperatorUtils.getSqlOperator(columnFilter.getOperator()));
    sqlBuilder.append(" @");
    sqlBuilder.append(paramName);
    sqlBuilder.append("\n");
  }

  private void handleInClause(ColumnFilter columnFilter, ColumnConfig columnConfig,
      StringBuilder sqlBuilder, Map<String, QueryParameterValue> paramMap) {
    String paramName = "p" + paramMap.size();
    if (columnFilter.getValue() != null || columnFilter.getValueNumber() != null
        || columnFilter.getValueDate() != null || columnFilter.getValueNull() != null) {
      throw new BadRequestException("Can't use IN operator with single value filter");
    }
    List<BigDecimal> valueNumbers = columnFilter.getValueNumbers();
    List<String> valueStrings = columnFilter.getValues();
    if (!((valueNumbers != null && !valueNumbers.isEmpty())
        ^ (valueStrings != null && !valueStrings.isEmpty()))) {
      throw new BadRequestException("Either valueNumbers or valueStrings must be specified with "
          + "in clause on column " + columnFilter.getColumnName());
    }

    if (valueNumbers != null && !valueNumbers.isEmpty()) {
      if (!columnConfig.type.equals(ColumnType.INTEGER)) {
        throw new BadRequestException("Can't use valueNumbers with column " + columnConfig.name
            + " of type " + columnConfig.type);
      }
      paramMap.put(paramName, QueryParameterValue.array(
          valueNumbers.stream().map(BigDecimal::longValue).collect(
              Collectors.toList()).toArray(new Long[0]),
          Long.class));
    } else {
      if (!columnConfig.type.equals(ColumnType.STRING)) {
        throw new BadRequestException("Can't use values with column " + columnConfig.name
            + " of type " + columnConfig.type);
      }
      paramMap.put(paramName, QueryParameterValue.array(valueStrings.toArray(new String[0]),
          String.class));
    }
    sqlBuilder.append(columnFilter.getColumnName());
    sqlBuilder.append(" in unnest(@");
    sqlBuilder.append(paramName);
    sqlBuilder.append(")\n");

  }

  private void handleColumnFilter(ColumnFilter columnFilter, TableQueryAndConfig tableQueryAndConfig,
      StringBuilder sqlBuilder, Map<String, QueryParameterValue> paramMap) {
    if (Strings.isNullOrEmpty(columnFilter.getColumnName())) {
      throw new BadRequestException("Missing column name for column filter");
    }
    ColumnConfig columnConfig = tableQueryAndConfig.getColumn(columnFilter.getColumnName());
    if (columnConfig == null) {
      // TODO: consider having link to documentation or valid column expressions here
      throw new BadRequestException("Column " + columnFilter.getColumnName() + " does not exist");
    }
    if (columnFilter.getOperator() == null) {
      columnFilter.setOperator(Operator.EQUAL);
    }

    if (columnFilter.getOperator().equals(Operator.IN)) {
      handleInClause(columnFilter, columnConfig, sqlBuilder, paramMap);
    } else {
      handleComparison(columnFilter, columnConfig, sqlBuilder, paramMap);
    }
  }

  private void handleColumnFilters(List<ColumnFilter> columnFilters,
      TableQueryAndConfig tableQueryAndConfig,
      StringBuilder sqlBuilder, Map<String, QueryParameterValue> paramMap) {
    if (columnFilters.isEmpty()) {
      throw new BadRequestException("Empty column filter list is invalid");
    }
    sqlBuilder.append("(");
    boolean first = true;
    for (ColumnFilter columnFilter : columnFilters) {
      if (first) {
        first = false;
      } else {
        sqlBuilder.append("\nand\n");
      }
      handleColumnFilter(columnFilter, tableQueryAndConfig, sqlBuilder, paramMap);
    }
    sqlBuilder.append(")");
  }

  public QueryJobConfiguration buildQuery(ParticipantCriteria participantCriteria,
      TableQueryAndConfig tableQueryAndConfig, long resultSize, long offset) {
    TableQuery tableQuery = tableQueryAndConfig.getTableQuery();
    List<String> columnNames = tableQuery.getColumns();
    String tableName = tableQuery.getTableName();

    StringBuilder startSql = new StringBuilder("select ");
    // TODO: add column aliases, use below
    startSql.append(Joiner.on(", ").join(columnNames));
    startSql.append("\nfrom `${projectId}.${dataSetId}.");
    startSql.append(tableName);
    startSql.append("` ");
    startSql.append(tableName);
    startSql.append("\nwhere\n");

    Map<String, QueryParameterValue> paramMap = new HashMap<>();
    List<List<ColumnFilter>> columnFilters = tableQuery.getFilters();
    if (columnFilters != null && !columnFilters.isEmpty()) {
     startSql.append("(");
      boolean first = true;
      for (List<ColumnFilter> filterList : columnFilters) {
        if (first) {
          first = false;
        } else {
          startSql.append("\nor\n");
        }
        handleColumnFilters(filterList, tableQueryAndConfig, startSql, paramMap);
      }
      startSql.append(")\nand\n");
    }

    StringBuilder endSql = new StringBuilder("order by ");
    List<String> orderBy = tableQuery.getOrderBy();
    if (orderBy.isEmpty()) {
      throw new BadRequestException("Order by list must not be empty");
    }
    endSql.append(Joiner.on(", ").join(orderBy));
    endSql.append(" limit ");
    endSql.append(resultSize);
    if (offset > 0) {
      endSql.append(" offset ");
      endSql.append(offset);
    }
    return participantCounter.buildQuery(participantCriteria, startSql.toString(), endSql.toString(),
        tableName, paramMap);
  }

  public Map<String, Object> extractResults(TableQueryAndConfig tableQueryAndConfig,
      List<FieldValue> row) {
    TableQuery tableQuery = tableQueryAndConfig.getTableQuery();
     List<ColumnConfig> columnConfigs = tableQuery.getColumns().stream()
        .map(columnName -> tableQueryAndConfig.getColumn(columnName)).collect(Collectors.toList());
    Map<String, Object> results = new HashMap<>(tableQuery.getColumns().size());
    for (int i = 0; i < columnConfigs.size(); i++) {
      FieldValue fieldValue = row.get(i);
      ColumnConfig columnConfig = columnConfigs.get(i);
      if (!fieldValue.isNull()) {
        Object value;
        switch (columnConfig.type) {
          case DATE:
            value = fieldValue.getStringValue();
            break;
          case FLOAT:
            value = fieldValue.getDoubleValue();
            break;
          case INTEGER:
            value = fieldValue.getLongValue();
            break;
          case STRING:
            value = fieldValue.getStringValue();
            break;
          case TIMESTAMP:
            value = DATE_TIME_FORMAT.print(fieldValue.getTimestampValue() / 1000L);
            break;
          default:
            throw new IllegalStateException("Unrecognized column type: " + columnConfig.type);
        }
        results.put(columnConfig.name, value);
      }
    }
    return results;
  }
}
