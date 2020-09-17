package org.pmiops.workbench.cohortbuilder.util;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.StandardSQLTypeName;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.hibernate.engine.jdbc.internal.BasicFormatterImpl;
import org.jetbrains.annotations.NotNull;
import org.pmiops.workbench.utils.Matchers;

public final class QueryParameterValues {
  private static final int MICROSECONDS_IN_MILLISECOND = 1000;

  // For creating a Timestamp QueryParameterValue, use this formatter.
  // example error when using the RowToInsert version (below): "Invalid format:
  // "1989-02-17 00:00:00.000000" is too short". See https://stackoverflow.com/a/55155067/611672
  // for a nice walkthrough of the machinations involved.
  public static final DateTimeFormatter QPV_TIMESTAMP_FORMATTER =
      DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss.SSSSSSxxx");

  // For inserting a Timestamp in a RowToInsert map for an InsertAllRequest, use this format
  public static final DateTimeFormatter ROW_TO_INSERT_TIMESTAMP_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")
          .withZone(ZoneOffset.UTC)
          .withLocale(Locale.US);

  /** Generate a unique parameter name and add it to the parameter map provided. */
  public static String buildParameter(
      Map<String, QueryParameterValue> queryParameterValueMap,
      @NotNull QueryParameterValue queryParameterValue) {
    String parameterName = "p" + queryParameterValueMap.size();
    queryParameterValueMap.put(parameterName, queryParameterValue);
    return decorateParameterName(parameterName);
  }

  @Nullable
  public static QueryParameterValue instantToQPValue(@Nullable Instant instant) {
    return Optional.ofNullable(instant)
        .map(Instant::toEpochMilli)
        .map(milli -> milli * MICROSECONDS_IN_MILLISECOND)
        .map(QueryParameterValue::timestamp)
        .orElse(null);
  }

  @Nullable
  public static Instant timestampStringToInstant(@Nullable String timestamp) {
    return Optional.ofNullable(timestamp)
        .map(s -> ZonedDateTime.parse(s, ROW_TO_INSERT_TIMESTAMP_FORMATTER))
        .map(ZonedDateTime::toInstant)
        .orElse(null);
  }

  @Nullable
  public static Instant timestampQpvToInstant(@Nullable QueryParameterValue queryParameterValue) {
    if (queryParameterValue == null || queryParameterValue.getValue() == null) {
      return null;
    }
    if (!isTimestampQpv(queryParameterValue)) {
      throw new IllegalArgumentException(
          String.format(
              "QueryParameterValue %s is not a timestamp", queryParameterValue.getValue()));
    }
    return ZonedDateTime.parse(queryParameterValue.getValue(), QPV_TIMESTAMP_FORMATTER).toInstant();
  }

  @Nullable
  public static OffsetDateTime timestampQpvToOffsetDateTime(
      @Nullable QueryParameterValue queryParameterValue) {
    return Optional.ofNullable(queryParameterValue)
        .map(QueryParameterValues::timestampQpvToInstant)
        .map(i -> OffsetDateTime.ofInstant(i, ZoneOffset.UTC))
        .orElse(null);
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

  @Nullable
  public static QueryParameterValue toTimestampQpv(@Nullable OffsetDateTime offsetDateTime) {
    return Optional.ofNullable(offsetDateTime)
        .map(t -> QueryParameterValue.timestamp(QPV_TIMESTAMP_FORMATTER.format(t)))
        .orElse(null);
  }

  @Nullable
  public static String toInsertRowString(@Nullable OffsetDateTime offsetDateTime) {
    return Optional.ofNullable(offsetDateTime)
        .map(ROW_TO_INSERT_TIMESTAMP_FORMATTER::format)
        .orElse(null);
  }

  @Nullable
  public static OffsetDateTime rowToInsertStringToOffsetTimestamp(@Nullable String timeString) {
    return Optional.ofNullable(timeString)
        .filter(s -> s.length() > 0)
        .map(ROW_TO_INSERT_TIMESTAMP_FORMATTER::parse)
        .map(LocalDateTime::from)
        .map(ldt -> OffsetDateTime.of(ldt, ZoneOffset.UTC))
        .orElse(null);
  }

  @Nullable
  public static <T extends Enum<T>> QueryParameterValue enumToQpv(T enumValue) {
    return Optional.ofNullable(enumValue)
        .map(T::toString)
        .map(QueryParameterValue::string)
        .orElse(null);
  }

  private static String getReplacementString(QueryParameterValue parameterValue) {
    final String value =
        Optional.ofNullable(parameterValue).map(QueryParameterValue::getValue).orElse("NULL");

    if (isTimestampQpv(parameterValue)) {
      return String.format("TIMESTAMP '%s'", value);
    } else {
      return value;
    }
  }

  private static boolean isTimestampQpv(QueryParameterValue parameterValue) {
    return Optional.ofNullable(parameterValue)
        .map(QueryParameterValue::getType)
        .map(t -> t == StandardSQLTypeName.TIMESTAMP)
        .orElse(false);
  }
}
