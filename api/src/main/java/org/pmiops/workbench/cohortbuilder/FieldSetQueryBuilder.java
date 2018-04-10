package org.pmiops.workbench.cohortbuilder;

import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.pmiops.workbench.cohortbuilder.QueryConfiguration.ColumnInfo;
import org.pmiops.workbench.config.CdrBigQuerySchemaConfig;
import org.pmiops.workbench.config.CdrBigQuerySchemaConfig.ColumnConfig;
import org.pmiops.workbench.config.CdrBigQuerySchemaConfig.ColumnType;
import org.pmiops.workbench.config.CdrBigQuerySchemaConfig.TableConfig;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.model.ColumnFilter;
import org.pmiops.workbench.model.FieldSet;
import org.pmiops.workbench.model.Operator;
import org.pmiops.workbench.model.ResultFilters;
import org.pmiops.workbench.model.TableQuery;
import org.pmiops.workbench.utils.OperatorUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Builds queries based of a {@link FieldSet} and {@link ParticipantCriteria}.
 */
// TODO: figure out how to return nicer error messages to users for bad queries
// TODO: consider whether we want to impose limits on number of columns, joins, etc. requested
@Service
public class FieldSetQueryBuilder {

  private static final String TABLE_SEPARATOR = ".";
  private static final String ALIAS_SEPARATOR = "_";
  private static final String DESCENDING_PREFIX = "DESCENDING(";
  private static final String DATE_FORMAT_PATTERN = "yyyy-MM-dd";
  private static final String DATE_TIME_FORMAT_PATTERN = "yyyy-MM-dd HH:mm:ss zzz";

  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(DATE_FORMAT_PATTERN);
  private static final DateTimeFormatter DATE_TIME_FORMAT =
      DateTimeFormat.forPattern(DATE_TIME_FORMAT_PATTERN).withZoneUTC();

  private final CohortQueryBuilder cohortQueryBuilder;

  /**
   * A container for state needed to turn a query into SQL. We pass this around to avoid having
   * to pass around a whole bunch of arguments.
   */
  private static final class QueryState {
    // SQL for the from clause
    private StringBuilder fromSql = new StringBuilder();
    // A map of parameter values for bound parameters referenced in the query
    private Map<String, QueryParameterValue> paramMap = new HashMap<>();
    // A map from table name to (column name -> ColumnConfig) for tables referenced in the query.
    private Map<String, Map<String, ColumnConfig>> columnConfigTable = new HashMap<>();
    // A map from table aliases to SQL table names.
    private Map<String, String> aliasToTableName = new HashMap<>();
    // The name of the main table.
    private String mainTableName;
    // The CDR schema configuration.
    private CdrBigQuerySchemaConfig schemaConfig;
    // A map of (column name -> ColumnConfig) for the columns on the main table (pulled from
    // columnConfigTable).
    private Map<String, ColumnConfig> mainTableColumns;
  }

  private static final class TableNameAndAlias {
    private final String tableName;
    private final String alias;

    TableNameAndAlias(String tableName, String alias) {
      this.tableName = tableName;
      this.alias = alias;
    }
  }

  @Autowired
  public FieldSetQueryBuilder(CohortQueryBuilder cohortQueryBuilder) {
    this.cohortQueryBuilder = cohortQueryBuilder;
  }

  private ColumnConfig findPrimaryKey(Iterable<ColumnConfig> columnConfigs) {
    for (ColumnConfig columnConfig : columnConfigs) {
      if (columnConfig.primaryKey != null && columnConfig.primaryKey) {
        return columnConfig;
      }
    }
    throw new IllegalStateException("Table lacks primary key!");
  }

  private List<String> parseColumnName(String columnName) {
    return Splitter.on(TABLE_SEPARATOR).splitToList(columnName);
  }

  private String toTableAlias(List<String> columnParts, int endIndex) {
    return Joiner.on(ALIAS_SEPARATOR).join(columnParts.subList(0, endIndex));
  }

  private String getForeignKeyColumn(String columnPart) {
    return columnPart + "_id";
  }

  private Map<String, ColumnConfig> getColumnConfigs(
      QueryState queryState, String tableName, boolean tableNeedsPersonId) {
    Map<String, ColumnConfig> configTable = queryState.columnConfigTable.get(tableName);
    if (configTable == null) {
      TableConfig tableConfig = queryState.schemaConfig.cohortTables.get(tableName);
      if (tableConfig == null) {
        if (tableNeedsPersonId) {
          throw new BadRequestException("Not a valid cohort table (lacks person_id column): " + tableName);
        }
        tableConfig = queryState.schemaConfig.metadataTables.get(tableName);
        if (tableConfig == null) {
          throw new BadRequestException("Table not found: " + tableName);
        }
      }
      configTable = new HashMap<>();
      queryState.columnConfigTable.put(tableName, configTable);
      for (ColumnConfig columnConfig : tableConfig.columns) {
        configTable.put(columnConfig.name, columnConfig);
      }
    }
    return configTable;
  }

