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
import java.time.temporal.TemporalAccessor;
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
  // "1989-02-17 00:00:00.000000" is too short".
  public static final DateTimeFormatter QPV_TIMESTAMP_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSZZ").withZone(ZoneOffset.UTC);
  // For inserting a Timestamp in a RowToInsert map for an InsertAllRequest, use this format
  public static final DateTimeFormatter ROW_TO_INSERT_TIMESTAMP_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")
          .withZone(ZoneOffset.UTC)
          .withLocale(Locale.US);

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

  public static Instant timestampStringToInstant(String timestamp) {
    return ZonedDateTime.parse(timestamp, ROW_TO_INSERT_TIMESTAMP_FORMATTER).toInstant();
  }

  public static Instant timestampQpvToInstant(QueryParameterValue queryParameterValue) {
    if (!isTimestampQpv(queryParameterValue)) {
      throw new IllegalArgumentException(
          String.format(
              "QueryParameterValue %s is not a timestamp",
              queryParameterValue == null ? "null" : queryParameterValue.getValue()));
    }
    return ZonedDateTime.parse(queryParameterValue.getValue(), QPV_TIMESTAMP_FORMATTER).toInstant();
  }

  public static OffsetDateTime timestampQpvToOffsetDateTime(
      QueryParameterValue queryParameterValue) {
    return OffsetDateTime.ofInstant(timestampQpvToInstant(queryParameterValue), ZoneOffset.UTC);
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

  @NotNull
  public static OffsetDateTime rowToInsertStringToOffsetTimestamp(String timeString) {
    final TemporalAccessor temporalAccessor = ROW_TO_INSERT_TIMESTAMP_FORMATTER.parse(timeString);
    final LocalDateTime localDateTime = LocalDateTime.from(temporalAccessor);
    return OffsetDateTime.of(localDateTime, ZoneOffset.UTC);
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
