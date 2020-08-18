package org.pmiops.workbench.reporting.insertion;

import com.google.cloud.bigquery.QueryParameterValue;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;

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

  // Define a hendful of common QPV builders that take in an object, to be supplied by
  // getObjectValue().
  // The object is needed for streaming inserts (InsertAllRequest maps), and the QPV for the DAL
  // insert
  // statements.
  // Use a short name so this doesn't clutter enum constructor lines too much.
  enum QPVFn implements Function<Object, QueryParameterValue> {
    INT64(obj -> QueryParameterValue.int64((Long) obj)),
    STRING(obj -> QueryParameterValue.string((String) obj)),
    BOOLEAN(obj -> QueryParameterValue.bool((Boolean) obj)),
    TIMESTAMP_MICROS(
        w ->
            QueryParameterValue.timestamp(
                Constants.TIMESTAMP_FORMATTER.format(Instant.ofEpochMilli((Long) w))));

    private final Function<Object, QueryParameterValue> fromObjectFunction;

    QPVFn(Function<Object, QueryParameterValue> fromObjectFunction) {
      this.fromObjectFunction = fromObjectFunction;
    }

    @Override
    public QueryParameterValue apply(Object o) {
      return fromObjectFunction.apply(o);
    }

    private static class Constants {

      private static final String BIG_QUERY_TIMESTAMP_STRING_INPUT_FORMAT =
          "yyyy-MM-dd HH:mm:ss.SSSSSSZZ";
      private static final DateTimeFormatter TIMESTAMP_FORMATTER =
          DateTimeFormatter.ofPattern(BIG_QUERY_TIMESTAMP_STRING_INPUT_FORMAT);
    }
  }
}
