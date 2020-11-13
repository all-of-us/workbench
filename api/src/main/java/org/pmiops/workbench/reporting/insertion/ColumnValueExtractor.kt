package org.pmiops.workbench.reporting.insertion

import com.google.cloud.bigquery.QueryParameterValue
import java.util.function.Function

/*
 * Implementers of this interface provide details about a particular BigQuery table column
 * and how to extract appropriately typed values for either DML upload ( a
 * QueryParameterValue-valued function) or Streaming insert (InsertAllRequest.RowToInsert map entry).
 *
 * Since tables usually have multiple columns needing this type of extractor, the first use case
 * of this interface is as a contract for enum classes, one per table.
 */
interface ColumnValueExtractor<MODEL_T> {
    // Parameter name (without any @ sign). The convention is snake_case. This value is used in
    // creating named parameter keys (with a numerical suffix) for DML statements and map keys for
    // RowToInsert objects.
    val parameterName: String?

    // Provide a function to map the target model to a column-specific Java type corresponding to the
    // conventions of BQ InsertAllRequests. There are some small differences with this representation
    // and
    // QueryParameterValue types. The function should return return null if the value is not present
    // on the model.
    val rowToInsertValueFunction: Function<MODEL_T, Any?>

    // Provide a function that constructs a QueryParameterValue object of the appropriate type
    // for the column. Expected number and String formats for InsertAllRequest.RowToInsert don't
    //  always match those for QueryParameterValue. For missing values, a QueryParameterValue
    // object of the correct type should be returned containing the value null.
    val queryParameterValueFunction: Function<MODEL_T, QueryParameterValue>

    // A friendly method to call the instance-provided rowToInsertValueFunction. Returns
    // a map value (or null) for a RowToInsert object.
    fun getRowToInsertValue(model: MODEL_T): Any? {
        return rowToInsertValueFunction.apply(model)
    }

    // A friendly method to call the instance-proviced queryParameterValueFunction.
    fun toParameterValue(model: MODEL_T): QueryParameterValue {
        return queryParameterValueFunction.apply(model)
    }
}
