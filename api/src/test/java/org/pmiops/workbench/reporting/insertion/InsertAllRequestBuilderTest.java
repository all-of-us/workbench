package org.pmiops.workbench.reporting.insertion;

import static com.google.common.truth.Truth.assertThat;
import static org.pmiops.workbench.testconfig.ReportingTestUtils.USER__DEMOGRAPHIC_SURVEY_COMPLETION_TIME;
import static org.pmiops.workbench.testconfig.ReportingTestUtils.USER__DISABLED;
import static org.pmiops.workbench.testconfig.ReportingTestUtils.USER__FREE_TIER_CREDITS_LIMIT_DAYS_OVERRIDE;
import static org.pmiops.workbench.testconfig.ReportingTestUtils.USER__PROFESSIONAL_URL;
import static org.pmiops.workbench.testconfig.ReportingTestUtils.WORKSPACE__BILLING_ACCOUNT_NAME;
import static org.pmiops.workbench.testconfig.ReportingTestUtils.WORKSPACE__CDR_VERSION_ID;
import static org.pmiops.workbench.testconfig.ReportingTestUtils.WORKSPACE__LAST_ACCESSED_TIME;
import static org.pmiops.workbench.testconfig.ReportingTestUtils.WORKSPACE__PUBLISHED;
import static org.pmiops.workbench.testconfig.ReportingTestUtils.createDtoUser;
import static org.pmiops.workbench.testconfig.ReportingTestUtils.createDtoWorkspace;
import static org.pmiops.workbench.utils.TimeAssertions.assertTimeApprox;

import com.google.cloud.bigquery.InsertAllRequest;
import com.google.cloud.bigquery.InsertAllRequest.RowToInsert;
import com.google.cloud.bigquery.TableId;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.model.BqDtoUser;
import org.pmiops.workbench.model.BqDtoWorkspace;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class InsertAllRequestBuilderTest {
  private static final InsertAllRequestBuilder<BqDtoUser> USER_INSERT_ALL_REQUEST_BUILDER =
      UserParameterColumn::values;
  private static final InsertAllRequestBuilder<BqDtoWorkspace> WORKSPACE_REQUEST_BUILDER =
      WorkspaceParameterColumn::values;
  private static final Instant PRINCE_PARTY_TIME = Instant.parse("1999-12-31T23:59:59.99Z");
  private static final Map<String, Object> FIXED_VALUES =
      ImmutableMap.of("snapshot_timestamp", PRINCE_PARTY_TIME.toEpochMilli());

  private static final List<BqDtoUser> USERS =
      ImmutableList.of(
          new BqDtoUser().username("user1").givenName("Onceler").disabled(false).userId(1L),
          new BqDtoUser().username(null).givenName("Nullson").disabled(false).userId(111L),
          new BqDtoUser().username("america@usa.gov").givenName(null).disabled(false).userId(212L),
          new BqDtoUser().username(null).givenName(null).disabled(true).userId(313L));
  private static final TableId TABLE_ID = TableId.of("project ID", "dataset", "researcher");

  // regression test for RW-5437
  @Test
  public void build_tolerates_nulls() {
    InsertAllRequest request = USER_INSERT_ALL_REQUEST_BUILDER.build(TABLE_ID, USERS, FIXED_VALUES);

    assertThat(request.getRows()).hasSize(USERS.size());
    assertThat(request.getRows().get(0).getContent().get("phone_number")).isNull();
    assertThat(request.getRows().get(0).getContent().get("username")).isEqualTo("user1");
  }

  @Test
  public void testBuildUser_AllFields() {
    final BqDtoUser user_all_fields = createDtoUser();
    final InsertAllRequest request =
        USER_INSERT_ALL_REQUEST_BUILDER.build(
            TABLE_ID, ImmutableList.of(user_all_fields), FIXED_VALUES);
    assertThat(request.getRows()).hasSize(1);
    final RowToInsert rowToInsert = request.getRows().get(0);
    assertThat(rowToInsert.getId()).isNotNull();

    final Map<String, Object> contentMap = rowToInsert.getContent();
    assertTimeApprox((long) contentMap.get("snapshot_timestamp"), PRINCE_PARTY_TIME.toEpochMilli());
    assertThat(contentMap).hasSize(FIXED_VALUES.size() + UserParameterColumn.values().length);
    assertThat(contentMap.get("professional_url")).isEqualTo(USER__PROFESSIONAL_URL);
    assertThat(contentMap.get("free_tier_credits_limit_days_override"))
        .isEqualTo(USER__FREE_TIER_CREDITS_LIMIT_DAYS_OVERRIDE);
    assertThat(contentMap.get("disabled")).isEqualTo(USER__DISABLED);
    assertTimeApprox(
        (OffsetDateTime) contentMap.get("demographic_survey_completion_time"),
        CommonMappers.offsetDateTimeUtc(USER__DEMOGRAPHIC_SURVEY_COMPLETION_TIME));
  }

  @Test
  public void testWorkspace_allFields() {
    final BqDtoWorkspace workspace = createDtoWorkspace();
    final InsertAllRequest request =
        WORKSPACE_REQUEST_BUILDER.build(TABLE_ID, ImmutableList.of(workspace), FIXED_VALUES);
    assertThat(request.getRows()).hasSize(1);
    final RowToInsert rowToInsert = request.getRows().get(0);
    assertThat(rowToInsert.getId()).isNotNull();

    final Map<String, Object> contentMap = rowToInsert.getContent();
    assertTimeApprox((long) contentMap.get("snapshot_timestamp"), PRINCE_PARTY_TIME.toEpochMilli());
    assertThat(contentMap).hasSize(FIXED_VALUES.size() + WorkspaceParameterColumn.values().length);
    assertThat(contentMap.get("billing_account_name")).isEqualTo(WORKSPACE__BILLING_ACCOUNT_NAME);
    assertThat(contentMap.get("cdr_version_id")).isEqualTo(WORKSPACE__CDR_VERSION_ID);
    assertThat(contentMap.get("published")).isEqualTo(WORKSPACE__PUBLISHED);
    assertTimeApprox(
        (OffsetDateTime) contentMap.get("last_accessed_time"),
        CommonMappers.offsetDateTimeUtc(WORKSPACE__LAST_ACCESSED_TIME));
  }
}
