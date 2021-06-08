package org.pmiops.workbench.actionaudit;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.pmiops.workbench.utils.TimeAssertions.assertTimeApprox;

import com.google.api.gax.paging.Page;
import com.google.cloud.bigquery.EmptyTableResult;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.LegacySQLTypeName;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.TableResult;
import com.google.common.collect.ImmutableList;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.SpringTest;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.model.AuditLogEntry;
import org.pmiops.workbench.model.UserAuditLogQueryResponse;
import org.pmiops.workbench.model.WorkspaceAuditLogQueryResponse;
import org.pmiops.workbench.utils.FakeSinglePage;
import org.pmiops.workbench.utils.FieldValues;
import org.pmiops.workbench.utils.mappers.AuditLogEntryMapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

public class ActionAuditQueryServiceTest extends SpringTest {

  // N.B. Field order must match that of SELECT statement in Workspaces query
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
  private static final Instant EVENT_INSTANT = Instant.parse("2010-06-30T01:20:00.00Z");
  public static final long EVENT_TIME_SECONDS = EVENT_INSTANT.getEpochSecond();
  private static final List<FieldValueList> WORKSPACE_RESULT_ROWS =
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
      new FakeSinglePage<>(WORKSPACE_RESULT_ROWS);
  private static final TableResult WORKSPACE_TABLE_RESULT =
      new TableResult(
          WORKSPACE_QUERY_SCHEMA, WORKSPACE_RESULT_ROWS.size(), WORKSPACE_QUERY_RESULT_PAGE);
  private static final TableResult EMPTY_RESULT = new EmptyTableResult(null);
  private static final long DEFAULT_LIMIT = 100L;
  private static final Instant DEFAULT_AFTER = Instant.parse("2005-02-14T01:20:00.02Z");
  private static final Instant DEFAULT_BEFORE = Instant.parse("2020-08-30T01:20:00.02Z");
  private static final long USER_DB_ID = 11223344L;
  @MockBean private BigQueryService mockBigQueryService;
  @Autowired private ActionAuditQueryService actionAuditQueryService;

  @TestConfiguration
  @Import({ActionAuditQueryServiceImpl.class, AuditLogEntryMapperImpl.class})
  static class Configuration {

    @Bean
    public WorkbenchConfig workbenchConfig() {
      final WorkbenchConfig workbenchConfig = WorkbenchConfig.createEmptyConfig();
      workbenchConfig.actionAudit.bigQueryDataset = "action_audit_unit_test";
      workbenchConfig.actionAudit.bigQueryTable = "action_audit_unit_test";
      workbenchConfig.server.projectId = "rw-wb-unit-test";
      return workbenchConfig;
    }
  }

  @Test
  public void testEmptyTableResultGivesEmptyResponse() {
    doReturn(EMPTY_RESULT).when(mockBigQueryService).executeQuery(any(QueryJobConfiguration.class));
    final WorkspaceAuditLogQueryResponse response =
        actionAuditQueryService.queryEventsForWorkspace(
            WORKSPACE_DATABASE_ID, DEFAULT_LIMIT, DEFAULT_AFTER, DEFAULT_BEFORE);

    assertThat(response.getLogEntries()).isEmpty();
    assertThat(response.getWorkspaceDatabaseId()).isEqualTo(WORKSPACE_DATABASE_ID);
    assertThat(response.getQuery()).contains("SELECT");
  }

  @Test
  public void testQueryEventsFromWorkspace_returnsRows() {
    doReturn(WORKSPACE_TABLE_RESULT)
        .when(mockBigQueryService)
        .executeQuery(any(QueryJobConfiguration.class));

    final WorkspaceAuditLogQueryResponse response =
        actionAuditQueryService.queryEventsForWorkspace(
            WORKSPACE_DATABASE_ID, DEFAULT_LIMIT, DEFAULT_AFTER, DEFAULT_BEFORE);
    assertThat(response.getLogEntries()).hasSize(WORKSPACE_RESULT_ROWS.size());

    final AuditLogEntry row1 = response.getLogEntries().get(0);
    assertThat(row1.getActionId()).isEqualTo(ACTION_ID_1);
    assertThat(row1.getTargetType()).isEqualTo("WORKSPACE");
    assertThat(row1.getPreviousValue()).isNull();
    assertThat(row1.getAgentId()).isEqualTo(AGENT_ID);
    assertTimeApprox(row1.getEventTime(), EVENT_INSTANT.toEpochMilli());

    final AuditLogEntry row2 = response.getLogEntries().get(1);
    assertThat(row2.getActionId()).isEqualTo(ACTION_ID_1);
    assertThat(row2.getTargetType()).isEqualTo("WORKSPACE");
    assertThat(row2.getPreviousValue()).isNull();

    assertThat(response.getQuery())
        .containsMatch(
            "WHERE\\s+jsonPayload\\.target_id\\s+=\\s+101\\s+AND\\s+jsonPayload.target_type\\s+=\\s+'WORKSPACE'");
  }

  @Test
  public void testQueryUserEvents() {
    doReturn(EMPTY_RESULT).when(mockBigQueryService).executeQuery(any(QueryJobConfiguration.class));

    final UserAuditLogQueryResponse response =
        actionAuditQueryService.queryEventsForUser(
            USER_DB_ID, DEFAULT_LIMIT, DEFAULT_AFTER, DEFAULT_BEFORE);
    assertThat(response.getLogEntries()).isEmpty();
    assertThat(response.getUserDatabaseId()).isEqualTo(USER_DB_ID);
    assertThat(response.getQuery()).contains("SELECT");
  }

  @Test
  public void testPartitionTimeBuffer() {
    doReturn(EMPTY_RESULT).when(mockBigQueryService).executeQuery(any(QueryJobConfiguration.class));
    final Instant after = Instant.parse("2020-03-10T09:30:00.00Z");
    final Instant before = after.plus(Duration.ofDays(5));

    final UserAuditLogQueryResponse response =
        actionAuditQueryService.queryEventsForUser(USER_DB_ID, DEFAULT_LIMIT, after, before);

    final String query = response.getQuery();
    assertThat(query)
        .containsMatch(
            "TIMESTAMP\\s+'2020-03-09 09:30:00\\.\\d{6}\\+00:00'\\s+<=\\s+_PARTITIONTIME");
    assertThat(query)
        .containsMatch(
            "AND\\s+_PARTITIONTIME\\s+<\\s+TIMESTAMP\\s+'2020-03-16 09:30:00\\.\\d{6}\\+00:00'");
  }
}