  private TableNameAndAlias getTableNameAndAlias(List<String> columnParts, QueryState queryState) {
    String tableName = queryState.mainTableName;
    String tableAlias = tableName;
    int i;
    Map<String, ColumnConfig> tableColumns = queryState.mainTableColumns;
    // Look for the longest already-joined table alias
    for (i = columnParts.size() - 1; i > 0; i--) {
      String alias = toTableAlias(columnParts, i);
      tableName = queryState.aliasToTableName.get(alias);
      if (tableName != null) {
        tableAlias = alias;
        tableColumns = getColumnConfigs(queryState, tableName, false);
        break;
      }
    }
    // Add in all the necessary remaining join tables starting from index i.
    for (int j = i; j < columnParts.size() - 1; j++) {
      String columnPart = columnParts.get(j);
      String foreignKeyColumn = getForeignKeyColumn(columnPart);
      ColumnConfig foreignKeyColumnConfig = tableColumns.get(foreignKeyColumn);
      if (foreignKeyColumnConfig == null) {
        throw new BadRequestException("No foreign key column found: " + foreignKeyColumn);
      }
      String foreignKeyTable = foreignKeyColumnConfig.foreignKey;
      if (foreignKeyTable == null) {
        throw new BadRequestException("Column is not a foreign key: " + foreignKeyColumn);
      }
      tableColumns = getColumnConfigs(queryState, foreignKeyTable, false);
      ColumnConfig foreignKeyTablePrimaryKey = findPrimaryKey(tableColumns.values());

      String foreignKeyAlias = toTableAlias(columnParts, j + 1);
      queryState.fromSql.append(String.format(
          "\nLEFT OUTER JOIN `${projectId}.${dataSetId}.%s` %s ON %s.%s = %s.%s",
          foreignKeyTable, foreignKeyAlias, tableAlias, foreignKeyColumn, foreignKeyAlias,
          foreignKeyTablePrimaryKey.name));
      queryState.aliasToTableName.put(foreignKeyAlias, foreignKeyTable);
      tableAlias = foreignKeyAlias;
      tableName = foreignKeyTable;
    }
    return new TableNameAndAlias(tableName, tableAlias);
  }

  private String handleSelect(QueryState queryState, List<String> columnNames,
      ImmutableList.Builder<ColumnInfo> selectColumns) {
    StringBuilder selectSql = new StringBuilder();
    String tableName = queryState.mainTableName;
    selectSql.append("select ");

    queryState.fromSql.append(String.format("\nfrom `${projectId}.${dataSetId}.%s` %s",
        tableName, tableName));
    queryState.mainTableColumns = getColumnConfigs(queryState, tableName, true);
    boolean first = true;
    for (String columnName : columnNames) {
      if (first) {
        first = false;
      } else {
        selectSql.append(", ");
      }
      List<String> columnParts = parseColumnName(columnName);
      if (columnParts.size() == 1) {
        ColumnConfig columnConfig = queryState.mainTableColumns.get(columnName);
        if (columnConfig == null) {
          throw new BadRequestException(
              String.format("No column %s found on table %s", tableName, columnName));
        }
        selectSql.append(String.format("%s.%s %s",
            tableName, columnName, columnName));
        selectColumns.add(new ColumnInfo(columnName, columnConfig));
      } else {

        TableNameAndAlias tableNameAndAlias = getTableNameAndAlias(columnParts,
            queryState);
        Map<String, ColumnConfig> aliasConfig = getColumnConfigs(queryState,
            tableNameAndAlias.tableName, false);
        String columnEnd = columnParts.get(columnParts.size() - 1);
        ColumnConfig columnConfig = aliasConfig.get(columnEnd);
        if (columnConfig == null) {
          throw new BadRequestException(
              String.format("No column %s found on table %s", tableNameAndAlias.tableName,
                  columnEnd));
        }
        selectSql.append(String.format("%s.%s %s_%s",
            tableNameAndAlias.alias, columnEnd,
            tableNameAndAlias.alias, columnEnd));
        selectColumns.add(new ColumnInfo(columnName, columnConfig));
      }
    }
    return selectSql.toString();
  }

