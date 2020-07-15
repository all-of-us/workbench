package org.pmiops.workbench.cohortbuilder.util;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.StandardSQLTypeName;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

public final class BQParameterUtil {
  private static final int MICROSECONDS_IN_MILLISECOND = 1000;

  /** Generate a unique parameter name and add it to the parameter map provided. */
  public static String buildParameter(
      Map<String, QueryParameterValue> queryParameterValueMap,
      QueryParameterValue queryParameterValue) {
    String parameterName = "p" + queryParameterValueMap.size();
    queryParameterValueMap.put(parameterName, queryParameterValue);
    return decorateParameterName(parameterName);
  }

  public static QueryParameterValue instantToQPValue(Instant instant) {
    return QueryParameterValue.timestamp(instant.toEpochMilli() * MICROSECONDS_IN_MILLISECOND);
  }

  // Since BigQuery doesn't expose the literal query string built from a QueryJobConfiguration,
  // this method does the next best thing. Useful for diagnostics, logging, testing, etc.
  public static String getReplacedQueryText(QueryJobConfiguration queryJobConfiguration) {
    String result = "-- reconstructed query text\n" + queryJobConfiguration.getQuery();
    final Map<String, QueryParameterValue> keyToNamedParameter =
        queryJobConfiguration.getNamedParameters().entrySet().stream()
            .collect(Collectors.toMap(e -> decorateParameterName(e.getKey()), Entry::getValue));

    // Sort in reverse lenght order so we don't partially replace any parameter names (e.g. replace
    // "@foo" before "@foo_bar").
    final List<String> keysByLengthDesc =
        keyToNamedParameter.keySet().stream()
            .sorted((a, b) -> b.length() - a.length())
            .collect(Collectors.toList());

    final Map<String, String> keyToStringValue =
        keysByLengthDesc.stream()
            .collect(Collectors.toMap(k -> k, k -> getReplacementString(k, keyToNamedParameter)));

    for (String key : keysByLengthDesc) {
      result = result.replace(key, keyToStringValue.getOrDefault(key, "NULL"));
    }
    result = result.replace("\n", " ");
    return result;
  }

  public static String decorateParameterName(String parameterName) {
    return "@" + parameterName;
  }

  private static String getReplacementString(
      String key, Map<String, QueryParameterValue> keyToNamedParameter) {
    final QueryParameterValue parameterValue = keyToNamedParameter.get(key);
    final String rawStringValue =
        Optional.ofNullable(parameterValue).map(QueryParameterValue::getValue).orElse("NULL");

    final StandardSQLTypeName typeName =
        Optional.ofNullable(parameterValue)
            .map(QueryParameterValue::getType)
            .orElse(StandardSQLTypeName.STRING);

    final String replacement;
    if (typeName == StandardSQLTypeName.TIMESTAMP) {
      replacement = String.format("TIMESTAMP '%s'", rawStringValue);
    } else {
      replacement = rawStringValue;
    }
    return replacement;
  }
}
