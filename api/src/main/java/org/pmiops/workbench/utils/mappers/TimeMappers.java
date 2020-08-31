package org.pmiops.workbench.utils.mappers;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.mapstruct.Named;

// Static utility class for time conversion that can be used Mapstruct mappers
// via `uses` as well as other contexts.
public class TimeMappers {
  public static Long timestamp(Timestamp timestamp) {
    if (timestamp != null) {
      return timestamp.getTime();
    }
    return null;
  }

  public static OffsetDateTime offsetDateTime(Timestamp timestamp) {
    return Optional.ofNullable(timestamp)
        .map(Timestamp::toInstant)
        .map(instant -> OffsetDateTime.ofInstant(instant, ZoneOffset.UTC))
        .orElse(null);
  }

  public static Timestamp timestamp(OffsetDateTime offsetDateTime) {
    return Optional.ofNullable(offsetDateTime)
        .map(OffsetDateTime::toInstant)
        .map(Timestamp::from)
        .orElse(null);
  }

  public static String timestampToString(Timestamp timestamp) {
    // We are using this method because mapstruct defaults to gregorian conversion. The difference
    // is:
    // Gregorian: "2020-03-30T18:31:50.000Z"
    // toString: "2020-03-30 18:31:50.0"
    if (timestamp != null) {
      return timestamp.toString();
    }
    return null;
  }

  public static Timestamp epochMillisToSqlTimestamp(Long epochMillis) {
    return Optional.ofNullable(epochMillis).map(Timestamp::new).orElse(null);
  }

  @Named("dateToString")
  public static String sqlDateToString(Date date) {
    // We are using this method because mapstruct defaults to gregorian conversion. The difference
    // is:
    // Gregorian: "2020-03-30T18:31:50.000Z"
    // toString: "2020-03-30"
    if (date != null) {
      return date.toString();
    }
    return null;
  }
}
