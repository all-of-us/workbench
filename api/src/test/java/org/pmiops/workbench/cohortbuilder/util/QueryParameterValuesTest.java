package org.pmiops.workbench.cohortbuilder.util;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.pmiops.workbench.cohortbuilder.util.QueryParameterValues.buildParameter;
import static org.pmiops.workbench.cohortbuilder.util.QueryParameterValues.decorateParameterName;
import static org.pmiops.workbench.cohortbuilder.util.QueryParameterValues.enumToQpv;
import static org.pmiops.workbench.cohortbuilder.util.QueryParameterValues.instantToQPValue;
import static org.pmiops.workbench.cohortbuilder.util.QueryParameterValues.rowToInsertStringToOffsetTimestamp;
import static org.pmiops.workbench.cohortbuilder.util.QueryParameterValues.timestampQpvToInstant;
import static org.pmiops.workbench.cohortbuilder.util.QueryParameterValues.timestampQpvToOffsetDateTime;
import static org.pmiops.workbench.cohortbuilder.util.QueryParameterValues.timestampStringToInstant;
import static org.pmiops.workbench.cohortbuilder.util.QueryParameterValues.toTimestampQpv;
import static org.pmiops.workbench.utils.FieldValues.MICROSECONDS_IN_MILLISECOND;
import static org.pmiops.workbench.utils.TimeAssertions.assertTimeApprox;

import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.StandardSQLTypeName;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.model.WorkspaceAccessLevel;

public class QueryParameterValuesTest {

  private static final Map<String, QueryParameterValue> PARAM_MAP = new HashMap<>();
  private static final Instant INSTANT = Instant.parse("2012-12-13T00:00:00.00Z");
  private static final OffsetDateTime OFFSET_DATE_TIME =
      OffsetDateTime.ofInstant(INSTANT, ZoneOffset.UTC);
  private static final QueryParameterValue TIMESTAMP_QPV =
      QueryParameterValue.timestamp(INSTANT.toEpochMilli() * MICROSECONDS_IN_MILLISECOND);

  @BeforeEach
  public void setup() {
    PARAM_MAP.put("foo", QueryParameterValue.int64(99));
    PARAM_MAP.put("bar", QueryParameterValue.string("hooray"));
  }

  @AfterEach
  public void tearDown() {
    PARAM_MAP.clear();
  }

  @Test
  public void testDecorateParameterName() {
    assertThat(decorateParameterName("foo")).isEqualTo("@foo");
  }

  @Test
  public void testBuildParameter() {
    final QueryParameterValue newParameter = QueryParameterValue.int64(42);

    final String parameter = buildParameter(PARAM_MAP, newParameter);
    assertThat(parameter).isEqualTo("@p2");

    assertThat(PARAM_MAP.get("p2")).isEqualTo(newParameter);
  }

  @Test
  public void testBuildParameter_emptyMap_nullQpv() {
    PARAM_MAP.clear();
    final QueryParameterValue nullString = QueryParameterValue.string(null);
    final String parameterName = buildParameter(PARAM_MAP, nullString);
    assertThat(parameterName).isEqualTo("@p0");
    assertThat(PARAM_MAP).hasSize(1);
    assertThat(PARAM_MAP.get("p0")).isEqualTo(nullString);
  }

  @Test
  public void testInstantToQpvValue() {
    final QueryParameterValue qpv = instantToQPValue(INSTANT);

    final Optional<Instant> roundTripInstant = timestampQpvToInstant(qpv);
    assertThat(roundTripInstant).isPresent();
    assertTimeApprox(roundTripInstant.get(), INSTANT);
  }

  @Test
  public void testInstantToQpvValue_nullInput() {
    final QueryParameterValue qpv = instantToQPValue(null);
    assertThat(qpv.getType()).isEqualTo(StandardSQLTypeName.TIMESTAMP);
    assertThat(qpv.getValue()).isNull();
  }

  @Test
  public void testToTimestampQpv() {
    final QueryParameterValue qpv = toTimestampQpv(OFFSET_DATE_TIME);
    assertThat(qpv).isNotNull();
    assertThat(qpv.getType()).isEqualTo(StandardSQLTypeName.TIMESTAMP);
    final Optional<OffsetDateTime> roundTripOdt = timestampQpvToOffsetDateTime(qpv);
    assertThat(roundTripOdt).isPresent();
    assertTimeApprox(roundTripOdt.get(), OFFSET_DATE_TIME);
  }

  @Test
  public void testTimestampQpvToInstant_nullQpvValue() {
    assertThat(timestampQpvToInstant(QueryParameterValue.timestamp((String) null))).isEmpty();
  }

  @Test
  public void testTimestampQpvToInstant_wrongQpvType() {
    assertThrows(IllegalArgumentException.class, () -> timestampQpvToInstant(QueryParameterValue.bool(false)));
  }

  @Test
  public void testTimestampQpvToOffsetDateTime() {
    final Optional<OffsetDateTime> offsetDateTime = timestampQpvToOffsetDateTime(TIMESTAMP_QPV);
    assertTimeApprox(offsetDateTime.get().toInstant(), INSTANT);
  }

  @Test
  public void testTimestampQpvToOffset_nullInput() {
    assertThat(timestampQpvToOffsetDateTime(null)).isEmpty();
  }

  @Test
  public void testRowToInsertStringToOffsetTimestamp() {
    final String timestampString = "2020-09-17 04:30:15.000000";
    final Optional<OffsetDateTime> convertedOdt =
        rowToInsertStringToOffsetTimestamp(timestampString);
    assertThat(convertedOdt).isPresent();

    final OffsetDateTime expected = OffsetDateTime.parse("2020-09-17T04:30:15Z");
    assertTimeApprox(convertedOdt.get(), expected);

    assertThat(rowToInsertStringToOffsetTimestamp(null)).isEmpty();
  }

  @Test
  public void testTimestampStringToInstant_nullInput() {
    assertThat(timestampStringToInstant(null)).isEmpty();
  }

  @Test
  public void testEnumToQpv() {
    final QueryParameterValue qpv = enumToQpv(WorkspaceAccessLevel.READER);
    assertThat(qpv.getType()).isEqualTo(StandardSQLTypeName.STRING);
    assertThat(qpv.getValue()).isEqualTo("READER");
  }

  @Test
  public void testEnumToQpv_nullInput() {
    final QueryParameterValue qpv = enumToQpv(null);
    assertThat(qpv.getType()).isEqualTo(StandardSQLTypeName.STRING);
    assertThat(qpv.getValue()).isNull();
  }
}
