package org.pmiops.workbench.elasticsearch;

import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.core.CountRequest;
import org.elasticsearch.index.query.AbstractQueryBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class ElasticSearchService {

  private static final String INDEX = "index";
  private static final String TYPE = "type";
  private RestHighLevelClient client;

  @Autowired
  public ElasticSearchService(RestHighLevelClient client) {
    this.client = client;
  }

  /**
   * This method will be used to generate counts from a
   * org.pmiops.workbench.model.SearchRequest.
   *
   * @param userRequest
   * @return
   */
  public Long elasticCount(org.pmiops.workbench.model.SearchRequest userRequest) throws IOException {
    CountRequest countRequest = new CountRequest()
      //What indices should we use here?
      .indices(INDEX, TYPE)
      .source(new SearchSourceBuilder().query(createQueryBuilder(userRequest)));
    return client.count(countRequest, RequestOptions.DEFAULT).getCount();
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
}
