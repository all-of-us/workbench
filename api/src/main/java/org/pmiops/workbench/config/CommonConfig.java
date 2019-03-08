package org.pmiops.workbench.config;

import com.google.api.client.extensions.appengine.http.UrlFetchTransport;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Random;
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

  @Bean
  Random random() { return new SecureRandom(); }
  
  @Bean
  GoogleCredential.Builder googleCredentialBuilder() { return new GoogleCredential.Builder(); }
}
