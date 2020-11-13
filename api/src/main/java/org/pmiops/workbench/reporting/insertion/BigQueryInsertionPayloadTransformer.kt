package org.pmiops.workbench.reporting.insertion

/*
 * Base interface for BigQuery payload builders. Parameterized class is a payload model type,
 * and the intent is to pair each implementation with an enum type inheriting from
 * QueryParameterColumn<T>.
 *
 * Builder interfaces for separate BigQuery upload paths are possible.
 */
interface BigQueryInsertionPayloadTransformer<MODEL_T> {
    // Extending classes need only provide an  array of QueryParameterColumns, such
    // as an enum class's values() array.
    val queryParameterColumns: Array<ColumnValueExtractor<MODEL_T>?>?
}
