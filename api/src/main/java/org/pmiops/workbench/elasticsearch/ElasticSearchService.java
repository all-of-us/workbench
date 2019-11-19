package org.pmiops.workbench.elasticsearch;

import static org.pmiops.workbench.elasticsearch.AggregationUtils.RANGE_18_44;
import static org.pmiops.workbench.elasticsearch.AggregationUtils.RANGE_45_64;
import static org.pmiops.workbench.elasticsearch.AggregationUtils.RANGE_GT_65;
import static org.pmiops.workbench.elasticsearch.AggregationUtils.buildDemoChartAggregation;
import static org.pmiops.workbench.elasticsearch.AggregationUtils.unwrapDemoChartBuckets;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.logging.Logger;
import javax.inject.Provider;
import jnr.ffi.annotations.Synchronized;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.core.CountRequest;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.json.JSONObject;
import org.pmiops.workbench.cdr.CdrVersionContext;
import org.pmiops.workbench.cdr.dao.CBCriteriaDao;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.config.WorkbenchConfig.ElasticsearchConfig;
import org.pmiops.workbench.google.CloudStorageService;
import org.pmiops.workbench.model.DemoChartInfo;
import org.pmiops.workbench.model.SearchRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ElasticSearchService {

  private static final Logger log = Logger.getLogger(ElasticSearchService.class.getName());
  private RestHighLevelClient client;
  private CBCriteriaDao cbCriteriaDao;
  private CloudStorageService cloudStorageService;
  private Provider<WorkbenchConfig> configProvider;

  @Autowired
  public ElasticSearchService(
      CBCriteriaDao cbCriteriaDao,
      CloudStorageService cloudStorageService,
      Provider<WorkbenchConfig> configProvider) {
    this.cbCriteriaDao = cbCriteriaDao;
    this.cloudStorageService = cloudStorageService;
    this.configProvider = configProvider;
  }

  /** Get the total participant count matching the given search criteria. */
  public Long count(SearchRequest req) throws IOException {
    String personIndex =
        ElasticUtils.personIndexName(CdrVersionContext.getCdrVersion().getElasticIndexBaseName());
    QueryBuilder filter = ElasticFilters.fromCohortSearch(cbCriteriaDao, req);
    log.info("Elastic filter: " + filter.toString());
    long count =
        client()
            .count(
                new CountRequest(personIndex)
                    .source(SearchSourceBuilder.searchSource().query(filter)),
                RequestOptions.DEFAULT)
            .getCount();
    return count;
  }

  /** Get the demographic data info for the given search criteria. */
  public List<DemoChartInfo> demoChartInfo(SearchRequest req) throws IOException {
    String personIndex =
        ElasticUtils.personIndexName(CdrVersionContext.getCdrVersion().getElasticIndexBaseName());
    QueryBuilder filter = ElasticFilters.fromCohortSearch(cbCriteriaDao, req);
    log.info("Elastic filter: " + filter.toString());
    SearchResponse searchResponse =
        client()
            .search(
                new org.elasticsearch.action.search.SearchRequest(personIndex)
                    .source(
                        SearchSourceBuilder.searchSource()
                            .size(0) // reduce the payload since were only interested in the
                            // aggregations
                            .query(filter)
                            .aggregation(buildDemoChartAggregation(RANGE_18_44))
                            .aggregation(buildDemoChartAggregation(RANGE_45_64))
                            .aggregation(buildDemoChartAggregation(RANGE_GT_65))),
                RequestOptions.DEFAULT);
    return unwrapDemoChartBuckets(searchResponse, RANGE_18_44, RANGE_45_64, RANGE_GT_65);
  }

  /**
   * Implementing RestHighLevelClient init here because injecting Provider<WorkbenchConfig> into a
   * Configuration singleton class was causing a BeanInstantiationException due to WorkbenchConfig
   * being request scoped. This works but need to add Synchronized annotation to make this method
   * thread safe.
   */
  @Synchronized
  private RestHighLevelClient client() throws IOException {
    ElasticsearchConfig esConfig = configProvider.get().elasticsearch;
    if (client == null) {
      URL url = new URL(esConfig.baseUrl);
      RestClientBuilder builder =
          RestClient.builder(new HttpHost(url.getHost(), url.getPort(), url.getProtocol()));
      if (esConfig.enableBasicAuth) {
        JSONObject creds = cloudStorageService.getElasticCredentials();
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(
            AuthScope.ANY,
            new UsernamePasswordCredentials(
                creds.getString("username"), creds.getString("password")));
        builder.setHttpClientConfigCallback(
            (httpClientBuilder) ->
                httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));
      }
      client = new RestHighLevelClient(builder);
    }
    return client;
  }
}
