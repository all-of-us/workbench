package org.pmiops.workbench.cohortbuilder.util;

import com.google.cloud.bigquery.QueryParameterValue;
import java.util.Map;

public final class BQParameterUtil {

  /** Generate a unique parameter name and add it to the parameter map provided. */
  public static String buildParameter(
      Map<String, QueryParameterValue> queryParameterValueMap,
      QueryParameterValue queryParameterValue) {
    String parameterName = "p" + queryParameterValueMap.size();
    queryParameterValueMap.put(parameterName, queryParameterValue);
    return "@" + parameterName;
  }
}
