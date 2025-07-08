package org.pmiops.workbench.reporting;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

import jakarta.inject.Provider;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.jdbc.ReportingQueryService;
import org.pmiops.workbench.model.ReportingBase;

@ExtendWith(MockitoExtension.class)
public class ReportingTableServiceTest {

  @Mock private Provider<WorkbenchConfig> mockWorkbenchConfigProvider;
  @Mock private ReportingQueryService mockReportingQueryService;

  private ReportingTableService reportingTableService;

  @BeforeEach
  public void setUp() {
    // Create real config objects since they have public fields, not getter methods
    WorkbenchConfig workbenchConfig = WorkbenchConfig.createEmptyConfig();
    workbenchConfig.reporting.maxRowsPerInsert = 1000;

    when(mockWorkbenchConfigProvider.get()).thenReturn(workbenchConfig);

    reportingTableService =
        new ReportingTableService(mockWorkbenchConfigProvider, mockReportingQueryService);
  }

  @Test
  public void testGetAll_withValidTableNames_returnsFilteredTables() {
    // Test filtering with valid table names
    List<String> requestedTables = Arrays.asList("cohort", "user", "workspace");

    List<ReportingTableParams<? extends ReportingBase>> result =
        reportingTableService.getAll(requestedTables);

    assertThat(result).hasSize(3);

    // Verify that the returned tables match the requested table names
    List<String> returnedTableNames =
        result.stream().map(ReportingTableParams::bqTableName).toList();

    assertThat(returnedTableNames).containsExactlyElementsIn(requestedTables);
  }

  @Test
  public void testGetAll_withSingleTableName_returnsSingleTable() {
    List<String> requestedTables = List.of("institution");

    List<ReportingTableParams<? extends ReportingBase>> result =
        reportingTableService.getAll(requestedTables);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).bqTableName()).isEqualTo("institution");
  }

  @Test
  public void testGetAll_withNonExistentTableNames_returnsEmptyList() {
    List<String> requestedTables = Arrays.asList("non_existent_table", "another_fake_table");

    List<ReportingTableParams<? extends ReportingBase>> result =
        reportingTableService.getAll(requestedTables);

    assertThat(result).isEmpty();
  }

  @Test
  public void testGetAll_withMixedValidAndInvalidTableNames_returnsOnlyValidTables() {
    List<String> requestedTables =
        Arrays.asList("cohort", "invalid_table", "user", "another_invalid_table", "dataset");

    List<ReportingTableParams<? extends ReportingBase>> result =
        reportingTableService.getAll(requestedTables);

    assertThat(result).hasSize(3);

    List<String> returnedTableNames =
        result.stream().map(ReportingTableParams::bqTableName).toList();

    assertThat(returnedTableNames).containsExactly("cohort", "user", "dataset");
  }

  @Test
  public void testGetAll_withEmptyTableNamesList_returnsEmptyList() {
    List<String> requestedTables = Collections.emptyList();

    List<ReportingTableParams<? extends ReportingBase>> result =
        reportingTableService.getAll(requestedTables);

    assertThat(result).isEmpty();
  }

  @Test
  public void testGetAll_withAllValidTableNames_returnsAllTables() {
    List<String> allValidTableNames =
        Arrays.asList(
            "cohort",
            "dataset",
            "dataset_domain_value",
            "institution",
            "leonardo_app_usage",
            "new_user_satisfaction_survey",
            "user",
            "user_general_discovery_source",
            "user_partner_discovery_source",
            "workspace",
            "workspace_free_tier_usage");

    List<ReportingTableParams<? extends ReportingBase>> result =
        reportingTableService.getAll(allValidTableNames);

    // Should return all 11 tables defined in getAll()
    assertThat(result).hasSize(11);

    List<String> returnedTableNames =
        result.stream().map(ReportingTableParams::bqTableName).toList();

    assertThat(returnedTableNames).containsExactlyElementsIn(allValidTableNames);
  }

  @Test
  public void testGetAll_withDuplicateTableNames_returnsUniqueTablesOnly() {
    List<String> requestedTablesWithDuplicates =
        Arrays.asList(
            "cohort",
            "user",
            "cohort", // duplicate
            "workspace",
            "user" // duplicate
            );

    List<ReportingTableParams<? extends ReportingBase>> result =
        reportingTableService.getAll(requestedTablesWithDuplicates);

    assertThat(result).hasSize(3);

    List<String> returnedTableNames =
        result.stream().map(ReportingTableParams::bqTableName).toList();

    assertThat(returnedTableNames).containsExactly("cohort", "user", "workspace");
  }

  @Test
  public void testGetAll_withCaseSensitiveTableNames_matchesCaseInsensitive() {
    List<String> requestedTablesWithWrongCase = Arrays.asList("COHORT", "User", "workspace");

    List<ReportingTableParams<? extends ReportingBase>> result =
        reportingTableService.getAll(requestedTablesWithWrongCase);
    assertThat(result).hasSize(3);

    List<String> returnedTableNames =
        result.stream().map(ReportingTableParams::bqTableName).toList();

    assertThat(returnedTableNames).containsExactly("cohort", "user", "workspace");
  }
}
