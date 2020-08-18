package org.pmiops.workbench.reporting.insertion;

import com.google.cloud.bigquery.QueryParameterValue;
import java.time.Instant;
import java.util.function.Function;
import org.pmiops.workbench.cohortbuilder.util.QueryParameterValues;

public interface QueryParameterColumn<T> {

  int MICROSECODNS_IN_MILLISECOND = 1000;

  //  w -> QueryParameterValue.string
  // parameter name (without any @ sign). Also matches column name
  String getParameterName();

  Function<T, Object> getObjectValueFunction();

  Function<Object, QueryParameterValue> getQueryParameterValueFunction();

  default Object getObjectValue(T model) {
    return getObjectValueFunction().apply(model);
  };

  default QueryParameterValue toParameterValue(T model) {
    return getQueryParameterValueFunction().apply(getObjectValue(model));
  }

}
