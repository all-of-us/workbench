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

  // QueryParameterValue can have a null value, so no need to return an Optional.
  public static QueryParameterValue instantToQPValue(@Nullable Instant instant) {
    final Long epochMicros = Optional.ofNullable(instant)
        .map(Instant::toEpochMilli)
        .map(milli -> milli * MICROSECONDS_IN_MILLISECOND)
        .orElse(null);
    return QueryParameterValue.timestamp(epochMicros);
  }

  // Will return an empty Optional for null input, but parse errors will still throw
  // DateTimeParseException.
  @Nullable
  public static Optional<Instant> timestampStringToInstant(@Nullable String timestamp) {
    return Optional.ofNullable(timestamp)
        .map(s -> ZonedDateTime.parse(s, ROW_TO_INSERT_TIMESTAMP_FORMATTER))
        .map(ZonedDateTime::toInstant);
  }

  public static Optional<Instant> timestampQpvToInstant(@Nullable QueryParameterValue qpv) {
    verifyQpvType(qpv, StandardSQLTypeName.TIMESTAMP);
    return Optional.ofNullable(qpv)
        .map(QueryParameterValue::getValue)
        .map(s -> ZonedDateTime.parse(s, QPV_TIMESTAMP_FORMATTER))
        .map(ZonedDateTime::toInstant);
  }

  public static void verifyQpvType(@NotNull QueryParameterValue queryParameterValue, StandardSQLTypeName expectedType) {
    if (!matchesQpvType(queryParameterValue, expectedType)) {
      throw new IllegalArgumentException(
          String.format(
              "QueryParameterValue %s is not a timestamp", queryParameterValue.getValue()));
    }
  }

  public static Optional<OffsetDateTime> timestampQpvToOffsetDateTime(
      @Nullable QueryParameterValue queryParameterValue) {
    verifyQpvType(queryParameterValue, StandardSQLTypeName.TIMESTAMP);
    return Optional.ofNullable(queryParameterValue)
        .flatMap(QueryParameterValues::timestampQpvToInstant)
        .map(i -> OffsetDateTime.ofInstant(i, ZoneOffset.UTC));
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
    final String arg = Optional.ofNullable(offsetDateTime)
        .map(QPV_TIMESTAMP_FORMATTER::format)
        .orElse(null);
    return  QueryParameterValue.timestamp(arg);
  }

  // Return null instead of Optional.empty() so the return value can go directly into
  // the content map of an InsertAllRequest.RowToInsert.
  @Nullable
  public static String toInsertRowString(@Nullable OffsetDateTime offsetDateTime) {
    return Optional.ofNullable(offsetDateTime)
        .map(ROW_TO_INSERT_TIMESTAMP_FORMATTER::format)
        .orElse(null);
  }

  // BigQuery TIMESTAMP types don't include a zone or offset, but are always UTC.
  public static Optional<OffsetDateTime> rowToInsertStringToOffsetTimestamp(@Nullable String bqTimeString) {
    return Optional.ofNullable(bqTimeString)
        .filter(s -> s.length() > 0)
        .map(ROW_TO_INSERT_TIMESTAMP_FORMATTER::parse)
        .map(LocalDateTime::from)
        .map(ldt -> OffsetDateTime.of(ldt, ZoneOffset.UTC));
  }

  public static <T extends Enum<T>> QueryParameterValue enumToQpv(T enumValue) {
    final String value = Optional.ofNullable(enumValue)
        .map(T::toString)
        .orElse(null);
    return QueryParameterValue.string(value);
  }

  private static String getReplacementString(QueryParameterValue parameterValue) {
    final String value =
        Optional.ofNullable(parameterValue).map(QueryParameterValue::getValue).orElse("NULL");

    if (isQpvTimestamp(parameterValue)) {
      return String.format("TIMESTAMP '%s'", value);
    } else {
      return value;
    }
  }

  private static boolean isQpvTimestamp(QueryParameterValue parameterValue) {
    return matchesQpvType(parameterValue, StandardSQLTypeName.TIMESTAMP);
  }

  // return false if teh parameterValue is non-null does not match the expected type.
  private static boolean matchesQpvType(QueryParameterValue parameterValue, StandardSQLTypeName expectedType) {
    return Optional.ofNullable(parameterValue)
        .map(QueryParameterValue::getType)
        .map(t -> t == expectedType)
        .orElse(true);
  }
}
