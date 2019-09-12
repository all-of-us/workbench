package org.pmiops.workbench.config;

import com.google.api.client.extensions.appengine.http.UrlFetchTransport;
import com.google.api.client.http.HttpTransport;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppEngineConfig {

  @Bean
  HttpTransport httpTransport() {
    return UrlFetchTransport.getDefaultInstance();
  }
}
