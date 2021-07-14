package org.pmiops.workbench.reporting.insertion;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static org.pmiops.workbench.cohortbuilder.util.QueryParameterValues.rowToInsertStringToOffsetTimestamp;
import static org.pmiops.workbench.testconfig.ReportingTestUtils.WORKSPACE__CDR_VERSION_ID;
import static org.pmiops.workbench.testconfig.ReportingTestUtils.WORKSPACE__LAST_ACCESSED_TIME;
import static org.pmiops.workbench.testconfig.ReportingTestUtils.WORKSPACE__NAME;
import static org.pmiops.workbench.testconfig.ReportingTestUtils.WORKSPACE__PUBLISHED;
import static org.pmiops.workbench.testconfig.ReportingTestUtils.createDtoWorkspace;
import static org.pmiops.workbench.testconfig.fixtures.ReportingUserFixture.USER__DEMOGRAPHIC_SURVEY_COMPLETION_TIME;
import static org.pmiops.workbench.testconfig.fixtures.ReportingUserFixture.USER__DISABLED;
import static org.pmiops.workbench.testconfig.fixtures.ReportingUserFixture.USER__PROFESSIONAL_URL;
import static org.pmiops.workbench.utils.TimeAssertions.assertTimeApprox;
import static org.pmiops.workbench.utils.mappers.CommonMappers.offsetDateTimeUtc;

import com.google.cloud.bigquery.InsertAllRequest;
import com.google.cloud.bigquery.InsertAllRequest.RowToInsert;
import com.google.cloud.bigquery.TableId;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.SpringTest;
import org.pmiops.workbench.cohortbuilder.util.QueryParameterValues;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.model.ReportingUser;
import org.pmiops.workbench.model.ReportingWorkspace;
import org.pmiops.workbench.testconfig.ReportingTestConfig;
import org.pmiops.workbench.testconfig.fixtures.ReportingTestFixture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

public class InsertAllRequestBuilderTest extends SpringTest {

  private static final InsertAllRequestPayloadTransformer<ReportingUser>
      USER_INSERT_ALL_REQUEST_BUILDER = UserColumnValueExtractor::values;
  private static final InsertAllRequestPayloadTransformer<ReportingWorkspace>
      WORKSPACE_REQUEST_BUILDER = WorkspaceColumnValueExtractor::values;
  private static final Instant PRINCE_PARTY_TIME = Instant.parse("1999-12-31T23:59:59.99Z");
  private static final Map<String, Object> FIXED_VALUES =
      ImmutableMap.of("snapshot_timestamp", PRINCE_PARTY_TIME.toEpochMilli());

  private static final List<ReportingUser> USERS_WITH_NULL_FIELDS =
      ImmutableList.of(
          new ReportingUser().username("user1").givenName("Onceler").disabled(false).userId(1L),
          new ReportingUser().username(null).givenName("Nullson").disabled(false).userId(111L),
          new ReportingUser()
              .username("america@usa.gov")
              .givenName(null)
              .disabled(false)
              .userId(212L),
          new ReportingUser().username(null).givenName(null).disabled(true).userId(313L));
  private static final TableId TABLE_ID = TableId.of("project ID", "dataset", "researcher");
  public static final int INCOMPLETE_USER_SIZE = 5; // includes snapshot_timestamp

  @Autowired ReportingTestFixture<DbUser, ReportingUser> userFixture;

  @TestConfiguration
  @Import({ReportingTestConfig.class})
  public static class Config {}

  // regression test for RW-5437
  @Test
  public void build_tolerates_nulls() {
    final InsertAllRequest request =
        USER_INSERT_ALL_REQUEST_BUILDER.build(TABLE_ID, USERS_WITH_NULL_FIELDS, FIXED_VALUES);
    assertThat(request.getTable()).isEqualTo(TABLE_ID);
    assertThat(request.ignoreUnknownValues()).isFalse();

    assertThat(request.getRows()).hasSize(USERS_WITH_NULL_FIELDS.size());

    final Map<String, Object> row0ContentMap = request.getRows().get(0).getContent();
    assertThat(row0ContentMap).hasSize(INCOMPLETE_USER_SIZE);
    assertThat(row0ContentMap.get("phone_number")).isNull();
    assertThat(row0ContentMap.get("username")).isEqualTo("user1");
  }

  @Test
  public void testBuild_emptyInputs() {
    final InsertAllRequest request =
        USER_INSERT_ALL_REQUEST_BUILDER.build(
            TABLE_ID, Collections.emptyList(), Collections.emptyMap());
    assertThat(request).isNotNull();
    assertThat(request.getTable()).isEqualTo(TABLE_ID);
    assertThat(request.ignoreUnknownValues()).isFalse();
    assertThat(request.getRows()).isEmpty();
  }

  @Test
  public void testBuildUser_AllFields() {
    final ReportingUser user_all_fields = userFixture.createDto();
    final InsertAllRequest request =
        USER_INSERT_ALL_REQUEST_BUILDER.build(
            TABLE_ID, ImmutableList.of(user_all_fields), FIXED_VALUES);
    assertThat(request.getRows()).hasSize(1);
    final RowToInsert rowToInsert = request.getRows().get(0);
    assertThat(rowToInsert.getId()).isNotNull();

    final Map<String, Object> contentMap = rowToInsert.getContent();
    assertTimeApprox((long) contentMap.get("snapshot_timestamp"), PRINCE_PARTY_TIME.toEpochMilli());
    assertThat(contentMap).isNotEmpty();
    assertThat(contentMap.get("professional_url")).isEqualTo(USER__PROFESSIONAL_URL);
    assertThat(contentMap.get("disabled")).isEqualTo(USER__DISABLED);
    final Optional<OffsetDateTime> odt =
        rowToInsertStringToOffsetTimestamp(
            (String) contentMap.get("demographic_survey_completion_time"));
    assertThat(odt).isPresent();
    assertTimeApprox(odt.get(), offsetDateTimeUtc(USER__DEMOGRAPHIC_SURVEY_COMPLETION_TIME));
  }

  @Test
  public void testWorkspace_allFields() {
    final ReportingWorkspace workspace = createDtoWorkspace();
    final InsertAllRequest request =
        WORKSPACE_REQUEST_BUILDER.build(TABLE_ID, ImmutableList.of(workspace), FIXED_VALUES);
    assertThat(request.getRows()).hasSize(1);
    final RowToInsert rowToInsert = request.getRows().get(0);
    assertThat(rowToInsert.getId()).isNotNull();

    final Map<String, Object> contentMap = rowToInsert.getContent();
    assertTimeApprox((long) contentMap.get("snapshot_timestamp"), PRINCE_PARTY_TIME.toEpochMilli());
    assertThat(contentMap)
        .hasSize(FIXED_VALUES.size() + WorkspaceColumnValueExtractor.values().length);
    assertThat(contentMap.get("cdr_version_id")).isEqualTo(WORKSPACE__CDR_VERSION_ID);
    assertThat(contentMap.get("name")).isEqualTo(WORKSPACE__NAME);
    assertThat(contentMap.get("published")).isEqualTo(WORKSPACE__PUBLISHED);

    final String timeString = (String) contentMap.get("last_accessed_time");
    final OffsetDateTime offsetDateTime =
        QueryParameterValues.rowToInsertStringToOffsetTimestamp(timeString).get();
    assertTimeApprox(offsetDateTime, WORKSPACE__LAST_ACCESSED_TIME);
  }
}
