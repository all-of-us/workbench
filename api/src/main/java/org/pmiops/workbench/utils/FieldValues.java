package org.pmiops.workbench.utils;

import com.google.cloud.bigquery.FieldList;
import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.FieldValue.Attribute;
import com.google.cloud.bigquery.FieldValueList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.joda.time.DateTime;

/** Utility class for working with FieldValueLists, FieldValues, and Fields */
public final class FieldValues {

  public static final int MICROSECONDS_IN_MILLISECOND = 1000;

  private FieldValues() {}

  /** Return an Optional<FieldValue> which is empty if the value is null and present if not. */
  public static Optional<FieldValue> getOptional(FieldValueList row, String fieldName) {
    final FieldValue value = row.get(fieldName);
    if (value.isNull()) {
      return Optional.empty();
    } else {
      return Optional.of(value);
    }
  }

  public static Optional<String> getString(FieldValueList row, String fieldName) {
    return FieldValues.getOptional(row, fieldName).map(FieldValue::getStringValue);
  }

  public static Optional<Long> getLong(FieldValueList row, String fieldName) {
    return FieldValues.getOptional(row, fieldName).map(FieldValue::getLongValue);
  }

  public static Optional<Long> getTimestampMicroseconds(FieldValueList row, String fieldName) {
    return FieldValues.getOptional(row, fieldName).map(FieldValue::getTimestampValue);
  }

  public static Optional<DateTime> getDateTime(FieldValueList row, String fieldName) {
    return getTimestampMicroseconds(row, fieldName)
        .map(micro -> micro / MICROSECONDS_IN_MILLISECOND)
        .map(DateTime::new);
  }

  public static FieldValue toPrimitiveFieldValue(Object value) {
    return FieldValue.of(Attribute.PRIMITIVE, value);
  }

  public static FieldValueList buildFieldValueList(FieldList schemaFieldList, List<Object> values) {
    return FieldValueList.of(
        values.stream().map(FieldValues::toPrimitiveFieldValue).collect(Collectors.toList()),
        schemaFieldList);
  }
}
