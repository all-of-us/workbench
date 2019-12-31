package org.pmiops.workbench.config;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Random;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
public class CommonConfig {

  @Bean
  JsonFactory jsonFactory() {
    return new JacksonFactory();
  }

  @Bean
  Clock clock() {
    return Clock.systemUTC();
  }

  @Bean
  Random random() {
    return new SecureRandom();
  }
}
