package org.pmiops.workbench.reporting.insertion;

import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;

/*
 * Implementers of this interface provide details about a particular BigQuery table column
 * and how to extract appropriately typed values for either DML upload ( a
 * QueryParameterValue-valued function) or Streaming insert (InsertAllRequest.RowToInsert map entry).
 *
 * Since tables usually have multiple columns needing this type of extractor, the first use case
 * of this interface is as a contract for enum classes, one per table.
 */
public interface ColumnValueExtractor<MODEL_T> {

  // Parameter name (without any @ sign). The convention is snake_case. This value is used in
  // creating named parameter keys (with a numerical suffix) for DML statements and map keys for
  // RowToInsert objects.
  String getParameterName();

  // Provide a function to map the target model to a column-specific Java type corresponding to the
  // conventions of BQ InsertAllRequests. There are some small differences with this representation
  // and
  // QueryParameterValue types. The function should return null if the value is not present
  // on the model.
  Function<MODEL_T, Object> getRowToInsertValueFunction();

  // A friendly method to call the instance-provided rowToInsertValueFunction.  Returns a Stream of
  // a map entry for a RowToInsert object, or an empty Stream if the value is null.
  default Stream<Entry<String, Object>> getRowToInsertEntry(@NotNull MODEL_T model) {
    return Stream.ofNullable(getRowToInsertValueFunction().apply(model))
        .map(insertValue -> Map.entry(getParameterName(), insertValue));
  }
}