  private void handleComparison(ColumnFilter columnFilter, ColumnInfo columnInfo,
      QueryState queryState, StringBuilder whereSql) {
    ColumnConfig columnConfig = columnInfo.getColumnConfig();
    String paramName = "p" + queryState.paramMap.size();
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
      queryState.paramMap.put(paramName, QueryParameterValue.string(columnFilter.getValue()));
    } else if (columnFilter.getValueDate() != null) {
      if (columnConfig.type.equals(ColumnType.DATE)) {
        try {
          DATE_FORMAT.parse(columnFilter.getValueDate());
          queryState.paramMap.put(paramName, QueryParameterValue.date(columnFilter.getValueDate()));
        } catch (ParseException e) {
          throw new BadRequestException("Couldn't parse date value " + columnFilter.getValueDate()
              + "; expected format: " + DATE_FORMAT_PATTERN);
        }
      } else if (columnConfig.type.equals(ColumnType.TIMESTAMP)) {
        try {
          long timestamp = DATE_TIME_FORMAT.parseDateTime(columnFilter.getValueDate()).getMillis() * 1000;
          queryState.paramMap.put(paramName, QueryParameterValue.timestamp(timestamp));
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
        queryState.paramMap.put(paramName, QueryParameterValue.float64(columnFilter.getValueNumber().doubleValue()));
      } else if (columnConfig.type.equals(ColumnType.INTEGER)) {
        queryState.paramMap.put(paramName, QueryParameterValue.int64(columnFilter.getValueNumber().longValue()));
      } else {
        throw new BadRequestException("Can't use valueNumber with column " + columnConfig.name
            + " of type " + columnConfig.type);
      }
    } else if (columnFilter.getValueNull() != null && columnFilter.getValueNull()) {
      if (operator != Operator.EQUAL && operator != Operator.NOT_EQUAL) {
        throw new BadRequestException("Unsupported operator for valueNull: " + operator);
      }
      whereSql.append(columnInfo.getColumnName());
      if (operator.equals(Operator.EQUAL)) {
        whereSql.append(" is null\n");
      } else {
        whereSql.append(" is not null\n");
      }
      return;
    }
    whereSql.append(String.format("%s %s @%s",
        columnInfo.getColumnName(), OperatorUtils.getSqlOperator(columnFilter.getOperator()),
        paramName));
  }

