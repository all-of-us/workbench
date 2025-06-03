package org.pmiops.workbench.initialcredits;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.pmiops.workbench.utils.BigQueryUtils.tableRow;

import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.LegacySQLTypeName;
import com.google.cloud.bigquery.Schema;
import com.google.common.base.Stopwatch;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.utils.BigQueryUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;

@DataJpaTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InitialCreditsBigQueryServiceTest {

  @Autowired private InitialCreditsBigQueryService initialCreditsBigQueryService;

  @MockBean private BigQueryService mockBigQueryService;

  private static WorkbenchConfig config;

  @TestConfiguration
  @Import({
    FakeClockConfiguration.class,
    InitialCreditsBigQueryService.class,
    Stopwatch.class,
  })
  static class Configuration {
    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public WorkbenchConfig workbenchConfig() {
      return config;
    }
  }

  @BeforeEach
  public void init() {
    config = WorkbenchConfig.createEmptyConfig();
  }

  @Test
  public void getAllTerraWorkspaceCostsFromBQ_returnsCorrectCosts() {
    Schema schema =
        Schema.of(
            Field.of("id", LegacySQLTypeName.STRING), Field.of("cost", LegacySQLTypeName.STRING));

    List<FieldValueList> tableRows =
        List.of(tableRow("workspace1", "100.5"), tableRow("workspace2", "200.75"));

    when(mockBigQueryService.executeQuery(any()))
        .thenReturn(BigQueryUtils.newTableResult(schema, tableRows));

    Map<String, Double> result = initialCreditsBigQueryService.getAllTerraWorkspaceCostsFromBQ();

    assertEquals(2, result.size());
    assertEquals(100.5, result.get("workspace1"));
    assertEquals(200.75, result.get("workspace2"));
  }

  @Test
  public void getAllTerraWorkspaceCostsFromBQ_handlesEmptyResult() {
    Schema schema =
        Schema.of(
            Field.of("id", LegacySQLTypeName.STRING), Field.of("cost", LegacySQLTypeName.FLOAT));

    when(mockBigQueryService.executeQuery(any()))
        .thenReturn(BigQueryUtils.newTableResult(schema, List.of()));

    Map<String, Double> result = initialCreditsBigQueryService.getAllTerraWorkspaceCostsFromBQ();

    assertTrue(result.isEmpty());
  }

  @Test
  public void getAllVWBProjectCostsFromBQ_returnsCorrectCosts() {
    Schema schema =
        Schema.of(
            Field.of("id", LegacySQLTypeName.STRING),
            Field.of("total_cost", LegacySQLTypeName.STRING),
            Field.of("vwb_pod_id", LegacySQLTypeName.STRING));

    List<FieldValueList> tableRows =
        List.of(
            tableRow("1", "50.0", "pod1"),
            tableRow("2", "75.0", "pod2"),
            tableRow("3", "25.0", "pod1"));

    when(mockBigQueryService.executeQuery(any()))
        .thenReturn(BigQueryUtils.newTableResult(schema, tableRows));

    Map<String, Double> result = initialCreditsBigQueryService.getAllVWBProjectCostsFromBQ();

    assertEquals(2, result.size());
    assertEquals(75.0, result.get("pod1"));
    assertEquals(75.0, result.get("pod2"));
  }

  @Test
  public void getAllVWBProjectCostsFromBQ_handlesNullPodId() {
    Schema schema =
        Schema.of(
            Field.of("id", LegacySQLTypeName.STRING),
            Field.of("total_cost", LegacySQLTypeName.FLOAT),
            Field.of("vwb_pod_id", LegacySQLTypeName.STRING));

    List<FieldValueList> tableRows =
        List.of(tableRow("1", "50.0", null), tableRow("2", "75.0", "pod2"));

    when(mockBigQueryService.executeQuery(any()))
        .thenReturn(BigQueryUtils.newTableResult(schema, tableRows));

    Map<String, Double> result = initialCreditsBigQueryService.getAllVWBProjectCostsFromBQ();

    assertEquals(1, result.size());
    assertEquals(75.0, result.get("pod2"));
  }

  @Test
  public void getAllVWBProjectCostsFromBQ_handlesEmptyResult() {
    Schema schema =
        Schema.of(
            Field.of("id", LegacySQLTypeName.STRING),
            Field.of("total_cost", LegacySQLTypeName.FLOAT),
            Field.of("vwb_pod_id", LegacySQLTypeName.STRING));

    when(mockBigQueryService.executeQuery(any()))
        .thenReturn(BigQueryUtils.newTableResult(schema, List.of()));

    Map<String, Double> result = initialCreditsBigQueryService.getAllVWBProjectCostsFromBQ();

    assertTrue(result.isEmpty());
  }
}
