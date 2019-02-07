package org.pmiops.workbench.config;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.inject.Provider;

@Configuration
public class ElasticSearchConfig {

  @Autowired
  private Provider<WorkbenchConfig> configProvider;

  //Is Singleton scope appropriate here? or should we use another scope for elasticsearch?
  //If a scope beside Singleton is used we need to make sure we close this client when finished.
  @Bean
  public RestHighLevelClient client() {
    HttpHost httpHost = new HttpHost(
      configProvider.get().elasticsearch.hostname,
      configProvider.get().elasticsearch.port,
      configProvider.get().elasticsearch.schema
    );
    return new RestHighLevelClient(RestClient.builder(httpHost));
  }
}
