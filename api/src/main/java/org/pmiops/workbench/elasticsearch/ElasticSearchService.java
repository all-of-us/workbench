package org.pmiops.workbench.elasticsearch;

import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableResult;
import jnr.ffi.annotations.Synchronized;
import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.client.RequestOptions;
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
   */
  public Long elasticCount(org.pmiops.workbench.model.SearchRequest userRequest) {
    //For now if enableElasticsearchBackend is true
    //we get the cluster name, status and log it
    //then fallback to bigquery count.
    //TODO: Implement this method to call either getElasticCount()
    //or getBigQueryCount() based on certain condition(s) like -> if it's
    //temporal or not
    if (configProvider.get().elasticsearch.enableElasticsearchBackend) {
      try {
        ClusterHealthRequest healthRequest = new ClusterHealthRequest();
        ClusterHealthResponse response = client().cluster().health(healthRequest, RequestOptions.DEFAULT);

        log.info(String.format("Cluster Name: %s - Cluster Status: %s",
          response.getClusterName(), response.getStatus().name()));
      } catch (IOException e) {
        log.severe(e.getMessage());
      }
    }
    return getBigQueryCount(userRequest);
  }

  /**
   * Get count with Elasticsearch.
   * @param userRequest
   */
  private Long getElasticCount(org.pmiops.workbench.model.SearchRequest userRequest) {
    //TODO: implement this in a future story
    //Will probably use elasticsearch querybuilders
    //https://www.elastic.co/guide/en/elasticsearch/client/java-rest/current/java-rest-high-query-builders.html
    return 0L;
  }

  /**
   * Get the count with BigQuery.
   * @param userRequest
   */
  private Long getBigQueryCount(org.pmiops.workbench.model.SearchRequest userRequest) {
    QueryJobConfiguration qjc = bigQueryService.filterBigQueryConfig(
      participantCounter.buildParticipantCounterQuery(new ParticipantCriteria(userRequest))
    );
    TableResult result = bigQueryService.executeQuery(qjc);
    Map<String, Integer> rm = bigQueryService.getResultMapper(result);

    List<FieldValue> row = result.iterateAll().iterator().next();
    return bigQueryService.getLong(row, rm.get("count"));
  }

  /**
   * Implementing RestHighLevelClient init here because injecting
   * Provider<WorkbenchConfig> into a Configuration singleton class
   * was causing a BeanInstantiationException due to WorkbenchConfig
   * being request scoped. This works but need to add Synchronized
   * annotation to make this method thread safe.
   */
  @Synchronized
  private RestHighLevelClient client() {
    if (client == null) {
      String[] vars = configProvider.get().elasticsearch.host.split(":");
      String host = vars[0];
      int port = Integer.parseInt(vars[1]);
      client = new RestHighLevelClient(RestClient.builder(new HttpHost(host, port, "http")));
    }
    return client;
  }
}
