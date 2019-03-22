package org.pmiops.workbench.elasticsearch;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;
import javax.inject.Provider;

import jnr.ffi.annotations.Synchronized;
import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.core.CountRequest;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.pmiops.workbench.cdr.CdrVersionContext;
import org.pmiops.workbench.cdr.dao.CriteriaDao;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.elasticsearch.ElasticFilters.ElasticFilterResponse;
import org.pmiops.workbench.model.DemoChartInfo;
import org.pmiops.workbench.model.SearchRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static org.pmiops.workbench.elasticsearch.AggregationUtils.buildDemoChartAggregation;
import static org.pmiops.workbench.elasticsearch.AggregationUtils.unwrapDemoChartBuckets;
import static org.pmiops.workbench.elasticsearch.AggregationUtils.RANGE_19_44;
import static org.pmiops.workbench.elasticsearch.AggregationUtils.RANGE_45_64;
import static org.pmiops.workbench.elasticsearch.AggregationUtils.RANGE_GT_65;

@Service
public class ElasticSearchService {

  private static final Logger log = Logger.getLogger(ElasticSearchService.class.getName());
  private RestHighLevelClient client;
  private CriteriaDao criteriaDao;
  private Provider<WorkbenchConfig> configProvider;

  @Autowired
  public ElasticSearchService(CriteriaDao criteriaDao, Provider<WorkbenchConfig> configProvider) {
    this.criteriaDao = criteriaDao;
    this.configProvider = configProvider;
  }

  /**
   * Get the total participant count matching the given search criteria.
   */
  public ElasticFilterResponse<Long> count(SearchRequest req) throws IOException {
    String personIndex =
        ElasticUtils.personIndexName(CdrVersionContext.getCdrVersion().getCdrDbName());
    ElasticFilterResponse<QueryBuilder> filter = ElasticFilters.fromCohortSearch(criteriaDao, req);
    log.info("Elastic filter: "  + filter.value().toString());
    long count = client().count(new CountRequest(personIndex)
        .source(SearchSourceBuilder.searchSource().query(filter.value())), RequestOptions.DEFAULT).getCount();
    return new ElasticFilterResponse<>(count, filter.isApproximate());
  }

  /**
   * Get the demographic data info for the given search criteria.
   */
  public ElasticFilterResponse<List<DemoChartInfo>> demoChartInfo(SearchRequest req) throws IOException {
    String personIndex =
      ElasticUtils.personIndexName(CdrVersionContext.getCdrVersion().getCdrDbName());
    ElasticFilterResponse<QueryBuilder> filter = ElasticFilters.fromCohortSearch(criteriaDao, req);
    log.info("Elastic filter: "  + filter.value().toString());
    SearchResponse searchResponse =
      client().search(new org.elasticsearch.action.search.SearchRequest(personIndex).source(
        SearchSourceBuilder
          .searchSource()
          .size(0)//reduce the payload since were only interested in the aggregations
          .query(filter.value())
          .aggregation(buildDemoChartAggregation(RANGE_19_44))
          .aggregation(buildDemoChartAggregation(RANGE_45_64))
          .aggregation(buildDemoChartAggregation(RANGE_GT_65))), RequestOptions.DEFAULT);
    return new ElasticFilterResponse<>(unwrapDemoChartBuckets(searchResponse, RANGE_19_44, RANGE_45_64, RANGE_GT_65), filter.isApproximate());
  }

  /**
   * Implementing RestHighLevelClient init here because injecting Provider<WorkbenchConfig> into a Configuration
   * singleton class was causing a BeanInstantiationException due to WorkbenchConfig being request scoped. This
   * works but need to add Synchronized annotation to make this method thread safe.
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
