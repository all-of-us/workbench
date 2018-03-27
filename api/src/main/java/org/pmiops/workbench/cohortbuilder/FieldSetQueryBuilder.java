package org.pmiops.workbench.cohortbuilder;

import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
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
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Builds queries based of a {@link FieldSet} and {@link ParticipantCriteria}.
 */
// TODO: figure out how to return nicer error messages to users for bad queries
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

  private ColumnConfig findPrimaryKey(TableConfig tableConfig) {
    for (ColumnConfig columnConfig : tableConfig.columns) {
      if (columnConfig.primaryKey) {
        return columnConfig;
      }
    }
    throw new IllegalStateException("Table lacks primary key!");
  }

  private ColumnConfig findColumn(TableConfig tableConfig, String columnName) {
    for (ColumnConfig columnConfig : tableConfig.columns) {
      if (columnConfig.name.equals(columnName)) {
        return columnConfig;
      }
    }
    return null;
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
    sqlBuilder.append(columnFilter.getOperator());
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

  private void handleColumnFilter(ColumnFilter columnFilter, TableConfig tableConfig,
      StringBuilder sqlBuilder, Map<String, QueryParameterValue> paramMap) {
    if (Strings.isNullOrEmpty(columnFilter.getColumnName())) {
      throw new BadRequestException("Missing column name for column filter");
    }
    ColumnConfig columnConfig = findColumn(tableConfig, columnFilter.getColumnName());
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

  private void handleColumnFilters(List<ColumnFilter> columnFilters, TableConfig tableConfig,
      StringBuilder sqlBuilder, Map<String, QueryParameterValue> paramMap) {
    boolean first = false;
    if (columnFilters.isEmpty()) {
      throw new BadRequestException("Empty column filter list is invalid");
    }
    if (columnFilters.size() == 1) {
      handleColumnFilter(columnFilters.get(0), tableConfig, sqlBuilder, paramMap);
    } else {
      sqlBuilder.append("(");
      for (ColumnFilter columnFilter : columnFilters) {
        if (first) {
          first = false;
        } else {
          sqlBuilder.append("\nand\n");
        }
        handleColumnFilter(columnFilters.get(0), tableConfig, sqlBuilder, paramMap);
      }
      sqlBuilder.append(")");
    }
  }

  public QueryJobConfiguration buildQuery(ParticipantCriteria participantCriteria, FieldSet fieldSet,
      long resultSize, long offset) {
    CdrSchemaConfig schemaConfig = cdrSchemaConfigProvider.get();
    TableQuery tableQuery = fieldSet.getTableQuery();
    if (tableQuery == null) {
      // TODO: support other kinds of field sets
      throw new BadRequestException("tableQuery must be specified in field sets");
    }
    String tableName = tableQuery.getTableName();
    if (Strings.isNullOrEmpty(tableName)) {
      throw new BadRequestException("Table name must be specified in field sets");
    }
    TableConfig tableConfig = schemaConfig.cohortTables.get(tableName);
    if (tableConfig == null) {
      throw new BadRequestException("Table " + tableName + " not found in cohort tables");
    }
    List<String> tableColumnNames = tableConfig.columns.stream()
        .map(columnConfig -> columnConfig.name).collect(Collectors.toList());
    List<String> columnNames = tableQuery.getColumns();
    if (columnNames == null || columnNames.isEmpty()) {
      // By default, return all columns on the table in question in our configuration.
      columnNames = tableColumnNames;
    } else {
      for (String columnName : columnNames) {
        // TODO: handle columns on foreign key tables
        if (!tableColumnNames.contains(columnName)) {
          throw new BadRequestException("Unrecognized column name: " + columnName);
        }
      }
    }
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
        handleColumnFilters(columnFilters.get(0), tableConfig, startSql, paramMap);
      } else {
        startSql.append("(");
        boolean first = true;
        for (List<ColumnFilter> filterList : columnFilters) {
          if (first) {
            first = false;
          } else {
            startSql.append("\nor\n");
          }
          handleColumnFilters(filterList, tableConfig, startSql, paramMap);
        }
        startSql.append(")");
      }
      startSql.append("\nand\n");
    }


    StringBuilder endSql = new StringBuilder();


    return participantCounter.buildQuery(participantCriteria, startSql.toString(), endSql.toString());
  }
}
