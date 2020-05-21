package org.pmiops.workbench.actionaudit;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

import com.google.api.gax.paging.Page;
import com.google.cloud.bigquery.EmptyTableResult;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.FieldList;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.TableResult;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.model.AuditLogEntriesResponse;
import org.pmiops.workbench.utils.FakeSinglePage;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class ActionAuditQueryServiceTest {

  private static final List<Field> WORKSPACE_FIELDS = ImmutableList.of();
  private static final FieldList WORKSPACE_QUERY_FIELDLIST = FieldList.of(WORKSPACE_FIELDS);
  private static final Schema WORKSPACE_QUERY_SCHEMA = Schema.of(WORKSPACE_QUERY_FIELDLIST);
  private static final Page<FieldValueList> WORKSPACE_QUERY_RESULT_PAGE =
      new FakeSinglePage<>(ImmutableList.of());
  private static final long RESULT_PAGE_ROW_COUNT = 3;
  private static final TableResult TABLE_RESULT =
      new TableResult(WORKSPACE_QUERY_SCHEMA, RESULT_PAGE_ROW_COUNT, WORKSPACE_QUERY_RESULT_PAGE);
  private static final TableResult EMPTY_RESULT = new EmptyTableResult();
  public static final long DEFAULT_LIMIT = 100L;
  public static final long WORKSPACE_DATABASE_ID = 101L;

  @MockBean private BigQueryService mockBigQueryService;
  @Autowired private ActionAuditQueryService actionAuditQueryService;

  @TestConfiguration
  @Import({ActionAuditQueryServiceImpl.class})
  @MockBean({WorkspaceService.class})
  static class Configuration {

    @Bean
    public WorkbenchConfig workbenchConfig() {
      final WorkbenchConfig workbenchConfig = WorkbenchConfig.createEmptyConfig();
      workbenchConfig.actionAudit.bigQueryDataset = "action_audit_unit_test";
      workbenchConfig.actionAudit.bigQueryDataset = "action_audit_unit_test";
      return workbenchConfig;
    }
  }

  @Before
  public void setup() {
    //    doReturn(EMPTY_RESULT).when(mockBigQueryService).executeQuery(any());
    //    doReturn(TABLE_RESULT).when(mockBigQueryService).executeQuery(QUERY_JOB_CONFIGURATION);
  }

  @Test
  public void testEmptyTableResultGivesEmptyResponse() {
    doReturn(EMPTY_RESULT).when(mockBigQueryService).executeQuery(any());
    final AuditLogEntriesResponse response =
        actionAuditQueryService.queryEventsForWorkspace(WORKSPACE_DATABASE_ID, DEFAULT_LIMIT);
    assertThat(response.getLogEntries()).isEmpty();

    @SuppressWarnings("unchecked")
    final Map<String, String> metadataMap = (Map<String, String>) response.getQueryMetadata();
    assertThat(metadataMap.get("workspaceDatabaseId"))
        .isEqualTo(Long.toString(WORKSPACE_DATABASE_ID));
    assertThat(metadataMap.get("query")).contains("SELECT");
  }

  @Test
  public void testQueryEventsFromWorkspace_findsRows() {}
}
