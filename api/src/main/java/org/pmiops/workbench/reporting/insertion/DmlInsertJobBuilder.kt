package org.pmiops.workbench.reporting.insertion

import com.google.cloud.bigquery.QueryJobConfiguration
import com.google.cloud.bigquery.QueryParameterValue
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import java.util.*
import java.util.AbstractMap.SimpleImmutableEntry
import java.util.function.Function
import java.util.function.Predicate
import java.util.stream.Collectors
import java.util.stream.IntStream

interface DmlInsertJobBuilder<T> : BigQueryInsertionPayloadTransformer<T> {
    val columnNameList: String
        get() = Arrays.stream(queryParameterColumns)
                .map<String>(ColumnValueExtractor::parameterName)
                .collect(Collectors.joining(", "))

    fun build(
            tableName: String, models: List<T>, snapshotTimestamp: QueryParameterValue?): QueryJobConfiguration? {
        val columnNames: List<String?> = Arrays.stream(queryParameterColumns)
                .map<String>(ColumnValueExtractor::parameterName)
                .collect(ImmutableList.toImmutableList())
        val query = """INSERT INTO $tableName (snapshot_timestamp, $columnNameList)
${createNamedParameterValuesListKeys(models.size, columnNames)};"""
        val keyToParameterValue = getKeyToParameterValue(models, snapshotTimestamp)
        return QueryJobConfiguration.newBuilder(query).setNamedParameters(keyToParameterValue).build()
    }

    fun createNamedParameterValuesListKeys(rowCount: Int, columnNames: List<String?>): String {
        return ("VALUES\n"
                + IntStream.range(0, rowCount)
                .mapToObj { row: Int -> writeNamedParameterValuesRow(row, columnNames) }
                .collect(Collectors.joining(",\n")))
    }

    // column count is number of  fields in the enum (no including timestamp)
    fun writeNamedParameterValuesRow(rowNum: Int, columnNames: List<String?>): String? {
        val parameterNameList = columnNames.stream()
                .map { c: String? -> "@" + parameterName(rowNum, c) }
                .collect(Collectors.joining(", "))
        return "(@snapshot_timestamp, $parameterNameList)"
    }

    fun getKeyToParameterValue(
            models: List<T>, snapshotTimestamp: QueryParameterValue?): Map<String, QueryParameterValue?> {
        val builder = ImmutableMap.builder<String, QueryParameterValue?>()
        builder.put("snapshot_timestamp", snapshotTimestamp) // same for all rows
        for (row in models.indices) {
            builder.putAll(toNamedParameterMap(models[row], row))
        }
        return builder.build()
    }

    fun parameterName(rowNum: Int, columnName: String?): String {
        return String.format("%s__%d", columnName, rowNum)
    }

    fun toNamedParameterMap(target: T, rowIndex: Int): Map<String, QueryParameterValue?>? {
        return Arrays.stream(queryParameterColumns)
                .map(
                        Function<ColumnValueExtractor<T>, SimpleImmutableEntry<String, QueryParameterValue>> { e: ColumnValueExtractor<T> ->
                            SimpleImmutableEntry(
                                    parameterName(rowIndex, e.parameterName), e.toParameterValue(target))
                        })
                .filter(Predicate<SimpleImmutableEntry<String?, QueryParameterValue?>> { e: SimpleImmutableEntry<String?, QueryParameterValue?> -> Objects.nonNull(e.value) })
                .collect(ImmutableMap.toImmutableMap<SimpleImmutableEntry<String?, QueryParameterValue?>, String?, QueryParameterValue?>({ obj: SimpleImmutableEntry<String?, QueryParameterValue?> -> obj.key }) { obj: SimpleImmutableEntry<String?, QueryParameterValue?> -> obj.value })
    }
}
