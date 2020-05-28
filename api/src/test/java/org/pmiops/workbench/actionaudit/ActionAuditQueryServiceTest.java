package org.pmiops.workbench.actionaudit;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

import com.google.api.gax.paging.Page;
import com.google.cloud.bigquery.EmptyTableResult;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.LegacySQLTypeName;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.TableResult;
import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.List;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.model.AuditLogEntry;
import org.pmiops.workbench.model.WorkspaceAuditLogQueryResponse;
import org.pmiops.workbench.utils.FakeSinglePage;
import org.pmiops.workbench.utils.FieldValues;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class ActionAuditQueryServiceTest {

  // N.B. Field order must match that of SELECT statement in Workspaces query.
  private static final Schema WORKSPACE_QUERY_SCHEMA =
      Schema.of(
          ImmutableList.of(
              Field.of("event_time", LegacySQLTypeName.TIMESTAMP),
              Field.of("agent_type", LegacySQLTypeName.STRING),
              Field.of("agent_id", LegacySQLTypeName.INTEGER),
              Field.of("agent_username", LegacySQLTypeName.STRING),
              Field.of("action_id", LegacySQLTypeName.INTEGER),
              Field.of("action_type", LegacySQLTypeName.STRING),
              Field.of("target_type", LegacySQLTypeName.STRING),
              Field.of("target_id", LegacySQLTypeName.INTEGER),
              Field.of("target_property", LegacySQLTypeName.STRING),
              Field.of("prev_value", LegacySQLTypeName.STRING),
              Field.of("new_value", LegacySQLTypeName.STRING)));

  private static final long WORKSPACE_DATABASE_ID = 101L;
  private static final long AGENT_ID = 202L;
  private static final String ACTION_ID_1 = "abfcb9ed-fa65-4e98-acb2-08b0d8b30000";
  private static final String USERNAME = "jay@unit-test-aou.org";
  private static final DateTime EVENT_DATETIME = DateTime.parse("2010-06-30T01:20+02:00");
  public static final long EVENT_TIME_SECONDS = EVENT_DATETIME.getMillis() / 1000;
  private static final List<FieldValueList> RESULT_ROWS =
      ImmutableList.of(
          FieldValues.buildFieldValueList(
              WORKSPACE_QUERY_SCHEMA.getFields(),
              Arrays.asList(
                  new Object[] {
                    Long.toString(EVENT_TIME_SECONDS),
                    "USER",
                    Long.toString(AGENT_ID),
                    USERNAME,
                    ACTION_ID_1,
                    "CREATE",
                    "WORKSPACE",
                    Long.toString(WORKSPACE_DATABASE_ID),
                    "intended_study",
                    null,
                    "Beats. Bears. Battlestar Gallactica."
                  })),
          FieldValues.buildFieldValueList(
              WORKSPACE_QUERY_SCHEMA.getFields(),
              Arrays.asList(
                  new Object[] {
                    Long.toString(EVENT_TIME_SECONDS),
                    "USER",
                    Long.toString(AGENT_ID),
                    USERNAME,
                    ACTION_ID_1,
                    "DELETE",
                    "WORKSPACE",
                    Long.toString(WORKSPACE_DATABASE_ID),
                    null,
                    null,
                    null
                  })));
  private static final Page<FieldValueList> WORKSPACE_QUERY_RESULT_PAGE =
      new FakeSinglePage<>(RESULT_ROWS);
  private static final TableResult TABLE_RESULT =
      new TableResult(WORKSPACE_QUERY_SCHEMA, RESULT_ROWS.size(), WORKSPACE_QUERY_RESULT_PAGE);
  private static final TableResult EMPTY_RESULT = new EmptyTableResult();
  private static final long DEFAULT_LIMIT = 100L;
  private static final DateTime DEFAULT_AFTER_INCLUSIVE = DateTime.parse("205-02-14T01:20+02:00");
  private static final DateTime DEFAULT_BEFORE_EXCLUSIVE = DateTime.parse("2020-08-30T01:20+02:00");
  private static final long TIME_TOLERANCE_MILLIS = 500;

  @MockBean private BigQueryService mockBigQueryService;
  @Autowired private ActionAuditQueryService actionAuditQueryService;

  @TestConfiguration
  @Import({ActionAuditQueryServiceImpl.class})
  static class Configuration {

    @Bean
    public WorkbenchConfig workbenchConfig() {
      final WorkbenchConfig workbenchConfig = WorkbenchConfig.createEmptyConfig();
      workbenchConfig.actionAudit.bigQueryDataset = "action_audit_unit_test";
      workbenchConfig.actionAudit.bigQueryDataset = "action_audit_unit_test";
      return workbenchConfig;
    }
  }

  @Test
  public void testEmptyTableResultGivesEmptyResponse() {
    doReturn(EMPTY_RESULT).when(mockBigQueryService).executeQuery(any(QueryJobConfiguration.class));
    final WorkspaceAuditLogQueryResponse response =
        actionAuditQueryService.queryEventsForWorkspace(
            WORKSPACE_DATABASE_ID,
            DEFAULT_LIMIT,
            DEFAULT_AFTER_INCLUSIVE,
            DEFAULT_BEFORE_EXCLUSIVE);

    assertThat(response.getLogEntries()).isEmpty();
    assertThat(response.getWorkspaceDatabaseId()).isEqualTo(WORKSPACE_DATABASE_ID);
    assertThat(response.getQuery()).contains("SELECT");
  }

  @Test
  public void testQueryEventsFromWorkspace_returnsRows() {
    doReturn(TABLE_RESULT).when(mockBigQueryService).executeQuery(any(QueryJobConfiguration.class));

    final WorkspaceAuditLogQueryResponse response =
        actionAuditQueryService.queryEventsForWorkspace(
            WORKSPACE_DATABASE_ID,
            DEFAULT_LIMIT,
            DEFAULT_AFTER_INCLUSIVE,
            DEFAULT_BEFORE_EXCLUSIVE);
    assertThat(response.getLogEntries()).hasSize(RESULT_ROWS.size());

    final AuditLogEntry row1 = response.getLogEntries().get(0);
    assertThat(row1.getActionId()).isEqualTo(ACTION_ID_1);
    assertThat(row1.getTargetType()).isEqualTo("WORKSPACE");
    assertThat(row1.getPreviousValue()).isNull();
    assertThat(row1.getAgentId()).isEqualTo(AGENT_ID);
    assertThat((double) row1.getEventTime().getMillis())
        .isWithin(TIME_TOLERANCE_MILLIS)
        .of(EVENT_DATETIME.getMillis());

    final AuditLogEntry row2 = response.getLogEntries().get(1);
    assertThat(row2.getActionId()).isEqualTo(ACTION_ID_1);
    assertThat(row2.getTargetType()).isEqualTo("WORKSPACE");
    assertThat(row2.getPreviousValue()).isNull();
  }
}
