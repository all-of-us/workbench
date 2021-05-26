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
  @NotNull
  public static String buildParameter(
      @NotNull Map<String, QueryParameterValue> queryParameterValueMap,
      @NotNull QueryParameterValue queryParameterValue) {
    String parameterName = "p" + queryParameterValueMap.size();
    queryParameterValueMap.put(parameterName, queryParameterValue);
    return decorateParameterName(parameterName);
  }

  // QueryParameterValue can have a null value, so no need to return an Optional.
  @NotNull
  public static QueryParameterValue instantToQPValue(@Nullable Instant instant) {
    if (instant == null) {
      return QueryParameterValue.timestamp((Long) null);
    } else {
      final long epochMicros = instant.toEpochMilli() * MICROSECONDS_IN_MILLISECOND;
      return QueryParameterValue.timestamp(epochMicros);
    }
  }

  // Will return an empty Optional for null input, but parse errors will still throw
  // DateTimeParseException.
  public static Optional<Instant> timestampStringToInstant(@Nullable String timestamp) {
    return Optional.ofNullable(timestamp)
        .map(s -> ZonedDateTime.parse(s, ROW_TO_INSERT_TIMESTAMP_FORMATTER))
        .map(ZonedDateTime::toInstant);
  }

  public static Optional<Instant> timestampQpvToInstant(@NotNull QueryParameterValue qpv) {
    verifyQpvType(qpv, StandardSQLTypeName.TIMESTAMP);
    return Optional.ofNullable(qpv.getValue())
        .map(s -> ZonedDateTime.parse(s, QPV_TIMESTAMP_FORMATTER))
        .map(ZonedDateTime::toInstant);
  }

  public static void verifyQpvType(
      @Nullable QueryParameterValue queryParameterValue, StandardSQLTypeName expectedType) {
    if (queryParameterValue != null && !matchesQpvType(queryParameterValue, expectedType)) {
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
  @NotNull
  public static String replaceNamedParameters(
      @NotNull QueryJobConfiguration queryJobConfiguration) {
    String result = queryJobConfiguration.getQuery();
    final Map<Pattern, String> patternToReplacement =
        queryJobConfiguration.getNamedParameters().entrySet().stream()
            .collect(
                Collectors.toMap(
                    e -> buildParameterRegex(e.getKey()), e -> getReplacementString(e.getValue())));
    return Matchers.replaceAllInMap(patternToReplacement, result);
  }

  @NotNull
  public static String formatQuery(@NotNull String query) {
    return new BasicFormatterImpl().format(query);
  }

  // use lookbehind for non-word character, since "'"(@" or " @" don't represent left-side word
  // boundaries.
  @NotNull
  private static Pattern buildParameterRegex(@NotNull String parameterName) {
    return Pattern.compile(String.format("(?<=\\W)%s\\b", decorateParameterName(parameterName)));
  }

  @NotNull
  public static String decorateParameterName(@NotNull String parameterName) {
    return "@" + parameterName;
  }

  @Nullable
  public static QueryParameterValue toTimestampQpv(@Nullable OffsetDateTime offsetDateTime) {
    if (offsetDateTime == null) {
      return QueryParameterValue.timestamp((String) null);
    } else {
      return QueryParameterValue.timestamp(QPV_TIMESTAMP_FORMATTER.format(offsetDateTime));
    }
  }

  // Return null instead of Optional.empty() so the return value can go directly into
  // the content map of an InsertAllRequest.RowToInsert.
  @Nullable
  public static String toInsertRowString(@Nullable OffsetDateTime offsetDateTime) {
    if (offsetDateTime == null) {
      return null;
    } else {
      return ROW_TO_INSERT_TIMESTAMP_FORMATTER.format(offsetDateTime);
    }
  }

  // BigQuery TIMESTAMP types don't include a zone or offset, but are always UTC.
  public static Optional<OffsetDateTime> rowToInsertStringToOffsetTimestamp(
      @Nullable String bqTimeString) {
    return Optional.ofNullable(bqTimeString)
        .filter(s -> s.length() > 0)
        .map(ROW_TO_INSERT_TIMESTAMP_FORMATTER::parse)
        .map(LocalDateTime::from)
        .map(ldt -> OffsetDateTime.of(ldt, ZoneOffset.UTC));
  }

  @NotNull
  public static <T extends Enum<T>> QueryParameterValue enumToQpv(@Nullable T enumValue) {
    return QueryParameterValue.string(enumToString(enumValue));
  }

  // RowToInsert enum string or null (to be omitted)
  @Nullable
  public static <T extends Enum<T>> String enumToString(@Nullable T enumValue) {
    if (enumValue == null) {
      return null;
    } else {
      return enumValue.toString();
    }
  }

  @NotNull
  private static String getReplacementString(@Nullable QueryParameterValue parameterValue) {
    final String value =
        Optional.ofNullable(parameterValue).map(QueryParameterValue::getValue).orElse("NULL");

    if (isQpvTimestamp(parameterValue)) {
      return String.format("TIMESTAMP '%s'", value);
    } else {
      return value;
    }
  }

  @NotNull
  private static boolean isQpvTimestamp(@Nullable QueryParameterValue parameterValue) {
    return matchesQpvType(parameterValue, StandardSQLTypeName.TIMESTAMP);
  }

  // return false if teh parameterValue is non-null does not match the expected type.
  private static boolean matchesQpvType(
      QueryParameterValue parameterValue, StandardSQLTypeName expectedType) {
    return Optional.ofNullable(parameterValue.getType()).map(t -> t == expectedType).orElse(true);
  }
}
