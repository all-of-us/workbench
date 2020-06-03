package org.pmiops.workbench.config;

import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Random;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
public class CommonConfig {

  public static final String DATASET_PREFIX_CODE = "DATASET_PREFIX_CODE";

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

  @Bean
  @Qualifier(DATASET_PREFIX_CODE)
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  String randomCharacters() {
    return RandomStringUtils.randomNumeric(8);
  }
}
