package org.pmiops.workbench.utils;

import static com.google.common.truth.Truth.assertThat;

import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.LegacySQLTypeName;
import com.google.cloud.bigquery.Schema;
import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;


public class FieldValuesTest {

  private static final Schema WORKSPACE_QUERY_SCHEMA =
      Schema.of(
          ImmutableList.of(
              Field.of("int_field", LegacySQLTypeName.INTEGER),
              Field.of("null_int_field", LegacySQLTypeName.INTEGER),
              Field.of("double_field", LegacySQLTypeName.FLOAT),
              Field.of("null_double_field", LegacySQLTypeName.FLOAT)));
  public static final long LONG_VALUE = 123456L;
  public static final double DOUBLE_VALUE = 3.14159;
  private static final FieldValueList ROW_1 =
      FieldValues.buildFieldValueList(
          WORKSPACE_QUERY_SCHEMA.getFields(),
          Arrays.asList(
              new Object[] {Long.toString(LONG_VALUE), null, Double.toString(DOUBLE_VALUE), null}));
  public static final double TOLERANCE = 1e-6;

  @Test
  public void testGetLong() {
    final Optional<Long> result = FieldValues.getLong(ROW_1, 0);
    assertThat(result.isPresent()).isTrue();
    assertThat(result.get()).isEqualTo(LONG_VALUE);

    assertThat(FieldValues.getLong(ROW_1, 1).isPresent()).isFalse();
    assertThat(FieldValues.getLong(ROW_1, "null_int_field").isPresent()).isFalse();
  }

  @Test
  public void testGetDouble() {
    assertThat(FieldValues.getDouble(ROW_1, 2).isPresent()).isTrue();
    assertThat(FieldValues.getDouble(ROW_1, "double_field").get())
        .isWithin(TOLERANCE)
        .of(DOUBLE_VALUE);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetLong_invalidFieldName() {
    FieldValues.getLong(ROW_1, "wrong_name");
  }

  @Test(expected = ArrayIndexOutOfBoundsException.class)
  public void testGetLong_invalidIndex() {
    FieldValues.getLong(ROW_1, 999);
  }
}
