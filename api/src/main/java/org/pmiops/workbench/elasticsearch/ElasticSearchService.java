package org.pmiops.workbench.elasticsearch;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.AbstractQueryBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.inject.Provider;
import java.io.IOException;

@Service
public class ElasticSearchService {

  private static final String INDEX = "index";
  private static final String TYPE = "type";
  private RestHighLevelClient client;
  @Autowired
  private Provider<WorkbenchConfig> configProvider;

  /**
   * This method will be used to generate counts from a
   * org.pmiops.workbench.model.SearchRequest.
   *
   * @param userRequest
   * @return
   */
  public Long elasticCount(org.pmiops.workbench.model.SearchRequest userRequest) throws IOException {
//    CountRequest countRequest = new CountRequest()
//      .indices(INDEX, TYPE)
//      .source(new SearchSourceBuilder().query(createQueryBuilder(userRequest)));
//    return client().count(countRequest, RequestOptions.DEFAULT).getCount();
    return new Long(client().info().getVersion().id);
  }

  //Is it reasonable to user QueryBuilders for this? Or do we need something more flexible to
  //to accomplish all of our query needs?
  private AbstractQueryBuilder createQueryBuilder(org.pmiops.workbench.model.SearchRequest userRequest) {
    //Convert cohort definition into a QueryBuilder?
    //May need to generate different QueryBuilders depending on the cohort definition complexity???
    //Need to gather more knowledge about what needs to happen to get counts from elasticsearch

    //This is an example of what might get returned from this conversion??
    BoolQueryBuilder queryBuilder = new BoolQueryBuilder();
    queryBuilder.must(new TermQueryBuilder("concept_id", "1001"));
    queryBuilder.must(new TermQueryBuilder("age_at_event", "33"));
    return queryBuilder;
  }

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
