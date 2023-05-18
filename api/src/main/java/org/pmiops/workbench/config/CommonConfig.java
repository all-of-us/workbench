package org.pmiops.workbench.config;

import static com.google.api.client.googleapis.util.Utils.getDefaultJsonFactory;

import com.google.api.client.json.JsonFactory;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Random;
import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CommonConfig {
  @Bean
  JsonFactory jsonFactory() {
    return getDefaultJsonFactory();
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
  Javers javers() {
    return JaversBuilder.javers().build();
  }
}