  private void handleInClause(ColumnFilter columnFilter, ColumnInfo columnInfo,
      QueryState queryState, StringBuilder whereSql) {
    ColumnConfig columnConfig = columnInfo.getColumnConfig();
    String paramName = "p" + queryState.paramMap.size();
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
      queryState.paramMap.put(paramName, QueryParameterValue.array(
          valueNumbers.stream().map(BigDecimal::longValue).collect(
              Collectors.toList()).toArray(new Long[0]),
          Long.class));
    } else {
      if (!columnConfig.type.equals(ColumnType.STRING)) {
        throw new BadRequestException("Can't use values with column " + columnConfig.name
            + " of type " + columnConfig.type);
      }
      queryState.paramMap.put(paramName, QueryParameterValue.array(valueStrings.toArray(new String[0]),
          String.class));
    }
    whereSql.append(String.format("%s in unnest(@%s)",
        columnInfo.getColumnName(), paramName));
  }

  private ColumnInfo getColumnInfo(QueryState queryState, String columnName) {
    List<String> columnParts = parseColumnName(columnName);
    ColumnConfig columnConfig;
    if (columnParts.size() == 1) {
      columnConfig = queryState.mainTableColumns.get(columnName);
      if (columnConfig == null) {
        throw new BadRequestException("No such column " + columnName +
            "on table " + queryState.mainTableName);
      }
      return new ColumnInfo(columnName, columnConfig);
    } else {
      TableNameAndAlias tableNameAndAlias = getTableNameAndAlias(columnParts, queryState);
      Map<String, ColumnConfig> aliasConfig = getColumnConfigs(queryState,
          tableNameAndAlias.tableName, false);
      String columnEnd = columnParts.get(columnParts.size() - 1);
      columnConfig = aliasConfig.get(columnEnd);
      if (columnConfig == null) {
        throw new BadRequestException(
            String.format("No column %s found on table %s", tableNameAndAlias.tableName,
                columnEnd));
      }
      return new ColumnInfo(
          String.format("%s.%s", tableNameAndAlias.alias, columnEnd),
          columnConfig);
    }
  }

  private void handleColumnFilter(ColumnFilter columnFilter, QueryState queryState,
      StringBuilder whereSql) {
    if (Strings.isNullOrEmpty(columnFilter.getColumnName())) {
      throw new BadRequestException("Missing column name for column filter");
    }

    ColumnInfo columnInfo = getColumnInfo(queryState, columnFilter.getColumnName());

    if (columnFilter.getOperator() == null) {
      columnFilter.setOperator(Operator.EQUAL);
    }

    if (columnFilter.getOperator().equals(Operator.IN)) {
      handleInClause(columnFilter, columnInfo, queryState, whereSql);
    } else {
      handleComparison(columnFilter, columnInfo, queryState, whereSql);
    }
  }

  private void handleResultFilters(ResultFilters resultFilters, QueryState queryState,
      StringBuilder whereSql) {
    if (!((resultFilters.getColumnFilter() != null) ^ (resultFilters.getAllOf() != null)
        ^ (resultFilters.getAnyOf() != null))) {
      throw new BadRequestException("Exactly one of allOf, anyOf, or columnFilter must be "
          + "specified for result filters");
    }
    if (resultFilters.getNot() != null && resultFilters.getNot()) {
      whereSql.append("not ");
    }
    if (resultFilters.getColumnFilter() != null) {
      handleColumnFilter(resultFilters.getColumnFilter(), queryState, whereSql);
    } else {
      String operator;
      List<ResultFilters> childFilters;
      if (resultFilters.getAllOf() != null) {
        operator = "and";
        childFilters = resultFilters.getAllOf();
      } else {
        operator = "or";
        childFilters = resultFilters.getAnyOf();
      }
      whereSql.append("(");
      boolean first = true;
      for (ResultFilters childFilter : childFilters) {
        if (first) {
          first = false;
        } else {
          whereSql.append("\n");
          whereSql.append(operator);
          whereSql.append("\n");
        }
        handleResultFilters(childFilter, queryState, whereSql);
      }
      whereSql.append(")\n");
    }
  }

  private StringBuilder handleOrderBy(QueryState queryState, List<String> orderBy) {
    if (orderBy.isEmpty()) {
      throw new BadRequestException("Order by list must not be empty");
    }
    List<String> orderByColumns = new ArrayList<>();
    for (String columnName : orderBy) {
      String columnStart = columnName;
      boolean descending = false;
      if (columnName.toUpperCase().startsWith(DESCENDING_PREFIX) && columnName.endsWith(")")) {
        columnStart = columnName.substring(DESCENDING_PREFIX.length(), columnName.length() - 1);
        descending = true;
      }
      ColumnInfo columnInfo = getColumnInfo(queryState, columnStart);
      orderByColumns.add(descending ? columnInfo.getColumnName() + " DESC"
          : columnInfo.getColumnName());
    }
    StringBuilder orderBySql = new StringBuilder("order by ");
    orderBySql.append(Joiner.on(", ").join(orderByColumns));
    return orderBySql;
  }

  public QueryConfiguration buildQuery(ParticipantCriteria participantCriteria,
      TableQueryAndConfig tableQueryAndConfig, long resultSize, long offset) {
    QueryState queryState = new QueryState();
    queryState.schemaConfig = tableQueryAndConfig.getConfig();
    TableQuery tableQuery = tableQueryAndConfig.getTableQuery();
    queryState.mainTableName = tableQuery.getTableName();

    ImmutableList.Builder<ColumnInfo> selectColumns = ImmutableList.builder();
    String selectSql = handleSelect(queryState, tableQuery.getColumns(), selectColumns);

    StringBuilder whereSql = new StringBuilder("\nwhere\n");

    if (tableQuery.getFilters() != null) {
      handleResultFilters(tableQuery.getFilters(), queryState, whereSql);
      whereSql.append("\nand\n");
    }
    StringBuilder endSql = handleOrderBy(queryState, tableQuery.getOrderBy());
    endSql.append(" limit ");
    endSql.append(resultSize);
    if (offset > 0) {
      endSql.append(" offset ");
      endSql.append(offset);
    }
    QueryJobConfiguration jobConfiguration = cohortQueryBuilder.buildQuery(participantCriteria,
        selectSql + queryState.fromSql.toString() + whereSql.toString(),
        endSql.toString(),
        queryState.mainTableName, queryState.paramMap);
    return new QueryConfiguration(selectColumns.build(), jobConfiguration);
  }

  public Map<String, Object> extractResults(TableQueryAndConfig tableQueryAndConfig,
      ImmutableList<ColumnInfo> columns,
      List<FieldValue> row) {
    TableQuery tableQuery = tableQueryAndConfig.getTableQuery();
    Map<String, Object> results = new HashMap<>(tableQuery.getColumns().size());
    for (int i = 0; i < columns.size(); i++) {
      FieldValue fieldValue = row.get(i);
      ColumnInfo columnInfo = columns.get(i);
      ColumnConfig columnConfig = columnInfo.getColumnConfig();
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
        results.put(columnInfo.getColumnName(), value);
      }
    }
    return results;
  }
}
