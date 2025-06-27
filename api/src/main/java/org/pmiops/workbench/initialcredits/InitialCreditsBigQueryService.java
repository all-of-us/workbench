package org.pmiops.workbench.initialcredits;

import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.QueryJobConfiguration;
import jakarta.inject.Provider;
import java.util.HashMap;
import java.util.Map;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.springframework.stereotype.Service;

@Service
public class InitialCreditsBigQueryService {

  private final BigQueryService bigQueryService;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;

  public InitialCreditsBigQueryService(
      BigQueryService bigQueryService, Provider<WorkbenchConfig> workbenchConfigProvider) {
    this.bigQueryService = bigQueryService;
    this.workbenchConfigProvider = workbenchConfigProvider;
  }

  Map<String, Double> getAllTerraWorkspaceCostsFromBQ() {
    final QueryJobConfiguration queryConfig =
        QueryJobConfiguration.newBuilder(
                "SELECT id, SUM(cost) cost FROM `"
                    + workbenchConfigProvider.get().billing.exportBigQueryTable
                    + "` WHERE id IS NOT NULL "
                    + "GROUP BY id ORDER BY cost desc;")
            .build();

    final Map<String, Double> liveCostByWorkspace = new HashMap<>();
    for (FieldValueList tableRow : bigQueryService.executeQuery(queryConfig).getValues()) {
      final String googleProject = tableRow.get("id").getStringValue();
      liveCostByWorkspace.put(googleProject, tableRow.get("cost").getDoubleValue());
    }

    return liveCostByWorkspace;
  }

  /**
   * Gets all VWB project costs from BigQuery, the method aggregates costs by vwb_pod_id.
   *
   * @return a map of vwb_pod_id to total cost.
   */
  Map<String, Double> getAllVWBProjectCostsFromBQ() {
    StringBuilder queryBuilder = new StringBuilder();
    queryBuilder
        .append("SELECT total_cost, vwb_pod_id ")
        .append("FROM `")
        .append(workbenchConfigProvider.get().billing.vwbExportBigQueryTable)
        .append("` ")
        .append("WHERE vwb_org_id = \"")
        .append(workbenchConfigProvider.get().vwb.organizationId)
        .append("\";");

    final QueryJobConfiguration queryConfig =
        QueryJobConfiguration.newBuilder(queryBuilder.toString()).build();

    // Group by the pod
    final Map<String, Double> costByVwbPodId = new HashMap<>();
    for (FieldValueList tableRow : bigQueryService.executeQuery(queryConfig).getValues()) {
      final String vwbPodId =
          tableRow.get("vwb_pod_id").isNull() ? null : tableRow.get("vwb_pod_id").getStringValue();
      final double totalCost = tableRow.get("total_cost").getDoubleValue();
      if (vwbPodId != null) {
        costByVwbPodId.merge(vwbPodId, totalCost, Double::sum);
      }
    }

    return costByVwbPodId;
  }
}
