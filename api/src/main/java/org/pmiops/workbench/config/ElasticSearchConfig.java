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

  @Bean
  public RestHighLevelClient client() {
    String[] vars = configProvider.get().elasticsearch.host.split(":");
    String host = vars[0];
    int port = Integer.parseInt(vars[1]);
    String scheme = vars[2];
    return new RestHighLevelClient(RestClient.builder(new HttpHost(host, port, scheme)));
  }
}
