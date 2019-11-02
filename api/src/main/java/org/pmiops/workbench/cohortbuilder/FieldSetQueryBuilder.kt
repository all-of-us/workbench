package org.pmiops.workbench.cohortbuilder

import com.google.cloud.bigquery.FieldValue
import com.google.cloud.bigquery.QueryJobConfiguration
import com.google.cloud.bigquery.QueryParameterValue
import com.google.cloud.bigquery.TableResult
import com.google.common.base.Joiner
import com.google.common.base.Splitter
import com.google.common.base.Strings
import com.google.common.collect.ImmutableList
import com.google.common.collect.Iterables
import java.math.BigDecimal
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.HashMap
import java.util.HashSet
import kotlin.collections.Map.Entry
import java.util.stream.Collectors
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import org.pmiops.workbench.api.BigQueryService
import org.pmiops.workbench.cohortbuilder.QueryConfiguration.ColumnInfo
import org.pmiops.workbench.config.CdrBigQuerySchemaConfig
import org.pmiops.workbench.config.CdrBigQuerySchemaConfig.ColumnConfig
import org.pmiops.workbench.config.CdrBigQuerySchemaConfig.ColumnType
import org.pmiops.workbench.config.CdrBigQuerySchemaConfig.TableConfig
import org.pmiops.workbench.exceptions.BadRequestException
import org.pmiops.workbench.model.ColumnFilter
import org.pmiops.workbench.model.FieldSet
import org.pmiops.workbench.model.Operator
import org.pmiops.workbench.model.ResultFilters
import org.pmiops.workbench.model.TableQuery
import org.pmiops.workbench.utils.OperatorUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/** Builds queries based of a [FieldSet] and [ParticipantCriteria].  */
// TODO: figure out how to return nicer error messages to users for bad queries
// TODO: consider whether we want to impose limits on number of columns, joins, etc. requested
@Service
class FieldSetQueryBuilder @Autowired
constructor(
        private val cohortQueryBuilder: CohortQueryBuilder, private val bigQueryService: BigQueryService) {

    private class JoinedTableInfo {

        private val startTableAlias: String? = null
        private val startTableJoinColumn: String? = null

        private val joinedTableName: String? = null
        private val joinedTablePrimaryKey: ColumnConfig? = null

        // True if this table is referenced in the where clause or order by clause,
        // and thus must be joined to before a LIMIT is applied.
        private val beforeLimitRequired: Boolean = false
    }

    private class SelectedColumn {

        // The table alias for the table this column lives on.
        private val tableAlias: String? = null
        // The alias for the column.
        private val columnAlias: String? = null

        private val columnInfo: ColumnInfo? = null
    }

    private class OrderByColumn {
        private val columnInfo: ColumnInfo? = null
        private val descending: Boolean = false
    }

    /**
     * A container for state needed to turn a query into SQL. We pass this around to avoid having to
     * pass around a whole bunch of arguments.
     */
    private class QueryState {

        // A map of parameter values for bound parameters referenced in the query
        private val paramMap = HashMap<String, QueryParameterValue>()
        // A map from table name to (column name -> ColumnConfig) for tables referenced in the query.
        private val columnConfigTable = HashMap<String, Map<String, ColumnConfig>>()
        // A map from table aliases to joined table information.
        private val aliasToJoinedTableInfo = HashMap<String, JoinedTableInfo>()
        // The name of the main table.
        private val mainTableName: String? = null
        // The CDR schema configuration.
        private val schemaConfig: CdrBigQuerySchemaConfig? = null
        // A map of (column name -> ColumnConfig) for the columns on the main table (pulled from
        // columnConfigTable).
        private val mainTableColumns: Map<String, ColumnConfig>? = null
    }

    private class TableNameAndAlias internal constructor(private val tableName: String, private val alias: String)

    private fun findPrimaryKey(columnConfigs: Iterable<ColumnConfig>): ColumnConfig {
        for (columnConfig in columnConfigs) {
            if (columnConfig.primaryKey != null && columnConfig.primaryKey) {
                return columnConfig
            }
        }
        throw IllegalStateException("Table lacks primary key!")
    }

    private fun parseColumnName(columnName: String): List<String> {
        return Splitter.on(TABLE_SEPARATOR).splitToList(columnName)
    }

    private fun toTableAlias(columnParts: List<String>, endIndex: Int): String {
        return Joiner.on(ALIAS_SEPARATOR).join(columnParts.subList(0, endIndex))
    }

    private fun getForeignKeyColumn(columnPart: String): String {
        return columnPart + "_id"
    }

    private fun getColumnConfigs(
            queryState: QueryState, tableName: String?, tableNeedsPersonId: Boolean): Map<String, ColumnConfig> {
        var configTable: MutableMap<String, ColumnConfig>? = queryState.columnConfigTable.get(tableName)
        if (configTable == null) {
            var tableConfig: TableConfig? = queryState.schemaConfig!!.cohortTables[tableName]
            if (tableConfig == null) {
                if (tableNeedsPersonId) {
                    throw BadRequestException(
                            "Not a valid cohort table (lacks person_id column): " + tableName!!)
                }
                tableConfig = queryState.schemaConfig.metadataTables[tableName]
                if (tableConfig == null) {
                    throw BadRequestException("Table not found: " + tableName!!)
                }
            }
            configTable = HashMap()
            queryState.columnConfigTable[tableName] = configTable
            for (columnConfig in tableConfig.columns) {
                configTable[columnConfig.name] = columnConfig
            }
        }
        return configTable
    }

    private fun getTableNameAndAlias(
            columnParts: List<String>, queryState: QueryState, beforeLimitRequired: Boolean): TableNameAndAlias {
        var tableName = queryState.mainTableName
        var joinedTableInfo: JoinedTableInfo? = null
        var tableAlias = tableName
        var i: Int
        var tableColumns = queryState.mainTableColumns
        // Look for the longest already-joined table alias
        i = columnParts.size - 1
        while (i > 0) {
            val alias = toTableAlias(columnParts, i)
            joinedTableInfo = queryState.aliasToJoinedTableInfo[alias]
            if (joinedTableInfo != null) {
                if (beforeLimitRequired && !joinedTableInfo.beforeLimitRequired) {
                    joinedTableInfo.beforeLimitRequired = true
                    // Mark all other tables this table joins to as beforeLimitRequired = true.
                    for (j in i - 1 downTo 1) {
                        val beforeAlias = toTableAlias(columnParts, j)
                        val beforeInfo = queryState.aliasToJoinedTableInfo[beforeAlias]
                        beforeInfo.beforeLimitRequired = true
                    }
                }
                tableName = joinedTableInfo.joinedTableName
                tableAlias = alias
                tableColumns = getColumnConfigs(queryState, tableName, false)
                break
            }
            i--
        }
        // Add in all the necessary remaining join tables starting from index i.
        for (j in i until columnParts.size - 1) {
            val columnPart = columnParts[j]
            val foreignKeyColumn = getForeignKeyColumn(columnPart)
            val foreignKeyColumnConfig = tableColumns!![foreignKeyColumn]
                    ?: throw BadRequestException("No foreign key column found: $foreignKeyColumn")
            val foreignKeyTable = foreignKeyColumnConfig.foreignKey
                    ?: throw BadRequestException("Column is not a foreign key: $foreignKeyColumn")
            tableColumns = getColumnConfigs(queryState, foreignKeyTable, false)
            val foreignKeyTablePrimaryKey = findPrimaryKey(tableColumns.values)

            val foreignKeyAlias = toTableAlias(columnParts, j + 1)
            joinedTableInfo = JoinedTableInfo()
            joinedTableInfo.joinedTableName = foreignKeyTable
            joinedTableInfo.joinedTablePrimaryKey = foreignKeyTablePrimaryKey
            joinedTableInfo.startTableAlias = tableAlias
            joinedTableInfo.startTableJoinColumn = foreignKeyColumn
            joinedTableInfo.beforeLimitRequired = beforeLimitRequired
            queryState.aliasToJoinedTableInfo[foreignKeyAlias] = joinedTableInfo
            tableAlias = foreignKeyAlias
            tableName = foreignKeyTable
        }
        return TableNameAndAlias(tableName, tableAlias)
    }

    private fun handleSelect(
            queryState: QueryState, columnNames: List<String>): ImmutableList<SelectedColumn> {
        val selectColumns = ImmutableList.builder<SelectedColumn>()
        val tableName = queryState.mainTableName
        queryState.mainTableColumns = getColumnConfigs(queryState, tableName, true)
        for (columnName in columnNames) {
            val columnParts = parseColumnName(columnName)
            if (columnParts.size == 1) {
                val columnConfig = queryState.mainTableColumns[columnName]
                        ?: throw BadRequestException(
                                String.format("No column %s found on table %s", columnName, tableName))
                val selectedColumn = SelectedColumn()
                selectedColumn.columnInfo = ColumnInfo(columnName, columnConfig)
                selectedColumn.tableAlias = queryState.mainTableName
                selectedColumn.columnAlias = columnName
                selectColumns.add(selectedColumn)
            } else {

                val tableNameAndAlias = getTableNameAndAlias(columnParts, queryState, false)
                val aliasConfig = getColumnConfigs(queryState, tableNameAndAlias.tableName, false)
                val columnEnd = columnParts[columnParts.size - 1]
                val columnConfig = aliasConfig[columnEnd]
                        ?: throw BadRequestException(
                                String.format(
                                        "No column %s found on table %s", columnEnd, tableNameAndAlias.tableName))
                val selectedColumn = SelectedColumn()
                selectedColumn.columnInfo = ColumnInfo(columnName, columnConfig)
                selectedColumn.tableAlias = tableNameAndAlias.alias
                // Separate table aliases from column aliases with "__" in the results. These can be
                // replaced with "." to produce dot notation column names in the output. (BigQuery does not
                // allow column aliases to contain dots, so we can't do that here directly.)
                selectedColumn.columnAlias = String.format("%s__%s", tableNameAndAlias.alias, columnEnd)
                selectColumns.add(selectedColumn)
            }
        }
        return selectColumns.build()
    }

    private fun handleComparison(
            columnFilter: ColumnFilter,
            columnInfo: ColumnInfo,
            queryState: QueryState,
            whereSql: StringBuilder) {
        val columnConfig = columnInfo.columnConfig
        val paramName = "p" + queryState.paramMap.size
        val operator = columnFilter.getOperator()
        if (columnFilter.getValueNumbers() != null || columnFilter.getValues() != null) {
            throw BadRequestException("Can't use valueNumbers or values with operator $operator")
        }
        if (!((columnFilter.getValue() != null)
                        xor (columnFilter.getValueDate() != null)
                        xor (columnFilter.getValueNumber() != null)
                        xor (columnFilter.getValueNull() != null && columnFilter.getValueNull()))) {
            throw BadRequestException(
                    "Exactly one of value, valueDate, valueNumber, and valueNull "
                            + "must be specified for filter on column "
                            + columnConfig.name)
        }
        if (operator.equals(Operator.LIKE) && columnFilter.getValue() == null) {
            throw BadRequestException("LIKE operator only support with value")
        }
        if (columnFilter.getValue() != null) {
            if (columnConfig.type != ColumnType.STRING) {
                throw BadRequestException(
                        "Can't use value with column " + columnConfig.name + " of type " + columnConfig.type)
            }
            queryState.paramMap[paramName] = QueryParameterValue.string(columnFilter.getValue())
        } else if (columnFilter.getValueDate() != null) {
            if (columnConfig.type == ColumnType.DATE) {
                try {
                    DATE_FORMAT.parse(columnFilter.getValueDate())
                    queryState.paramMap[paramName] = QueryParameterValue.date(columnFilter.getValueDate())
                } catch (e: ParseException) {
                    throw BadRequestException(
                            "Couldn't parse date value "
                                    + columnFilter.getValueDate()
                                    + "; expected format: "
                                    + DATE_FORMAT_PATTERN)
                }

            } else if (columnConfig.type == ColumnType.TIMESTAMP) {
                try {
                    val timestamp = DATE_TIME_FORMAT.parseDateTime(columnFilter.getValueDate()).millis * 1000
                    queryState.paramMap[paramName] = QueryParameterValue.timestamp(timestamp)
                } catch (e: IllegalArgumentException) {
                    throw BadRequestException(
                            "Couldn't parse timestamp value "
                                    + columnFilter.getValueDate()
                                    + "; expected format: "
                                    + DATE_TIME_FORMAT_PATTERN)
                }

            } else {
                throw BadRequestException(
                        "Can't use valueDate with column "
                                + columnConfig.name
                                + " of type "
                                + columnConfig.type)
            }
        } else if (columnFilter.getValueNumber() != null) {
            if (columnConfig.type == ColumnType.FLOAT) {
                queryState.paramMap[paramName] = QueryParameterValue.float64(columnFilter.getValueNumber().doubleValue())
            } else if (columnConfig.type == ColumnType.INTEGER) {
                queryState.paramMap[paramName] = QueryParameterValue.int64(columnFilter.getValueNumber().longValue())
            } else {
                throw BadRequestException(
                        "Can't use valueNumber with column "
                                + columnConfig.name
                                + " of type "
                                + columnConfig.type)
            }
        } else if (columnFilter.getValueNull() != null && columnFilter.getValueNull()) {
            if (operator !== Operator.EQUAL && operator !== Operator.NOT_EQUAL) {
                throw BadRequestException("Unsupported operator for valueNull: $operator")
            }
            whereSql.append(columnInfo.columnName)
            if (operator.equals(Operator.EQUAL)) {
                whereSql.append(" is null\n")
            } else {
                whereSql.append(" is not null\n")
            }
            return
        }
        whereSql.append(
                String.format(
                        "%s %s @%s",
                        columnInfo.columnName,
                        OperatorUtils.getSqlOperator(columnFilter.getOperator()),
                        paramName))
    }

    private fun handleInClause(
            columnFilter: ColumnFilter,
            columnInfo: ColumnInfo,
            queryState: QueryState,
            whereSql: StringBuilder) {
        val columnConfig = columnInfo.columnConfig
        val paramName = "p" + queryState.paramMap.size
        if (columnFilter.getValue() != null
                || columnFilter.getValueNumber() != null
                || columnFilter.getValueDate() != null
                || columnFilter.getValueNull() != null) {
            throw BadRequestException("Can't use IN operator with single value filter")
        }
        val valueNumbers = columnFilter.getValueNumbers()
        val valueStrings = columnFilter.getValues()
        if (!((valueNumbers != null && !valueNumbers!!.isEmpty()) xor (valueStrings != null && !valueStrings!!.isEmpty()))) {
            throw BadRequestException(
                    "Either valueNumbers or valueStrings must be specified with "
                            + "in clause on column "
                            + columnFilter.getColumnName())
        }

        if (valueNumbers != null && !valueNumbers!!.isEmpty()) {
            if (columnConfig.type != ColumnType.INTEGER) {
                throw BadRequestException(
                        "Can't use valueNumbers with column "
                                + columnConfig.name
                                + " of type "
                                + columnConfig.type)
            }
            queryState.paramMap[paramName] = QueryParameterValue.array(
                    valueNumbers!!.stream()
                            .map(Function<BigDecimal, Long> { it.toLong() })
                            .collect(Collectors.toList<Long>())
                            .toTypedArray(),
                    Long::class.java)
        } else {
            if (columnConfig.type != ColumnType.STRING) {
                throw BadRequestException(
                        "Can't use values with column " + columnConfig.name + " of type " + columnConfig.type)
            }
            queryState.paramMap[paramName] = QueryParameterValue.array(valueStrings!!.toTypedArray(), String::class.java)
        }
        whereSql.append(String.format("%s in unnest(@%s)", columnInfo.columnName, paramName))
    }

    private fun getColumnInfo(
            queryState: QueryState, columnName: String, beforeLimitRequired: Boolean): ColumnInfo {
        val columnParts = parseColumnName(columnName)
        val columnConfig: ColumnConfig?
        if (columnParts.size == 1) {
            columnConfig = queryState.mainTableColumns!![columnName]
            if (columnConfig == null) {
                throw BadRequestException(
                        "No such column " + columnName + "on table " + queryState.mainTableName)
            }
            return ColumnInfo(
                    String.format("%s.%s", queryState.mainTableName, columnName), columnConfig)
        } else {
            val tableNameAndAlias = getTableNameAndAlias(columnParts, queryState, beforeLimitRequired)
            val aliasConfig = getColumnConfigs(queryState, tableNameAndAlias.tableName, false)
            val columnEnd = columnParts[columnParts.size - 1]
            columnConfig = aliasConfig[columnEnd]
            if (columnConfig == null) {
                throw BadRequestException(
                        String.format(
                                "No column %s found on table %s", columnEnd, tableNameAndAlias.tableName))
            }
            return ColumnInfo(
                    String.format("%s.%s", tableNameAndAlias.alias, columnEnd), columnConfig)
        }
    }

    private fun handleColumnFilter(
            columnFilter: ColumnFilter, queryState: QueryState, whereSql: StringBuilder) {
        if (Strings.isNullOrEmpty(columnFilter.getColumnName())) {
            throw BadRequestException("Missing column name for column filter")
        }

        val columnInfo = getColumnInfo(queryState, columnFilter.getColumnName(), true)

        if (columnFilter.getOperator() == null) {
            columnFilter.setOperator(Operator.EQUAL)
        }

        if (columnFilter.getOperator().equals(Operator.IN)) {
            handleInClause(columnFilter, columnInfo, queryState, whereSql)
        } else {
            handleComparison(columnFilter, columnInfo, queryState, whereSql)
        }
    }

    private fun handleResultFilters(
            resultFilters: ResultFilters, queryState: QueryState, whereSql: StringBuilder) {
        if (!((resultFilters.getColumnFilter() != null)
                        xor (resultFilters.getAllOf() != null)
                        xor (resultFilters.getAnyOf() != null))) {
            throw BadRequestException(
                    "Exactly one of allOf, anyOf, or columnFilter must be " + "specified for result filters")
        }
        if (resultFilters.getIfNot() != null && resultFilters.getIfNot()) {
            whereSql.append("not ")
        }
        if (resultFilters.getColumnFilter() != null) {
            handleColumnFilter(resultFilters.getColumnFilter(), queryState, whereSql)
        } else {
            val operator: String
            val childFilters: List<ResultFilters>
            if (resultFilters.getAllOf() != null) {
                operator = "and"
                childFilters = resultFilters.getAllOf()
            } else {
                operator = "or"
                childFilters = resultFilters.getAnyOf()
            }
            whereSql.append("(")
            var first = true
            for (childFilter in childFilters) {
                if (first) {
                    first = false
                } else {
                    whereSql.append("\n")
                    whereSql.append(operator)
                    whereSql.append("\n")
                }
                handleResultFilters(childFilter, queryState, whereSql)
            }
            whereSql.append(")\n")
        }
    }

    private fun handleOrderBy(queryState: QueryState, orderBy: List<String>): ImmutableList<OrderByColumn> {
        if (orderBy.isEmpty()) {
            throw BadRequestException("Order by list must not be empty")
        }
        val orderByColumns = ImmutableList.builder<OrderByColumn>()
        for (columnName in orderBy) {
            var columnStart = columnName
            var descending = false
            if (columnName.toUpperCase().startsWith(DESCENDING_PREFIX) && columnName.endsWith(")")) {
                columnStart = columnName.substring(DESCENDING_PREFIX.length, columnName.length - 1)
                descending = true
            }
            val orderByColumn = OrderByColumn()
            orderByColumn.columnInfo = getColumnInfo(queryState, columnStart, true)
            orderByColumn.descending = descending
            orderByColumns.add(orderByColumn)
        }
        return orderByColumns.build()
    }

    private fun addJoin(
            sql: StringBuilder, joinedTableAlias: String, joinedTableInfo: JoinedTableInfo) {
        sql.append(
                String.format(
                        "\nLEFT OUTER JOIN `\${projectId}.\${dataSetId}.%s` %s ON %s.%s = %s.%s",
                        joinedTableInfo.joinedTableName,
                        joinedTableAlias,
                        joinedTableInfo.startTableAlias,
                        joinedTableInfo.startTableJoinColumn,
                        joinedTableAlias,
                        joinedTableInfo.joinedTablePrimaryKey!!.name))
    }

    private fun getColumnAlias(tableAndColumnExpression: String): String {
        return tableAndColumnExpression.replace(TABLE_SEPARATOR, ALIAS_SEPARATOR)
    }

    private fun buildSql(
            participantCriteria: ParticipantCriteria,
            queryState: QueryState,
            selectColumns: ImmutableList<SelectedColumn>,
            whereSql: String,
            orderByColumns: ImmutableList<OrderByColumn>,
            resultSize: Long?,
            offset: Long): String {

        // Joining to tables after applying the LIMIT performs better in BigQuery than
        // joining to them before. Figure out if there are any tables that can be joined to after
        // the limit (they do not appear in the WHERE or ORDER BY clauses);
        // if so, run the SQL for all the where criteria and ordering in an inner query,
        // and do the joins to these other tables in an outer query.

        val tableName = queryState.mainTableName
        val beforeLimitTables = HashMap<String, JoinedTableInfo>()
        val afterLimitTables = HashMap<String, JoinedTableInfo>()

        for ((key, value) in queryState.aliasToJoinedTableInfo) {
            if (value.beforeLimitRequired) {
                beforeLimitTables[key] = value
            } else {
                afterLimitTables[key] = value
            }
        }

        val hasAfterLimitTables = !afterLimitTables.isEmpty()
        val innerSelectExpressions = ArrayList<String>()
        val outerSelectExpressions = ArrayList<String>()
        val innerSelectColumnAliases = HashSet<String>()
        for (column in selectColumns) {
            val columnSql = String.format(
                    "%s.%s %s",
                    column.tableAlias, column.columnInfo!!.columnConfig.name, column.columnAlias)
            // If the table we're retrieving this from is found in the inner query, select the column
            // in both the inner and outer query.
            if (column.tableAlias == queryState.mainTableName || beforeLimitTables.containsKey(column.tableAlias)) {
                innerSelectExpressions.add(columnSql)
                if (hasAfterLimitTables) {
                    innerSelectColumnAliases.add(column.columnAlias)
                    outerSelectExpressions.add(String.format("inner_results.%s", column.columnAlias))
                }
            } else {
                outerSelectExpressions.add(columnSql)
            }
        }
        for ((_, tableInfo) in afterLimitTables) {
            // Columns that outer tables join to from inner tables must appear in the inner select clause.
            if (tableInfo.startTableAlias == queryState.mainTableName || beforeLimitTables.containsKey(tableInfo.startTableAlias)) {
                val joinColumn = String.format("%s.%s", tableInfo.startTableAlias, tableInfo.startTableJoinColumn)
                val columnAlias = getColumnAlias(joinColumn)
                if (!innerSelectColumnAliases.contains(columnAlias)) {
                    innerSelectExpressions.add(String.format("%s %s", joinColumn, columnAlias))
                    innerSelectColumnAliases.add(columnAlias)
                }
                // Since the table is being joined to the result of a subquery, change the start table
                // alias and column.
                tableInfo.startTableAlias = "inner_results"
                tableInfo.startTableJoinColumn = columnAlias
            }
        }
        val innerOrderByExpressions = ArrayList<String>()
        for (column in orderByColumns) {
            innerOrderByExpressions.add(
                    if (column.descending)
                        column.columnInfo!!.columnName + " DESC"
                    else
                        column.columnInfo!!.columnName)
            if (hasAfterLimitTables) {
                // Columns that appear in the inner ORDER BY appear in the outer ORDER BY, too; make sure
                // they get returned.
                val columnAlias = getColumnAlias(column.columnInfo.columnName)
                if (!innerSelectColumnAliases.contains(columnAlias)) {
                    innerSelectExpressions.add(
                            String.format("%s %s", column.columnInfo.columnName, columnAlias))
                    innerSelectColumnAliases.add(columnAlias)
                }
            }
        }
        val limitOffsetSql: StringBuilder
        if (resultSize == null) {
            limitOffsetSql = StringBuilder("\n")
        } else {
            limitOffsetSql = StringBuilder("\nlimit ")
            limitOffsetSql.append(resultSize)
        }
        if (offset > 0) {
            limitOffsetSql.append(" offset ")
            limitOffsetSql.append(offset)
        }

        val commaJoiner = Joiner.on(", ")
        val innerSql = StringBuilder("select ")
        innerSql.append(commaJoiner.join(innerSelectExpressions))
        innerSql.append(
                String.format("\nfrom `\${projectId}.\${dataSetId}.%s` %s", tableName, tableName))
        for ((key, value) in beforeLimitTables) {
            addJoin(innerSql, key, value)
        }
        innerSql.append(whereSql)
        cohortQueryBuilder.addWhereClause(
                participantCriteria, queryState.mainTableName, innerSql, queryState.paramMap)
        innerSql.append("\norder by ")
        innerSql.append(commaJoiner.join(innerOrderByExpressions))

        innerSql.append(limitOffsetSql)
        if (!hasAfterLimitTables) {
            return innerSql.toString()
        }
        val outerSql = StringBuilder("select ")
        outerSql.append(commaJoiner.join(outerSelectExpressions))
        outerSql.append("\nfrom (")
        outerSql.append(innerSql)
        outerSql.append(") inner_results")
        for ((key, value) in afterLimitTables) {
            addJoin(outerSql, key, value)
        }
        // In the outer SQL, refer to the order by columns using their aliases from the inner query.
        outerSql.append("\norder by ")
        val outerOrderByExpressions = ArrayList<String>()
        for (column in orderByColumns) {
            val columnAlias = getColumnAlias(column.columnInfo!!.columnName)
            outerOrderByExpressions.add(if (column.descending) "$columnAlias DESC" else columnAlias)
        }
        outerSql.append(commaJoiner.join(outerOrderByExpressions))
        if (resultSize != null) {
            outerSql.append("\nlimit ")
            outerSql.append(resultSize)
        }

        return outerSql.toString()
    }

    fun getQueryJobConfiguration(
            participantCriteria: ParticipantCriteria,
            tableQueryAndConfig: TableQueryAndConfig,
            resultSize: Long?): QueryJobConfiguration {
        val queryConfiguration = buildQuery(participantCriteria, tableQueryAndConfig, resultSize, 0L)
        return bigQueryService.filterBigQueryConfig(queryConfiguration.queryJobConfiguration)
    }

    private fun buildQuery(
            participantCriteria: ParticipantCriteria,
            tableQueryAndConfig: TableQueryAndConfig,
            resultSize: Long?,
            offset: Long): QueryConfiguration {
        val queryState = QueryState()
        queryState.schemaConfig = tableQueryAndConfig.config
        val tableQuery = tableQueryAndConfig.tableQuery
        queryState.mainTableName = tableQuery.getTableName()

        val selectColumns = handleSelect(queryState, tableQuery.getColumns())

        val whereSql = StringBuilder("\nwhere\n")

        if (tableQuery.getFilters() != null) {
            handleResultFilters(tableQuery.getFilters(), queryState, whereSql)
            whereSql.append("\nand\n")
        }
        val orderByColumns = handleOrderBy(queryState, tableQuery.getOrderBy())
        val jobConfiguration = QueryJobConfiguration.newBuilder(
                buildSql(
                        participantCriteria,
                        queryState,
                        selectColumns,
                        whereSql.toString(),
                        orderByColumns,
                        resultSize,
                        offset))
                .setNamedParameters(queryState.paramMap)
                .setUseLegacySql(false)
                .build()
        return QueryConfiguration(
                ImmutableList.copyOf(selectColumns.stream().map<ColumnInfo>(Function<SelectedColumn, ColumnInfo> { it.getColumnInfo() }).iterator()),
                jobConfiguration)
    }

    private fun extractResults(
            tableQueryAndConfig: TableQueryAndConfig,
            columns: ImmutableList<ColumnInfo>,
            row: List<FieldValue>?): Map<String, Any> {
        val tableQuery = tableQueryAndConfig.tableQuery
        val results = HashMap(tableQuery.getColumns().size())
        for (i in columns.indices) {
            val fieldValue = row!![i]
            val columnInfo = columns[i]
            val columnConfig = columnInfo.columnConfig
            if (!fieldValue.isNull) {
                val value: Any
                when (columnConfig.type) {
                    CdrBigQuerySchemaConfig.ColumnType.DATE -> value = fieldValue.stringValue
                    CdrBigQuerySchemaConfig.ColumnType.FLOAT -> value = fieldValue.doubleValue
                    CdrBigQuerySchemaConfig.ColumnType.INTEGER -> value = fieldValue.longValue
                    CdrBigQuerySchemaConfig.ColumnType.STRING -> value = fieldValue.stringValue
                    CdrBigQuerySchemaConfig.ColumnType.TIMESTAMP -> value = DATE_TIME_FORMAT.print(fieldValue.timestampValue / 1000L)
                    else -> throw IllegalStateException("Unrecognized column type: " + columnConfig.type)
                }
                results.put(columnInfo.columnName, value)
            }
        }
        return results
    }

    /**
     * Materializes a cohort with the specified table query and participant criteria, and returns a
     * [Iterable] of [Map] objects representing dictionaries of key-value pairs.
     */
    fun materializeTableQuery(
            tableQueryAndConfig: TableQueryAndConfig,
            criteria: ParticipantCriteria,
            limit: Int,
            offset: Long): Iterable<Map<String, Any>> {
        val queryConfiguration = buildQuery(criteria, tableQueryAndConfig, limit.toLong(), offset)
        val result: TableResult
        val jobConfiguration = queryConfiguration.queryJobConfiguration
        result = bigQueryService.executeQuery(bigQueryService.filterBigQueryConfig(jobConfiguration))
        return Iterables.transform<FieldValueList, Map<String, Any>>(
                result.iterateAll()
        ) { row -> extractResults(tableQueryAndConfig, queryConfiguration.selectColumns, row) }
    }

    companion object {

        private val TABLE_SEPARATOR = "."
        private val ALIAS_SEPARATOR = "_"
        private val DESCENDING_PREFIX = "DESCENDING("
        private val DATE_FORMAT_PATTERN = "yyyy-MM-dd"
        private val DATE_TIME_FORMAT_PATTERN = "yyyy-MM-dd HH:mm:ss zzz"

        private val DATE_FORMAT = SimpleDateFormat(DATE_FORMAT_PATTERN)
        private val DATE_TIME_FORMAT = DateTimeFormat.forPattern(DATE_TIME_FORMAT_PATTERN).withZoneUTC()
    }
}
