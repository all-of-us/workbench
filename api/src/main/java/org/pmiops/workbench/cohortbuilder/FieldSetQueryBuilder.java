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
import javax.inject.Provider;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.pmiops.workbench.config.CdrSchemaConfig;
import org.pmiops.workbench.config.CdrSchemaConfig.ColumnConfig;
import org.pmiops.workbench.config.CdrSchemaConfig.ColumnType;
import org.pmiops.workbench.config.CdrSchemaConfig.TableConfig;
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
  private static final String DATE_TIME_FORMAT_PATTERN = "yyyy-MM-dd HH:mm:ss.SSSSSS";

  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(DATE_FORMAT_PATTERN);
  private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormat.forPattern(DATE_TIME_FORMAT_PATTERN);

  private final ParticipantCounter participantCounter;
  private final Provider<CdrSchemaConfig> cdrSchemaConfigProvider;

  @Autowired
  public FieldSetQueryBuilder(ParticipantCounter participantCounter,
      Provider<CdrSchemaConfig> cdrSchemaConfigProvider) {
    this.participantCounter = participantCounter;
    this.cdrSchemaConfigProvider = cdrSchemaConfigProvider;
  }


  private void handleComparison(ColumnFilter columnFilter, ColumnConfig columnConfig,
      StringBuilder sqlBuilder, Map<String, QueryParameterValue> paramMap) {
    String paramName = "p" + paramMap.size();
    Operator operator = columnFilter.getOperator();
    if (columnFilter.getValueNumbers() != null || columnFilter.getValues() != null) {
      throw new BadRequestException("Can't use valueNumbers or values with operator " + operator);
    }
    boolean foundIt = false;
    if (columnFilter.getValue() != null) {
      foundIt = true;
      if (!columnConfig.type.equals(ColumnType.STRING)) {
        throw new BadRequestException("Can't use value with column " + columnConfig.name
            + " of type " + columnConfig.type);
      }
      paramMap.put(paramName, QueryParameterValue.string(columnFilter.getValue()));
    }
    if (columnFilter.getValueDate() != null) {
      if (foundIt) {
        throw new BadRequestException("Can't specify multiple value columns");
      }
      foundIt = true;
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
          long timestamp = DATE_TIME_FORMAT.parseDateTime(columnFilter.getValueDate()).getMillis();
          paramMap.put(paramName, QueryParameterValue.timestamp(timestamp));
        } catch (IllegalArgumentException e) {
          throw new BadRequestException("Couldn't parse timestamp value " +
              columnFilter.getValueDate() + "; expected format: " + DATE_TIME_FORMAT_PATTERN);
        }
      } else {
        throw new BadRequestException("Can't use valueDate with column " + columnConfig.name
            + " of type " + columnConfig.type);
      }
    }
    if (columnFilter.getValueNumber() != null) {
      if (foundIt) {
        throw new BadRequestException("Can't specify multiple value columns");
      }
      foundIt = true;
      if (columnConfig.type.equals(ColumnType.FLOAT)) {
        paramMap.put(paramName, QueryParameterValue.float64(columnFilter.getValueNumber().doubleValue()));
      } else if (columnConfig.type.equals(ColumnType.INTEGER)) {
        paramMap.put(paramName, QueryParameterValue.int64(columnFilter.getValueNumber().longValue()));
      } else {
        throw new BadRequestException("Can't use valueNumber with column " + columnConfig.name
            + " of type " + columnConfig.type);
      }
    }
    if (columnFilter.getValueNull() != null && columnFilter.getValueNull()) {
      if (foundIt) {
        throw new BadRequestException("Can't specify multiple value columns");
      }
      sqlBuilder.append(columnFilter.getColumnName());
      sqlBuilder.append(" is null\n");
      return;
    }
    if (!foundIt) {
      throw new BadRequestException(
          "One of value, valueNumber, valueDate, or valueNull must be specified for filter on column " +
              columnFilter.getColumnName());
    }
    sqlBuilder.append(columnFilter.getColumnName());
    sqlBuilder.append(' ');
    sqlBuilder.append(OperatorUtils.getSqlOperator(columnFilter.getOperator()));
    sqlBuilder.append(" ${");
    sqlBuilder.append(paramName);
    sqlBuilder.append("}\n");
  }

  private void handleInClause(ColumnFilter columnFilter, ColumnConfig columnConfig,
      StringBuilder sqlBuilder, Map<String, QueryParameterValue> paramMap) {
    String paramName = "p" + paramMap.size();
    if (columnFilter.getValue() != null || columnFilter.getValueNumber() != null
        || columnFilter.getValueDate() != null || columnFilter.getValueNull() != null) {
      throw new BadRequestException("Can't use in operator with single value filter");
    }
    List<BigDecimal> valueNumbers = columnFilter.getValueNumbers();
    List<String> valueStrings = columnFilter.getValues();

    if (valueNumbers == null || valueNumbers.isEmpty()) {
      if (valueStrings == null || valueStrings.isEmpty()) {
        throw new BadRequestException("Can't use in operator without values or valueNumbers");
      } else {
        if (!columnConfig.type.equals(ColumnType.STRING)) {
          throw new BadRequestException("Can't use values with column " + columnConfig.name
              + " of type " + columnConfig.type);
        }
        paramMap.put(paramName, QueryParameterValue.array(valueStrings.toArray(new String[0]),
            String.class));
      }
    } else {
      if (valueStrings == null || valueStrings.isEmpty()) {
        if (!columnConfig.type.equals(ColumnType.INTEGER)) {
          throw new BadRequestException("Can't use valueNumbers with column " + columnConfig.name
              + " of type " + columnConfig.type);
        }
        paramMap.put(paramName, QueryParameterValue.array(
            valueNumbers.stream().map(BigDecimal::longValue).collect(
                Collectors.toList()).toArray(new Long[0]),
            Long.class));
      } else {
        throw new BadRequestException("Can't use both values and valueNumbers");
      }
    }
    sqlBuilder.append(columnFilter.getColumnName());
    sqlBuilder.append(" in unnest(${");
    sqlBuilder.append(paramName);
    sqlBuilder.append("})\n");

  }

  private void handleColumnFilter(ColumnFilter columnFilter, TableQueryAndConfig tableQueryAndConfig,
      StringBuilder sqlBuilder, Map<String, QueryParameterValue> paramMap) {
    if (Strings.isNullOrEmpty(columnFilter.getColumnName())) {
      throw new BadRequestException("Missing column name for column filter");
    }
    ColumnConfig columnConfig = tableQueryAndConfig.getColumn(columnFilter.getColumnName());
    if (columnConfig == null) {
      throw new BadRequestException("Column " + columnFilter.getColumnName() + " does not exist");
    }
    Operator operator = columnFilter.getOperator();
    if (operator == null) {
      operator = Operator.EQUAL;
    }

    if (operator.equals(Operator.IN)) {
      handleInClause(columnFilter, columnConfig, sqlBuilder, paramMap);
    } else {
      handleComparison(columnFilter, columnConfig, sqlBuilder, paramMap);
    }
  }

  private void handleColumnFilters(List<ColumnFilter> columnFilters,
      TableQueryAndConfig tableQueryAndConfig,
      StringBuilder sqlBuilder, Map<String, QueryParameterValue> paramMap) {
    boolean first = false;
    if (columnFilters.isEmpty()) {
      throw new BadRequestException("Empty column filter list is invalid");
    }
    if (columnFilters.size() == 1) {
      handleColumnFilter(columnFilters.get(0), tableQueryAndConfig, sqlBuilder, paramMap);
    } else {
      sqlBuilder.append("(");
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
  }

  public QueryJobConfiguration buildQuery(ParticipantCriteria participantCriteria,
      TableQueryAndConfig tableQueryAndConfig, long resultSize, long offset) {
    TableQuery tableQuery = tableQueryAndConfig.getTableQuery();
    TableConfig tableConfig = tableQueryAndConfig.getTableConfig();
    List<String> columnNames = tableQuery.getColumns();
    String tableName = tableQuery.getTableName();

    StringBuilder startSql = new StringBuilder("select ");
    startSql.append(Joiner.on(", ").join(columnNames));
    startSql.append("\nfrom `${projectId}.${dataSetId}.");
    startSql.append(tableName);
    startSql.append("` ");
    startSql.append(tableName);
    startSql.append("\nwhere\n");

    Map<String, QueryParameterValue> paramMap = new HashMap<>();
    List<List<ColumnFilter>> columnFilters = tableQuery.getFilters();
    if (columnFilters != null && !columnFilters.isEmpty()) {
      if (columnFilters.size() == 1) {
        handleColumnFilters(columnFilters.get(0), tableQueryAndConfig, startSql, paramMap);
      } else {
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
        startSql.append(")");
      }
      startSql.append("\nand\n");
    }

    StringBuilder endSql = new StringBuilder("order by ");
    List<String> orderBy = tableQuery.getOrderBy();
    endSql.append(Joiner.on(", ").join(orderBy));
    endSql.append(" limit ");
    endSql.append(resultSize);
    if (offset > 0) {
      endSql.append(" offset ");
      endSql.append(offset);
    }
    return participantCounter.buildQuery(participantCriteria, startSql.toString(), endSql.toString(),
        paramMap);
  }

  public Map<String, Object> extractResults(TableQueryAndConfig tableQueryAndConfig,
      List<FieldValue> row) {
    TableQuery tableQuery = tableQueryAndConfig.getTableQuery();
    TableConfig tableConfig = tableQueryAndConfig.getTableConfig();
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
            value = DATE_TIME_FORMAT.print(fieldValue.getLongValue());
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
