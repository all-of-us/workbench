package org.pmiops.workbench.config;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.apache.ApacheHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by danrodney on 7/13/17.
 */
@Configuration
public class CommonConfig {

  @Bean
  JsonFactory jsonFactory() {
    return new JacksonFactory();
  }

  @Bean
  HttpTransport httpTransport() {
    return new ApacheHttpTransport();
  }
}
