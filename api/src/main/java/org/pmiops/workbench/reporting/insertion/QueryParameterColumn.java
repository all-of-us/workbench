package org.pmiops.workbench.reporting.insertion;

import com.google.cloud.bigquery.QueryParameterValue;
import java.util.function.Function;

public interface QueryParameterColumn<T> {

  int MICROSECODNS_IN_MILLISECOND = 1000;

  // parameter name (without any @ sign). Also matches column name
  String getParameterName();

  Function<T, QueryParameterValue> getQueryParameterValueFunction();

  default QueryParameterValue toParameterValue(T target) {
    return getQueryParameterValueFunction().apply(target);
  }
}
