package org.pmiops.workbench.config;

import static com.google.api.client.googleapis.util.Utils.getDefaultJsonFactory;

import com.google.api.client.json.JsonFactory;
import com.google.common.base.Stopwatch;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Random;
import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

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

  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  public Stopwatch getStopwatch() {
    return Stopwatch.createUnstarted();
  }
}
