package org.pmiops.workbench.elasticsearch;

import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableResult;
import jnr.ffi.annotations.Synchronized;
import org.apache.http.HttpHost;
import org.elasticsearch.action.main.MainResponse;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.cohortbuilder.ParticipantCounter;
import org.pmiops.workbench.cohortbuilder.ParticipantCriteria;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.inject.Provider;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@Service
public class ElasticSearchService {

  private static final Logger log = Logger.getLogger(ElasticSearchService.class.getName());
  private RestHighLevelClient client;
  private Provider<WorkbenchConfig> configProvider;
  private BigQueryService bigQueryService;
  private ParticipantCounter participantCounter;

  @Autowired
  public ElasticSearchService(Provider<WorkbenchConfig> configProvider,
                              BigQueryService bigQueryService,
                              ParticipantCounter participantCounter) {
    this.configProvider = configProvider;
    this.bigQueryService = bigQueryService;
    this.participantCounter = participantCounter;
  }

  /**
   * This method will generate counts per the
   * org.pmiops.workbench.model.SearchRequest
   * using elasticsearch with BigQuery being
   * the fallback.
   *
   * @param userRequest
   * @return
   */
  public Long elasticCount(org.pmiops.workbench.model.SearchRequest userRequest) {
    //For now if enableElasticsearchBackend is true
    //we get the cluster name and log it then run the bigquery count.
    if (configProvider.get().elasticsearch.enableElasticsearchBackend) {
      try {
        MainResponse response = client().info();
        log.info("Cluser Name: " + response.getClusterName().toString());
        log.info("Cluster Uuid: " + response.getClusterUuid());
        log.info("Node Name: " + response.getNodeName());
        log.info("Version: " + response.getVersion().toString());
        log.info("Build: " + response.getBuild().toString());
      } catch (IOException e) {
        log.severe(e.getMessage());
      }
    }
    return getBigQueryCount(userRequest);
  }

  /**
   * Get count with Elasticsearch.
   * @param userRequest
   * @return
   */
  private Long getElasticCount(org.pmiops.workbench.model.SearchRequest userRequest) {
    //implement this in a future story
    return 0L;
  }

  /**
   * Get the count with BigQuery.
   * @param userRequest
   * @return
   */
  private Long getBigQueryCount(org.pmiops.workbench.model.SearchRequest userRequest) {
    QueryJobConfiguration qjc = bigQueryService.filterBigQueryConfig(participantCounter.buildParticipantCounterQuery(
      new ParticipantCriteria(userRequest)));
    TableResult result = bigQueryService.executeQuery(qjc);
    Map<String, Integer> rm = bigQueryService.getResultMapper(result);

    List<FieldValue> row = result.iterateAll().iterator().next();
    return bigQueryService.getLong(row, rm.get("count"));
  }

  @Synchronized
  private RestHighLevelClient client() {
    if (configProvider.get().elasticsearch.enableElasticsearchBackend && client == null) {
      String[] vars = configProvider.get().elasticsearch.host.split(":");
      String host = vars[0];
      int port = Integer.parseInt(vars[1]);
      client = new RestHighLevelClient(RestClient.builder(new HttpHost(host, port, "http")));
    }
    return client;
  }
}
