package org.pmiops.workbench.config;

import com.google.api.client.extensions.appengine.http.UrlFetchTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CommonConfig {

  @Bean
  JsonFactory jsonFactory() {
    return new JacksonFactory();
  }

  @Bean
  HttpTransport httpTransport() {
    return UrlFetchTransport.getDefaultInstance();
  }

  @Bean
  Clock clock() { return Clock.systemUTC(); }
}
