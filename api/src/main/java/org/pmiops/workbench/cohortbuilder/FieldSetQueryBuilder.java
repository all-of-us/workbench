package org.pmiops.workbench.cohortbuilder;

import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.QueryResult;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.pmiops.workbench.api.BigQueryService;
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
  private final BigQueryService bigQueryService;

  private static final class JoinedTableInfo {

    private String startTableAlias;
    private String startTableJoinColumn;

    private String joinedTableName;
    private ColumnConfig joinedTablePrimaryKey;

    // True if this table is referenced in the where clause or order by clause,
    // and thus must be joined to before a LIMIT is applied.
    private boolean beforeLimitRequired;
  }

  private static final class SelectedColumn {

    // The table alias for the table this column lives on.
    private String tableAlias;
    // The alias for the column.
    private String columnAlias;

    private ColumnInfo columnInfo;

    private ColumnInfo getColumnInfo() {
      return columnInfo;
    }
  }

  private static final class OrderByColumn {
    private ColumnInfo columnInfo;
    private boolean descending;
  }

  /**
   * A container for state needed to turn a query into SQL. We pass this around to avoid having
   * to pass around a whole bunch of arguments.
   */
  private static final class QueryState {

    // A map of parameter values for bound parameters referenced in the query
    private Map<String, QueryParameterValue> paramMap = new HashMap<>();
    // A map from table name to (column name -> ColumnConfig) for tables referenced in the query.
    private Map<String, Map<String, ColumnConfig>> columnConfigTable = new HashMap<>();
    // A map from table aliases to joined table information.
    private Map<String, JoinedTableInfo> aliasToJoinedTableInfo = new HashMap<>();
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
  public FieldSetQueryBuilder(CohortQueryBuilder cohortQueryBuilder,
      BigQueryService bigQueryService) {
    this.cohortQueryBuilder = cohortQueryBuilder;
    this.bigQueryService = bigQueryService;
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

  private TableNameAndAlias getTableNameAndAlias(List<String> columnParts, QueryState queryState,
      boolean beforeLimitRequired) {
    String tableName = queryState.mainTableName;
    JoinedTableInfo joinedTableInfo = null;
    String tableAlias = tableName;
    int i;
    Map<String, ColumnConfig> tableColumns = queryState.mainTableColumns;
    // Look for the longest already-joined table alias
    for (i = columnParts.size() - 1; i > 0; i--) {
      String alias = toTableAlias(columnParts, i);
      joinedTableInfo = queryState.aliasToJoinedTableInfo.get(alias);
      if (joinedTableInfo != null) {
        if (beforeLimitRequired && !joinedTableInfo.beforeLimitRequired) {
          joinedTableInfo.beforeLimitRequired = true;
          // Mark all other tables this table joins to as beforeLimitRequired = true.
          for (int j = i - 1; j > 0; j--) {
            String beforeAlias = toTableAlias(columnParts, j);
            JoinedTableInfo beforeInfo = queryState.aliasToJoinedTableInfo.get(beforeAlias);
            beforeInfo.beforeLimitRequired = true;
          }
        }
        tableName = joinedTableInfo.joinedTableName;
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
      joinedTableInfo = new JoinedTableInfo();
      joinedTableInfo.joinedTableName = foreignKeyTable;
      joinedTableInfo.joinedTablePrimaryKey = foreignKeyTablePrimaryKey;
      joinedTableInfo.startTableAlias = tableAlias;
      joinedTableInfo.startTableJoinColumn = foreignKeyColumn;
      joinedTableInfo.beforeLimitRequired = beforeLimitRequired;
      queryState.aliasToJoinedTableInfo.put(foreignKeyAlias, joinedTableInfo);
      tableAlias = foreignKeyAlias;
      tableName = foreignKeyTable;
    }
    return new TableNameAndAlias(tableName, tableAlias);
  }

  private ImmutableList<SelectedColumn> handleSelect(QueryState queryState, List<String> columnNames) {
    ImmutableList.Builder<SelectedColumn> selectColumns = ImmutableList.builder();
    String tableName = queryState.mainTableName;
    queryState.mainTableColumns = getColumnConfigs(queryState, tableName, true);
    for (String columnName : columnNames) {
      List<String> columnParts = parseColumnName(columnName);
      if (columnParts.size() == 1) {
        ColumnConfig columnConfig = queryState.mainTableColumns.get(columnName);
        if (columnConfig == null) {
          throw new BadRequestException(
              String.format("No column %s found on table %s", tableName, columnName));
        }
        SelectedColumn selectedColumn = new SelectedColumn();
        selectedColumn.columnInfo = new ColumnInfo(columnName, columnConfig);
        selectedColumn.tableAlias = queryState.mainTableName;
        selectedColumn.columnAlias = columnName;
        selectColumns.add(selectedColumn);
      } else {

        TableNameAndAlias tableNameAndAlias = getTableNameAndAlias(columnParts,
            queryState, false);
        Map<String, ColumnConfig> aliasConfig = getColumnConfigs(queryState,
            tableNameAndAlias.tableName, false);
        String columnEnd = columnParts.get(columnParts.size() - 1);
        ColumnConfig columnConfig = aliasConfig.get(columnEnd);
        if (columnConfig == null) {
          throw new BadRequestException(
              String.format("No column %s found on table %s", tableNameAndAlias.tableName,
                  columnEnd));
        }
        SelectedColumn selectedColumn = new SelectedColumn();
        selectedColumn.columnInfo = new ColumnInfo(columnName, columnConfig);
        selectedColumn.tableAlias = tableNameAndAlias.alias;
        selectedColumn.columnAlias = String.format("%s_%s", tableNameAndAlias.alias, columnEnd);
        selectColumns.add(selectedColumn);
      }
    }
    return selectColumns.build();
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

  private ColumnInfo getColumnInfo(QueryState queryState, String columnName, boolean beforeLimitRequired) {
    List<String> columnParts = parseColumnName(columnName);
    ColumnConfig columnConfig;
    if (columnParts.size() == 1) {
      columnConfig = queryState.mainTableColumns.get(columnName);
      if (columnConfig == null) {
        throw new BadRequestException("No such column " + columnName +
            "on table " + queryState.mainTableName);
      }
      return new ColumnInfo(String.format("%s.%s", queryState.mainTableName, columnName),
          columnConfig);
    } else {
      TableNameAndAlias tableNameAndAlias = getTableNameAndAlias(columnParts, queryState, beforeLimitRequired);
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

    ColumnInfo columnInfo = getColumnInfo(queryState, columnFilter.getColumnName(), true);

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

  private ImmutableList<OrderByColumn> handleOrderBy(QueryState queryState, List<String> orderBy) {
    if (orderBy.isEmpty()) {
      throw new BadRequestException("Order by list must not be empty");
    }
    ImmutableList.Builder<OrderByColumn> orderByColumns = ImmutableList.builder();
    for (String columnName : orderBy) {
      String columnStart = columnName;
      boolean descending = false;
      if (columnName.toUpperCase().startsWith(DESCENDING_PREFIX) && columnName.endsWith(")")) {
        columnStart = columnName.substring(DESCENDING_PREFIX.length(), columnName.length() - 1);
        descending = true;
      }
      OrderByColumn orderByColumn = new OrderByColumn();
      orderByColumn.columnInfo = getColumnInfo(queryState, columnStart, true);
      orderByColumn.descending = descending;
      orderByColumns.add(orderByColumn);
    }
    return orderByColumns.build();
  }

  private void addJoin(StringBuilder sql, String joinedTableAlias, JoinedTableInfo joinedTableInfo) {
    sql.append(String.format("\nLEFT OUTER JOIN `${projectId}.${dataSetId}.%s` %s ON %s.%s = %s.%s",
        joinedTableInfo.joinedTableName, joinedTableAlias,
        joinedTableInfo.startTableAlias, joinedTableInfo.startTableJoinColumn,
        joinedTableAlias, joinedTableInfo.joinedTablePrimaryKey.name));
  }

  private String getColumnAlias(String tableAndColumnExpression) {
    return tableAndColumnExpression.replace(TABLE_SEPARATOR, ALIAS_SEPARATOR);
  }

  private String buildSql(ParticipantCriteria participantCriteria, QueryState queryState,
      ImmutableList<SelectedColumn> selectColumns, String whereSql,
      ImmutableList<OrderByColumn> orderByColumns, long resultSize, long offset) {

    // Joining to tables after applying the LIMIT performs better in BigQuery than
    // joining to them before. Figure out if there are any tables that can be joined to after
    // the limit (they do not appear in the WHERE or ORDER BY clauses);
    // if so, run the SQL for all the where criteria and ordering in an inner query,
    // and do the joins to these other tables in an outer query.

    String tableName = queryState.mainTableName;
    Map<String, JoinedTableInfo> beforeLimitTables = new HashMap<>();
    Map<String, JoinedTableInfo> afterLimitTables = new HashMap<>();

    for (Entry<String, JoinedTableInfo> entry : queryState.aliasToJoinedTableInfo.entrySet()) {
      if (entry.getValue().beforeLimitRequired) {
        beforeLimitTables.put(entry.getKey(), entry.getValue());
      } else {
        afterLimitTables.put(entry.getKey(), entry.getValue());
      }
    }

    boolean hasAfterLimitTables = !afterLimitTables.isEmpty();
    List<String> innerSelectExpressions = new ArrayList<>();
    List<String> outerSelectExpressions = new ArrayList<>();
    Set<String> innerSelectColumnAliases = new HashSet<>();
    for (SelectedColumn column : selectColumns) {
      String columnSql = String.format("%s.%s %s",
          column.tableAlias, column.columnInfo.getColumnConfig().name,
          column.columnAlias);
      // If the table we're retrieving this from is found in the inner query, select the column
      // in both the inner and outer query.
      if (column.tableAlias.equals(queryState.mainTableName) ||
          beforeLimitTables.containsKey(column.tableAlias)) {
        innerSelectExpressions.add(columnSql);
        if (hasAfterLimitTables) {
          innerSelectColumnAliases.add(column.columnAlias);
          outerSelectExpressions.add(String.format("inner_results.%s", column.columnAlias));
        }
      } else {
        outerSelectExpressions.add(columnSql);
      }
    }
    for (Entry<String, JoinedTableInfo> entry : afterLimitTables.entrySet()) {
      // Columns that outer tables join to from inner tables must appear in the inner select clause.
      JoinedTableInfo tableInfo = entry.getValue();
      if (tableInfo.startTableAlias.equals(queryState.mainTableName)
          || beforeLimitTables.containsKey(tableInfo.startTableAlias)) {
        String joinColumn = String.format("%s.%s", tableInfo.startTableAlias,
            tableInfo.startTableJoinColumn);
        String columnAlias = getColumnAlias(joinColumn);
        if (!innerSelectColumnAliases.contains(columnAlias)) {
          innerSelectExpressions.add(String.format("%s %s", joinColumn, columnAlias));
          innerSelectColumnAliases.add(columnAlias);
        }
        // Since the table is being joined to the result of a subquery, change the start table
        // alias and column.
        tableInfo.startTableAlias = "inner_results";
        tableInfo.startTableJoinColumn = columnAlias;
      }
    }
    List<String> innerOrderByExpressions = new ArrayList<>();
    for (OrderByColumn column : orderByColumns) {
      innerOrderByExpressions.add(column.descending ? column.columnInfo.getColumnName() + " DESC"
          : column.columnInfo.getColumnName());
      if (hasAfterLimitTables) {
        // Columns that appear in the inner ORDER BY appear in the outer ORDER BY, too; make sure
        // they get returned.
        String columnAlias = getColumnAlias(column.columnInfo.getColumnName());
        if (!innerSelectColumnAliases.contains(columnAlias)) {
          innerSelectExpressions.add(String.format("%s %s", column.columnInfo.getColumnName(), columnAlias));
          innerSelectColumnAliases.add(columnAlias);
        }
      }
    }
    StringBuilder limitOffsetSql = new StringBuilder("\nlimit ");
    limitOffsetSql.append(resultSize);
    if (offset > 0) {
      limitOffsetSql.append(" offset ");
      limitOffsetSql.append(offset);
    }

    Joiner commaJoiner = Joiner.on(", ");
    StringBuilder innerSql = new StringBuilder("select ");
    innerSql.append(commaJoiner.join(innerSelectExpressions));
    innerSql.append(String.format(
        "\nfrom `${projectId}.${dataSetId}.%s` %s", tableName, tableName));
    for (Entry<String, JoinedTableInfo> entry : beforeLimitTables.entrySet()) {
      addJoin(innerSql, entry.getKey(), entry.getValue());
    }
    innerSql.append(whereSql);
    cohortQueryBuilder.addWhereClause(participantCriteria, queryState.mainTableName,
        innerSql, queryState.paramMap);
    innerSql.append("\norder by ");
    innerSql.append(commaJoiner.join(innerOrderByExpressions));

    innerSql.append(limitOffsetSql);
    if (!hasAfterLimitTables) {
      return innerSql.toString();
    }
    StringBuilder outerSql = new StringBuilder("select ");
    outerSql.append(commaJoiner.join(outerSelectExpressions));
    outerSql.append("\nfrom (");
    outerSql.append(innerSql);
    outerSql.append(") inner_results");
    for (Entry<String, JoinedTableInfo> entry : afterLimitTables.entrySet()) {
      addJoin(outerSql, entry.getKey(), entry.getValue());
    }
    // In the outer SQL, refer to the order by columns using their aliases from the inner query.
    outerSql.append("\norder by ");
    List<String> outerOrderByExpressions = new ArrayList<>();
    for (OrderByColumn column : orderByColumns) {
      String columnAlias = getColumnAlias(column.columnInfo.getColumnName());
      outerOrderByExpressions.add(column.descending ? columnAlias + " DESC" : columnAlias);
    }
    outerSql.append(commaJoiner.join(outerOrderByExpressions));
    outerSql.append("\nlimit ");
    outerSql.append(resultSize);

    return outerSql.toString();
  }

  private QueryConfiguration buildQuery(ParticipantCriteria participantCriteria,
      TableQueryAndConfig tableQueryAndConfig, long resultSize, long offset) {
    QueryState queryState = new QueryState();
    queryState.schemaConfig = tableQueryAndConfig.getConfig();
    TableQuery tableQuery = tableQueryAndConfig.getTableQuery();
    queryState.mainTableName = tableQuery.getTableName();

    ImmutableList<SelectedColumn> selectColumns = handleSelect(queryState, tableQuery.getColumns());

    StringBuilder whereSql = new StringBuilder("\nwhere\n");

    if (tableQuery.getFilters() != null) {
      handleResultFilters(tableQuery.getFilters(), queryState, whereSql);
      whereSql.append("\nand\n");
    }
    ImmutableList<OrderByColumn> orderByColumns = handleOrderBy(queryState, tableQuery.getOrderBy());
    QueryJobConfiguration jobConfiguration = QueryJobConfiguration
        .newBuilder(buildSql(participantCriteria, queryState, selectColumns,
            whereSql.toString(), orderByColumns, resultSize, offset))
        .setNamedParameters(queryState.paramMap)
        .setUseLegacySql(false)
        .build();
    return new QueryConfiguration(
        ImmutableList.copyOf(selectColumns.stream().map(SelectedColumn::getColumnInfo).iterator()),
        jobConfiguration);
  }

  private Map<String, Object> extractResults(TableQueryAndConfig tableQueryAndConfig,
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

  /**
   * Materializes a cohort with the specified table query and participant criteria, and returns
   * a {@link Iterable} of {@link Map} objects representing dictionaries of key-value pairs.
   */
  public Iterable<Map<String, Object>> materializeTableQuery(TableQueryAndConfig tableQueryAndConfig,
      ParticipantCriteria criteria, int limit, long offset) {
    QueryConfiguration queryConfiguration = buildQuery(criteria, tableQueryAndConfig, limit, offset);
    QueryResult result;
    QueryJobConfiguration jobConfiguration = queryConfiguration.getQueryJobConfiguration();
    result = bigQueryService.executeQuery(bigQueryService.filterBigQueryConfig(jobConfiguration));
    return Iterables.transform(result.iterateAll(),
        (row) -> extractResults(tableQueryAndConfig, queryConfiguration.getSelectColumns(), row));
  }

}
