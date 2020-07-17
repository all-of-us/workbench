package org.pmiops.workbench.cohortbuilder.util;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.StandardSQLTypeName;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.hibernate.engine.jdbc.internal.BasicFormatterImpl;
import org.pmiops.workbench.utils.Matchers;

public final class QueryParameterValues {
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
  public static String replaceNamedParameters(QueryJobConfiguration queryJobConfiguration) {
    String result = queryJobConfiguration.getQuery();
    final Map<Pattern, String> patternToReplacement =
        queryJobConfiguration.getNamedParameters().entrySet().stream()
            .collect(
                Collectors.toMap(
                    e -> buildParameterRegex(e.getKey()), e -> getReplacementString(e.getValue())));
    return Matchers.replaceAllInMap(patternToReplacement, result);
  }

  public static String formatQuery(String query) {
    return new BasicFormatterImpl().format(query);
  }

  // use lookbehind for non-word character, since "'"(@" or " @" don't represent left-side word
  // boundaries.
  private static Pattern buildParameterRegex(String parameterName) {
    return Pattern.compile(String.format("(?<=\\W)%s\\b", decorateParameterName(parameterName)));
  }

  public static String decorateParameterName(String parameterName) {
    return "@" + parameterName;
  }

  private static String getReplacementString(QueryParameterValue parameterValue) {
    final String value =
        Optional.ofNullable(parameterValue).map(QueryParameterValue::getValue).orElse("NULL");

    final boolean isTimestamp =
        Optional.ofNullable(parameterValue)
            .map(QueryParameterValue::getType)
            .map(t -> t == StandardSQLTypeName.TIMESTAMP)
            .orElse(false);

    if (isTimestamp) {
      return String.format("TIMESTAMP '%s'", value);
    } else {
      return value;
    }
  }
}
