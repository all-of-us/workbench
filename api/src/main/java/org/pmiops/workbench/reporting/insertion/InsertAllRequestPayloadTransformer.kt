package org.pmiops.workbench.reporting.insertion

import com.google.cloud.bigquery.InsertAllRequest
import com.google.cloud.bigquery.InsertAllRequest.RowToInsert
import com.google.cloud.bigquery.TableId
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import org.pmiops.workbench.utils.RandomUtils
import java.util.*
import java.util.AbstractMap.SimpleImmutableEntry
import java.util.function.Function

/*
* Class with InsertAllRequest payload-specific implementation to change model objects
* (e.g. ReportingUser) into RowToInsert instances. A different template specialization of this
* interface with corresponding object (possibly of an anonymous class) should be used for each
* destination table in the upload. In order to construct such an instance, it's only necessary to
* override the method getQueryParameterColumns() with a method returning an array of instances.
* Enum#values() is very useful here.
*
* In order to instantiate a table-specific object implementing this instance, just do something like
* @code{
*  InsertAllRequestPayloadTransformer<ReportingUser> userTransformer =
*    UserColumnValueExtractor::values;
* }
*/
interface InsertAllRequestPayloadTransformer<MODEL_T> : BigQueryInsertionPayloadTransformer<MODEL_T> {
    /*
   * Construct an InsertAllRequest from all of the provided models, one row per model. The fixedValues
   * argument is to allow a value (like snapshot_timestamp) to span all rows in its column.
   */
    fun build(
            tableId: TableId?, models: List<MODEL_T>, fixedValues: Map<String?, Any?>?): InsertAllRequest? {
        return InsertAllRequest.newBuilder(tableId)
                .setIgnoreUnknownValues(false) // consider non-schema-conforming values bad rows.
                .setRows(modelsToRowsToInsert(models, fixedValues))
                .build()
    }

    // Wrap modelToRowToInsert() and apply to the whole input list of models.
    fun modelsToRowsToInsert(
            models: Collection<MODEL_T>, fixedValues: Map<String?, Any?>?): List<RowToInsert?>? {
        return models.stream()
                .map { m: MODEL_T -> modelToRowToInsert(m, fixedValues) }
                .collect(ImmutableList.toImmutableList<RowToInsert>())
    }

    /*
   * Build a RowToInsert object for each model instance, which is basically a poorly typed Map.
   * Null values are supposed to be omitted from the map (or have @Value or @NullValue annotations).
   */
    fun modelToRowToInsert(model: MODEL_T, fixedValues: Map<String?, Any?>?): RowToInsert? {
        val columnToValueBuilder = ImmutableMap.builder<String?, Any?>()
        columnToValueBuilder.putAll(fixedValues) // assumed to have non-null values
        Arrays.stream(queryParameterColumns)
                .map(
                        Function<ColumnValueExtractor<MODEL_T>, SimpleImmutableEntry<String?, Any?>> { col: ColumnValueExtractor<MODEL_T> ->
                            SimpleImmutableEntry(
                                    col.parameterName, col.getRowToInsertValue(model))
                        })
                .filter { e: SimpleImmutableEntry<String?, Any?> -> Objects.nonNull(e.value) }
                .forEach { entry: SimpleImmutableEntry<String?, Any?>? -> columnToValueBuilder.put(entry) }
        return RowToInsert.of(generateInsertId(), columnToValueBuilder.build())
    }

    /*
   * As an aid for best-effort deduplication, we can specify a unique 16-character key for each row.
   */
    fun generateInsertId(): String? {
        return RandomUtils.generateRandomChars(INSERT_ID_CHARS, INSERT_ID_LENGTH)
    }

    companion object {
        const val INSERT_ID_CHARS = "abcdefghijklmnopqrstuvwxyz"
        const val INSERT_ID_LENGTH = 16
    }
}
