package org.pmiops.workbench.cohortbuilder;

import com.google.cloud.bigquery.QueryParameterValue;
import java.util.Map;

public class QueryParameterUtil {

  /** Generate a unique parameter name and add it to the parameter map provided. */
  static String addQueryParameterValue(
      Map<String, QueryParameterValue> queryParameterValueMap,
      QueryParameterValue queryParameterValue) {
    String parameterName = "p" + queryParameterValueMap.size();
    queryParameterValueMap.put(parameterName, queryParameterValue);
    return "@" + parameterName;
  }
}
