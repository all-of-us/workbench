package org.pmiops.workbench.reporting.insertion;

import com.google.cloud.bigquery.QueryParameterValue;
import java.util.function.Function;

public interface QueryParameterColumn<T> {
  // parameter name (without any @ sign). Also matches column name
  String getParameterName();

  Function<T, Object> getRowToInsertValueFunction();

  Function<T, QueryParameterValue> getQueryParameterValueFunction();

  // Expected number and String formats for InsertAllRequest.RowToInsert don't
  // always match those for QueryParameterValue
  default Object getRowToInsertValue(T model) {
    return getRowToInsertValueFunction().apply(model);
  };

  default QueryParameterValue toParameterValue(T model) {
    return getQueryParameterValueFunction().apply(model);
  }
}
